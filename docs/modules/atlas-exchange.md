# AtlasExchange

`smd_resource_id: 8759` · Depends on AtlasCore (`8757`)

AtlasExchange provides a player-driven marketplace for ships, stations, items, and weapons.

## Features

- Four-category marketplace: Ships, Stations, Items, Weapons
- Listings persist server-side and sync to all clients on login
- Add listings directly from your catalog or inventory
- Buy listings using in-game credits
- `EXCHANGE` top-bar button and `J` keybind

## Listing Types

| Category | Data | Notes |
|----------|------|-------|
| `SHIP` | Blueprint name, price, mass, classification | Transferred via catalog |
| `STATION` | Blueprint name, price, mass, classification | Transferred via catalog |
| `ITEM` | Item ID, count, price | Direct item transfer |
| `WEAPON` | Item ID, count, price | Direct item transfer |

## Data Fields

`ExchangeData` stores per-listing:

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Display name |
| `catalogName` | `String` | Blueprint UID (for ships/stations) |
| `description` | `String` | Seller description |
| `producer` | `String` | Owner UID |
| `price` | `int` | Credit price |
| `category` | `ExchangeDataCategory` | SHIP / STATION / ITEM / WEAPON |
| `classification` | `BlueprintClassification` | Blueprint class (ships/stations only) |
| `mass` | `float` | Entity mass (ships/stations only) |
| `itemId` | `short` | Element ID (items/weapons only) |
| `itemCount` | `int` | Item quantity (items/weapons only) |
