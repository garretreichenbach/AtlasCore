# Custom Data Types

AtlasCore's `DataTypeRegistry` replaces the old hardcoded `SerializableData.DataType` enum with a runtime registry. Any sub-mod can register its own data types.

## How Routing Works

When `SendDataPacket` arrives, it reads the `dataTypeName` string and calls:

```java
DataTypeRegistry.get(dataTypeName).getManager(isServer).updateClientCache(data);
```

When `SyncRequestPacket` arrives, it calls:

```java
DataTypeRegistry.get(dataTypeName).getManager(true).sendAllDataToPlayer(player);
```

## Registering a Type

Call `DataTypeRegistry.register(entry)` in your `onAtlasCoreReady()`:

```java
DataTypeRegistry.register(new DataTypeRegistry.Entry() {
    @Override public String getName() { return "WIDGET_DATA"; }

    @Override
    public SerializableData deserializeNetwork(PacketReadBuffer buf) throws IOException {
        return new WidgetData(buf);  // network constructor
    }

    @Override
    public SerializableData deserializeJSON(JSONObject obj) {
        return new WidgetData(obj);  // JSON constructor
    }

    @Override
    public DataManager<?> getManager(boolean server) {
        return WidgetDataManager.getInstance(server);
    }
});
```

## `SerializableData` Contract

Your data class must:

1. Call `super("WIDGET_DATA")` in every constructor.
2. Override `getDataTypeName()` to return the same string.
3. Implement `serialize()` / `deserialize(JSONObject)` for persistence.
4. Implement `serializeNetwork()` / `deserializeNetwork()` for packet transfer.

```java
public class WidgetData extends SerializableData {

    public WidgetData(String owner) {
        super("WIDGET_DATA");
        // ...
    }

    public WidgetData(PacketReadBuffer buf) throws IOException {
        super("WIDGET_DATA");
        deserializeNetwork(buf);
    }

    public WidgetData(JSONObject data) {
        super("WIDGET_DATA");
        deserialize(data);
    }

    @Override
    public String getDataTypeName() { return "WIDGET_DATA"; }

    // ... serialize / deserialize / serializeNetwork / deserializeNetwork
}
```

## Type Name Uniqueness

Type names must be globally unique across all loaded mods. Use a namespaced prefix to avoid collisions:

```
GOOD: "MYMOD_WIDGET_DATA"
BAD:  "DATA"
```
