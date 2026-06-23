package atlas.contracts.data.contract;

import api.mod.config.PersistentObjectUtil;
import atlas.contracts.AtlasContracts;
import atlas.contracts.manager.ConfigManager;
import atlas.contracts.utils.SectorUtils;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static atlas.contracts.gui.contract.newcontract.NewContractPanel.getProductionFilter;

public class ContractDataManager extends DataManager<ContractData> {

	private final Set<ContractData> clientCache = ConcurrentHashMap.newKeySet();
	private static ContractDataManager serverInstance;
	private static ContractDataManager clientInstance;

	public static ContractDataManager getInstance(boolean server) {
		if(server) {
			if(serverInstance == null) serverInstance = new ContractDataManager();
			return serverInstance;
		} else {
			if(clientInstance == null) {
				clientInstance = new ContractDataManager();
				clientInstance.requestFromServer();
			}
			return clientInstance;
		}
	}

	/**
	 * Server-side completion: guards against an offline/missing player, pays out, removes the player's
	 * claim, and deletes the contract. Safe to call when {@code playerData}'s player is offline.
	 */
	public static void completeContract(PlayerData playerData, ContractData contract) {
		if(playerData == null || contract == null) return;
		PlayerState player = playerData.getPlayerState();
		if(player == null || !contract.canComplete(player)) return;
		contract.onCompletion(player);
		playerData.removeContract(contract.getUUID());
		PlayerDataManager.getInstance(true).updateData(playerData, true);
		getInstance(true).removeData(contract, true);
	}

	@Override
	public Set<ContractData> getServerCache() {
		Set<ContractData> serverCache = new HashSet<>();
		for(Object o : PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), BountyContract.class)) serverCache.add((BountyContract) o);
		for(Object o : PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), ItemsContract.class)) serverCache.add((ItemsContract) o);
		for(Object o : PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), EscortContract.class)) serverCache.add((EscortContract) o);
		return serverCache;
	}

	@Override
	public String getDataTypeName() {
		return ContractData.DATA_TYPE;
	}

	@Override
	public Set<ContractData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(ContractData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(ContractData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(ContractData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {
	}

	public void generateRandomContract() {
		Random random = new Random();
		ContractData.ContractType contractType = ContractData.ContractType.getRandomType();
		// Escort contracts are disabled until trader/cargo blueprints exist — substitute an items contract.
		if(contractType == ContractData.ContractType.ESCORT && !ConfigManager.isEscortEnabled()) {
			contractType = ContractData.ContractType.ITEMS;
		}
		ContractData randomContract = null;
		ContractData.Difficulty difficulty = ContractData.Difficulty.getRandomDifficulty();
		switch(contractType) {
			case ITEMS:
				ArrayList<Short> possibleIDs = new ArrayList<>();
				for(ElementInformation info : getProductionFilter()) possibleIDs.add(info.getId());
				if(possibleIDs.isEmpty()) return;
				int min = ConfigManager.getItemsMinAmount();
				int max = ConfigManager.getItemsMaxAmount();
				int amountInt = min + random.nextInt(Math.max(1, max - min + 1));
				short productionID = possibleIDs.get(random.nextInt(possibleIDs.size()));
				String contractName = "[" + difficulty.displayName + "] Produce x" + amountInt + " " + ElementKeyMap.getInfo(productionID).getName();
				long reward = Math.max(ConfigManager.getItemsRewardMin(), (long) amountInt / ConfigManager.getItemsRewardDivisor());
				randomContract = new ItemsContract(FactionManager.TRAIDING_GUILD_ID, contractName, reward, productionID, amountInt, difficulty);
				break;
			case BOUNTY:
				randomContract = BountyContract.generateRandomMob(FactionManager.TRAIDING_GUILD_ID);
				break;
			case ESCORT:
				randomContract = generateRandomEscort(difficulty);
				break;
		}
		if(randomContract != null) addData(randomContract, true);
	}

	private EscortContract generateRandomEscort(ContractData.Difficulty difficulty) {
		int routeLength = ConfigManager.getEscortRouteMinLength() + new Random().nextInt(ConfigManager.getEscortRouteMaxLength() - ConfigManager.getEscortRouteMinLength() + 1);
		return buildEscortContract(FactionManager.TRAIDING_GUILD_ID, difficulty, routeLength, ConfigManager.getEscortCargoCount(), ConfigManager.getEscortBaseReward());
	}

	/**
	 * Builds (but does not register) an escort contract with a randomly-generated, star-safe route and
	 * the configured cargo/defender blueprint pools. Shared by random generation and player creation.
	 */
	public EscortContract buildEscortContract(int contractorId, ContractData.Difficulty difficulty, int routeLength, int cargoCount, long reward) {
		Random random = new Random();
		routeLength = Math.clamp(routeLength, ConfigManager.getEscortRouteMinLength(), ConfigManager.getEscortRouteMaxLength());
		cargoCount = Math.max(1, Math.min(10, cargoCount));
		List<Vector3i> route = new ArrayList<>();
		Vector3i current = SectorUtils.getRandomSector(10);
		route.add(current);
		for(int i = 1; i < routeLength; i++) {
			// Each waypoint is 2-4 sectors away from the previous, never inside a star's damage zone.
			Vector3i next;
			int attempts = 0;
			do {
				int dx = (random.nextInt(3) + 2) * (random.nextBoolean() ? 1 : -1);
				int dy = (random.nextInt(3)) * (random.nextBoolean() ? 1 : -1);
				int dz = (random.nextInt(3) + 2) * (random.nextBoolean() ? 1 : -1);
				next = new Vector3i(current.x + dx, current.y + dy, current.z + dz);
				attempts++;
			} while(SectorUtils.tooCloseToStar(next) && attempts < 20);
			// Fall back to a guaranteed-safe sector rather than accepting a star sector (was a sun-damage trap).
			if(SectorUtils.tooCloseToStar(next)) next = SectorUtils.getRandomSector(10);
			route.add(next);
			current = next;
		}

		List<String> cargoBPs = new ArrayList<>();
		for(String bp : ConfigManager.getEscortCargoBlueprintPool()) {
			String trimmed = bp.trim();
			if(!trimmed.isEmpty()) cargoBPs.add(trimmed);
		}
		if(cargoBPs.isEmpty()) cargoBPs.add("T180-18");

		List<String> defenderBPs = new ArrayList<>();
		for(String bp : ConfigManager.getEscortDefenderBlueprintPool()) {
			String trimmed = bp.trim();
			if(!trimmed.isEmpty()) defenderBPs.add(trimmed);
		}

		Vector3i dest = route.get(route.size() - 1);
		String contractName = "[" + difficulty.displayName + "] Escort " + cargoCount + " cargo ships to Sector " + dest;
		return new EscortContract(contractorId, contractName, reward, difficulty, route, cargoBPs, defenderBPs, cargoCount);
	}

	public boolean canCompleteAny(PlayerState player) {
		if(player == null) return false;
		return getClientCache().stream().anyMatch(contract -> contract.canComplete(player));
	}

	public List<? extends ContractData> getContractsOfType(Class<? extends ContractData> contractType, boolean isServer) {
		return getCache(isServer).stream().filter(contractType::isInstance).collect(Collectors.toList());
	}

	/**
	 * Server-side housekeeping: removes contracts whose contracting faction no longer exists.
	 * Replaces the old side-effecting {@code getContractor()} that deleted contracts mid-GUI-render.
	 */
	public void purgeOrphanedContracts() {
		for(ContractData contract : new ArrayList<>(getServerCache())) {
			if(contract.getContractorID() != 0 && contract.getContractor() == null) {
				AtlasContracts.getInstance().logInfo("Purging orphaned contract " + contract.getUUID() + " (faction " + contract.getContractorID() + " gone).");
				removeData(contract, true);
			}
		}
	}
}
