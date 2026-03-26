package atlas.exchange.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.data.SerializableData;
import org.json.JSONObject;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;

import java.io.IOException;

/**
 * Data model for an item listed on the Atlas Exchange marketplace.
 *
 * @author TheDerpGamer
 */
public class ExchangeData extends SerializableData {

    public enum ExchangeDataCategory {
        SHIP, STATION, ITEM, WEAPON
    }

    // Tab index constants matching ExchangeDialog
    public static final int SHIPS    = 0;
    public static final int STATIONS = 1;
    public static final int WEAPONS  = 2;
    public static final int ITEMS    = 3;

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
        super("EXCHANGE_DATA");
    }

    public ExchangeData(String name, String catalogName, String description, String producer, int price,
                        ExchangeDataCategory category, BlueprintClassification classification, float mass) {
        super("EXCHANGE_DATA");
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
        super("EXCHANGE_DATA");
        assert category == ExchangeDataCategory.ITEM || category == ExchangeDataCategory.WEAPON
                : "ExchangeData of type ITEM or WEAPON must be used for itemId and itemCount";
        this.name = name;
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.category = category;
    }

    public ExchangeData(PacketReadBuffer readBuffer) throws IOException {
        super("EXCHANGE_DATA");
        deserializeNetwork(readBuffer);
    }

    public ExchangeData(JSONObject data) {
        super("EXCHANGE_DATA");
        deserialize(data);
    }

    @Override
    public String getDataTypeName() { return "EXCHANGE_DATA"; }

    public void setFromCatalogEntry(CatalogPermission permission) {
        name = permission.getUid();
        catalogName = permission.getUid();
        description = permission.description;
        producer = permission.ownerUID;
        mass = permission.mass;
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
        data.put("category", category != null ? category.name() : "SHIP");
        data.put("classification", classification != null ? classification.name() : "NONE");
        data.put("mass", mass);
        data.put("itemId", itemId);
        data.put("itemCount", itemCount);
        return data;
    }

    @Override
    public void deserialize(JSONObject data) {
        dataUUID = data.getString("uuid");
        name = data.getString("name");
        catalogName = data.getString("catalogName");
        description = data.getString("description");
        producer = data.getString("producer");
        price = data.getInt("price");
        category = ExchangeDataCategory.valueOf(data.getString("category"));
        String classStr = data.getString("classification");
        classification = classStr.equals("NONE") ? null : BlueprintClassification.valueOf(classStr);
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
        writeBuffer.writeString(category != null ? category.name() : "SHIP");
        writeBuffer.writeString(classification != null ? classification.name() : "NONE");
        writeBuffer.writeFloat(mass);
        writeBuffer.writeShort(itemId);
        writeBuffer.writeInt(itemCount);
    }

    @Override
    public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
        readBuffer.readByte(); // version
        dataUUID = readBuffer.readString();
        name = readBuffer.readString();
        catalogName = readBuffer.readString();
        description = readBuffer.readString();
        producer = readBuffer.readString();
        price = readBuffer.readInt();
        category = ExchangeDataCategory.valueOf(readBuffer.readString());
        String classStr = readBuffer.readString();
        classification = classStr.equals("NONE") ? null : BlueprintClassification.valueOf(classStr);
        mass = readBuffer.readFloat();
        itemId = readBuffer.readShort();
        itemCount = readBuffer.readInt();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCatalogName() { return catalogName; }
    public void setCatalogName(String catalogName) { this.catalogName = catalogName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public ExchangeDataCategory getCategory() { return category; }
    public void setCategory(ExchangeDataCategory category) { this.category = category; }

    public void setCategory(int tabIndex) {
        switch(tabIndex) {
            case SHIPS:    this.category = ExchangeDataCategory.SHIP;    break;
            case STATIONS: this.category = ExchangeDataCategory.STATION; break;
            case WEAPONS:  this.category = ExchangeDataCategory.WEAPON;  break;
            case ITEMS:    this.category = ExchangeDataCategory.ITEM;    break;
        }
    }

    public BlueprintClassification getClassification() { return classification; }
    public void setClassification(BlueprintClassification classification) { this.classification = classification; }

    public float getMass() { return mass; }
    public void setMass(float mass) { this.mass = mass; }

    public short getItemId() { return itemId; }
    public void setItemId(short itemId) { this.itemId = itemId; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public ElementInformation getItemInfo() { return ElementKeyMap.getInfo(itemId); }
}
