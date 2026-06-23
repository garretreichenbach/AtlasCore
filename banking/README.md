# AtlasBanking

Player banking system for the Atlas mod suite. Tracks per-player credit balances and transaction history.

**Depends on:** AtlasCore

## Features

- Per-player credit accounts stored as JSON and synchronised to clients on spawn
- Full transaction history (deposits, withdrawals, transfers) with timestamps
- Server-authoritative deposit / withdraw / transfer actions (the client never mutates balances directly)
- Admin `SET_CREDITS` action for commands / other mods
- In-game **Banking** dialog accessible from the top bar or the **Open Banking Menu** key (default `N`, rebindable in the controls menu)

## Data model

### `BankingData`

| Field | Type | Description |
|-------|------|-------------|
| `playerName` | `String` | Owner of this account |
| `storedCredits` | `long` | Current balance (whole credits) |
| `transactionHistory` | `Set<BankTransactionData>` | All past transactions |

### `BankTransactionData`

| Field | Type |
|-------|------|
| `amount` | `long` |
| `fromUUID` | `String` |
| `toUUID` | `String` |
| `subject` | `String` |
| `message` | `String` |
| `timestamp` | `long` |
| `transactionType` | `DEPOSIT` / `WITHDRAW` / `TRANSFER` |

## Server-side actions

All are registered under stable string keys and resolve the acting player from the
authenticated `sender`, never from packet args.

| Constant | Args | Effect |
|----------|------|--------|
| `AtlasBanking.DEPOSIT` | `amount` | Moves `amount` from the sender's wallet into their bank |
| `AtlasBanking.WITHDRAW` | `amount` | Moves `amount` from the sender's bank into their wallet |
| `AtlasBanking.TRANSFER` | `targetName`, `amount`, `subject`, `message` | Transfers `amount` from the sender's bank to `targetName` |
| `AtlasBanking.SET_CREDITS` | `playerName`, `amount` | **Admin only.** Sets a player's balance to `amount` |

```java
// From client code — the server validates funds/identity and applies the change:
PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(AtlasBanking.DEPOSIT, "5000"));
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
