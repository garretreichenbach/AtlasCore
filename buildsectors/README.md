# AtlasBuildSectors

Protected build zones for the Atlas mod suite. Allows server administrators to designate sectors where only permitted players and factions can interact with specific ships and stations.

**Depends on:** AtlasCore

## Features

- Per-sector entity registration (ships and stations that belong to the build zone)
- Per-entity permission table — granular control over who can interact with each entity
- Per-user permission overrides on top of global sector defaults
- In-game **Build Sectors** dialog accessible from the top bar
- HUD overlay drawn via `BuildSectorHudDrawer` (registered through `RegisterWorldDrawersEvent`)
- Automatic stale-entity pruning via `BuildSectorData.prune()`

## Permission types

Permissions are defined in `BuildSectorData.PermissionTypes` and can be toggled per-entity or globally for a user. Examples include spawn rights, AI control, turret management, and invulnerability overrides.

## Data model

### `BuildSectorData`

Represents one protected sector. Contains:

- Sector coordinates
- A map of `entityUID → BuildSectorEntityData`
- Per-user permission entries

### `BuildSectorEntityData`

Represents one registered entity within a sector:

- Entity UID and type (`SHIP` / `STATION`)
- Per-user permission map (`BuildSectorData.PermissionTypes → Boolean`)

## Optional cross-mod integration

Other Atlas modules can check build-sector membership without importing this module:

```java
if (SubModRegistry.isLoaded("atlas_buildsectors")) {
    try {
        Class<?> bsData = Class.forName("atlas.buildsectors.data.BuildSectorData");
        // call getActiveSectorForPlayer(playerState) via reflection
    } catch (Exception ignored) {}
}
```

AtlasCore's `ECCatalogScrollableListNew` and AtlasExchange's `ExchangeItemScrollableList` both use this pattern to gate blueprint spawning.

## Build

```
gradle :buildsectors:jar
```

Output: `{starmade_root}mods/AtlasBuildSectors-{version}.jar`
