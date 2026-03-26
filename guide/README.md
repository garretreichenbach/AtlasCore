# AtlasGuide

In-game markdown guide viewer for the Atlas mod suite. Mods bundle `.md` files in their jar and AtlasGuide renders them using StarMade's native GUI system.

**Depends on:** AtlasCore

## Features

- Two-panel layout: searchable topic list on the left, rendered content on the right
- Both panels scroll independently
- Live search filtering — topic list narrows as you type
- Full markdown rendering: H1/H2/H3 headings, paragraphs, bullet and ordered lists, bold, italic, bold-italic, inline code, fenced code blocks, and horizontal rules
- Code blocks have a dark background with non-breaking-space whitespace preservation
- Content is lazily rendered per-topic selection, keeping the open cost low
- Opened from the top bar **GUIDE** button or `F5` key
- `/guide` command opens the dialog from chat

## Adding guide documents

AtlasGuide loads documents from two sources at `onClientCreated()` time:

| Source | Path | Who uses it |
|--------|------|-------------|
| Jar-bundled | `docs/` inside the mod jar | Mod developers |
| Filesystem | `moddata/AtlasGuide/docs/` | Server administrators |

Both sources are additive — filesystem documents are loaded after jar documents and appear in the same dialog. Filesystem loading allows server operators to add or edit documents (server rules, custom guides) without recompiling any jar. The directory is created automatically on first startup if it does not exist.

### Jar-bundled documents (developer workflow)

Documents are `.md` files placed in `guide/docs/markdown/`. At build time the Gradle tasks `syncDocumentationResources` and `generateDocumentationIndex` copy them into the jar under `docs/` and write a `docs.index` manifest.

1. Create a markdown file in `guide/docs/markdown/` (for jar-bundled docs) **or** drop a `.md` file into `moddata/AtlasGuide/docs/` on the server (for filesystem docs):

   ```markdown
   # My Feature

   Welcome to **My Feature**. Here's how it works...
   ```

2. The first `# Heading` becomes the display title in the topic list. If no heading is found, the filename is used.

3. For jar-bundled docs, run `gradle :guide:jar` — the file is automatically bundled and indexed. For filesystem docs, no build step is needed; restart the client to pick up changes.

## Third-party integration

Any mod can contribute documents to the Guide dialog without modifying AtlasGuide. The registry is additive — each call to `GuideManager.loadDocs(mod)` appends that mod's documents to the shared list.

### 1. Gradle dependency

```groovy
// build.gradle
dependencies {
    compileOnly files("<starmade_root>/mods/AtlasCore-1.0.0.jar")
    compileOnly files("<starmade_root>/mods/AtlasGuide-1.0.0.jar")
}
```

Declare both as `compileOnly` — neither jar is bundled in your output.

### 2. `mod.json`

```json
{
  "dependencies": [9999, 10000]
}
```

This ensures AtlasCore (9999) and AtlasGuide (10000) are loaded before your mod.

### 3. Bundle documents in your jar

Copy the Gradle tasks from `guide/build.gradle` into your own `build.gradle`, pointing at your own source directory:

```groovy
def docsSourceDir = layout.projectDirectory.dir('docs/markdown')
def docsResourceDir = layout.projectDirectory.dir('src/main/resources/docs')

tasks.register('syncDocumentationResources', Sync) {
    from(docsSourceDir)
    into(docsResourceDir)
    includeEmptyDirs = false
}

tasks.register('generateDocumentationIndex') {
    dependsOn('syncDocumentationResources')
    outputs.file(docsResourceDir.file('docs.index'))
    doLast {
        def dir = docsResourceDir.asFile
        dir.mkdirs()
        def files = fileTree(dir) { include '**/*.md' }.files.collect {
            dir.toPath().relativize(it.toPath()).toString().replace(File.separatorChar, '/' as char)
        }.sort()
        docsResourceDir.file('docs.index').asFile.text =
            files.isEmpty() ? '' : files.join(System.lineSeparator()) + System.lineSeparator()
    }
}

tasks.named('processResources') { dependsOn('generateDocumentationIndex') }
```

Place your `.md` files in `docs/markdown/`. At build time they are copied to `src/main/resources/docs/` and indexed.

### 4. Call `loadDocs` and optionally `loadDocsFromDirectory` at startup

```java
@Override
public void onClientCreated(ClientInitializeEvent event) {
    // Jar-bundled docs — uses this mod's own classloader
    GuideManager.loadDocs(this);

    // Optional: also load from moddata/MyMod/docs/ so server admins can add docs without recompiling
    File docsDir = new File(getSkeleton().getResourcesFolder(), "docs");
    GuideManager.loadDocsFromDirectory(docsDir, this);
}
```

`GuideManager.loadDocs` uses the calling mod's own classloader, so it reads resources from your jar, not AtlasGuide's. `loadDocsFromDirectory` scans a live filesystem path recursively — the directory is created automatically if absent. Both calls are additive; the Guide dialog shows all documents from all mods in registration order.

> **Title collisions** — if two mods register a document with the same `# Heading`, the later-loaded one wins. Use specific headings (e.g. `# MyMod — Getting Started`) to avoid conflicts.

## `GuideManager` API

```java
// Load documents from the calling mod's jar (call from onClientCreated)
GuideManager.loadDocs(StarMod mod);

// Load documents from a filesystem directory (recursive *.md scan, no docs.index needed)
GuideManager.loadDocsFromDirectory(File dir, StarMod mod);

// Get all registered titles (insertion order)
List<String> titles = GuideManager.getTitles();

// Get raw markdown for a title (empty string if not found)
String markdown = GuideManager.getRaw("My Feature");
```

Both loading methods are additive. AtlasGuide itself calls both in `onClientCreated`: first `loadDocs(this)` for jar-bundled content, then `loadDocsFromDirectory(new File(getSkeleton().getResourcesFolder(), "docs"), this)` for `moddata/AtlasGuide/docs/`.

## Tests

```
/run_tests atlas.guide.tests.*
```

Covers `getTitles()` non-null, unknown-title returns `""`, every registered title has non-empty content, and the returned list is unmodifiable.

## Build

```
gradle :guide:jar
```

Output: `{starmade_root}mods/AtlasGuide-{version}.jar`

The build tasks `syncDocumentationResources` → `generateDocumentationIndex` → `processResources` run automatically as part of `jar`.
