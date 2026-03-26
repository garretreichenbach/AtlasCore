# AtlasCore

`smd_resource_id: 9999` · Required by all sub-mods

AtlasCore is the shared infrastructure layer. It provides the data system, networking, sub-mod API, element registration, and Mixin support.

## Sub-Mod API

### `IAtlasSubMod`

Implement this interface in your sub-mod's main class:

```java
public class MyMod extends StarMod implements IAtlasSubMod {

    @Override
    public String getModId() { return "my_mod"; }

    @Override
    public StarMod getMod() { return this; }

    @Override
    public void onEnable() {
        SubModRegistry.register(this);
    }

    @Override
    public void onAtlasCoreReady() {
        // Register data types, actions, commands here
    }

    // Optional callbacks:
    @Override
    public void registerTopBarButtons(GUITopBar.ExpandedButton dropdown) { ... }

    @Override
    public void onPlayerSpawn(PlayerSpawnEvent event) { ... }

    @Override
    public void onPlayerJoinWorld(PlayerJoinWorldEvent event) { ... }

    @Override
    public void onKeyPress(String bindingName) { ... }
}
```

### `SubModRegistry`

```java
// Register your sub-mod (call in onEnable)
SubModRegistry.register(this);

// Check if a sub-mod is loaded (e.g., for optional integration)
if (SubModRegistry.isLoaded("atlas_banking")) { ... }
```

### `DataTypeRegistry`

Register a custom `SerializableData` type so AtlasCore's packet system can route it:

```java
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

### `PlayerActionRegistry`

Register server-side action handlers triggered by `PlayerActionCommandPacket`:

```java
public static int MY_ACTION;

MY_ACTION = PlayerActionRegistry.register(args -> {
    // args[0], args[1], ... as passed by the packet
});
```

## Data System

### `SerializableData`

All persistent data extends `SerializableData`:

```java
public class MyData extends SerializableData {

    public MyData(String playerName) {
        super("MY_DATA");  // dataTypeName passed to super
    }

    @Override public String getDataTypeName() { return "MY_DATA"; }

    @Override public JSONObject serialize() { ... }
    @Override public void deserialize(JSONObject data) { ... }
    @Override public void serializeNetwork(PacketWriteBuffer buf) throws IOException { ... }
    @Override public void deserializeNetwork(PacketReadBuffer buf) throws IOException { ... }
}
```

### `DataManager<E>`

Extend `DataManager` to handle caching and syncing:

```java
public class MyDataManager extends DataManager<MyData> {
    @Override public String getDataTypeName() { return "MY_DATA"; }
    @Override public Set<MyData> getServerCache() { ... }
    @Override public Set<MyData> getClientCache() { ... }
    @Override public void addToClientCache(MyData data) { ... }
    @Override public void removeFromClientCache(MyData data) { ... }
    @Override public void updateClientCache(MyData data) { ... }
    @Override public void createMissingData(Object... args) { ... }
}
```

## Mixins

AtlasCore hosts all game-class Mixins in `atlas.core.mixin`, registered in `atlascore.mixins.json`. See [Mixins](../api/mixins.md) for details.
