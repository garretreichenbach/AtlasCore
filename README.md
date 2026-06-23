# Atlas

Atlas is a modular StarMade server mod system that replaces the monolithic EdenCore with a set of independent, loosely-coupled sub-mods. Each module can be installed on its own; only **AtlasCore** is required by all others.

## Modules

| Module | Jar | Description |
|--------|-----|-------------|
| [AtlasCore](core/) | `AtlasCore-x.x.x.jar` | Shared infrastructure — data types, networking, player actions, sub-mod registry |
| [AtlasBanking](banking/) | `AtlasBanking-x.x.x.jar` | Player credit accounts and transaction history |
| [AtlasBuildSectors](buildsectors/) | `AtlasBuildSectors-x.x.x.jar` | Protected build zones with per-entity permission control |
| [AtlasExchange](exchange/) | `AtlasExchange-x.x.x.jar` | Blueprint and item marketplace with gold bar currency |

## Installation

Drop any combination of jars into your StarMade `mods/` directory. AtlasCore must always be present.

```
StarMade/mods/
  AtlasCore-x.x.x.jar       ← required
  AtlasBanking-x.x.x.jar    ← optional
  AtlasBuildSectors-x.x.x.jar
  AtlasExchange-x.x.x.jar
```

## Building

### Prerequisites

Edit `gradle.properties` and set `starmade_root` to your local StarMade installation directory (trailing slash required).

```properties
starmade_root=C:/path/to/StarMade/
```

### Build all jars

```
gradle buildAll
```

Jars are written directly to `{starmade_root}/mods/`.

### Build a single module

```
gradle :core:jar
gradle :banking:jar
gradle :buildsectors:jar
gradle :exchange:jar
```

### Full build with tests (compile-time verification)

```
gradle build
```

## In-game tests

Test classes are bundled in each jar and registered at startup via `onRegisterTests()`. Run them from an admin account:

```
/run_tests *                        # all modules
/run_tests atlas.core.tests.*
/run_tests atlas.banking.tests.*
/run_tests atlas.exchange.tests.*
```