package atlas.buildsectors;

import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.draw.RegisterWorldDrawersEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.PacketReadBuffer;
import atlas.buildsectors.data.BuildSectorData;
import atlas.buildsectors.data.BuildSectorDataManager;
import atlas.buildsectors.drawer.BuildSectorHudDrawer;
import atlas.buildsectors.gui.BuildSectorDialog;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataManager;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.SerializableData;
import atlas.core.manager.PlayerActionRegistry;
import atlas.core.utils.EntityUtils;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Sector;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AtlasBuildSectors — private player creative building sectors.
 * Depends on AtlasCore.
 */
public class AtlasBuildSectors extends StarMod implements IAtlasSubMod {

	/**
	 * Distance from origin at which build sectors are placed.
	 */
	public static final int BUILD_SECTOR_DISTANCE = 1000000;

	public static int ENTER_BUILD_SECTOR;
	public static int LEAVE_BUILD_SECTOR;
	public static int TOGGLE_AI;
	public static int SET_INVULNERABLE;
	public static int DELETE_ENTITY;

	private static AtlasBuildSectors instance;

	/**
	 * Remembers the sector each player was in before entering a build sector, so
	 * LEAVE_BUILD_SECTOR can warp them back. Session-only; lost on server restart.
	 */
	private final Map<String, Vector3i> savedPlayerSectors = new ConcurrentHashMap<>();

	public AtlasBuildSectors() {
		instance = this;
	}

	public static AtlasBuildSectors getInstance() {
		return instance;
	}

	// ── sector save/restore helpers ──────────────────────────────────────────

	private static void openBuildSector() {
		api.utils.textures.StarLoaderTexture.runOnGraphicsThread(() -> new BuildSectorDialog().activate());
	}

	/**
	 * Returns the online PlayerState for the given name, or null if not found.
	 */
	private static PlayerState getPlayerByName(String name) {
		try {
			for(PlayerState ps : GameServerState.instance.getPlayerStatesByName().values()) {
				if(ps.getName().equals(name)) return ps;
			}
		} catch(Exception e) {
			instance.logException("getPlayerByName failed for " + name, e);
		}
		return null;
	}

	public void savePlayerSector(String playerName, Vector3i sector) {
		savedPlayerSectors.put(playerName, new Vector3i(sector));
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	public Vector3i getSavedPlayerSector(String playerName) {
		return savedPlayerSectors.get(playerName);
	}

	public void clearSavedPlayerSector(String playerName) {
		savedPlayerSectors.remove(playerName);
	}

	@Override
	public void onEnable() {
		SubModRegistry.register(this);
	}

	@Override
	public void onDisable() {
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		StarLoader.registerListener(RegisterWorldDrawersEvent.class, new Listener<RegisterWorldDrawersEvent>() {
			@Override
			public void onEvent(RegisterWorldDrawersEvent e) {
				e.getModDrawables().add(new BuildSectorHudDrawer());
			}
		}, this);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
	}

	@Override
	public String getModId() {
		return "atlas_buildsectors";
	}

	@Override
	public StarMod getMod() {
		return this;
	}

	@Override
	public void onAtlasCoreReady() {
		registerBuildSectorDataType();
		registerActionHandlers();
	}

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("BUILD SECTOR", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openBuildSector();
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
			BuildSectorDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
		}
	}

	// ── private helpers ───────────────────────────────────────────────────────

	@Override
	public void onPlayerJoinWorld(PlayerJoinWorldEvent event) {
		BuildSectorDataManager.getInstance(true).createMissingData(event.getPlayerState().getName());
	}

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Build Sector Menu".equals(bindingName)) openBuildSector();
	}

	private void registerActionHandlers() {
		ENTER_BUILD_SECTOR = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args[0] = buildSectorData UUID.
			// The sender's identity is taken from the authenticated PlayerState, not args.
			if(sender == null || args.length < 1) return;
			String playerName = sender.getName();
			String sectorUUID = args[0];

			BuildSectorData data = BuildSectorDataManager.getInstance(true).getFromUUID(sectorUUID, true);
			if(data == null) {
				logWarning("[BuildSectors] ENTER: no sector found for UUID " + sectorUUID);
				return;
			}

			// Server-side permission check against the authenticated sender.
			if(!data.getOwner().equals(playerName) && data.getPermissionsForUser(playerName) == null) {
				logWarning("[BuildSectors] ENTER: player " + playerName + " has no permission for sector " + sectorUUID);
				return;
			}

			// Save current sector so LEAVE can warp them back
			savePlayerSector(playerName, sender.getCurrentSector());

			try {
				Sector sector = data.getServerSector();
				sender.forcePlayerIntoEntity(sector, null);
			} catch(Exception e) {
				logException("[BuildSectors] ENTER: failed to teleport " + playerName, e);
			}
		});

		LEAVE_BUILD_SECTOR = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. No args needed; identity comes from authenticated sender.
			if(sender == null) return;
			String playerName = sender.getName();

			Vector3i savedSector = getSavedPlayerSector(playerName);
			if(savedSector == null) {
				// No saved sector — warp to server spawn sector (0,2,0 in most configs) or origin
				savedSector = new Vector3i(0, 2, 0);
			}

			try {
				Sector sector = GameServerState.instance.getUniverse().getSector(savedSector);
				sender.forcePlayerIntoEntity(sector, null);
				clearSavedPlayerSector(playerName);
			} catch(Exception e) {
				logException("[BuildSectors] LEAVE: failed to teleport " + playerName, e);
			}
		});

		TOGGLE_AI = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args[0] = entityUID, args[1] = "true"/"false".
			// Permission is checked against the authenticated sender, not args.
			if(sender == null || args.length < 2) return;
			String entityUID = args[0];
			boolean value = Boolean.parseBoolean(args[1]);
			String playerName = sender.getName();

			for(BuildSectorData data : BuildSectorDataManager.getInstance(true).getServerCache()) {
				if(data.getPermissionForEntityOrGlobal(playerName, entityUID, BuildSectorData.PermissionTypes.TOGGLE_AI_SPECIFIC)) {
					SegmentController entity = BuildSectorData.getEntity(entityUID);
					if(entity != null) EntityUtils.toggleAI(entity, value);
					return;
				}
			}
			logWarning("[BuildSectors] TOGGLE_AI: player " + playerName + " denied for entity " + entityUID);
		});

		SET_INVULNERABLE = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args[0] = entityUID, args[1] = "true"/"false".
			if(sender == null || args.length < 2) return;
			String entityUID = args[0];
			boolean value = Boolean.parseBoolean(args[1]);
			String playerName = sender.getName();

			for(BuildSectorData data : BuildSectorDataManager.getInstance(true).getServerCache()) {
				BuildSectorData.BuildSectorEntityData entityData = data.getEntityData(BuildSectorData.getEntity(entityUID));
				if(entityData != null && data.getPermissionForEntityOrGlobal(playerName, entityUID, BuildSectorData.PermissionTypes.TOGGLE_DAMAGE_SPECIFIC)) {
					entityData.setInvulnerable(value, true);
					return;
				}
			}
			logWarning("[BuildSectors] SET_INVULNERABLE: player " + playerName + " denied for entity " + entityUID);
		});

		DELETE_ENTITY = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args[0] = entityUID.
			if(sender == null || args.length < 1) return;
			String entityUID = args[0];
			String playerName = sender.getName();

			for(BuildSectorData data : BuildSectorDataManager.getInstance(true).getServerCache()) {
				if(data.getPermissionForEntityOrGlobal(playerName, entityUID, BuildSectorData.PermissionTypes.DELETE_SPECIFIC)) {
					SegmentController entity = BuildSectorData.getEntity(entityUID);
					if(entity != null) {
						data.removeEntity(entity, true);
						EntityUtils.delete(entity);
					}
					return;
				}
			}
			logWarning("[BuildSectors] DELETE_ENTITY: player " + playerName + " denied for entity " + entityUID);
		});
	}

	private void registerBuildSectorDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() {
				return "BUILD_SECTOR_DATA";
			}

			@Override
			public SerializableData deserializeNetwork(PacketReadBuffer buf) throws java.io.IOException {
				return new BuildSectorData(buf);
			}

			@Override
			public SerializableData deserializeJSON(org.json.JSONObject obj) {
				return new BuildSectorData(obj);
			}

			@Override
			public DataManager<?> getManager(boolean server) {
				return BuildSectorDataManager.getInstance(server);
			}
		});
	}
}
