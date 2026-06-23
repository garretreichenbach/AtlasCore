package atlas.banking;

import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import atlas.banking.data.BankingData;
import atlas.banking.data.BankingDataManager;
import atlas.banking.gui.BankingDialog;
import atlas.banking.tests.BankingDataTest;
import org.schema.game.client.view.mainmenu.GuidesRegistry;
import org.schema.game.server.test.TestRegistry;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.manager.PlayerActionRegistry;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.input.KeyboardContext;
import org.schema.schine.input.KeyboardMappings;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

/**
 * AtlasBanking — player banking, economy, and prize bar system.
 * Depends on AtlasCore.
 */
public class AtlasBanking extends StarMod implements IAtlasSubMod {

	public static String SET_CREDITS;
	public static String DEPOSIT;
	public static String WITHDRAW;
	public static String TRANSFER;
	private static AtlasBanking instance;
	private KeyboardMappings openBankingKey;

	public AtlasBanking() {
		instance = this;
	}

	public static AtlasBanking getInstance() {
		return instance;
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	private static void openBanking() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> new BankingDialog().activate());
	}

	@Override
	public void onEnable() {
		SubModRegistry.register(this);
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		// Registered through StarMade's official keybinding API; appears in the
		// in-game controls menu and is player-remappable.
		openBankingKey = KeyboardMappings.registerMapping(this, "Open Banking Menu", GLFW.GLFW_KEY_N, KeyboardContext.GENERAL);
		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent keyEvent) {
				if(keyEvent.isKeyDown() && keyEvent.isMapping(openBankingKey)) openBanking();
			}
		}, this);
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public void onBlockConfigLoad(BlockConfig config) {}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(BankingDataTest.class);
	}

	@Override
	public void onRegisterGuides(GuidesRegistry.ModGuideRegistrar registrar) {
		registrar.registerFromResource("atlas", "Atlas", "AtlasBanking", "guides/atlas-banking.md", this);
	}

	@Override
	public String getModId() {
		return "atlas_banking";
	}

	@Override
	public StarMod getMod() {
		return this;
	}

	@Override
	public void onAtlasCoreReady() {
		registerBankingDataType();

		SET_CREDITS = PlayerActionRegistry.register("atlas_banking:set_credits", (args, sender) -> {
			// Server-only, admin-only action. Target taken from args is acceptable here
			// ONLY because the action is gated behind an authenticated admin check.
			if(sender == null) return;
			if(!sender.isAdmin()) {
				logWarning("[Banking] SET_CREDITS: non-admin player " + sender.getName() + " attempted to set credits.");
				return;
			}
			if(args.length < 2) return;
			String playerName = args[0];
			try {
				long amount = Long.parseLong(args[1]);
				if(amount < 0) return;
				if(BankingDataManager.getInstance(true).getFromPlayerName(playerName, true) == null) {
					logWarning("[Banking] SET_CREDITS: no banking account for '" + playerName + "'");
					return;
				}
				BankingDataManager.getInstance(true).setPlayerCredits(playerName, amount, true);
			} catch(NumberFormatException e) {
				logWarning("[Banking] SET_CREDITS: invalid amount value '" + args[1] + "' from " + sender.getName());
			}
		});

		DEPOSIT = PlayerActionRegistry.register("atlas_banking:deposit", (args, sender) -> {
			// Server-only. args[0] = amount. Moves wallet credits -> bank for the SENDER.
			if(sender == null || args.length < 1) return;
			long amount = parsePositiveLong(args[0]);
			if(amount <= 0) return;
			if(sender.getCredits() < amount) {
				logWarning("[Banking] DEPOSIT: " + sender.getName() + " has insufficient wallet credits");
				return;
			}
			BankingData data = BankingDataManager.getInstance(true).getFromPlayerName(sender.getName(), true);
			if(data == null) return;
			sender.modCreditsServer(-amount); // authoritative wallet debit
			data.setStoredCredits(data.getStoredCredits() + amount);
			data.addTransaction(new BankingData.BankTransactionData(amount, data.getUUID(), data.getUUID(), "Deposit", "", BankingData.BankTransactionData.TransactionType.DEPOSIT));
			BankingDataManager.getInstance(true).updateData(data, true);
		});

		WITHDRAW = PlayerActionRegistry.register("atlas_banking:withdraw", (args, sender) -> {
			// Server-only. args[0] = amount. Moves bank credits -> wallet for the SENDER.
			if(sender == null || args.length < 1) return;
			long amount = parsePositiveLong(args[0]);
			if(amount <= 0) return;
			BankingData data = BankingDataManager.getInstance(true).getFromPlayerName(sender.getName(), true);
			if(data == null) return;
			if(data.getStoredCredits() < amount) {
				logWarning("[Banking] WITHDRAW: " + sender.getName() + " has insufficient bank balance");
				return;
			}
			data.setStoredCredits(data.getStoredCredits() - amount);
			sender.modCreditsServer(amount); // authoritative wallet credit
			data.addTransaction(new BankingData.BankTransactionData(amount, data.getUUID(), data.getUUID(), "Withdraw", "", BankingData.BankTransactionData.TransactionType.WITHDRAW));
			BankingDataManager.getInstance(true).updateData(data, true);
		});

		TRANSFER = PlayerActionRegistry.register("atlas_banking:transfer", (args, sender) -> {
			// Server-only. args[0] = targetName, args[1] = amount, args[2] = subject, args[3] = message.
			// Source account is ALWAYS the authenticated sender — never an arg.
			if(sender == null || args.length < 2) return;
			String targetName = args[0];
			long amount = parsePositiveLong(args[1]);
			if(amount <= 0) return;
			String subject = args.length > 2 ? args[2] : "";
			String message = args.length > 3 ? args[3] : "";
			if(sender.getName().equals(targetName)) return; // no self-transfer

			BankingDataManager mgr = BankingDataManager.getInstance(true);
			BankingData from = mgr.getFromPlayerName(sender.getName(), true);
			BankingData to = mgr.getFromPlayerName(targetName, true);
			if(from == null || to == null) {
				logWarning("[Banking] TRANSFER: missing account (from=" + sender.getName() + ", to=" + targetName + ")");
				return;
			}
			if(from.getStoredCredits() < amount) {
				logWarning("[Banking] TRANSFER: " + sender.getName() + " has insufficient bank balance");
				return;
			}
			from.setStoredCredits(from.getStoredCredits() - amount);
			to.setStoredCredits(to.getStoredCredits() + amount);
			BankingData.BankTransactionData record = new BankingData.BankTransactionData(amount, from.getUUID(), to.getUUID(), subject, message, BankingData.BankTransactionData.TransactionType.TRANSFER);
			from.addTransaction(record);
			to.addTransaction(record);
			mgr.updateData(from, true);
			mgr.updateData(to, true);
		});
	}

	/** Parses a non-negative {@code long}; returns -1 on any parse failure. */
	private static long parsePositiveLong(String s) {
		try {
			return Long.parseLong(s.trim());
		} catch(NumberFormatException e) {
			return -1;
		}
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("BANKING", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openBanking();
			}

			@Override
			public boolean isOccluded() {
				return false;
			}
		}, new GUIActivationHighlightCallback() {
			@Override
			public boolean isHighlighted(InputState s) {
				return false;
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
			BankingDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
		}
	}

	@Override
	public void onPlayerJoinWorld(PlayerJoinWorldEvent event) {
		BankingDataManager.getInstance(true).createMissingData(event.getPlayerState().getName());
	}

	// ── private ───────────────────────────────────────────────────────────────

	private void registerBankingDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() {
				return "BANKING_DATA";
			}

			@Override
			public atlas.core.data.SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return new BankingData(buf);
			}

			@Override
			public atlas.core.data.SerializableData deserializeJSON(org.json.JSONObject obj) {
				return new BankingData(obj);
			}

			@Override
			public atlas.core.data.DataManager<?> getManager(boolean server) {
				return BankingDataManager.getInstance(server);
			}
		});
	}
}
