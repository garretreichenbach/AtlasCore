# AtlasGuide

`smd_resource_id: 10000` · Depends on AtlasCore (`9999`)

AtlasGuide provides an in-game markdown guide dialog using StarMade's native GUI rendering system.

## Features

- Markdown documents rendered natively with StarMade's GUI (no external libraries)
- Scrollable two-panel layout: document list on the left, rich content on the right
- In-game `/guide` command and keybind (`GUIDE` top-bar button)
- Documents loaded from two sources: jar-bundled (developers) and live filesystem (server admins)
- Filesystem directory created automatically on first startup

## Adding Guide Documents

AtlasGuide loads from two sources in order, both additive:

| Source | Path | Notes |
|--------|------|-------|
| Jar-bundled | `docs/` inside the mod jar | Packaged at build time via Gradle |
| Filesystem | `moddata/AtlasGuide/docs/` | Editable at runtime; no build step |

### Jar-bundled (developer workflow)

Place `.md` files in `guide/docs/markdown/`. At build time, Gradle syncs them to `src/main/resources/docs/` and generates `docs.index`.

```
guide/
  docs/markdown/
    my-guide.md         ← your content
    server-rules.md
```

The first `# Heading` in each file becomes the document's title in the list.

### Filesystem (server admin workflow)

Drop any `.md` file into `moddata/AtlasGuide/docs/` (subdirectories are included). The folder is created automatically on first startup. No Gradle tasks or jar rebuild needed — restart the client to pick up changes.

```
StarMade/
  moddata/
    AtlasGuide/
      docs/
        server-rules.md     ← add, edit, or delete freely
        custom-guide.md
```

## Markdown Support

| Element | Syntax |
|---------|--------|
| Headings | `# H1`, `## H2`, `### H3` |
| Bold | `**text**` or `__text__` |
| Italic | `*text*` or `_text_` |
| Bold+Italic | `***text***` |
| Inline code | `` `code` `` |
| Code block | ` ```lang ` … ` ``` ` |
| Bullet list | `- item` or `* item` |
| Ordered list | `1. item` |
| Separator | `---` or `***` |

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/guide` | `/glossar`, `/glossary` | Open the guide dialog |

## Third-party integration

Any mod can contribute documents to the shared guide dialog. The registry is **additive** — each `GuideManager.loadDocs(mod)` call appends that mod's documents without clearing earlier entries.

### Dependencies

```groovy
// build.gradle
dependencies {
    compileOnly files("<starmade_root>/mods/AtlasCore-1.0.0.jar")
    compileOnly files("<starmade_root>/mods/AtlasGuide-1.0.0.jar")
}
```

```json
// mod.json
{ "dependencies": [9999, 10000] }
```

### Bundle docs in your jar

Copy the `syncDocumentationResources` and `generateDocumentationIndex` Gradle tasks from `guide/build.gradle` into your own build script. Place your `.md` files in `docs/markdown/` — they are automatically packaged and indexed at build time.

### Load at startup

```java
@Override
public void onClientCreated(ClientInitializeEvent event) {
    // Jar-bundled docs — uses this mod's own classloader
    GuideManager.loadDocs(this);

    // Optional: also expose a moddata folder so server admins can add docs without recompiling
    File docsDir = new File(getSkeleton().getResourcesFolder(), "docs");
    GuideManager.loadDocsFromDirectory(docsDir, this);
}
```

`loadDocs` uses the calling mod's classloader, finding resources in your jar. `loadDocsFromDirectory` scans any live filesystem path recursively — the directory is created automatically if absent, giving server operators an immediate place to put files.

> **Avoid title collisions.** If two mods register a document with the same `# Heading`, the later-loaded document wins. Prefix headings with your mod name (e.g. `# MyMod — Installation`) to stay unique.

See [Creating a Sub-Mod — AtlasGuide Integration](../api/creating-a-submod.md#atlasguide-integration) for a complete walkthrough.
