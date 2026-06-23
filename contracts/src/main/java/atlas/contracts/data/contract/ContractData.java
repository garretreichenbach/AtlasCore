package atlas.contracts.data.contract;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.contracts.manager.ConfigManager;
import atlas.core.data.SerializableData;
import atlas.core.utils.GoldBarUtils;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all contracts (bounty, items, escort). Rewards are paid in Gold Bars.
 * Registered with AtlasCore under the {@code "CONTRACT_DATA"} data type; the registry
 * deserializer dispatches on the {@code type} discriminator to the correct subclass.
 */
public abstract class ContractData extends SerializableData {

	public static final String DATA_TYPE = "CONTRACT_DATA";

	private final byte VERSION = 1;
	private ContractType type;
	protected String name;
	protected int contractorID;
	protected long reward;
	protected Difficulty difficulty = Difficulty.NORMAL;
	protected HashMap<String, Long> claimants = new HashMap<>();

	protected ContractData(ContractType type, int contractorID, String name, long reward, Difficulty difficulty) {
		super(DATA_TYPE);
		this.type = type;
		this.contractorID = contractorID;
		this.name = name;
		this.difficulty = difficulty;
		this.reward = (long) (reward * difficulty.rewardMultiplier);
	}

	protected ContractData(JSONObject jsonObject) {
		deserialize(jsonObject);
		dataTypeName = DATA_TYPE;
	}

	protected ContractData(PacketReadBuffer packetReadBuffer) throws IOException {
		deserializeNetwork(packetReadBuffer);
		dataTypeName = DATA_TYPE;
	}

	/** Network deserializer: reads the leading type discriminator and constructs the matching subclass. */
	public static ContractData readContract(PacketReadBuffer packetReadBuffer) throws IOException {
		ContractType type = ContractType.fromString(packetReadBuffer.readString());
		switch(Objects.requireNonNull(type)) {
			case BOUNTY:
				return new BountyContract(packetReadBuffer);
			case ITEMS:
				return new ItemsContract(packetReadBuffer);
			case ESCORT:
				return new EscortContract(packetReadBuffer);
			default:
				return null;
		}
	}

	/** JSON deserializer: reads the {@code type} field and constructs the matching subclass. */
	public static ContractData readContract(JSONObject json) {
		ContractType type = ContractType.fromString(json.getString("type"));
		switch(Objects.requireNonNull(type)) {
			case BOUNTY:
				return new BountyContract(json);
			case ITEMS:
				return new ItemsContract(json);
			case ESCORT:
				return new EscortContract(json);
			default:
				return null;
		}
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = new JSONObject();
		data.put("version", VERSION);
		data.put("uuid", dataUUID);
		data.put("type", type.name());
		data.put("name", name);
		data.put("contractor_id", contractorID);
		data.put("reward", reward);
		data.put("difficulty", difficulty.name());
		JSONObject claimantsJson = new JSONObject();
		for(Map.Entry<String, Long> entry : claimants.entrySet()) {
			claimantsJson.put(entry.getKey(), entry.getValue());
		}
		data.put("claimants", claimantsJson);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = data.has("version") ? (byte) data.getInt("version") : 0;
		dataUUID = data.getString("uuid");
		type = ContractType.fromString(data.getString("type"));
		name = data.getString("name");
		contractorID = data.getInt("contractor_id");
		reward = data.getLong("reward");
		difficulty = version >= 1 && data.has("difficulty") ? Difficulty.fromString(data.getString("difficulty")) : Difficulty.NORMAL;
		claimants = new HashMap<>();
		if(data.has("claimants")) {
			JSONObject claimantsJson = data.getJSONObject("claimants");
			for(Object keyObj : claimantsJson.keySet()) {
				String key = (String) keyObj;
				claimants.put(key, claimantsJson.getLong(key));
			}
		}
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeString(type.name());
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(name);
		writeBuffer.writeInt(contractorID);
		writeBuffer.writeLong(reward);
		writeBuffer.writeString(difficulty.name());
		writeBuffer.writeInt(claimants.size());
		for(Map.Entry<String, Long> entry : claimants.entrySet()) {
			writeBuffer.writeString(entry.getKey());
			writeBuffer.writeLong(entry.getValue());
		}
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		name = readBuffer.readString();
		contractorID = readBuffer.readInt();
		reward = readBuffer.readLong();
		difficulty = version >= 1 ? Difficulty.fromString(readBuffer.readString()) : Difficulty.NORMAL;
		int claimantCount = readBuffer.readInt();
		claimants = new HashMap<>(claimantCount);
		for(int i = 0; i < claimantCount; i++) {
			String playerName = readBuffer.readString();
			long timestamp = readBuffer.readLong();
			claimants.put(playerName, timestamp);
		}
	}

	public String getName() {
		return name;
	}

	public int getContractorID() {
		return contractorID;
	}

	/**
	 * Returns the contracting faction, or {@code null} if it no longer exists. Pure read — does NOT
	 * mutate state (the original self-removed the contract here, corrupting GUI render passes).
	 * Stale-faction cleanup is handled server-side by {@link ContractDataManager#purgeOrphanedContracts()}.
	 */
	public Faction getContractor() {
		if(GameCommon.getGameState().getFactionManager().existsFaction(contractorID)) {
			return GameCommon.getGameState().getFactionManager().getFaction(contractorID);
		}
		return null;
	}

	public String getContractorName() {
		if(contractorID == 0) return "Non-Aligned";
		Faction faction = getContractor();
		return faction != null ? faction.getName() : "Unknown";
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public long getReward() {
		return reward;
	}

	/** The reward in Gold Bars after the global multiplier, clamped to at least 1 bar. */
	public long getScaledReward() {
		return Math.max(1, (long) (reward * ConfigManager.getRewardBaseMultiplier()));
	}

	/** Server-side: pays the contract reward to {@code player} as Gold Bars (offline-safe pending delivery). */
	public void payoutReward(PlayerState player) {
		GoldBarUtils.pay(player.getName(), (int) getScaledReward());
	}

	public HashMap<String, Long> getClaimants() {
		return claimants;
	}

	public long getTimeRemaining(String playerName) {
		if(!claimants.containsKey(playerName)) return 0;
		long elapsed = System.currentTimeMillis() - claimants.get(playerName);
		return Math.max(0, ConfigManager.getContractTimeoutTimer() - elapsed);
	}

	public abstract ContractType getContractType();

	public abstract boolean canComplete(PlayerState player);

	public abstract void onCompletion(PlayerState player);

	public enum ContractType {
		ALL("All"),
		BOUNTY("Bounty"),
		ITEMS("Items"),
		ESCORT("Escort");

		public final String displayName;

		ContractType(String displayName) {
			this.displayName = displayName;
		}

		public static ContractType fromString(String s) {
			return Arrays.stream(values())
					.filter(type -> s.trim().equalsIgnoreCase(type.displayName.trim()) || s.trim().equalsIgnoreCase(type.name()))
					.findFirst().orElse(null);
		}

		public static ContractType getRandomType() {
			return values()[(int) (Math.random() * (values().length - 1)) + 1];
		}
	}

	public enum Difficulty {
		EASY("Easy", 0.75f),
		NORMAL("Normal", 1.0f),
		HARD("Hard", 1.5f),
		EXTREME("Extreme", 2.5f);

		public final String displayName;
		public final float rewardMultiplier;

		Difficulty(String displayName, float rewardMultiplier) {
			this.displayName = displayName;
			this.rewardMultiplier = rewardMultiplier;
		}

		public static Difficulty fromString(String s) {
			return Arrays.stream(values())
					.filter(d -> s.trim().equalsIgnoreCase(d.name().trim()))
					.findFirst().orElse(NORMAL);
		}

		public static Difficulty getRandomDifficulty() {
			return values()[(int) (Math.random() * values().length)];
		}
	}
}
