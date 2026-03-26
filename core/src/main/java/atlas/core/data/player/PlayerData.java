package atlas.core.data.player;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.data.SerializableData;
import com.bulletphysics.linearmath.Transform;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerData extends SerializableData {

	private static final byte VERSION = 0;

	private String name = "";
	private int factionId;
	private Vector3i lastRealSector = new Vector3i();
	private Transform lastRealTransform = new Transform();
	private int pendingExchangeCredits;
	/**
	 * UID of the virtual blueprint entity sitting in this player's staging sector, or empty if none.
	 */
	private String pendingExchangeDesignUID = "";

	public PlayerData(String name, int factionId, Vector3i lastRealSector, Transform lastRealTransform) {
		super("PLAYER_DATA");
		this.name = name;
		this.factionId = factionId;
		this.lastRealSector.set(lastRealSector);
		this.lastRealTransform.set(lastRealTransform);
	}

	public PlayerData(PlayerState playerState) {
		this(playerState.getName(), playerState.getFactionId(), playerState.getCurrentSector(), new Transform());
	}

	public PlayerData(PacketReadBuffer readBuffer) throws IOException {
		deserializeNetwork(readBuffer);
		dataTypeName = "PLAYER_DATA";
	}

	public PlayerData(JSONObject data) {
		deserialize(data);
		dataTypeName = "PLAYER_DATA";
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("uuid", getUUID());
		data.put("name", name);
		data.put("factionId", factionId);
		JSONObject lastRealSectorData = new JSONObject();
		lastRealSectorData.put("x", lastRealSector.x);
		lastRealSectorData.put("y", lastRealSector.y);
		lastRealSectorData.put("z", lastRealSector.z);
		data.put("lastRealSector", lastRealSectorData);
		JSONObject lastRealTransformData = new JSONObject();
		JSONObject lastRealTransformOrigin = new JSONObject();
		lastRealTransformOrigin.put("x", lastRealTransform.origin.x);
		lastRealTransformOrigin.put("y", lastRealTransform.origin.y);
		lastRealTransformOrigin.put("z", lastRealTransform.origin.z);
		lastRealTransformData.put("origin", lastRealTransformOrigin);
		data.put("lastRealTransform", lastRealTransformData);
		data.put("pendingExchangeCredits", pendingExchangeCredits);
		data.put("pendingExchangeDesignUID", pendingExchangeDesignUID);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		dataUUID = data.getString("uuid");
		name = data.getString("name");
		factionId = data.getInt("factionId");
		JSONObject lastRealSectorData = data.getJSONObject("lastRealSector");
		lastRealSector.set(lastRealSectorData.getInt("x"), lastRealSectorData.getInt("y"), lastRealSectorData.getInt("z"));
		JSONObject lastRealTransformData = data.getJSONObject("lastRealTransform");
		JSONObject lastRealTransformOrigin = lastRealTransformData.getJSONObject("origin");
		lastRealTransform.setIdentity();
		lastRealTransform.origin.set((float) lastRealTransformOrigin.getDouble("x"), (float) lastRealTransformOrigin.getDouble("y"), (float) lastRealTransformOrigin.getDouble("z"));
		pendingExchangeCredits = data.optInt("pendingExchangeCredits", 0);
		pendingExchangeDesignUID = data.optString("pendingExchangeDesignUID", "");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(factionId);
		writeBuffer.writeInt(lastRealSector.x);
		writeBuffer.writeInt(lastRealSector.y);
		writeBuffer.writeInt(lastRealSector.z);
		writeBuffer.writeFloat(lastRealTransform.origin.x);
		writeBuffer.writeFloat(lastRealTransform.origin.y);
		writeBuffer.writeFloat(lastRealTransform.origin.z);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		name = readBuffer.readString();
		factionId = readBuffer.readInt();
		lastRealSector = new Vector3i();
		lastRealSector.set(readBuffer.readInt(), readBuffer.readInt(), readBuffer.readInt());
		lastRealTransform = new Transform();
		lastRealTransform.setIdentity();
		lastRealTransform.origin.set(readBuffer.readFloat(), readBuffer.readFloat(), readBuffer.readFloat());
	}

	@Override
	public String getDataTypeName() {
		return "PLAYER_DATA";
	}

	public String getName() {
		return name;
	}

	public int getFactionId() {
		PlayerState playerState = getPlayerState();
		if(playerState != null && factionId != playerState.getFactionId()) {
			factionId = playerState.getFactionId();
			PlayerDataManager.getInstance(playerState.isOnServer()).updateData(this, playerState.isOnServer());
		}
		return factionId;
	}

	public PlayerState getPlayerState() {
		return GameCommon.getPlayerFromName(name);
	}

	public Faction getFaction() {
		return GameCommon.getGameState().getFactionManager().getFaction(getFactionId());
	}

	public String getFactionName() {
		return getPlayerState().getFactionName();
	}

	public Vector3i getLastRealSector() {
		return lastRealSector;
	}

	public void setLastRealSector(Vector3i sector) {
		lastRealSector.set(sector);
		PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
	}

	public Transform getLastRealTransform() {
		return lastRealTransform;
	}

	public void setLastRealTransform(Transform transform) {
		lastRealTransform.set(transform);
		PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
	}

	/**
	 * Gold Bars owed from Exchange sales. Persisted to disk; not sent over the network.
	 */
	public int getPendingExchangeCredits() {
		return pendingExchangeCredits;
	}

	/**
	 * Sets pending Exchange credits without auto-saving; caller must call {@code updateData} explicitly.
	 */
	public void setPendingExchangeCredits(int amount) {
		pendingExchangeCredits = amount;
	}

	/**
	 * UID of the virtual blueprint entity sitting in this player's staging sector.
	 * Empty string when the player has no pending exchange design. Persisted to disk only.
	 */
	public String getPendingExchangeDesignUID() {
		return pendingExchangeDesignUID;
	}

	/**
	 * Sets the pending exchange design UID without auto-saving; caller must call {@code updateData} explicitly.
	 */
	public void setPendingExchangeDesignUID(String uid) {
		pendingExchangeDesignUID = (uid != null) ? uid : "";
	}
}
