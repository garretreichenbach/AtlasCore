# AtlasExchange

Player-driven blueprint and item marketplace for the Atlas mod suite. Producers list ships, stations, weapons, and items for sale; buyers spend gold bars to purchase them.

**Depends on:** AtlasCore
**Optional integration:** AtlasBanking (gold bar crediting), AtlasBuildSectors (spawn-zone gating)

## Features

- Four listing categories: **Ships**, **Stations**, **Weapons**, **Items**
- Producers list their blueprints or items with a name, description, price, and classification
- Buyers spend **Gold Bars** (custom items registered by this module) to purchase listings
- Blueprint purchases use `RemoteBlueprintPlayerRequest` — the blueprint spawns directly at the buyer
- Item purchases dispatch a `GIVE_ITEM` server action
- Gold bar sales revenue is credited back to the producer via the `ADD_BARS` server action
- Full-text and category filtering in the exchange dialog
- Add-listing dialog with catalog picker, block-type picker, and weapon-subtype picker

## Custom items

AtlasExchange registers three currency items via `atlas.exchange.element.ElementRegistry`:

| Item | Texture | Description |
|------|---------|-------------|
| Bronze Bar | Element 341 | A rare bronze bar redeemable for prizes |
| Silver Bar | Element 342 | An esteemed silver bar redeemable for prizes |
| Gold Bar | Element 343 | An exquisite gold bar redeemable for prizes |

These are registered during `onBlockConfigLoad()`.

## Server-side actions

| Constant | Args | Effect |
|----------|------|--------|
| `AtlasExchange.GIVE_ITEM` | `playerName`, `itemId`, `count`, `meta` | Gives items directly to a player's inventory |
| `AtlasExchange.ADD_BARS` | `playerName`, `bronzeCount`, `silverCount`, `goldCount` | Credits prize bars to a player |

## Data model

### `ExchangeData`

| Field | Type | Notes |
|-------|------|-------|
| `name` | `String` | Display name of the listing |
| `catalogName` | `String` | Blueprint UID (ships/stations) |
| `description` | `String` | |
| `producer` | `String` | Faction/player UID of the seller |
| `price` | `int` | Cost in gold bars |
| `category` | `ExchangeDataCategory` | `SHIP`, `STATION`, `ITEM`, `WEAPON` |
| `classification` | `BlueprintClassification` | Ships/stations only |
| `mass` | `float` | Ships/stations only |
| `itemId` | `short` | Items/weapons only |
| `itemCount` | `int` | Items/weapons only |

## Tests

```
/run_tests atlas.exchange.tests.*
```

Covers category assignment, JSON round-trip, price, tab-index constants, and `setCategory(tabIndex)`.

## Build

```
gradle :exchange:jar
```

Output: `{starmade_root}mods/AtlasExchange-{version}.jar`
