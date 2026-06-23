package atlas.contracts.data.contract.active;

import api.mod.config.PersistentObjectUtil;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import atlas.contracts.data.contract.EscortContract;
import atlas.contracts.manager.ConfigManager;
import atlas.contracts.manager.EscortManager;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import org.schema.game.common.data.player.PlayerState;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActiveContractDataManager extends DataManager<ActiveContractData> {

	private final Set<ActiveContractData> clientCache = ConcurrentHashMap.newKeySet();
	private static ActiveContractDataManager serverInstance;
	private static ActiveContractDataManager clientInstance;

	public static ActiveContractDataManager getInstance(boolean server) {
		if(server) {
			if(serverInstance == null) serverInstance = new ActiveContractDataManager();
			return serverInstance;
		} else {
			if(clientInstance == null) {
				clientInstance = new ActiveContractDataManager();
				clientInstance.requestFromServer();
			}
			return clientInstance;
		}
	}

	@Override
	public Set<ActiveContractData> getServerCache() {
		Set<ActiveContractData> data = new HashSet<>();
		for(Object o : PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), ActiveContractData.class)) data.add((ActiveContractData) o);
		return data;
	}

	@Override
	public String getDataTypeName() {
		return ActiveContractData.DATA_TYPE;
	}

	@Override
	public Set<ActiveContractData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(ActiveContractData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(ActiveContractData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(ActiveContractData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {
	}

	/**
	 * Server-authoritative accept. The escort start-sector check now runs here (previously client-only),
	 * so a laggy client cannot accept an escort from the wrong sector and spawn phantom cargo.
	 */
	public boolean acceptContract(ContractData contract, PlayerState player) {
		if(contract == null || player == null) return false;
		boolean server = player.isOnServer();
		PlayerData playerData = PlayerDataManager.getInstance(server).getFromName(player.getName(), server);
		if(playerData == null) return false;
		if(playerData.getContracts().size() >= ConfigManager.getClientMaxActiveContracts()) return false;
		if(playerData.getContracts().contains(contract.getUUID())) return false;
		if(contract instanceof EscortContract) {
			EscortContract escort = (EscortContract) contract;
			if(!player.getCurrentSector().equals(escort.getStartSector())) return false;
		}
		ActiveContractData activeContract = new ActiveContractData(contract, player.getName());
		addData(activeContract, server);
		playerData.getContracts().add(contract.getUUID());
		PlayerDataManager.getInstance(server).updateData(playerData, server);
		contract.getClaimants().put(player.getName(), System.currentTimeMillis());
		ContractDataManager.getInstance(server).updateData(contract, server);
		if(server && contract instanceof EscortContract) {
			EscortManager.getInstance().startEscort((EscortContract) contract, player);
		}
		return true;
	}

	public void completeContract(ActiveContractData activeContract, boolean server) {
		ContractData contract = activeContract.getTargetContract(server);
		if(contract == null) {
			removeData(activeContract, server);
			return;
		}
		PlayerData playerData = PlayerDataManager.getInstance(server).getFromName(activeContract.getClaimer(), server);
		if(playerData == null) return;
		if(contract instanceof EscortContract) {
			EscortManager.getInstance().removeSession(contract.getUUID());
		}
		ContractDataManager.completeContract(playerData, contract);
		removeData(activeContract, server);
	}

	/** Removes a player's claim on a contract without completing it (cancel claim). */
	public void cancelClaim(ContractData contract, PlayerState player, boolean server) {
		if(contract == null || player == null) return;
		contract.getClaimants().remove(player.getName());
		ContractDataManager.getInstance(server).updateData(contract, server);
		PlayerData playerData = PlayerDataManager.getInstance(server).getFromName(player.getName(), server);
		if(playerData != null) {
			playerData.getContracts().remove(contract.getUUID());
			PlayerDataManager.getInstance(server).updateData(playerData, server);
		}
		ActiveContractData active = getFromContractUUID(contract.getUUID(), player.getName(), server);
		if(active != null) {
			if(contract instanceof EscortContract) EscortManager.getInstance().removeSession(contract.getUUID());
			removeData(active, server);
		}
	}

	public List<ActiveContractData> getContractsForPlayer(String playerName, boolean server) {
		return getCache(server).stream().filter(data -> data.getClaimer().equals(playerName)).collect(Collectors.toList());
	}

	public ActiveContractData getFromContractUUID(String contractUUID, String playerName, boolean server) {
		return getCache(server).stream().filter(data -> data.getTargetContractID().equals(contractUUID) && data.getClaimer().equals(playerName)).findFirst().orElse(null);
	}
}
