# AtlasBanking

Player banking system for the Atlas mod suite. Tracks per-player credit balances and transaction history.

**Depends on:** AtlasCore

## Features

- Per-player credit accounts stored as JSON and synchronised to clients on spawn
- Full transaction history (deposits, withdrawals, transfers) with timestamps
- Server-side `SET_CREDITS` action for admin commands / other mods
- In-game **Banking** dialog accessible from the top bar or `N` key

## Data model

### `BankingData`

| Field | Type | Description |
|-------|------|-------------|
| `playerName` | `String` | Owner of this account |
| `storedCredits` | `double` | Current balance |
| `transactionHistory` | `Set<BankTransactionData>` | All past transactions |

### `BankTransactionData`

| Field | Type |
|-------|------|
| `amount` | `double` |
| `fromUUID` | `String` |
| `toUUID` | `String` |
| `subject` | `String` |
| `message` | `String` |
| `timestamp` | `long` |
| `transactionType` | `DEPOSIT` / `WITHDRAW` / `TRANSFER` |

## Server-side actions

| Constant | Args | Effect |
|----------|------|--------|
| `AtlasBanking.SET_CREDITS` | `playerName`, `amount` | Sets a player's balance to `amount` |

```java
// From any server-side code:
PlayerActionRegistry.process(AtlasBanking.SET_CREDITS, new String[]{ "PlayerName", "5000.0" });
```

Or send from a client:
```java
new PlayerActionCommandPacket(AtlasBanking.SET_CREDITS, "PlayerName", "5000.0").sendToServer();
```

## Tests

```
/run_tests atlas.banking.tests.*
```

Covers `BankingData` JSON round-trip (name, credits, UUID), transaction serialization, and history mutation.

## Build

```
gradle :banking:jar
```

Output: `{starmade_root}mods/AtlasBanking-{version}.jar`
