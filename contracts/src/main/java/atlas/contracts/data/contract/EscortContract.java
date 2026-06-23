package atlas.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.contracts.manager.ConfigManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EscortContract extends ContractData {

	private static final byte VERSION = 0;

	private List<Vector3i> route = new ArrayList<>();
	private List<String> cargoBlueprintNames = new ArrayList<>();
	private List<String> defenderBlueprintNames = new ArrayList<>();
	private int totalCargoCount;
	private int cargoDestroyed;
	private boolean routeComplete;

	public EscortContract(int contractorID, String name, long reward, Difficulty difficulty, List<Vector3i> route, List<String> cargoBlueprintNames, List<String> defenderBlueprintNames, int cargoCount) {
		super(ContractType.ESCORT, contractorID, name, reward, difficulty);
		this.route = route;
		this.cargoBlueprintNames = cargoBlueprintNames;
		this.defenderBlueprintNames = defenderBlueprintNames;
		totalCargoCount = cargoCount;
		cargoDestroyed = 0;
		routeComplete = false;
	}

	public EscortContract(PacketReadBuffer packetReadBuffer) throws IOException {
		super(packetReadBuffer);
	}

	public EscortContract(JSONObject json) {
		super(json);
	}

	public List<Vector3i> getRoute() {
		return route;
	}

	public Vector3i getStartSector() {
		return route.get(0);
	}

	public Vector3i getDestinationSector() {
		return route.get(route.size() - 1);
	}

	public List<String> getCargoBlueprintNames() {
		return cargoBlueprintNames;
	}

	public List<String> getDefenderBlueprintNames() {
		return defenderBlueprintNames;
	}

	public int getTotalCargoCount() {
		return totalCargoCount;
	}

	public int getCargoDestroyed() {
		return cargoDestroyed;
	}

	public int getCargoSurviving() {
		return totalCargoCount - cargoDestroyed;
	}

	public void onCargoDestroyed(boolean server) {
		cargoDestroyed++;
		ContractDataManager.getInstance(server).updateData(this, server);
	}

	public boolean isRouteComplete() {
		return routeComplete;
	}

	public void setRouteComplete(boolean server) {
		if(routeComplete) return;
		routeComplete = true;
		ContractDataManager.getInstance(server).updateData(this, server);
	}

	public long getAdjustedReward() {
		if(cargoDestroyed >= totalCargoCount) return 0;
		double penalty = cargoDestroyed * ConfigManager.getEscortCargoLossPenalty();
		double multiplier = Math.max(0.0, 1.0 - penalty);
		return (long) (reward * multiplier);
	}

	@Override
	public ContractType getContractType() {
		return ContractType.ESCORT;
	}

	@Override
	public boolean canComplete(PlayerState player) {
		return player != null && claimants.containsKey(player.getName()) && routeComplete && getCargoSurviving() > 0;
	}

	@Override
	public void onCompletion(PlayerState player) {
		long adjustedReward = getAdjustedReward();
		// Apply small ship bonus
		if(player.getFirstControlledTransformableWOExc() instanceof SegmentController) {
			float playerMass = ((SegmentController) player.getFirstControlledTransformableWOExc()).getMassWithDocks();
			if(playerMass > 0 && playerMass <= ConfigManager.getEscortSmallShipBonusMass()) {
				adjustedReward = (long) (adjustedReward * ConfigManager.getEscortSmallShipBonusMultiplier());
			}
		}
		// Temporarily override reward for payout, then restore. getScaledReward() floors at 1 bar.
		long originalReward = reward;
		reward = adjustedReward;
		payoutReward(player);
		reward = originalReward;
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = super.serialize();
		data.put("escort_version", VERSION);
		JSONArray routeArray = new JSONArray();
		for(Vector3i waypoint : route) {
			JSONObject wp = new JSONObject();
			wp.put("x", waypoint.x);
			wp.put("y", waypoint.y);
			wp.put("z", waypoint.z);
			routeArray.put(wp);
		}
		data.put("route", routeArray);
		JSONArray cargoArray = new JSONArray();
		for(String bp : cargoBlueprintNames) cargoArray.put(bp);
		data.put("cargo_blueprints", cargoArray);
		JSONArray defenderArray = new JSONArray();
		for(String bp : defenderBlueprintNames) defenderArray.put(bp);
		data.put("defender_blueprints", defenderArray);
		data.put("total_cargo_count", totalCargoCount);
		data.put("cargo_destroyed", cargoDestroyed);
		data.put("route_complete", routeComplete);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		super.deserialize(data);
		route = new ArrayList<>();
		JSONArray routeArray = data.getJSONArray("route");
		for(int i = 0; i < routeArray.length(); i++) {
			JSONObject wp = routeArray.getJSONObject(i);
			route.add(new Vector3i(wp.getInt("x"), wp.getInt("y"), wp.getInt("z")));
		}
		cargoBlueprintNames = new ArrayList<>();
		JSONArray cargoArray = data.getJSONArray("cargo_blueprints");
		for(int i = 0; i < cargoArray.length(); i++) cargoBlueprintNames.add(cargoArray.getString(i));
		defenderBlueprintNames = new ArrayList<>();
		if(data.has("defender_blueprints")) {
			JSONArray defenderArray = data.getJSONArray("defender_blueprints");
			for(int i = 0; i < defenderArray.length(); i++) defenderBlueprintNames.add(defenderArray.getString(i));
		}
		totalCargoCount = data.getInt("total_cargo_count");
		cargoDestroyed = data.optInt("cargo_destroyed", 0);
		routeComplete = data.optBoolean("route_complete", false);
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		super.serializeNetwork(writeBuffer);
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeInt(route.size());
		for(Vector3i waypoint : route) {
			writeBuffer.writeInt(waypoint.x);
			writeBuffer.writeInt(waypoint.y);
			writeBuffer.writeInt(waypoint.z);
		}
		writeBuffer.writeInt(cargoBlueprintNames.size());
		for(String bp : cargoBlueprintNames) writeBuffer.writeString(bp);
		writeBuffer.writeInt(defenderBlueprintNames.size());
		for(String bp : defenderBlueprintNames) writeBuffer.writeString(bp);
		writeBuffer.writeInt(totalCargoCount);
		writeBuffer.writeInt(cargoDestroyed);
		writeBuffer.writeBoolean(routeComplete);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		super.deserializeNetwork(readBuffer);
		byte version = readBuffer.readByte();
		int routeSize = readBuffer.readInt();
		route = new ArrayList<>();
		for(int i = 0; i < routeSize; i++) {
			route.add(new Vector3i(readBuffer.readInt(), readBuffer.readInt(), readBuffer.readInt()));
		}
		int cargoSize = readBuffer.readInt();
		cargoBlueprintNames = new ArrayList<>();
		for(int i = 0; i < cargoSize; i++) cargoBlueprintNames.add(readBuffer.readString());
		int defenderSize = readBuffer.readInt();
		defenderBlueprintNames = new ArrayList<>();
		for(int i = 0; i < defenderSize; i++) defenderBlueprintNames.add(readBuffer.readString());
		totalCargoCount = readBuffer.readInt();
		cargoDestroyed = readBuffer.readInt();
		routeComplete = readBuffer.readBoolean();
	}
}
