# Creating a Sub-Mod

This guide walks through building a third-party sub-mod that integrates with AtlasCore.

## 1. Gradle Setup

Add AtlasCore as a `compileOnly` dependency. It will not be bundled in your jar:

```groovy
// build.gradle
dependencies {
    compileOnly files("<starmade_root>/mods/AtlasCore-x.x.x.jar")
    // or if in the same multi-project:
    compileOnly project(':core')
}
```

## 2. `mod.json`

```json
{
  "name": "MyMod",
  "author": "YourName",
  "version": "1.0.0",
  "starmade_version": "0.205.4",
  "client_mod": true,
  "server_mod": true,
  "main_class": "com.example.mymod.MyMod",
  "smd_resource_id": 20000,
  "dependencies": [8757],
  "core_mod": false,
  "requires_class_resize": false,
  "hard_load_all_classes": false
}
```

The `"dependencies": [8757]` entry ensures AtlasCore is loaded first.

## 3. Main Class

```java
public class MyMod extends StarMod implements IAtlasSubMod {

    private static MyMod instance;

    public MyMod() { instance = this; }
    public static MyMod getInstance() { return instance; }

    @Override
    public void onEnable() {
        SubModRegistry.register(this);  // Must be called in onEnable
    }

    @Override
    public String getModId() { return "my_mod"; }

    @Override
    public StarMod getMod() { return this; }

    @Override
    public void onAtlasCoreReady() {
        // Called after all mods have enabled and AtlasCore has finished setup.
        // Register data types, player actions, and commands here.
        registerMyDataType();
    }
}
```

## 4. Custom Data Type

```java
// In onAtlasCoreReady():
DataTypeRegistry.register(new DataTypeRegistry.Entry() {
    @Override public String getName() { return "MY_DATA"; }

    @Override
    public SerializableData deserializeNetwork(PacketReadBuffer buf) throws IOException {
        return new MyData(buf);
    }

    @Override
    public SerializableData deserializeJSON(JSONObject obj) {
        return new MyData(obj);
    }

    @Override
    public DataManager<?> getManager(boolean server) {
        return MyDataManager.getInstance(server);
    }
});
```

## 5. Top-Bar Button

```java
@Override
public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
    playerDropdown.addExpandedButton("MY MENU", new GUICallback() {
        @Override
        public void callback(GUIElement e, MouseEvent event) {
            if (event.pressedLeftMouse()) openMyMenu();
        }
        @Override public boolean isOccluded() { return false; }
    }, new GUIActivationHighlightCallback() {
        @Override public boolean isHighlighted(InputState s) { return false; }
        @Override public boolean isVisible(InputState s) { return true; }
        @Override public boolean isActive(InputState s) { return true; }
    });
}
```

## 6. Player Events

```java
@Override
public void onPlayerSpawn(PlayerSpawnEvent event) {
    if (event.getPlayer().isOnServer()) {
        MyDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
    }
}

@Override
public void onPlayerJoinWorld(PlayerJoinWorldEvent event) {
    MyDataManager.getInstance(true).createMissingData(event.getPlayerState().getName());
}
```

## 7. AtlasGuide Integration

If AtlasGuide is installed, your mod can add documents to the shared in-game guide. This is entirely optional — AtlasGuide is not required to use AtlasCore.

### Dependencies

Add AtlasGuide as a `compileOnly` dependency and declare it in `mod.json`:

```groovy
// build.gradle
dependencies {
    compileOnly files("<starmade_root>/mods/AtlasCore-x.x.x.jar")
    compileOnly files("<starmade_root>/mods/AtlasGuide-x.x.x.jar")
}
```

```json
// mod.json — load AtlasCore (8757) and AtlasGuide (8758) before this mod
{ "dependencies": [8757, 8758] }
```

### Gradle build tasks

Copy the following tasks into your `build.gradle`. They sync your `.md` sources into `src/main/resources/docs/` and generate the `docs.index` manifest that `GuideManager` reads at runtime:

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

Place your `.md` files in `docs/markdown/` — the first `# Heading` becomes the document's display title.

### Loading documents at runtime

```java
@Override
public void onClientCreated(ClientInitializeEvent event) {
    // Guard so startup doesn't fail if AtlasGuide is absent
    if (SubModRegistry.isLoaded("atlas_guide")) {
        // Jar-bundled docs (requires docs/docs.index in your jar)
        GuideManager.loadDocs(this);

        // Optional: filesystem docs from moddata/MyMod/docs/
        // The directory is created automatically; server admins can drop .md files there
        File docsDir = new File(getSkeleton().getResourcesFolder(), "docs");
        GuideManager.loadDocsFromDirectory(docsDir, this);
    }
}
```

`loadDocs` uses your mod's classloader to find resources in your jar. `loadDocsFromDirectory` scans a live filesystem directory recursively — no `docs.index` is required. Both methods append to the shared registry; the Guide dialog shows entries from all mods in load order.

---

## 8. Key Bindings

```java
// In onClientCreated:
ControlBindingData.load(this);
ControlBindingData.registerBinding(this, "Open My Menu", "Opens my menu.", 77 /* M */);

// In onKeyPress:
@Override
public void onKeyPress(String bindingName) {
    if ("Open My Menu".equals(bindingName)) openMyMenu();
}
```
