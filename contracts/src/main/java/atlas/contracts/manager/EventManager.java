package atlas.contracts.manager;

import api.common.GameCommon;
import api.listener.Listener;
import api.listener.events.entity.SegmentControllerOverheatEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.mod.StarLoader;
import api.utils.game.SegmentControllerUtils;
import atlas.contracts.AtlasContracts;
import atlas.contracts.data.contract.BountyContract;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import org.json.JSONObject;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.server.ServerMessage;

import java.util.List;

public class EventManager {

	public static void initialize(AtlasContracts instance) {
		StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
			@Override
			public void onEvent(PlayerDeathEvent event) {
				if(event.getDamager() == null) return;
				PlayerState killer = resolveKiller(event.getDamager());
				if(killer == null) return;
				// A player can't collect a bounty by killing themselves.
				if(killer.getName().equals(event.getPlayer().getName())) return;
				ContractDataManager mgr = ContractDataManager.getInstance(event.getPlayer().isOnServer());
				List<BountyContract> bounties = (List<BountyContract>) mgr.getContractsOfType(BountyContract.class, event.getPlayer().isOnServer());
				for(BountyContract bounty : bounties) {
					if(bounty.getBountyType() != BountyContract.PLAYER) continue;
					JSONObject targetData = bounty.getTargetData();
					if(targetData == null || !targetData.has("player_name")) continue;
					if(!targetData.getString("player_name").equals(event.getPlayer().getName())) continue;
					// setKilledTarget already broadcasts the update to all clients on the server side.
					bounty.setKilledTarget(event.getPlayer().isOnServer(), true);
					killer.sendServerMessage(new ServerMessage(new String[]{Lng.str("You can now turn in the \"" + bounty.getName() + "\" contract!")}, ServerMessage.MESSAGE_TYPE_INFO));
					return;
				}
			}
		}, instance);

		StarLoader.registerListener(SegmentControllerOverheatEvent.class, new Listener<SegmentControllerOverheatEvent>() {
			@Override
			public void onEvent(SegmentControllerOverheatEvent event) {
				if(!event.isKilled()) return;
				// Escort cargo/defender destruction tracking.
				EscortManager.getInstance().onEntityDestroyed(event.getEntity());
				if(!ConfigManager.isAutoBountyEnabled()) return;
				SegmentController victim = event.getEntity();
				int victimFactionId = victim.getFactionId();
				if(!FactionManager.isNPCFaction(victimFactionId)) return;
				PlayerState killer = resolveKiller(event.getLastDamager());
				if(killer == null) return;
				int killCount = AggressionManager.getInstance().recordKill(killer.getName(), victimFactionId);
				if(killCount >= ConfigManager.getAutoBountyKillThreshold()) {
					AggressionManager.getInstance().resetKills(killer.getName(), victimFactionId);
					int bountyCount = AggressionManager.getInstance().incrementBountyCount(killer.getName(), victimFactionId);
					long escalatedReward = ConfigManager.getAutoBountyReward() * bountyCount; // Gold Bars
					ContractData.Difficulty difficulty = getEscalatedDifficulty(bountyCount);
					JSONObject targetData = new JSONObject();
					targetData.put("target_type", BountyContract.PLAYER);
					targetData.put("player_name", killer.getName());
					targetData.put("reward", escalatedReward);
					Faction npcFaction = GameCommon.getGameState().getFactionManager().getFaction(victimFactionId);
					String factionName = npcFaction != null ? npcFaction.getName() : "Unknown Faction";
					String contractName = "[" + difficulty.displayName + "] " + factionName + " Bounty: Kill " + killer.getName();
					BountyContract bounty = new BountyContract(victimFactionId, contractName, targetData, difficulty);
					ContractDataManager.getInstance(true).addData(bounty, true);
					AtlasContracts.getInstance().logInfo("Auto-bounty #" + bountyCount + " placed on " + killer.getName() + " by " + factionName + " (reward: " + escalatedReward + " Gold Bars)");
					killer.sendServerMessage(new ServerMessage(new String[]{factionName + " has placed a " + difficulty.displayName + " bounty on your head!"}, ServerMessage.MESSAGE_TYPE_WARNING));
				}
			}
		}, instance);

		StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
			@Override
			public void onEvent(MainWindowTabAddEvent event) {
				GUIManager.getInstance().createContractsShopTab(event);
			}
		}, instance);
	}

	/** Resolves the controlling player behind a damager (direct, controlled entity, or its rail root). */
	private static PlayerState resolveKiller(Damager damager) {
        switch (damager) {
            case null -> {
                return null;
            }
            case PlayerState playerState -> {
                return playerState;
            }
            case SegmentController sc -> {
                if (sc.isConrolledByActivePlayer()) {
                    List<PlayerState> players = SegmentControllerUtils.getAttachedPlayers(sc);
                    if (!players.isEmpty()) return players.getFirst();
                }
                SegmentController root = sc.railController.getRoot();
                if (root != null && root.isConrolledByActivePlayer()) {
                    List<PlayerState> players = SegmentControllerUtils.getAttachedPlayers(root);
                    if (!players.isEmpty()) return players.getFirst();
                }
            }
            default -> {
            }
        }
        return null;
	}

	private static ContractData.Difficulty getEscalatedDifficulty(int bountyCount) {
		if(bountyCount >= 4) return ContractData.Difficulty.EXTREME;
		if(bountyCount == 3) return ContractData.Difficulty.HARD;
		if(bountyCount == 2) return ContractData.Difficulty.NORMAL;
		return ContractData.Difficulty.EASY;
	}
}
