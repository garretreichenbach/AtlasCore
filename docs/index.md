# Atlas Mod Framework

**Atlas** is a modular StarMade server mod framework that replaces the monolithic EdenCore design with a clean, decoupled architecture. Servers can mix and match sub-mods to enable only the features they need.

## What is Atlas?

Atlas is split into one core mod and several independent feature mods:

| Mod | ID | Features |
|-----|----|----------|
| **AtlasCore** | `9999` | Shared infrastructure: data system, networking, element API, mixins |
| **AtlasGuide** | `10000` | Markdown-based in-game guide system |
| **AtlasBanking** | `10001` | Player banking, credit transfers, prize bars |
| **AtlasBuildSectors** | `10002` | Private creative build sectors per player |
| **AtlasExchange** | `10003` | Player trading and blueprint marketplace |

## Why Atlas?

- **Modular** — Install only what you need. Remove a sub-mod and the server keeps running cleanly.
- **Modern** — SpongePowered Mixins replace bytecode class overwrites. StarMade's native markdown renderer replaces the deprecated Glossar library.
- **Extensible** — Third-party mods can depend on AtlasCore and register their own data types, actions, and top-bar buttons.

## Quick Links

- [Getting Started](getting-started.md)
- [Creating a Sub-Mod](api/creating-a-submod.md)
- [AtlasCore API Reference](modules/atlas-core.md)
