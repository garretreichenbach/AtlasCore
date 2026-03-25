package atlas.banking.data;

import api.mod.config.PersistentObjectUtil;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages banking data for all players.
 *
 * @author TheDerpGamer
 */
public class BankingDataManager extends DataManager<BankingData> {

	private final Set<BankingData> clientCache = new HashSet<>();
	private static BankingDataManager serverInstance;
	private static BankingDataManager clientInstance;

	public static BankingDataManager getInstance(boolean server) {
		if(server) {
			if(serverInstance == null) serverInstance = new BankingDataManager();
			return serverInstance;
		} else {
			if(clientInstance == null) {
				clientInstance = new BankingDataManager();
				clientInstance.requestFromServer();
			}
			return clientInstance;
		}
	}

	@Override
	public Set<BankingData> getServerCache() {
		List<Object> objects = PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), BankingData.class);
		Set<BankingData> data = new HashSet<>();
		for(Object object : objects) data.add((BankingData) object);
		return data;
	}

	@Override
	public String getDataTypeName() {
		return "BANKING_DATA";
	}

	@Override
	public Set<BankingData> getClientCache() {
		return Collections.unmodifiableSet(clientCache);
	}

	@Override
	public void addToClientCache(BankingData data) {
		clientCache.add(data);
	}

	@Override
	public void removeFromClientCache(BankingData data) {
		clientCache.remove(data);
	}

	@Override
	public void updateClientCache(BankingData data) {
		clientCache.remove(data);
		clientCache.add(data);
	}

	@Override
	public void createMissingData(Object... args) {
		try {
			String playerName = (String) args[0];
			if(getFromPlayerName(playerName, true) == null) {
				BankingData data = new BankingData(playerName);
				PersistentObjectUtil.addObject(AtlasCore.getInstance().getSkeleton(), data);
				PersistentObjectUtil.save(AtlasCore.getInstance().getSkeleton());
			}
		} catch(Exception exception) {
			AtlasCore.getInstance().logException("An error occurred while initializing banking data", exception);
		}
	}

	public BankingData getFromPlayerName(String name, boolean server) {
		for(BankingData data : (server ? getServerCache() : getClientCache())) {
			if(data.getPlayerName().equals(name)) return data;
		}
		return null;
	}

	public void setPlayerCredits(String playerName, double amount, boolean server) {
		BankingData data = getFromPlayerName(playerName, server);
		if(data != null) {
			data.setStoredCredits(amount);
			updateData(data, server);
		}
	}
}
