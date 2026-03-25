package atlas.buildsectors;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarMod;
import atlas.buildsectors.data.BuildSectorData;
import atlas.buildsectors.data.BuildSectorDataManager;
import atlas.buildsectors.gui.BuildSectorDialog;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.manager.PlayerActionRegistry;
import atlas.core.network.PlayerActionCommandPacket;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

/**
 * AtlasBuildSectors — private player creative building sectors.
 * Depends on AtlasCore.
 */
public class AtlasBuildSectors extends StarMod implements IAtlasSubMod {

	private static AtlasBuildSectors instance;

	/** Distance from the origin at which build sectors are spawned. */
	public static final int BUILD_SECTOR_DISTANCE = 1_000_000;

	public static int ENTER_BUILD_SECTOR;
	public static int LEAVE_BUILD_SECTOR;

	public AtlasBuildSectors() {
		instance = this;
	}

	public static AtlasBuildSectors getInstance() {
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
		ControlBindingData.registerBinding(this, "Open Build Sector Menu", "Opens the Build Sector menu.", 55 /* G */);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public String getModId() { return "atlas_buildsectors"; }

	@Override
	public StarMod getMod() { return this; }

	@Override
	public void onAtlasCoreReady() {
		registerBuildSectorDataType();

		ENTER_BUILD_SECTOR = PlayerActionRegistry.register(args -> {
			if(args.length >= 1) {
				String playerName = args[0];
				BuildSectorDataManager.getInstance(true).enterBuildSector(playerName);
			}
		});

		LEAVE_BUILD_SECTOR = PlayerActionRegistry.register(args -> {
			if(args.length >= 1) {
				String playerName = args[0];
				BuildSectorDataManager.getInstance(true).leaveBuildSector(playerName);
			}
		});
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("BUILD SECTOR", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openBuildSector();
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
			BuildSectorDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
		}
	}

	@Override
	public void onPlayerJoinWorld(PlayerJoinWorldEvent event) {
		BuildSectorDataManager.getInstance(true).createMissingData(event.getPlayerState().getName());
	}

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Build Sector Menu".equals(bindingName)) openBuildSector();
	}

	// ── private ───────────────────────────────────────────────────────────────

	private static void openBuildSector() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> new BuildSectorDialog().activate());
	}

	private void registerBuildSectorDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() { return "BUILD_SECTOR_DATA"; }

			@Override
			public atlas.core.data.SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return new BuildSectorData(buf);
			}

			@Override
			public atlas.core.data.SerializableData deserializeJSON(org.json.JSONObject obj) {
				BuildSectorData data = new BuildSectorData();
				data.deserialize(obj);
				return data;
			}

			@Override
			public atlas.core.data.DataManager<?> getManager(boolean server) {
				return BuildSectorDataManager.getInstance(server);
			}
		});
	}
}
