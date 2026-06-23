package atlas.contracts;

import api.common.GameClient;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.StarRunnable;
import atlas.contracts.commands.CompleteContractsCommand;
import atlas.contracts.commands.ListContractsCommand;
import atlas.contracts.commands.PurgeContractsCommand;
import atlas.contracts.commands.RandomContractsCommand;
import atlas.contracts.data.contract.BountyContract;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import atlas.contracts.data.contract.ItemsContract;
import atlas.contracts.data.contract.active.ActiveContractData;
import atlas.contracts.data.contract.active.ActiveContractDataManager;
import atlas.contracts.gui.contract.playercontractlist.PlayerContractsDialog;
import atlas.contracts.manager.ConfigManager;
import atlas.contracts.manager.EventManager;
import atlas.contracts.manager.GUIManager;
import atlas.contracts.tests.ContractDataTest;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataManager;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.SerializableData;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import atlas.core.manager.PlayerActionRegistry;
import atlas.core.utils.GoldBarUtils;
import org.json.JSONObject;
import org.schema.game.client.view.mainmenu.GuidesRegistry;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.server.test.TestRegistry;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.network.server.ServerMessage;

/**
 * AtlasContracts — completable bounty, item, and escort contracts paid in Gold Bars.
 * Depends on AtlasCore. Ported from the standalone Contracts mod onto the shared Atlas framework.
 */
public class AtlasContracts extends StarMod implements IAtlasSubMod {

	// Server-side action keys (registered in onAtlasCoreReady).
	public static String ACCEPT;
	public static String CANCEL_CLAIM;
	public static String COMPLETE;
	public static String CREATE_BOUNTY;
	public static String CREATE_ITEMS;
	public static String CREATE_ESCORT;
	public static String CANCEL_CONTRACT;

	private static AtlasContracts instance;

	public AtlasContracts() {
		instance = this;
	}

	public static AtlasContracts getInstance() {
		return instance;
	}

	private static void openContracts() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> {
			GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
			new PlayerContractsDialog().activate();
		});
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	@Override
	public void onEnable() {
		instance = this;
		SubModRegistry.register(this);
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		GUIManager.initialize();
		registerCommands();
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onServerCreated(ServerInitializeEvent event) {
		if(ConfigManager.isAutoGenerateContracts()) {
			new StarRunnable() {
				@Override
				public void run() {
					ContractDataManager mgr = ContractDataManager.getInstance(true);
					mgr.purgeOrphanedContracts();
					if(mgr.getCache(true).size() < ConfigManager.getMaxAutoGenerateContracts()) {
						mgr.generateRandomContract();
					}
				}
			}.runTimer(this, ConfigManager.getAutoGenerateContractCheckTimer());
		}
		// Escort mission update timer.
		new StarRunnable() {
			@Override
			public void run() {
				atlas.contracts.manager.EscortManager.getInstance().update();
			}
		}.runTimer(this, ConfigManager.getEscortUpdateInterval());
	}

	private void registerCommands() {
		StarLoader.registerCommand(new RandomContractsCommand());
		StarLoader.registerCommand(new PurgeContractsCommand());
		StarLoader.registerCommand(new CompleteContractsCommand());
		StarLoader.registerCommand(new ListContractsCommand());
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public String getModId() {
		return "atlas_contracts";
	}

	@Override
	public StarMod getMod() {
		return this;
	}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(ContractDataTest.class);
	}

	@Override
	public void onRegisterGuides(GuidesRegistry.ModGuideRegistrar registrar) {
		registrar.registerFromResource("atlas", "Atlas", "AtlasContracts", "guides/atlas-contracts.md", this);
	}

	@Override
	public void onAtlasCoreReady() {
		registerDataTypes();
		registerActions();
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("CONTRACTS", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openContracts();
			}

			@Override
			public boolean isOccluded() {
				return false;
			}
		}, new GUIActivationHighlightCallback() {
			@Override
			public boolean isHighlighted(InputState s) {
				return ContractDataManager.getInstance(false).canCompleteAny(GameClient.getClientPlayerState());
			}

			@Override
			public boolean isVisible(InputState s) {
				return true;
			}

			@Override
			public boolean isActive(InputState s) {
				return true;
			}
		});
	}

	@Override
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if(event.getPlayer().isOnServer()) {
			PlayerState player = event.getPlayer().getOwnerState();
			ContractDataManager.getInstance(true).sendAllDataToPlayer(player);
			ActiveContractDataManager.getInstance(true).sendAllDataToPlayer(player);
			// Deliver any Gold Bars owed (shared pending pool); idempotent if Exchange already delivered.
			GoldBarUtils.deliverPending(player);
		}
	}

	// ── data types & actions ────────────────────────────────────────────────

	private void registerDataTypes() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() {
				return ContractData.DATA_TYPE;
			}

			@Override
			public SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return ContractData.readContract(buf);
			}

			@Override
			public SerializableData deserializeJSON(JSONObject obj) {
				return ContractData.readContract(obj);
			}

			@Override
			public DataManager<?> getManager(boolean server) {
				return ContractDataManager.getInstance(server);
			}
		});

		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() {
				return ActiveContractData.DATA_TYPE;
			}

			@Override
			public SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return new ActiveContractData(buf);
			}

			@Override
			public SerializableData deserializeJSON(JSONObject obj) {
				return new ActiveContractData(obj);
			}

			@Override
			public DataManager<?> getManager(boolean server) {
				return ActiveContractDataManager.getInstance(server);
			}
		});
	}

	private void registerActions() {
		ACCEPT = PlayerActionRegistry.register("atlas_contracts:accept", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			ContractData contract = ContractDataManager.getInstance(true).getFromUUID(args[0], true);
			if(contract == null) return;
			boolean ok = ActiveContractDataManager.getInstance(true).acceptContract(contract, sender);
			sender.sendServerMessage(new ServerMessage(new String[]{ok ? "Contract accepted." : "Could not accept contract (already claimed, at your limit, or you must be in the start sector for escorts)."}, ok ? ServerMessage.MESSAGE_TYPE_INFO : ServerMessage.MESSAGE_TYPE_ERROR));
		});

		CANCEL_CLAIM = PlayerActionRegistry.register("atlas_contracts:cancel_claim", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			ContractData contract = ContractDataManager.getInstance(true).getFromUUID(args[0], true);
			if(contract == null) return;
			ActiveContractDataManager.getInstance(true).cancelClaim(contract, sender, true);
		});

		COMPLETE = PlayerActionRegistry.register("atlas_contracts:complete", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			ContractData contract = ContractDataManager.getInstance(true).getFromUUID(args[0], true);
			if(contract == null) return;
			if(!contract.getClaimants().containsKey(sender.getName())) return; // only a claimant can complete
			if(!contract.canComplete(sender)) {
				sender.sendServerMessage(new ServerMessage(new String[]{"This contract isn't ready to turn in yet."}, ServerMessage.MESSAGE_TYPE_WARNING));
				return;
			}
			ActiveContractData active = ActiveContractDataManager.getInstance(true).getFromContractUUID(contract.getUUID(), sender.getName(), true);
			if(active != null) {
				ActiveContractDataManager.getInstance(true).completeContract(active, true);
			} else {
				PlayerData pd = PlayerDataManager.getInstance(true).getFromName(sender.getName(), true);
				ContractDataManager.completeContract(pd, contract);
			}
			sender.sendServerMessage(new ServerMessage(new String[]{"Contract completed! Your Gold Bars are on the way."}, ServerMessage.MESSAGE_TYPE_INFO));
		});

		CREATE_BOUNTY = PlayerActionRegistry.register("atlas_contracts:create_bounty", (args, sender) -> {
			// args: [targetPlayerName, rewardBars]
			if(sender == null || args.length < 2) return;
			if(sender.getFactionId() == 0 && !sender.isAdmin()) {
				sendErr(sender, "You must be in a faction to post a bounty.");
				return;
			}
			String targetName = args[0].trim();
			int reward = parseInt(args[1], -1);
			if(targetName.isEmpty() || reward < ConfigManager.getBountyMinReward()) {
				sendErr(sender, "Invalid bounty (minimum " + ConfigManager.getBountyMinReward() + " Gold Bars).");
				return;
			}
			if(targetName.equalsIgnoreCase(sender.getName())) {
				sendErr(sender, "You cannot place a bounty on yourself.");
				return;
			}
			PlayerData targetData = PlayerDataManager.getInstance(true).getFromName(targetName, true);
			if(targetData == null) {
				sendErr(sender, "No such player: " + targetName);
				return;
			}
			if(!sender.isAdmin()) {
				if(targetData.getFactionId() != 0 && targetData.getFactionId() == sender.getFactionId()) {
					sendErr(sender, "You cannot place a bounty on a member of your own faction.");
					return;
				}
				if(sender.getFactionId() != 0 && targetData.getFactionId() != 0
						&& api.common.GameCommon.getGameState().getFactionManager().getRelation(sender.getFactionId(), targetData.getFactionId()) == FactionRelation.RType.FRIEND) {
					sendErr(sender, "You cannot place a bounty on an ally.");
					return;
				}
			}
			if(!GoldBarUtils.deduct(sender, reward)) {
				sendErr(sender, "You don't have " + reward + " Gold Bars.");
				return;
			}
			JSONObject td = new JSONObject();
			td.put("target_type", BountyContract.PLAYER);
			td.put("player_name", targetName);
			td.put("reward", (long) reward);
			String name = "[Bounty] Kill " + targetName;
			BountyContract bounty = new BountyContract(sender.getFactionId(), name, td, ContractData.Difficulty.NORMAL);
			ContractDataManager.getInstance(true).addData(bounty, true);
			sender.sendServerMessage(new ServerMessage(new String[]{"Posted a " + reward + " Gold Bar bounty on " + targetName + "."}, ServerMessage.MESSAGE_TYPE_INFO));
		});

		CREATE_ITEMS = PlayerActionRegistry.register("atlas_contracts:create_items", (args, sender) -> {
			// args: [itemId, amount, rewardBars]
			if(sender == null || args.length < 3) return;
			if(sender.getFactionId() == 0 && !sender.isAdmin()) {
				sendErr(sender, "You must be in a faction to post a contract.");
				return;
			}
			short itemId = (short) parseInt(args[0], -1);
			int amount = parseInt(args[1], -1);
			int reward = parseInt(args[2], -1);
			if(itemId <= 0 || !ElementKeyMap.exists(itemId) || amount <= 0 || reward < ConfigManager.getBountyMinReward()) {
				sendErr(sender, "Invalid items contract.");
				return;
			}
			if(!GoldBarUtils.deduct(sender, reward)) {
				sendErr(sender, "You don't have " + reward + " Gold Bars.");
				return;
			}
			String name = "[Items] Deliver x" + amount + " " + ElementKeyMap.getInfo(itemId).getName();
			ItemsContract contract = new ItemsContract(sender.getFactionId(), name, reward, itemId, amount, ContractData.Difficulty.NORMAL);
			ContractDataManager.getInstance(true).addData(contract, true);
			sender.sendServerMessage(new ServerMessage(new String[]{"Posted an items contract for " + reward + " Gold Bars."}, ServerMessage.MESSAGE_TYPE_INFO));
		});

		CREATE_ESCORT = PlayerActionRegistry.register("atlas_contracts:create_escort", (args, sender) -> {
			// args: [routeLength, cargoCount, rewardBars]
			if(sender == null || args.length < 3) return;
			if(!ConfigManager.isEscortEnabled()) {
				sendErr(sender, "Escort contracts are currently disabled.");
				return;
			}
			if(sender.getFactionId() == 0 && !sender.isAdmin()) {
				sendErr(sender, "You must be in a faction to post a contract.");
				return;
			}
			int routeLength = parseInt(args[0], ConfigManager.getEscortRouteMinLength());
			int cargoCount = parseInt(args[1], ConfigManager.getEscortCargoCount());
			int reward = parseInt(args[2], -1);
			if(reward < ConfigManager.getBountyMinReward()) {
				sendErr(sender, "Invalid escort contract (minimum " + ConfigManager.getBountyMinReward() + " Gold Bars).");
				return;
			}
			if(!GoldBarUtils.deduct(sender, reward)) {
				sendErr(sender, "You don't have " + reward + " Gold Bars.");
				return;
			}
			ContractData escort = ContractDataManager.getInstance(true).buildEscortContract(sender.getFactionId(), ContractData.Difficulty.NORMAL, routeLength, cargoCount, reward);
			ContractDataManager.getInstance(true).addData(escort, true);
			sender.sendServerMessage(new ServerMessage(new String[]{"Posted an escort contract for " + reward + " Gold Bars."}, ServerMessage.MESSAGE_TYPE_INFO));
		});

		CANCEL_CONTRACT = PlayerActionRegistry.register("atlas_contracts:cancel_contract", (args, sender) -> {
			// args: [contractUUID]. Contractor faction member or admin may cancel an unclaimed contract; reward is refunded to sender.
			if(sender == null || args.length < 1) return;
			ContractData contract = ContractDataManager.getInstance(true).getFromUUID(args[0], true);
			if(contract == null) return;
			boolean authorized = sender.isAdmin() || (sender.getFactionId() != 0 && sender.getFactionId() == contract.getContractorID());
			if(!authorized) {
				sendErr(sender, "You can't cancel that contract.");
				return;
			}
			if(!contract.getClaimants().isEmpty()) {
				sendErr(sender, "That contract has already been claimed and can't be cancelled.");
				return;
			}
			ContractDataManager.getInstance(true).removeData(contract, true);
			GoldBarUtils.pay(sender.getName(), (int) contract.getReward());
			sender.sendServerMessage(new ServerMessage(new String[]{"Contract cancelled; " + contract.getReward() + " Gold Bars refunded."}, ServerMessage.MESSAGE_TYPE_INFO));
		});
	}

	private static void sendErr(PlayerState player, String message) {
		player.sendServerMessage(new ServerMessage(new String[]{message}, ServerMessage.MESSAGE_TYPE_ERROR));
	}

	private static int parseInt(String s, int def) {
		try {
			return Integer.parseInt(s.trim());
		} catch(Exception e) {
			return def;
		}
	}
}
