package atlas.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.contracts.AtlasContracts;
import atlas.contracts.manager.ConfigManager;
import atlas.contracts.utils.BlueprintUtils;
import atlas.contracts.utils.FlavorUtils;
import atlas.contracts.utils.SectorUtils;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class BountyContract extends ContractData {

	public static final int PLAYER = 0;
	public static final int MOB = 1;

	private JSONObject targetData;
	private boolean killedTarget;

	public BountyContract(int contractorID, String name, JSONObject targetData, Difficulty difficulty) {
		super(ContractType.BOUNTY, contractorID, name, targetData.getLong("reward"), difficulty);
		this.targetData = targetData;
	}

	public BountyContract(PacketReadBuffer packetReadBuffer) throws IOException {
		super(packetReadBuffer);
	}

	public BountyContract(JSONObject json) {
		super(json);
	}

	public static BountyContract generateRandomMob(int factionId) {
		JSONObject targetData = generateMobTarget();
		if(targetData != null) {
			Difficulty difficulty = Difficulty.getRandomDifficulty();
			String name = FlavorUtils.generateGroupName(FlavorUtils.FlavorType.PIRATE);
			Vector3i sector = SectorUtils.getRandomSector(10);
			String contractName = "[" + difficulty.displayName + "] Defeat " + name + " in Sector " + sector;
			return new BountyContract(factionId, contractName, targetData, difficulty);
		}
		throw new IllegalStateException("Failed to generate a valid BountyContract for mobs.");
	}

	private static JSONObject generateMobTarget() {
		JSONObject targetData = new JSONObject();
		try {
			HashMap<BlueprintEntry, Float> spawnWeights = BlueprintUtils.getPirateSpawnWeights();
			if(spawnWeights.isEmpty()) return null;
			ArrayList<BlueprintEntry> blueprints = new ArrayList<>(spawnWeights.keySet());
			Random random = new Random();
			int maxMobs = ConfigManager.getMaxBountyMobCount();
			double maxMass = ConfigManager.getMaxBountyMobCombinedMass();
			ArrayList<JSONObject> mobList = new ArrayList<>();
			double totalMass = 0.0;
			int mobCount = random.nextInt(maxMobs) + 1;
			for(int i = 0; i < mobCount; i++) {
				BlueprintEntry blueprint = blueprints.get(random.nextInt(blueprints.size()));
				float weight = spawnWeights.get(blueprint);
				if(random.nextFloat() <= weight) {
					double mass = blueprint.getMass();
					if(totalMass + mass <= maxMass) {
						JSONObject mobData = new JSONObject();
						mobData.put("bp_name", blueprint.getName());
						mobData.put("spawn_name", FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.PIRATE));
						mobList.add(mobData);
						totalMass += mass;
					}
				}
			}
			if(mobList.isEmpty()) {
				AtlasContracts.getInstance().logWarning("No valid mobs generated for bounty target.");
				return null;
			}
			targetData.put("mob_list", mobList);
			JSONObject sectorData = new JSONObject();
			Vector3i sector = SectorUtils.getRandomSector(10);
			sectorData.put("x", sector.x);
			sectorData.put("y", sector.y);
			sectorData.put("z", sector.z);
			targetData.put("sector", sectorData);
			targetData.put("target_type", MOB);
			// Gold Bar reward: 3 bars per target ship, floor of 5.
			targetData.put("reward", Math.max(5L, 3L * mobList.size()));
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while generating a random BountyTargetMob", exception);
			return null;
		}
		return targetData;
	}

	public int getBountyType() {
		return targetData != null ? targetData.optInt("target_type", MOB) : MOB;
	}

	public JSONObject getTargetData() {
		return targetData;
	}

	public void setKilledTarget(boolean server, boolean value) {
		if(killedTarget == value) return;
		killedTarget = value;
		ContractDataManager.getInstance(server).updateData(this, server);
	}

	public boolean isKilledTarget() {
		return killedTarget;
	}

	@Override
	public boolean canComplete(PlayerState player) {
		return player != null && claimants.containsKey(player.getName()) && killedTarget;
	}

	@Override
	public void onCompletion(PlayerState player) {
		payoutReward(player);
	}

	@Override
	public JSONObject serialize() {
		JSONObject json = super.serialize();
		json.put("target", targetData);
		json.put("killed_target", killedTarget);
		return json;
	}

	@Override
	public void deserialize(JSONObject data) {
		super.deserialize(data);
		targetData = data.getJSONObject("target");
		killedTarget = data.optBoolean("killed_target", false);
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		super.serializeNetwork(writeBuffer);
		writeBuffer.writeString(targetData != null ? targetData.toString() : "{}");
		writeBuffer.writeBoolean(killedTarget);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		super.deserializeNetwork(readBuffer);
		targetData = new JSONObject(readBuffer.readString());
		killedTarget = readBuffer.readBoolean();
	}

	@Override
	public ContractType getContractType() {
		return ContractType.BOUNTY;
	}
}
