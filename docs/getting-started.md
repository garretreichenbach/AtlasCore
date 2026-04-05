# Getting Started

## Requirements

- StarMade `0.205.4` or later
- Java 8
- A StarMade dedicated server or local install

## Installation

1. Download the mod jars from the [Releases](https://github.com/videogoose/AtlasCore/releases) page.
2. Copy **AtlasCore** into your `mods/` directory — this is **required** by all other Atlas mods.
3. Copy any sub-mod jars you want (`AtlasGuide`, `AtlasBanking`, etc.) into `mods/`.
4. Start your server.

StarMade's mod loader will automatically detect the dependency relationship and initialize AtlasCore before any sub-mods.

## Sub-Mod Dependencies

All Atlas sub-mods declare `AtlasCore` (`smd_resource_id: 8757`) as a dependency in their `mod.json`. You do **not** need to configure this manually — the loader handles it.

```json
{
  "dependencies": [8757]
}
```

## Configuration

AtlasCore creates a config file at `mods/AtlasCore/config.cfg` on first run. Key settings:

| Key | Default | Description |
|-----|---------|-------------|
| `debug_mode` | `false` | Enable verbose logging |
| `discord_invite_code` | `""` | Discord invite shown in top-bar DISCORD button |
| `tip_interval_seconds` | `300` | How often server tips broadcast (seconds) |
| `tips` | `""` | Semicolon-separated tip messages |

## Building from Source

```bash
git clone https://github.com/videogoose/AtlasCore.git
cd AtlasCore
./gradlew build
```

Jars are output to `<starmade_root>/mods/`. Set `starmade_root` in `gradle.properties`.
