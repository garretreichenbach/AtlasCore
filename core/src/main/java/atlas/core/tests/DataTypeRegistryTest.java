package atlas.core.tests;

import atlas.core.data.DataManager;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.SerializableData;
import api.network.PacketReadBuffer;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests for {@link DataTypeRegistry}.
 * Run in-game via {@code /run_tests atlas.core.tests.*}.
 */
public class DataTypeRegistryTest {

	/** Unique name to avoid colliding with real registered types. */
	private static final String TEST_TYPE = "__ATLAS_TEST_DATA_TYPE__";

	@Test
	public void testRegisterAndRetrieveByName() {
		DataTypeRegistry.register(mockEntry(TEST_TYPE + "_A"));
		Assert.assertNotNull("Registered type must be retrievable by name",
				DataTypeRegistry.get(TEST_TYPE + "_A"));
	}

	@Test
	public void testGetNonExistentTypeReturnsNull() {
		Assert.assertNull("Unknown type name must return null",
				DataTypeRegistry.get(TEST_TYPE + "_NONEXISTENT_XYZ"));
	}

	@Test
	public void testRetrievedEntryNameMatchesRegistered() {
		String typeName = TEST_TYPE + "_NAME_CHECK";
		DataTypeRegistry.register(mockEntry(typeName));
		DataTypeRegistry.Entry entry = DataTypeRegistry.get(typeName);
		Assert.assertNotNull(entry);
		Assert.assertEquals("Entry name must match what was registered", typeName, entry.getName());
	}

	@Test
	public void testAllContainsRegisteredType() {
		String typeName = TEST_TYPE + "_ALL_CHECK";
		DataTypeRegistry.register(mockEntry(typeName));
		boolean found = false;
		for(DataTypeRegistry.Entry e : DataTypeRegistry.all()) {
			if(typeName.equals(e.getName())) { found = true; break; }
		}
		Assert.assertTrue("all() must contain the registered entry", found);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static DataTypeRegistry.Entry mockEntry(String name) {
		return new DataTypeRegistry.Entry() {
			@Override public String getName() { return name; }
			@Override public SerializableData deserializeNetwork(PacketReadBuffer buf) { return null; }
			@Override public SerializableData deserializeJSON(JSONObject obj) { return null; }
			@Override public DataManager<?> getManager(boolean server) { return null; }
		};
	}
}
