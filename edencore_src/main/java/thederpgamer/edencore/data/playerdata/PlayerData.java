package thederpgamer.edencore.data.playerdata;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import com.bulletphysics.linearmath.Transform;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.edencore.data.SerializableData;
import thederpgamer.edencore.utils.DateUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerData extends SerializableData {

	private static final byte VERSION = 0;

	private String name = "";
	private int factionId;
	private long storedCredits;
	private Set<PlayerBankTransactionData> transactionHistory = new HashSet<>();
	private Vector3i lastRealSector = new Vector3i();
	private Transform lastRealTransform = new Transform();
	
	public PlayerData(String name, int factionId, Vector3i lastRealSector, Transform lastRealTransform) {
		super(DataType.PLAYER_DATA);
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
		dataType = DataType.PLAYER_DATA;
	}

	public PlayerData(JSONObject data) {
		deserialize(data);
		dataType = DataType.PLAYER_DATA;
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("uuid", getUUID());
		data.put("name", name);
		data.put("factionId", factionId);
		data.put("storedCredits", storedCredits);
		JSONArray transactionArray = new JSONArray();
		for(PlayerBankTransactionData transaction : transactionHistory) transactionArray.put(transaction.serialize());
		data.put("transactionHistory", transactionArray);
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
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		dataUUID = data.getString("uuid");
		name = data.getString("name");
		factionId = data.getInt("factionId");
		storedCredits = data.getLong("storedCredits");
		transactionHistory.clear();
		JSONArray transactionArray = data.getJSONArray("transactionHistory");
		for(int i = 0; i < transactionArray.length(); i ++) transactionHistory.add(new PlayerBankTransactionData(transactionArray.getJSONObject(i)));
		JSONObject lastRealSectorData = data.getJSONObject("lastRealSector");
		lastRealSector.set(lastRealSectorData.getInt("x"), lastRealSectorData.getInt("y"), lastRealSectorData.getInt("z"));
		JSONObject lastRealTransformData = data.getJSONObject("lastRealTransform");
		JSONObject lastRealTransformOrigin = lastRealTransformData.getJSONObject("origin");
		lastRealTransform.setIdentity();
		lastRealTransform.origin.set((float) lastRealTransformOrigin.getDouble("x"), (float) lastRealTransformOrigin.getDouble("y"), (float) lastRealTransformOrigin.getDouble("z"));
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(factionId);
		writeBuffer.writeLong(storedCredits);
		writeBuffer.writeInt(transactionHistory.size());
		for(PlayerBankTransactionData transaction : transactionHistory) transaction.serializeNetwork(writeBuffer);
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
		storedCredits = readBuffer.readLong();
		int transactionCount = readBuffer.readInt();
		transactionHistory = new HashSet<>();
		for(int i = 0; i < transactionCount; i ++) transactionHistory.add(new PlayerBankTransactionData(readBuffer));
		lastRealSector = new Vector3i();
		lastRealSector.set(readBuffer.readInt(), readBuffer.readInt(), readBuffer.readInt());
		lastRealTransform = new Transform();
		lastRealTransform.setIdentity();
		lastRealTransform.origin.set(readBuffer.readFloat(), readBuffer.readFloat(), readBuffer.readFloat());
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
	
	public long getStoredCredits() {
		return storedCredits;
	}
	
	public void setStoredCredits(long credits) {
		storedCredits = credits;
		PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
	}
	
	public Set<PlayerBankTransactionData> getTransactionHistory() {
		return Collections.unmodifiableSet(transactionHistory);
	}
	
	public void addTransaction(PlayerBankTransactionData transaction) {
		transactionHistory.add(transaction);
		PlayerDataManager.getInstance(getPlayerState().isOnServer()).updateData(this, getPlayerState().isOnServer());
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
	
	public static class PlayerBankTransactionData extends SerializableData {

		public enum TransactionType {
			DEPOSIT,
			WITHDRAW,
			TRANSFER
		}
		
		private static final byte VERSION = 0;
		private long transactionAmount;
		private String fromUUID;
		private String toUUID;
		private String subject;
		private String message;
		private long time;
		private TransactionType transactionType;
		
		public PlayerBankTransactionData(long transactionAmount, PlayerData from, PlayerData to, String subject, String message, TransactionType transactionType) {
			super(DataType.PLAYER_BANKING_TRANSACTION_DATA);
			this.transactionAmount = transactionAmount;
			fromUUID = from.getUUID();
			toUUID = to.getUUID();
			this.subject = subject;
			this.message = message;
			time = System.currentTimeMillis();
			this.transactionType = transactionType;
		}

		public PlayerBankTransactionData(PacketReadBuffer readBuffer) throws IOException {
			deserializeNetwork(readBuffer);
			dataType = DataType.PLAYER_BANKING_TRANSACTION_DATA;
		}

		public PlayerBankTransactionData(JSONObject data) {
			deserialize(data);
			dataType = DataType.PLAYER_BANKING_TRANSACTION_DATA;
		}

		@Override
		public JSONObject serialize() {
			JSONObject data = new JSONObject();
			data.put("version", VERSION);
			data.put("uuid", getUUID());
			data.put("transactionAmount", transactionAmount);
			data.put("fromUUID", fromUUID);
			data.put("toUUID", toUUID);
			data.put("subject", subject);
			data.put("message", message);
			data.put("time", time);
			data.put("transactionType", transactionType.name());
			return data;
		}

		@Override
		public void deserialize(JSONObject data) {
			byte version = (byte) data.getInt("version");
			dataUUID = data.getString("uuid");
			transactionAmount = data.getLong("transactionAmount");
			fromUUID = data.getString("fromUUID");
			toUUID = data.getString("toUUID");
			subject = data.getString("subject");
			message = data.getString("message");
			time = data.getLong("time");
			transactionType = TransactionType.valueOf(data.getString("transactionType"));
		}

		@Override
		public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
			writeBuffer.writeByte(VERSION);
			writeBuffer.writeString(dataUUID);
			writeBuffer.writeLong(transactionAmount);
			writeBuffer.writeString(fromUUID);
			writeBuffer.writeString(toUUID);
			writeBuffer.writeString(subject);
			writeBuffer.writeString(message);
			writeBuffer.writeLong(time);
			writeBuffer.writeString(transactionType.name());
		}

		@Override
		public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
			byte version = readBuffer.readByte();
			dataUUID = readBuffer.readString();
			transactionAmount = readBuffer.readLong();
			fromUUID = readBuffer.readString();
			toUUID = readBuffer.readString();
			subject = readBuffer.readString();
			message = readBuffer.readString();
			time = readBuffer.readLong();
			transactionType = TransactionType.valueOf(readBuffer.readString());
		}
		
		@Override
		public String toString() {
			return "Transaction: " + transactionType.name() + " " + transactionAmount + " credits from " + fromUUID + " to " + toUUID + " at " + formatDate();
		}
		
		public long getTransactionAmount() {
			return transactionAmount;
		}
		
		public String getFromUUID() {
			return fromUUID;
		}
		
		public PlayerData getFrom(boolean server) {
			return PlayerDataManager.getInstance(server).getFromUUID(fromUUID, server);
		}
		
		public String getToUUID() {
			return toUUID;
		}
		
		public PlayerData getTo(boolean server) {
			return PlayerDataManager.getInstance(server).getFromUUID(toUUID, server);
		}
		
		public String formatDate() {
			return DateUtils.getTimeFormatted(time);
		}
		
		public long getTime() {
			return time;
		}
		
		public String getSubject() {
			return subject;
		}
		
		public String getMessage() {
			return message;
		}
		
		public TransactionType getTransactionType() {
			return transactionType;
		}
	}
}
