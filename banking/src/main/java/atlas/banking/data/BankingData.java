package atlas.banking.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.data.SerializableData;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Banking data for a single player, extracted from EdenCore's PlayerData.
 *
 * @author TheDerpGamer
 */
public class BankingData extends SerializableData {

	private static final byte VERSION = 0;

	private String playerName = "";
	private double storedCredits;
	private Set<BankTransactionData> transactionHistory = new HashSet<>();

	public BankingData(String playerName) {
		super("BANKING_DATA");
		this.playerName = playerName;
	}

	public BankingData(PacketReadBuffer readBuffer) throws IOException {
		super("BANKING_DATA");
		deserializeNetwork(readBuffer);
	}

	public BankingData(JSONObject data) {
		deserialize(data);
	}

	@Override
	public String getDataTypeName() {
		return "BANKING_DATA";
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("uuid", getUUID());
		data.put("playerName", playerName);
		data.put("storedCredits", storedCredits);
		JSONArray transactionArray = new JSONArray();
		for(BankTransactionData transaction : transactionHistory) transactionArray.put(transaction.serialize());
		data.put("transactionHistory", transactionArray);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		dataUUID = data.getString("uuid");
		playerName = data.getString("playerName");
		storedCredits = data.getDouble("storedCredits");
		transactionHistory.clear();
		JSONArray transactionArray = data.getJSONArray("transactionHistory");
		for(int i = 0; i < transactionArray.length(); i++) {
			transactionHistory.add(new BankTransactionData(transactionArray.getJSONObject(i)));
		}
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(playerName);
		writeBuffer.writeDouble(storedCredits);
		writeBuffer.writeInt(transactionHistory.size());
		for(BankTransactionData transaction : transactionHistory) {
			transaction.serializeNetwork(writeBuffer);
		}
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		playerName = readBuffer.readString();
		storedCredits = readBuffer.readDouble();
		int transactionCount = readBuffer.readInt();
		transactionHistory = new HashSet<>();
		for(int i = 0; i < transactionCount; i++) {
			transactionHistory.add(new BankTransactionData(readBuffer));
		}
	}

	public String getPlayerName() {
		return playerName;
	}

	public double getStoredCredits() {
		return storedCredits;
	}

	public void setStoredCredits(double credits) {
		storedCredits = credits;
	}

	public Set<BankTransactionData> getTransactionHistory() {
		return Collections.unmodifiableSet(transactionHistory);
	}

	public void addTransaction(BankTransactionData transaction) {
		transactionHistory.add(transaction);
	}

	public static class BankTransactionData extends SerializableData {

		private static final byte VERSION = 0;
		private double amount;
		private String fromUUID;
		private String toUUID;
		private String subject;
		private String message;
		private long timestamp;
		private TransactionType transactionType;
		public BankTransactionData(double amount, String fromUUID, String toUUID, String subject, String message, TransactionType transactionType) {
			super("BANK_TRANSACTION_DATA");
			this.amount = amount;
			this.fromUUID = fromUUID;
			this.toUUID = toUUID;
			this.subject = subject;
			this.message = message;
			timestamp = System.currentTimeMillis();
			this.transactionType = transactionType;
		}

		public BankTransactionData(PacketReadBuffer readBuffer) throws IOException {
			deserializeNetwork(readBuffer);
		}

		public BankTransactionData(JSONObject data) {
			deserialize(data);
		}

		@Override
		public String getDataTypeName() {
			return "BANK_TRANSACTION_DATA";
		}

		@Override
		public JSONObject serialize() {
			JSONObject data = new JSONObject();
			data.put("version", VERSION);
			data.put("uuid", getUUID());
			data.put("amount", amount);
			data.put("fromUUID", fromUUID);
			data.put("toUUID", toUUID);
			data.put("subject", subject);
			data.put("message", message);
			data.put("timestamp", timestamp);
			data.put("transactionType", transactionType.name());
			return data;
		}

		@Override
		public void deserialize(JSONObject data) {
			byte version = (byte) data.getInt("version");
			dataUUID = data.getString("uuid");
			amount = data.getDouble("amount");
			fromUUID = data.getString("fromUUID");
			toUUID = data.getString("toUUID");
			subject = data.getString("subject");
			message = data.getString("message");
			timestamp = data.getLong("timestamp");
			transactionType = TransactionType.valueOf(data.getString("transactionType"));
		}

		@Override
		public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
			writeBuffer.writeByte(VERSION);
			writeBuffer.writeString(dataUUID);
			writeBuffer.writeDouble(amount);
			writeBuffer.writeString(fromUUID);
			writeBuffer.writeString(toUUID);
			writeBuffer.writeString(subject);
			writeBuffer.writeString(message);
			writeBuffer.writeLong(timestamp);
			writeBuffer.writeString(transactionType.name());
		}

		@Override
		public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
			byte version = readBuffer.readByte();
			dataUUID = readBuffer.readString();
			amount = readBuffer.readDouble();
			fromUUID = readBuffer.readString();
			toUUID = readBuffer.readString();
			subject = readBuffer.readString();
			message = readBuffer.readString();
			timestamp = readBuffer.readLong();
			transactionType = TransactionType.valueOf(readBuffer.readString());
		}

		public double getAmount() {
			return amount;
		}

		public String getFromUUID() {
			return fromUUID;
		}

		public String getToUUID() {
			return toUUID;
		}

		public String getSubject() {
			return subject;
		}

		public String getMessage() {
			return message;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public TransactionType getTransactionType() {
			return transactionType;
		}

		@Override
		public String toString() {
			return "Transaction: " + transactionType.name() + " " + amount + " credits from " + fromUUID + " to " + toUUID + " at " + timestamp;
		}

		public enum TransactionType {
			DEPOSIT,
			WITHDRAW,
			TRANSFER
		}
	}
}
