package thederpgamer.edencore.data.exchangedata;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import thederpgamer.edencore.data.SerializableData;
import thederpgamer.edencore.gui.exchangemenu.ExchangeDialog;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ExchangeData extends SerializableData {

	public void setFromCatalogEntry(CatalogPermission permission) {
		name = permission.getUid();
		catalogName = permission.getUid();
		description = permission.description;
		producer = permission.ownerUID;
		mass = permission.mass;
		dataType = DataType.EXCHANGE_DATA;
	}

	public enum ExchangeDataCategory {
		SHIP,
		STATION,
		ITEM,
		WEAPON
	}

	private static final byte VERSION = 0;

	private String name = "";
	private String catalogName = "";
	private String description = "";
	private String producer = "";
	private int price;
	private ExchangeDataCategory category;
	private BlueprintClassification classification;
	private float mass;
	private short itemId;
	private int itemCount;

	public ExchangeData() {
		super(DataType.EXCHANGE_DATA);
	}

	public ExchangeData(String name, String catalogName, String description, String producer, int price, ExchangeDataCategory category, BlueprintClassification classification, float mass) {
		super(DataType.EXCHANGE_DATA);
		this.name = name;
		this.catalogName = catalogName;
		this.description = description;
		this.producer = producer;
		this.price = price;
		this.category = category;
		this.classification = classification;
		this.mass = mass;
	}

	public ExchangeData(String name, short itemId, int itemCount, ExchangeDataCategory category) {
		super(DataType.EXCHANGE_DATA);
		this.name = name;
		assert category == ExchangeDataCategory.ITEM || category == ExchangeDataCategory.WEAPON : new IllegalArgumentException("ExchangeData of type ITEM or WEAPON must be used for itemId and itemCount");
		this.itemId = itemId;
		this.itemCount = itemCount;
	}

	public ExchangeData(PacketReadBuffer readBuffer) throws IOException {
		deserializeNetwork(readBuffer);
		dataType = DataType.EXCHANGE_DATA;
	}

	public ExchangeData(JSONObject data) {
		deserialize(data);
		dataType = DataType.EXCHANGE_DATA;
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("uuid", getUUID());
		data.put("name", name);
		data.put("catalogName", catalogName);
		data.put("description", description);
		data.put("producer", producer);
		data.put("price", price);
		data.put("category", category.name());
		data.put("classification", classification.name());
		data.put("mass", mass);
		data.put("itemId", itemId);
		data.put("itemCount", itemCount);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		dataUUID = data.getString("uuid");
		name = data.getString("name");
		catalogName = data.getString("catalogName");
		description = data.getString("description");
		producer = data.getString("producer");
		price = data.getInt("price");
		category = ExchangeDataCategory.valueOf(data.getString("category"));
		classification = BlueprintClassification.valueOf(data.getString("classification"));
		mass = (float) data.getDouble("mass");
		itemId = (short) data.getInt("itemId");
		itemCount = data.getInt("itemCount");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(name);
		writeBuffer.writeString(catalogName);
		writeBuffer.writeString(description);
		writeBuffer.writeString(producer);
		writeBuffer.writeInt(price);
		writeBuffer.writeString(category.name());
		writeBuffer.writeString(classification.name());
		writeBuffer.writeFloat(mass);
		writeBuffer.writeShort(itemId);
		writeBuffer.writeInt(itemCount);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		name = readBuffer.readString();
		catalogName = readBuffer.readString();
		description = readBuffer.readString();
		producer = readBuffer.readString();
		price = readBuffer.readInt();
		category = ExchangeDataCategory.valueOf(readBuffer.readString());
		classification = BlueprintClassification.valueOf(readBuffer.readString());
		mass = readBuffer.readFloat();
		itemId = readBuffer.readShort();
		itemCount = readBuffer.readInt();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCatalogName() {
		return catalogName;
	}

	public void setCatalogName(String catalogName) {
		this.catalogName = catalogName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getProducer() {
		return producer;
	}

	public void setProducer(String producer) {
		this.producer = producer;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public ExchangeDataCategory getCategory() {
		return category;
	}

	public void setCategory(ExchangeDataCategory category) {
		this.category = category;
	}

	public void setCategory(int category) {
		switch(category) {
			case ExchangeDialog.SHIPS:
				this.category = ExchangeDataCategory.SHIP;
				break;
			case ExchangeDialog.STATIONS:
				this.category = ExchangeDataCategory.STATION;
				break;
			case ExchangeDialog.WEAPONS:
				this.category = ExchangeDataCategory.WEAPON;
				break;
			case ExchangeDialog.ITEMS:
				this.category = ExchangeDataCategory.ITEM;
				break;
		}
	}

	public BlueprintClassification getClassification() {
		return classification;
	}

	public void setClassification(BlueprintClassification classification) {
		this.classification = classification;
	}

	public float getMass() {
		return mass;
	}

	public void setMass(float mass) {
		this.mass = mass;
	}

	public short getItemId() {
		return itemId;
	}

	public void setItemId(short itemId) {
		this.itemId = itemId;
	}

	public int getItemCount() {
		return itemCount;
	}

	public void setItemCount(int itemCount) {
		this.itemCount = itemCount;
	}

	public ElementInformation getItemInfo() {
		return ElementKeyMap.getInfo(itemId);
	}
}
