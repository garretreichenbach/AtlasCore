# AtlasBuildSectors

Protected build zones for the Atlas mod suite. Allows server administrators to designate sectors where only permitted players and factions can interact with specific ships and stations.

**Depends on:** AtlasCore

## Features

- Per-sector entity registration (ships and stations that belong to the build zone)
- Per-entity permission table — granular control over who can interact with each entity
- Per-user permission overrides on top of global sector defaults
- In-game **Build Sectors** dialog accessible from the top bar or the **Open Build Sector Menu** key (default `B`, rebindable in the controls menu)
- Sector entry enforced server-side for every teleport type via `MixinSectorSwitch` (not just boundary crossings)
- HUD overlay drawn via `BuildSectorHudDrawer` (registered through `RegisterWorldDrawersEvent`)
- Automatic stale-entity pruning via `BuildSectorData.prune()`

## Permission types

Permissions are defined in `BuildSectorData.PermissionTypes` and can be toggled per-entity or globally for a user. Examples include spawn rights, AI control, turret management, and invulnerability overrides.

## Server-side actions

Entering/leaving, entity controls, and user/permission edits all go through string-keyed
`PlayerActionRegistry` handlers that authorise the acting player from the authenticated
`sender` before mutating and replicating server state — the client GUI never edits sector
data directly:

`atlas_buildsectors:enter`, `:leave`, `:toggle_ai`, `:set_invulnerable`, `:delete_entity`,
`:add_user`, `:remove_user`, `:set_permission`, `:set_entity_permission`.

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
