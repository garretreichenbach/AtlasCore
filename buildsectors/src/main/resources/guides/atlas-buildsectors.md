# AtlasBuildSectors

## Overview

AtlasBuildSectors gives every player a private creative sector where they can
build freely without hostile NPCs, damage, or interference from other players.

## Opening the Menu

Click the **BUILD SECTOR** button in the top-right player menu, or press the
**Open Build Sector Menu** key (default **B**). You can rebind it under
*Options → Controls → MOD CONTROLS* in the in-game settings.

The menu lists every build sector you own or have been invited to. Click
**WARP** next to a sector to be teleported into it, and **LEAVE** to return to
where you were before.

## Features

### Private Space

Your build sector belongs to you. Other players cannot enter unless you invite
them — entry is enforced by the server for every kind of teleport, not just
flying across the boundary. NPCs and pirates do not spawn inside, and damage is
disabled.

### Entities

Ships and stations inside your sector are listed in the **ENTITIES** tab, where
(subject to permissions) you can warp to them, toggle their AI, toggle their
invulnerability, delete them, and edit per-entity permissions.

### Inviting & Removing Players

In the **PERMISSIONS** tab, use **ADD USER** to grant another player access, and
**REMOVE USER** to revoke it. Invites and permission changes are sent to the
server, which verifies your authority, saves them, and replicates them — so they
take effect for everyone and survive restarts.

### Permissions

Each user you add has an individual set of permissions:

| Permission | Description |
|---|---|
| **Edit Own / Other Ships** | Place and remove blocks on ships |
| **Spawn / Spawn Enemies** | Spawn blueprints (and hostile entities) |
| **Delete Own / Other Ships** | Delete entities |
| **Toggle AI** | Turn ship AI on and off |
| **Toggle Damage** | Toggle entity invulnerability |
| **Invite / Kick** | Add or remove other users |
| **Edit Permissions** | Change other users' permissions |

The sector owner always has full permissions and cannot be removed. Per-entity
permissions can override these for a specific ship or station.

## Leaving Your Sector

Click **LEAVE** in the menu to return to your last real-space position. If no
prior position is recorded, you are sent to the server's spawn sector.
