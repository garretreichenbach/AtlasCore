# Creating a Sub-Mod

This guide walks through building a third-party sub-mod that integrates with AtlasCore.

## 1. Gradle Setup

Add AtlasCore as a `compileOnly` dependency. It will not be bundled in your jar:

```groovy
// build.gradle
dependencies {
    compileOnly files("<starmade_root>/mods/AtlasCore-1.0.0.jar")
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
  "dependencies": [9999],
  "core_mod": false,
  "requires_class_resize": false,
  "hard_load_all_classes": false
}
```

The `"dependencies": [9999]` entry ensures AtlasCore is loaded first.

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

## 7. Key Bindings

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
