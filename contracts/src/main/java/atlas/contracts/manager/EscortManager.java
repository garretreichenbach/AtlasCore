package atlas.contracts.manager;

import api.common.GameCommon;
import api.common.GameServer;
import atlas.contracts.AtlasContracts;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import atlas.contracts.data.contract.EscortContract;
import atlas.contracts.data.contract.active.ActiveContractData;
import atlas.contracts.data.contract.active.ActiveContractDataManager;
import atlas.contracts.utils.BlueprintUtils;
import atlas.contracts.utils.FlavorUtils;
import atlas.contracts.utils.SectorUtils;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.schine.network.server.ServerMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active escort missions on the server side. Tracks spawned cargo ships, defender escorts,
 * and pirate waves, advancing cargo along the route and spawning pirates on a timer.
 */
public class EscortManager {

	private static EscortManager instance;

	private final ConcurrentHashMap<String, EscortSession> activeSessions = new ConcurrentHashMap<>();

	public static EscortManager getInstance() {
		if(instance == null) instance = new EscortManager();
		return instance;
	}

	public void startEscort(EscortContract contract, PlayerState player) {
		String contractUUID = contract.getUUID();
		if(activeSessions.containsKey(contractUUID)) return;
		EscortSession session = new EscortSession(contract, player.getName());
		activeSessions.put(contractUUID, session);
		session.spawnCargoShips();
		session.spawnDefenders(contract.getDifficulty());
		if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Escort session started for " + player.getName() + " (contract: " + contractUUID + ")");
	}

	public void update() {
		Iterator<Map.Entry<String, EscortSession>> it = activeSessions.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, EscortSession> entry = it.next();
			EscortSession session = entry.getValue();
			if(session.isFinished()) {
				session.cleanup();
				it.remove();
				continue;
			}
			session.tick();
		}
	}

	public void onEntityDestroyed(SegmentController entity) {
		for(EscortSession session : activeSessions.values()) {
			session.onEntityDestroyed(entity);
		}
	}

	public EscortSession getSession(String contractUUID) {
		return activeSessions.get(contractUUID);
	}

	public void removeSession(String contractUUID) {
		EscortSession session = activeSessions.remove(contractUUID);
		if(session != null) session.cleanup();
	}

	/**
	 * A single active escort mission instance.
	 */
	public static class EscortSession {
		private final EscortContract contract;
		private final String playerName;
		private final List<String> cargoEntityNames = new ArrayList<>();
		private final List<String> defenderEntityNames = new ArrayList<>();
		private final Set<String> destroyedCargo = ConcurrentHashMap.newKeySet();
		private int currentWaypointIndex;
		private long lastPirateSpawnTime;
		private long lastUpdateTime;
		private boolean finished;
		private boolean cargoSpawned;

		public EscortSession(EscortContract contract, String playerName) {
			this.contract = contract;
			this.playerName = playerName;
			lastPirateSpawnTime = System.currentTimeMillis();
			lastUpdateTime = System.currentTimeMillis();
		}

		public void spawnCargoShips() {
			Vector3i startSector = contract.getStartSector();
			List<String> cargoBPs = contract.getCargoBlueprintNames();
			if(cargoBPs.isEmpty()) {
				AtlasContracts.getInstance().logWarning("Escort contract " + contract.getUUID() + " has no cargo blueprints; cannot spawn cargo.");
				finished = true;
				return;
			}
			int cargoCount = contract.getTotalCargoCount();
			int factionId = FactionManager.TRAIDING_GUILD_ID;
			for(int i = 0; i < cargoCount; i++) {
				String bpName = cargoBPs.get(i % cargoBPs.size());
				String spawnName = FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.TRADERS) + " Cargo-" + (i + 1);
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bpName);
				mob.put("spawn_name", spawnName);
				SegmentController spawned = BlueprintUtils.spawnAsMob(mob, startSector, factionId);
				if(spawned != null) {
					cargoEntityNames.add(spawned.getRealName());
					setEntityTarget(spawned, getNextWaypoint());
				}
			}
			cargoSpawned = true;
			if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Spawned " + cargoEntityNames.size() + " cargo ships for escort contract " + contract.getUUID());
		}

		public void spawnDefenders(ContractData.Difficulty difficulty) {
			List<String> defenderBPs = contract.getDefenderBlueprintNames();
			if(defenderBPs.isEmpty()) return;
			int defenderCount = switch (difficulty) {
                case EASY -> 3;
                case NORMAL -> 2;
                case HARD -> 1;
                default -> 0;
            };
            if(defenderCount == 0) return;
			Vector3i startSector = contract.getStartSector();
			int factionId = FactionManager.TRAIDING_GUILD_ID;
			for(int i = 0; i < defenderCount; i++) {
				String bpName = defenderBPs.get(i % defenderBPs.size());
				String spawnName = FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.TRADERS) + " Escort-" + (i + 1);
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bpName);
				mob.put("spawn_name", spawnName);
				SegmentController spawned = BlueprintUtils.spawnAsMob(mob, startSector, factionId);
				if(spawned != null) defenderEntityNames.add(spawned.getRealName());
			}
			if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Spawned " + defenderEntityNames.size() + " defender escorts");
		}

		public void tick() {
			long now = System.currentTimeMillis();
			if(now - lastUpdateTime < ConfigManager.getEscortUpdateInterval()) return;
			lastUpdateTime = now;

			if(!cargoSpawned || finished) return;

			PlayerState player = GameCommon.getPlayerFromName(playerName);

			// Offline player: fail the escort and clean up so cargo/defenders don't leak (bug fix).
			if(player == null) {
				if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Escort " + contract.getUUID() + " failed: " + playerName + " is offline.");
				failEscort(null, null);
				return;
			}

			// Player wandered too far from the convoy.
			Vector3i cargoSector = getLeadCargoSector();
			if(cargoSector != null && !SectorUtils.isInRange(player.getCurrentSector(), cargoSector, 5)) {
				failEscort(player, "Escort mission failed! You moved too far from the convoy.");
				return;
			}

			// All cargo destroyed → mission failed (no surviving cargo to deliver).
			if(destroyedCargo.size() >= contract.getTotalCargoCount()) {
				failEscort(player, "Escort mission failed! All cargo ships were destroyed.");
				return;
			}

			// Advance the convoy when all surviving cargo has reached the current waypoint.
			boolean allAtWaypoint = true;
			Vector3i targetWaypoint = getCurrentWaypoint();
			for(String cargoName : cargoEntityNames) {
				if(destroyedCargo.contains(cargoName)) continue;
				SegmentController cargo = getEntityByName(cargoName);
				if(cargo == null) continue;
				if(!SectorUtils.isInRange(cargo.getSector(new Vector3i()), targetWaypoint, 1)) {
					allAtWaypoint = false;
					break;
				}
			}

			if(allAtWaypoint && currentWaypointIndex < contract.getRoute().size() - 1) {
				currentWaypointIndex++;
				Vector3i nextWaypoint = getCurrentWaypoint();
				for(String cargoName : cargoEntityNames) {
					if(destroyedCargo.contains(cargoName)) continue;
					SegmentController cargo = getEntityByName(cargoName);
					if(cargo != null) setEntityTarget(cargo, nextWaypoint);
				}
				for(String defName : defenderEntityNames) {
					SegmentController defender = getEntityByName(defName);
					if(defender != null) setEntityTarget(defender, nextWaypoint);
				}
			}

			// Route completed — mark the contract ready to turn in.
			if(currentWaypointIndex >= contract.getRoute().size() - 1 && allAtWaypoint) {
				finished = true;
				contract.setRouteComplete(true);
				ActiveContractData activeContract = ActiveContractDataManager.getInstance(true).getFromContractUUID(contract.getUUID(), playerName, true);
				if(activeContract != null) activeContract.setCanComplete(true, true);
				int surviving = contract.getCargoSurviving();
				player.sendServerMessage(new ServerMessage(new String[]{"Escort complete! " + surviving + "/" + contract.getTotalCargoCount() + " cargo ships survived. Turn it in for your Gold Bars."}, ServerMessage.MESSAGE_TYPE_INFO));
				return;
			}

			// Spawn pirate waves.
			if(now - lastPirateSpawnTime >= ConfigManager.getEscortPirateWaveInterval()) {
				spawnPirateWave();
				lastPirateSpawnTime = now;
			}
		}

		/**
		 * Fails the escort: marks finished (the manager loop will despawn entities), strips the player's
		 * claim so the contract returns to the available pool, and removes the active-contract record.
		 * {@code player} may be {@code null} when the player is offline.
		 */
		private void failEscort(PlayerState player, String message) {
			finished = true;
			contract.getClaimants().remove(playerName);
			ContractDataManager.getInstance(true).updateData(contract, true);
			PlayerData playerData = PlayerDataManager.getInstance(true).getFromName(playerName, true);
			if(playerData != null) {
				playerData.getContracts().remove(contract.getUUID());
				PlayerDataManager.getInstance(true).updateData(playerData, true);
			}
			ActiveContractData activeContract = ActiveContractDataManager.getInstance(true).getFromContractUUID(contract.getUUID(), playerName, true);
			if(activeContract != null) ActiveContractDataManager.getInstance(true).removeData(activeContract, true);
			if(player != null && message != null) {
				player.sendServerMessage(new ServerMessage(new String[]{message}, ServerMessage.MESSAGE_TYPE_ERROR));
			}
		}

		private void spawnPirateWave() {
			Vector3i spawnSector = getLeadCargoSector();
			if(spawnSector == null) return;

			Random random = new Random();
			int pirateCount = random.nextInt(ConfigManager.getEscortPirateMaxPerWave()) + 1;
			HashMap<BlueprintEntry, Float> pirateWeights = BlueprintUtils.getPirateSpawnWeights();
			if(pirateWeights.isEmpty()) return;
			ArrayList<BlueprintEntry> pirateBPs = new ArrayList<>(pirateWeights.keySet());

			int spawned = 0;
			for(int i = 0; i < pirateCount; i++) {
				BlueprintEntry bp = pirateBPs.get(random.nextInt(pirateBPs.size()));
				if(random.nextFloat() > pirateWeights.get(bp)) continue;
				JSONObject mob = new JSONObject();
				mob.put("bp_name", bp.getName());
				mob.put("spawn_name", FlavorUtils.generateSpawnName(FlavorUtils.FlavorType.PIRATE) + " Wave-" + System.currentTimeMillis() % 10000);
				SegmentController pirate = BlueprintUtils.spawnAsMob(mob, spawnSector, FactionManager.PIRATES_ID);
				if(pirate != null) spawned++;
			}
			if(spawned > 0 && ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Spawned " + spawned + " pirates for escort contract " + contract.getUUID());
		}

		public void onEntityDestroyed(SegmentController entity) {
			String name = entity.getRealName();
			if(cargoEntityNames.contains(name) && !destroyedCargo.contains(name)) {
				destroyedCargo.add(name);
				contract.onCargoDestroyed(true);
				if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logInfo("Cargo ship destroyed: " + name + " (" + contract.getCargoSurviving() + " remaining)");
				PlayerState player = GameCommon.getPlayerFromName(playerName);
				if(player != null) {
					player.sendServerMessage(new ServerMessage(new String[]{"A cargo ship has been destroyed! " + contract.getCargoSurviving() + "/" + contract.getTotalCargoCount() + " remaining."}, ServerMessage.MESSAGE_TYPE_WARNING));
				}
			}
		}

		public void cleanup() {
			for(String name : cargoEntityNames) {
				if(destroyedCargo.contains(name)) continue;
				SegmentController entity = getEntityByName(name);
				if(entity != null) entity.markForPermanentDelete(true);
			}
			for(String name : defenderEntityNames) {
				SegmentController entity = getEntityByName(name);
				if(entity != null) entity.markForPermanentDelete(true);
			}
		}

		public boolean isFinished() {
			return finished;
		}

		private Vector3i getLeadCargoSector() {
			for(String cargoName : cargoEntityNames) {
				if(destroyedCargo.contains(cargoName)) continue;
				SegmentController cargo = getEntityByName(cargoName);
				if(cargo != null) return cargo.getSector(new Vector3i());
			}
			return null;
		}

		private Vector3i getCurrentWaypoint() {
			return contract.getRoute().get(Math.min(currentWaypointIndex, contract.getRoute().size() - 1));
		}

		private Vector3i getNextWaypoint() {
			int next = Math.min(currentWaypointIndex + 1, contract.getRoute().size() - 1);
			return contract.getRoute().get(next);
		}

		private void setEntityTarget(SegmentController entity, Vector3i sector) {
			try {
				if(entity instanceof Ship) {
					Ship ship = (Ship) entity;
					if(ship.getAiConfiguration().getAiEntityState().getCurrentProgram() instanceof TargetProgram) {
						TargetProgram<?> program = (TargetProgram<?>) ship.getAiConfiguration().getAiEntityState().getCurrentProgram();
						program.setSectorTarget(new Vector3i(sector));
					}
				}
			} catch(Exception e) {
				if(ConfigManager.isDebugMode()) AtlasContracts.getInstance().logException("Failed to set sector target for " + entity.getRealName(), e);
			}
		}

		private SegmentController getEntityByName(String name) {
			try {
				return GameServer.getServerState().getSegmentControllersByName().get(name);
			} catch(Exception e) {
				return null;
			}
		}

		public String getPlayerName() {
			return playerName;
		}

		public EscortContract getContract() {
			return contract;
		}
	}
}
