package atlas.exchange;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarMod;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.manager.PlayerActionRegistry;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
import atlas.exchange.element.ElementRegistry;
import atlas.exchange.gui.ExchangeDialog;
import atlas.exchange.tests.ExchangeDataTest;
import org.schema.game.server.test.TestRegistry;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

/**
 * AtlasExchange — player trading and blueprint marketplace.
 * Depends on AtlasCore.
 */
public class AtlasExchange extends StarMod implements IAtlasSubMod {

	private static AtlasExchange instance;

	/** Action ID for giving an item to a player on the server side. Registered in {@link #onAtlasCoreReady()}. */
	public static int GIVE_ITEM = -1;

	/** Action ID for crediting Gold Bars to a seller. Registered in {@link #onAtlasCoreReady()}. */
	public static int ADD_BARS = -1;

	public AtlasExchange() {
		instance = this;
	}

	public static AtlasExchange getInstance() {
		return instance;
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	@Override
	public void onEnable() {
		SubModRegistry.register(this);
	}

	@Override
	public void onDisable() {}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		ControlBindingData.load(this);
		ControlBindingData.registerBinding(this, "Open Exchange Menu", "Opens the Exchange menu.", 74 /* J */);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
	}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(ExchangeDataTest.class);
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public String getModId() { return "atlas_exchange"; }

	@Override
	public StarMod getMod() { return this; }

	@Override
	public void onAtlasCoreReady() {
		registerExchangeDataType();
		GIVE_ITEM = PlayerActionRegistry.register(args -> {
			// args: [playerName, itemId, count, meta]
			// TODO: implement server-side give item logic
		});
		ADD_BARS = PlayerActionRegistry.register(args -> {
			// args: [playerName, bronzeCount, silverCount, goldCount]
			// TODO: implement server-side bar credit logic
		});
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("EXCHANGE", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openExchange();
			}
			@Override
			public boolean isOccluded() { return false; }
		}, new GUIActivationHighlightCallback() {
			@Override
			public boolean isHighlighted(InputState s) { return false; }
			@Override
			public boolean isVisible(InputState s) { return true; }
			@Override
			public boolean isActive(InputState s) { return true; }
		});
	}

	@Override
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if(event.getPlayer().isOnServer()) {
			ExchangeDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
		}
	}

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Exchange Menu".equals(bindingName)) openExchange();
	}

	// ── private ───────────────────────────────────────────────────────────────

	private static void openExchange() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> new ExchangeDialog().activate());
	}

	private void registerExchangeDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() { return "EXCHANGE_DATA"; }

			@Override
			public atlas.core.data.SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return new ExchangeData(buf);
			}

			@Override
			public atlas.core.data.SerializableData deserializeJSON(org.json.JSONObject obj) {
				return new ExchangeData(obj);
			}

			@Override
			public atlas.core.data.DataManager<?> getManager(boolean server) {
				return ExchangeDataManager.getInstance(server);
			}
		});
	}
}
