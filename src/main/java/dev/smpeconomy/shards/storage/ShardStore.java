package dev.smpeconomy.shards.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongConsumer;

/**
 * SQLite persistence behind a single-thread executor so writes stay ordered.
 * The in-memory cache in ShardsService is the source of truth while a player
 * is online; this store only loads on join and mirrors changes.
 */
public final class ShardStore {

    private final HikariDataSource pool;
    private final ExecutorService dbThread =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "shards-db"));

    public ShardStore(File dataFolder) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "shards.db").getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setPoolName("shards-sqlite");
        this.pool = new HikariDataSource(config);
        try (Connection c = pool.getConnection();
             PreparedStatement st = c.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS shards (uuid TEXT PRIMARY KEY, balance INTEGER NOT NULL)")) {
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize shards.db", e);
        }
    }

    public void loadAsync(UUID uuid, LongConsumer onLoaded) {
        dbThread.execute(() -> {
            long balance = 0L;
            try (Connection c = pool.getConnection();
                 PreparedStatement st = c.prepareStatement("SELECT balance FROM shards WHERE uuid = ?")) {
                st.setString(1, uuid.toString());
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getLong(1);
                    }
                }
            } catch (SQLException ignored) {
                // fall through with 0; the upsert on first earn recreates the row
            }
            onLoaded.accept(balance);
        });
    }

    public void saveAsync(UUID uuid, long balance) {
        dbThread.execute(() -> {
            try (Connection c = pool.getConnection();
                 PreparedStatement st = c.prepareStatement(
                         "INSERT INTO shards (uuid, balance) VALUES (?, ?) "
                                 + "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance")) {
                st.setString(1, uuid.toString());
                st.setLong(2, balance);
                st.executeUpdate();
            } catch (SQLException ignored) {
            }
        });
    }

    public void shutdown() {
        dbThread.shutdown();
        pool.close();
    }
}
