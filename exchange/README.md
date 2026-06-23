# AtlasExchange

Player-driven blueprint and item marketplace for the Atlas mod suite. Producers list ships, stations, weapons, and items for sale; buyers spend gold bars to purchase them.

**Depends on:** AtlasCore
**Optional integration:** AtlasBanking (gold bar crediting), AtlasBuildSectors (spawn-zone gating)

## Features

- Four listing categories: **Ships**, **Stations**, **Weapons**, **Items**
- Producers list their blueprints or items with a name, description, price, and classification
- Buyers spend **Gold Bars** (StarMade's vanilla Gold Bar block) to purchase listings
- Blueprint purchases are handled server-side via `BUY_BLUEPRINT` / `BUY_DESIGN` actions — buyers receive an empty
  `BlueprintMetaItem` (manual construction) or a shipyard design item; no ship is ever spawned pre-filled
- **"Buy as Design"** button gives buyers a shipyard design item they can load directly into a shipyard
- All purchases are authoritative on the server: item, count, price, and seller come from the server's own listing (looked up by UUID), and the deduct/deliver/credit step is atomic with a refund on failure — clients cannot get free items or redirect payouts
- Item purchases dispatch a `BUY_ITEM` server action (by listing UUID)
- Listing creation/removal are authorised server-side (faction member or admin to list; producer or admin to remove)
- Daily login reward of 1–3 Gold Bars per player (interim Gold Bar source until a contract/mission system exists)
- Full-text and category filtering in the exchange dialog
- Add-listing dialog with catalog picker, block-type picker, and weapon-subtype picker

## Currency

The Exchange uses StarMade's existing **Gold Bar** block as its currency, accessed
via `api.utils.element.Blocks.GOLD_BAR`. AtlasExchange does not register any custom
items of its own. Players earn Gold Bars from the daily login reward (see Features);
all pricing, deduction, and seller payouts are denominated in Gold Bars.

## Server-side actions

All buy actions take only the listing UUID; the server derives price, item, and
seller from its own copy of the listing and resolves the buyer from the
authenticated `sender`.

| Constant | Args | Effect |
|----------|------|--------|
| `AtlasExchange.BUY_ITEM` | `listingUUID` | Buys an item/weapon listing: deducts Gold Bars, delivers the item, pays the seller |
| `AtlasExchange.BUY_BLUEPRINT` | `listingUUID` | Buys a blueprint listing: gives an empty `BlueprintMetaItem` |
| `AtlasExchange.BUY_DESIGN` | `listingUUID` | Buys a ship listing as a shipyard design item |
| `AtlasExchange.ADD_LISTING` | `serializedJSON` | Creates a listing (faction/admin only; producer forced to sender) |
| `AtlasExchange.REMOVE_LISTING` | `listingUUID` | Removes a listing (producer or admin only) |

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
