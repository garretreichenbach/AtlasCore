# AtlasCore

## Overview

AtlasCore is the shared infrastructure layer that every other Atlas module depends on.
It provides no gameplay features of its own, but must be installed for any Atlas mod to work.

## What It Provides

- **Data persistence** — JSON-backed player and server data that survives restarts
- **Network layer** — `PlayerActionCommandPacket` for secure server-side action dispatch
- **Sub-mod API** — the `IAtlasSubMod` interface that all Atlas modules implement
- **Player data** — shared `PlayerData` record tracking faction, position, and exchange state

## Installation

Place `AtlasCore.jar` in your server's `mods/` folder. All other Atlas modules require it.
It does not add any buttons, commands, or GUI of its own.

## For Developers

Third-party mods can depend on AtlasCore to access its APIs:

- Implement `IAtlasSubMod` and register with `SubModRegistry`
- Use `PlayerDataManager` to read and write persistent player records
- Use `PlayerActionRegistry` to register server-side action handlers
- Call `GuideManager.loadDocs(this)` from `onClientCreated()` to add docs to this guide

---

> AtlasCore is required. Do not remove it while any Atlas module is installed.
