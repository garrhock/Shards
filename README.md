# Shards

An inflation-resistant second currency for Paper Minecraft servers, earned only through PvP and used to price progression items that shouldn't be buyable with ordinary money.

![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/Paper%20API-1.21-blue)
![Build](https://img.shields.io/badge/build-Maven-red)

<!-- SCREENSHOT: the shard shop GUI. Save to docs/images/shard-shop.png and uncomment.
![Shard Shop](docs/images/shard-shop.png)
-->

## The problem it solves

On a long-running server, the primary currency inflates. Players accumulate faster than they spend, prices lose meaning, and anything priced in money eventually becomes free in real terms. That's fine for consumables and fatal for progression items — a spawner that's meant to take weeks to earn becomes trivial by month two.

Shards are a second currency deliberately insulated from that. They enter the economy through exactly two channels and no others:

1. **PvP kills** — the only gameplay source
2. **Admin grants** — `shardsadmin give` from console, the integration point for votes, crates, and events

Nothing else mints shards. Because supply is bounded by player activity rather than by time spent grinding, shard prices hold their meaning across a season, and progression items priced in shards stay gated.

## Design notes

**Collusion protection.** Any currency minted by player-versus-player interaction is vulnerable to kill-trading: two accounts, often one player with an alt, repeatedly killing each other to print currency from nothing. A configurable per-victim cooldown means killing the same player again pays nothing until it expires, so farming a cooperative partner yields no more than genuine combat.

**No stacking.** PvP kills are otherwise unrewarded on this server, so shard income can't be doubled up with another plugin's kill bounty.

**Buying is saving, not skipping.** Spawner *placement* remains gated behind a rank permission. Purchasing early banks the item; it doesn't bypass the progression ladder.

**Shared feel.** Earn feedback — action bar and sound — deliberately mirrors [CustomEconomy](https://github.com/garrhock/CustomEconomy), so both currencies read as parts of one system rather than two bolted-together plugins.

## Architecture

```
dev.smpeconomy.shards
├── api/        Public API for other plugins
├── command/    /shards, /shardshop, /shardsadmin
├── gui/        Shard shop inventory menu
├── listener/   PvP kill detection and earn hooks
├── papi/       PlaceholderAPI expansion
├── service/    Balance logic and the in-memory cache
├── storage/    SQLite persistence
└── util/       Message formatting and earn feedback
```

Balances are cached in memory while a player is online and mirrored to SQLite, so the hot path never touches disk. `ShardStore` runs every read and write on a single-threaded executor, which keeps the game loop unblocked and guarantees writes for a given player stay ordered. Persistence uses an upsert, so a player's row is created on first earn and updated thereafter.

## Requirements

- Java 21+
- Paper 1.21+
- Optional: PlaceholderAPI

## Building

```bash
mvn clean package
```

## Configuration

```yaml
earn:
  kill:
    amount: 10
    # Minutes before killing the SAME victim pays again.
    # Collusion-minting protection, not an activity throttle.
    same-victim-cooldown-minutes: 30

display:
  symbol: "⧫"

shop:
  title: "⧫ Shard Shop"
  rows: 3
  entries:
    zombie:
      slot: 13
      icon: SPAWNER
      name: "<green>Zombie Spawner</green>"
      cost: 400
      commands:
        - "silkspawners give %player% ZOMBIE 1"
```

Shop entries run configured console commands with `%player%` substituted, so the shop can sell anything another plugin can grant — no code changes needed to add stock.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/shards` | View your shard balance | `shards.use` |
| `/shardshop` | Open the shard shop | `shards.use` |
| `/shardsadmin <give\|take\|set\|reload> [player] [amount]` | Administration | `shards.admin` |

## Placeholders

With PlaceholderAPI installed:

| Placeholder | Returns |
| --- | --- |
| `%shards_balance%` | The player's current shard balance |

## Author

Garrett Hockersmith - [LinkedIn](https://www.linkedin.com/in/garrett-hockersmith/) - [texgeh@gmail.com](mailto:texgeh@gmail.com)

## License

Released under the [MIT License](LICENSE). Copyright 2026 Garrett Hockersmith.
