package atlas.core.data;

import api.network.PacketReadBuffer;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime registry for {@link SerializableData} types. Replaces the old hardcoded
 * {@code SerializableData.DataType} enum so sub-mods can register their own data
 * types without coupling to AtlasCore.
 *
 * <p>AtlasCore registers {@code "PLAYER_DATA"} on startup. Each sub-mod registers
 * its own types during {@code onAtlasCoreReady()}.
 */
public final class DataTypeRegistry {

    /**
     * Descriptor for a registered data type, providing deserialization and manager
     * lookup logic.
     */
    public interface Entry {
        String getName();
        SerializableData deserializeNetwork(PacketReadBuffer buf) throws IOException;
        SerializableData deserializeJSON(JSONObject obj);
        DataManager<?> getManager(boolean server);
    }

    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    private DataTypeRegistry() {}

    /** Registers a data type. Call from {@code onAtlasCoreReady()} or {@code onEnable()}. */
    public static void register(Entry entry) {
        entries.put(entry.getName(), entry);
    }

    /** Returns the entry for the given type name, or {@code null} if not registered. */
    public static Entry get(String name) {
        return entries.get(name);
    }

    public static Collection<Entry> all() {
        return Collections.unmodifiableCollection(entries.values());
    }
}
