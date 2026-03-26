# AtlasCore

Shared infrastructure for the Atlas mod system. Every other Atlas module depends on AtlasCore.

## Responsibilities

- **Sub-mod registry** — modules register themselves and receive lifecycle callbacks (`onAtlasCoreReady`, `registerTopBarButtons`, etc.)
- **Data type registry** — runtime registration of serializable data types without modifying core enums
- **Player action registry** — sub-mods register server-side action handlers and receive integer IDs to use with `PlayerActionCommandPacket`
- **Networking** — `SendDataPacket`, `SyncRequestPacket`, `PlayerActionCommandPacket`
- **Data managers** — base `DataManager<T>` for server/client data synchronisation
- **Element system** — `Item` and `ElementInterface` base classes for registering custom game elements
- **Config** — `ConfigManager` (tip interval, debug mode, MOTD, etc.)
- **GUI primitives** — `ECCatalogScrollableListNew`, `PlayerSearchableDropdownInput`, `ItemUtils`, `EntityUtils`
- **Control bindings** — `ControlBindingData` for key-bind registration

## Key APIs

### Registering a sub-mod

Implement `IAtlasSubMod` in your main mod class and call `SubModRegistry.register(this)` from `onEnable()`:

```java
public class MyMod extends StarMod implements IAtlasSubMod {
    @Override public void onEnable() { SubModRegistry.register(this); }
    @Override public String getModId() { return "my_mod"; }
    @Override public StarMod getMod() { return this; }

    @Override
    public void onAtlasCoreReady() {
        // safe to register data types, action handlers, etc.
    }
}
```

### Registering a server-side action

```java
// In onAtlasCoreReady():
public static int MY_ACTION = PlayerActionRegistry.register(args -> {
    String playerName = args[0];
    // handle action...
});

// From any client code:
new PlayerActionCommandPacket(MyMod.MY_ACTION, playerName, "extra", "args").sendToServer();
```

### Registering a data type

```java
DataTypeRegistry.register(new DataTypeRegistry.Entry() {
    @Override public String getName() { return "MY_DATA"; }
    @Override public SerializableData deserializeNetwork(PacketReadBuffer buf) throws IOException {
        return new MyData(buf);
    }
    @Override public SerializableData deserializeJSON(JSONObject obj) { return new MyData(obj); }
    @Override public DataManager<?> getManager(boolean server) { return MyDataManager.getInstance(server); }
});
```

### Checking if another module is loaded

```java
if (SubModRegistry.isLoaded("atlas_banking")) {
    // optional integration
}
```

## Tests

```
/run_tests atlas.core.tests.*
```

Covers `PlayerActionRegistry` (registration, invocation) and `DataTypeRegistry` (register/retrieve/all).

## Build

```
gradle :core:jar
```

Output: `{starmade_root}mods/AtlasCore-{version}.jar`
