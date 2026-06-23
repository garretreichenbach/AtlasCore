package atlas.contracts.utils;

import api.common.GameServer;
import atlas.contracts.AtlasContracts;
import atlas.contracts.manager.ConfigManager;
import com.bulletphysics.linearmath.Transform;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.catalog.CatalogManager;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityAlreadyExistsException;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.SegmentControllerSpawnCallbackDirect;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.settings.StateParameterNotFoundException;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Blueprint lookup and mob-spawning helpers for contracts. Blueprint metadata is cached and
 * refreshed on the configured {@code blueprint_update_interval}.
 */
public class BlueprintUtils {

	private static final HashMap<BlueprintEntry, CatalogPermission> blueprintMap = new HashMap<>();
	private static long lastBPUpdateCheck;

	private static void readBPs() {
		if(blueprintMap.isEmpty() || lastBPUpdateCheck == 0 || System.currentTimeMillis() - lastBPUpdateCheck > ConfigManager.getBlueprintUpdateInterval()) {
			blueprintMap.clear();
			try {
				List<BlueprintEntry> allBlueprints = BluePrintController.active.readBluePrints();
				List<CatalogPermission> catalogPermissions = getAllCatalogPermissions();
				for(BlueprintEntry blueprint : allBlueprints) {
					for(CatalogPermission catalogPermission : catalogPermissions) {
						if(Objects.equals(catalogPermission.getUid(), blueprint.getName())) {
							blueprintMap.put(blueprint, catalogPermission);
							break;
						}
					}
				}
			} catch(Exception exception) {
				AtlasContracts.getInstance().logException("An error occurred while getting blueprints", exception);
			}
			lastBPUpdateCheck = System.currentTimeMillis();
		}
	}

	/** Forces the blueprint cache to refresh on the next access (e.g. after blueprints change). */
	public static void invalidateCache() {
		lastBPUpdateCheck = 0;
	}

	public static ArrayList<BlueprintEntry> getPirateBPs() {
		ArrayList<BlueprintEntry> pirateBPs = new ArrayList<>();
		try {
			readBPs();
			for(BlueprintEntry blueprint : blueprintMap.keySet()) {
				if(blueprintMap.get(blueprint).enemyUsable() && blueprint.getType() == BlueprintType.SHIP) pirateBPs.add(blueprint);
			}
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while getting pirate blueprints", exception);
		}
		return pirateBPs;
	}

	public static HashMap<BlueprintEntry, Float> getPirateSpawnWeights() {
		HashMap<BlueprintEntry, Float> pirateSpawnWeights = new HashMap<>();
		try {
			ArrayList<BlueprintEntry> pirateBPs = getPirateBPs();
			for(BlueprintEntry blueprint : pirateBPs) {
				double mass = blueprint.getMass();
				float spawnWeight = (float) Math.clamp(1.0 - (mass / 1000000.0), 0.05, 1.0);
				pirateSpawnWeights.put(blueprint, spawnWeight);
			}
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while getting pirate spawn weights", exception);
		}
		return pirateSpawnWeights;
	}

	public static ArrayList<CatalogPermission> getAllCatalogPermissions() {
		ArrayList<CatalogPermission> catalogPermissions = new ArrayList<>();
		try {
			catalogPermissions.addAll(getCatalogManager().getCatalog());
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while getting catalog permissions", exception);
		}
		return catalogPermissions;
	}

	private static CatalogManager getCatalogManager() {
		return GameServer.getServerState().getCatalogManager();
	}

	public static SegmentController spawnAsMob(JSONObject mob, Vector3i sector, int factionId) {
		try {
			BlueprintEntry entry = getBlueprint(mob.getString("bp_name"));
			if(GameServer.getServerState().getSegmentControllersByName().containsKey(mob.getString("spawn_name"))) {
				return null;
			}
			if(entry != null) {
				Transform transform = new Transform();
				transform.setIdentity();
				transform.set(getRandomTransform());
				Vector3f forward = GlUtil.getForwardVector(new Vector3f(), transform);
				Vector3f size = entry.getBb().calculateHalfSize(new Vector3f());
				size.scale(0.5f);
				forward.scaleAdd(1.15f, size);
				transform.origin.set(forward);
				try {
					SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(GameServerState.instance, mob.getString("bp_name"), mob.getString("spawn_name"), transform, -1, factionId, sector, "Server", PlayerState.buffer, null, true, new ChildStats(false));
					return outline.spawn(sector, false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServer.getServerState(), sector) {
						@Override
						public void onNoDocker() {
						}
					});
				} catch(EntityNotFountException | IOException | EntityAlreadyExistsException | StateParameterNotFoundException exception) {
					AtlasContracts.getInstance().logException("An error occurred while spawning mob", exception);
				}
			}
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while spawning mob", exception);
		}
		return null;
	}

	public static Transform getRandomTransform() {
		int sectorSize = (Integer) ServerConfig.SECTOR_SIZE.getCurrentState();
		int x = (int) (Math.random() * sectorSize) * 10;
		int y = (int) (Math.random() * sectorSize) * 10;
		int z = (int) (Math.random() * sectorSize) * 10;
		Vector3f posInSector = new Vector3f(x, y, z);
		int xMult = Math.random() > 0.5 ? 1 : -1;
		int yMult = Math.random() > 0.5 ? 1 : -1;
		int zMult = Math.random() > 0.5 ? 1 : -1;
		posInSector.x *= xMult;
		posInSector.y *= yMult;
		posInSector.z *= zMult;

		float yaw = (float) (Math.random() * 360);
		float pitch = (float) (Math.random() * 360);
		float roll = (float) (Math.random() * 360);

		Transform transform = new Transform();
		transform.setIdentity();
		transform.origin.set(posInSector);
		transform.basis.rotX(pitch);
		transform.basis.rotY(yaw);
		transform.basis.rotZ(roll);
		return transform;
	}

	private static BlueprintEntry getBlueprint(String bpName) {
		try {
			readBPs();
			for(BlueprintEntry blueprint : blueprintMap.keySet()) {
				if(Objects.equals(blueprint.getName(), bpName)) return blueprint;
			}
		} catch(Exception exception) {
			AtlasContracts.getInstance().logException("An error occurred while getting blueprint", exception);
		}
		return null;
	}
}
