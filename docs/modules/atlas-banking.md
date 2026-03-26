# AtlasBanking

`smd_resource_id: 10001` · Depends on AtlasCore (`9999`)

AtlasBanking provides a player banking system separate from StarMade's built-in credit wallet, plus prize bar collectible items.

## Features

- Per-player stored credit balance (persisted separately from wallet credits)
- Deposit, withdraw, and send credits to other players
- Full transaction history with subject and message fields
- Three prize bar items: Bronze, Silver, and Gold (redeemable collectibles)
- `BANKING` top-bar button and `N` keybind

## Data

`BankingData` stores per-player:

| Field | Type | Description |
|-------|------|-------------|
| `playerName` | `String` | Player identifier |
| `storedCredits` | `double` | Bank balance |
| `transactionHistory` | `Set<BankTransactionData>` | All past transactions |

## Server Actions

| Action | ID | Arguments | Description |
|--------|----|-----------|-------------|
| `SET_CREDITS` | dynamic | `playerName, amount` | Set a player's stored credits |
| `ADD_BARS` | dynamic | (TBD) | Grant prize bars to a player |

## Prize Bars

Bronze, Silver, and Gold bars are non-stackable collectible items registered via AtlasCore's element system. They are not shoppable, not in recipes, and not placeable.
