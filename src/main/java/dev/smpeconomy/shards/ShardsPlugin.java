package dev.smpeconomy.shards;

import dev.smpeconomy.shards.api.ShardsAPI;
import dev.smpeconomy.shards.command.ShardsAdminCommand;
import dev.smpeconomy.shards.command.ShardsCommand;
import dev.smpeconomy.shards.listener.ShardListeners;
import dev.smpeconomy.shards.papi.ShardsExpansion;
import dev.smpeconomy.shards.service.ShardsService;
import dev.smpeconomy.shards.storage.ShardStore;
import dev.smpeconomy.shards.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ShardsPlugin extends JavaPlugin {

    private ShardStore store;
    private ShardsService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Msg msg = new Msg(this);

        this.store = new ShardStore(getDataFolder());
        this.service = new ShardsService(store);
        ShardsAPI.init(service);

        // Handles /reload-style enables while players are already online
        for (Player online : Bukkit.getOnlinePlayers()) {
            service.loadPlayer(online.getUniqueId());
        }

        dev.smpeconomy.shards.util.EarnFeedback feedback =
                new dev.smpeconomy.shards.util.EarnFeedback(this);
        Bukkit.getPluginManager().registerEvents(new ShardListeners(this, service, msg, feedback), this);

        dev.smpeconomy.shards.gui.ShardShop shop =
                new dev.smpeconomy.shards.gui.ShardShop(this, service, msg, feedback);
        Bukkit.getPluginManager().registerEvents(shop, this);

        Objects.requireNonNull(getCommand("shards")).setExecutor(new ShardsCommand(service, msg));
        Objects.requireNonNull(getCommand("shardsadmin")).setExecutor(new ShardsAdminCommand(this, service, msg, shop, feedback));
        Objects.requireNonNull(getCommand("shardshop")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                shop.open(player);
            }
            return true;
        });

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShardsExpansion(service, getDescription().getVersion()).register();
            getLogger().info("PlaceholderAPI expansion registered (%shards_balance%).");
        }

        getLogger().info("Shards enabled — kill reward: "
                + getConfig().getLong("earn.kill.amount", 10) + " shards.");
    }

    @Override
    public void onDisable() {
        if (service != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                service.unloadPlayer(online.getUniqueId());
            }
        }
        if (store != null) {
            store.shutdown();
        }
    }
}
