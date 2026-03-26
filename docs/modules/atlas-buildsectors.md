# AtlasBuildSectors

`smd_resource_id: 10002` · Depends on AtlasCore (`9999`)

AtlasBuildSectors gives each player a private creative sector far from the main universe, with a fine-grained permission system.

## Features

- One private sector per player, spawned at a random location `1,000,000` sectors from the origin
- Sector is locked (no entry/exit for others by default) and protected (no damage, no FP loss)
- Full per-user permission system with 18 permission types
- Per-entity permission overrides
- HUD overlay hides real coordinates while inside a build sector
- `BUILD SECTOR` top-bar button and `G` keybind

## Permissions

Global permissions (apply to the whole sector):

| Permission | Description |
|------------|-------------|
| `EDIT_OWN` | Edit ships you spawned |
| `EDIT_ANY` | Edit any ship in the sector |
| `SPAWN` | Spawn ships from catalog |
| `SPAWN_ENEMIES` | Spawn enemy (pirate faction) ships |
| `DELETE_OWN` | Delete ships you spawned |
| `DELETE_ANY` | Delete any ship |
| `TOGGLE_AI_OWN` / `TOGGLE_AI_ANY` | Toggle AI on own/any ships |
| `TOGGLE_DAMAGE_OWN` / `TOGGLE_DAMAGE_ANY` | Toggle damage on own/any ships |
| `INVITE` | Invite other players |
| `KICK` | Kick players from sector |
| `EDIT_PERMISSIONS` | Modify global permissions for users |

Entity-specific overrides:

| Permission | Description |
|------------|-------------|
| `EDIT_SPECIFIC` | Edit this entity only |
| `DELETE_SPECIFIC` | Delete this entity only |
| `TOGGLE_AI_SPECIFIC` | Toggle AI on this entity only |
| `TOGGLE_DAMAGE_SPECIFIC` | Toggle damage on this entity only |
| `EDIT_ENTITY_PERMISSIONS` | Modify this entity's permission list |

## Default Permission Sets

| Role | Granted |
|------|---------|
| `OWNER` | All permissions |
| `FRIEND` | Edit/spawn/delete own, edit any, invite; no kick, no delete any, no spawn enemies |
| `OTHER` | Edit own, spawn; no delete, no AI, no damage, no invite |
