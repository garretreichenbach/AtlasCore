package atlas.banking;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarMod;
import atlas.banking.data.BankingData;
import atlas.banking.data.BankingDataManager;
import atlas.banking.element.ElementRegistry;
import atlas.banking.gui.BankingDialog;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.manager.PlayerActionRegistry;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

/**
 * AtlasBanking — player banking, economy, and prize bar system.
 * Depends on AtlasCore.
 */
public class AtlasBanking extends StarMod implements IAtlasSubMod {

	public static int SET_CREDITS;
	public static int ADD_BARS;
	private static AtlasBanking instance;

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
		ControlBindingData.load(this);
		ControlBindingData.registerBinding(this, "Open Banking Menu", "Opens the Banking menu.", 78 /* N */);
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
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

		SET_CREDITS = PlayerActionRegistry.register(args -> {
			if(args.length >= 2) {
				String playerName = args[0];
				double amount = Double.parseDouble(args[1]);
				BankingDataManager.getInstance(true).setPlayerCredits(playerName, amount, true);
			}
		});

		ADD_BARS = PlayerActionRegistry.register(args -> {
			// TODO: Add prize bar implementation when element system is wired up
		});
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

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Banking Menu".equals(bindingName)) openBanking();
	}

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
