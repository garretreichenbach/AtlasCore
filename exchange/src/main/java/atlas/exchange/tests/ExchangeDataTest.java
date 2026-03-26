package atlas.exchange.tests;

import atlas.exchange.data.ExchangeData;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ExchangeData} construction and JSON serialization.
 * Run in-game via {@code /run_tests atlas.exchange.tests.*}.
 */
public class ExchangeDataTest {

	@Test
	public void testItemCategoryIsSet() {
		ExchangeData data = new ExchangeData("Iron Block", (short) 5, 64, ExchangeData.ExchangeDataCategory.ITEM);
		Assert.assertEquals(ExchangeData.ExchangeDataCategory.ITEM, data.getCategory());
	}

	@Test
	public void testWeaponCategoryIsSet() {
		ExchangeData data = new ExchangeData("Cannon", (short) 10, 1, ExchangeData.ExchangeDataCategory.WEAPON);
		Assert.assertEquals(ExchangeData.ExchangeDataCategory.WEAPON, data.getCategory());
	}

	@Test
	public void testItemSerializeRoundTrip() {
		ExchangeData original = new ExchangeData("Gold Bar", (short) 343, 10, ExchangeData.ExchangeDataCategory.ITEM);

		JSONObject json = original.serialize();
		ExchangeData restored = new ExchangeData(json);

		Assert.assertEquals("Name must survive round-trip", original.getName(), restored.getName());
		Assert.assertEquals("Category must survive round-trip", original.getCategory(), restored.getCategory());
		Assert.assertEquals("UUID must survive round-trip", original.getUUID(), restored.getUUID());
	}

	@Test
	public void testDataTypeName() {
		ExchangeData data = new ExchangeData();
		Assert.assertEquals("EXCHANGE_DATA", data.getDataTypeName());
	}

	@Test
	public void testSetAndGetPrice() {
		ExchangeData data = new ExchangeData("Bronze Bar", (short) 341, 5, ExchangeData.ExchangeDataCategory.ITEM);
		data.setPrice(250);
		Assert.assertEquals(250, data.getPrice());
	}

	@Test
	public void testShipCategoryTabConstant() {
		Assert.assertEquals("SHIPS constant must be 0", 0, ExchangeData.SHIPS);
	}

	@Test
	public void testStationCategoryTabConstant() {
		Assert.assertEquals("STATIONS constant must be 1", 1, ExchangeData.STATIONS);
	}

	@Test
	public void testSetCategoryFromTabIndex() {
		ExchangeData data = new ExchangeData();
		data.setCategory(ExchangeData.ITEMS);
		Assert.assertEquals(ExchangeData.ExchangeDataCategory.ITEM, data.getCategory());
	}
}
