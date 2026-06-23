package atlas.contracts.manager;

import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player kill counts against NPC factions, with time-based decay. Each kill is recorded
 * with a timestamp; kills older than the configured decay timer are pruned on access. Data is
 * persisted through the shared AtlasCore {@link PlayerData}.
 */
public class AggressionManager {

	private static AggressionManager instance;

	// playerName -> (factionId -> list of kill timestamps)
	private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, List<Long>>> aggressionMap = new ConcurrentHashMap<>();

	// playerName -> (factionId -> number of bounties placed so far)
	private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> bountyCountMap = new ConcurrentHashMap<>();

	// Tracks which players have been loaded from persistent storage
	private final Set<String> loadedPlayers = ConcurrentHashMap.newKeySet();

	public static AggressionManager getInstance() {
		if(instance == null) instance = new AggressionManager();
		return instance;
	}

	public void loadFromPlayerData(PlayerData playerData) {
		String playerName = playerData.getName();
		if(!playerData.getAggressionKills().isEmpty()) {
			ConcurrentHashMap<Integer, List<Long>> killMap = new ConcurrentHashMap<>();
			for(Map.Entry<Integer, List<Long>> entry : playerData.getAggressionKills().entrySet()) {
				killMap.put(entry.getKey(), Collections.synchronizedList(new ArrayList<>(entry.getValue())));
			}
			aggressionMap.put(playerName, killMap);
		}
		if(!playerData.getBountyCounts().isEmpty()) {
			bountyCountMap.put(playerName, new ConcurrentHashMap<>(playerData.getBountyCounts()));
		}
	}

	public void saveToPlayerData(PlayerData playerData) {
		String playerName = playerData.getName();
		ConcurrentHashMap<Integer, List<Long>> killMap = aggressionMap.get(playerName);
		playerData.getAggressionKills().clear();
		if(killMap != null) {
			for(Map.Entry<Integer, List<Long>> entry : killMap.entrySet()) {
				pruneExpired(entry.getValue());
				if(!entry.getValue().isEmpty()) {
					playerData.getAggressionKills().put(entry.getKey(), new ArrayList<>(entry.getValue()));
				}
			}
		}
		ConcurrentHashMap<Integer, Integer> bountyMap = bountyCountMap.get(playerName);
		playerData.getBountyCounts().clear();
		if(bountyMap != null) playerData.getBountyCounts().putAll(bountyMap);
	}

	private void ensureLoaded(String playerName) {
		if(loadedPlayers.add(playerName)) {
			PlayerData playerData = PlayerDataManager.getInstance(true).getFromName(playerName, true);
			if(playerData != null) loadFromPlayerData(playerData);
			else loadedPlayers.remove(playerName); // not ready yet — retry on next access
		}
	}

	public int recordKill(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>());
		List<Long> kills = playerMap.computeIfAbsent(factionId, k -> Collections.synchronizedList(new ArrayList<>()));
		kills.add(System.currentTimeMillis());
		pruneExpired(kills);
		persistPlayer(playerName);
		return kills.size();
	}

	public int getKillCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.get(playerName);
		if(playerMap == null) return 0;
		List<Long> kills = playerMap.get(factionId);
		if(kills == null) return 0;
		pruneExpired(kills);
		return kills.size();
	}

	public void resetKills(String playerName, int factionId) {
		ConcurrentHashMap<Integer, List<Long>> playerMap = aggressionMap.get(playerName);
		if(playerMap != null) playerMap.remove(factionId);
		persistPlayer(playerName);
	}

	public int incrementBountyCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, Integer> playerMap = bountyCountMap.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>());
		int count = playerMap.merge(factionId, 1, Integer::sum);
		persistPlayer(playerName);
		return count;
	}

	public int getBountyCount(String playerName, int factionId) {
		ensureLoaded(playerName);
		ConcurrentHashMap<Integer, Integer> playerMap = bountyCountMap.get(playerName);
		if(playerMap == null) return 0;
		return playerMap.getOrDefault(factionId, 0);
	}

	private void persistPlayer(String playerName) {
		PlayerData playerData = PlayerDataManager.getInstance(true).getFromName(playerName, true);
		if(playerData != null) {
			saveToPlayerData(playerData);
			PlayerDataManager.getInstance(true).updateData(playerData, true);
		}
	}

	private void pruneExpired(List<Long> kills) {
		long cutoff = System.currentTimeMillis() - ConfigManager.getAutoBountyDecayTimer();
		synchronized(kills) {
			kills.removeIf(ts -> ts < cutoff);
		}
	}
}
