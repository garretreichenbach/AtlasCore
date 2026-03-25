package thederpgamer.edencore.utils;

import api.common.GameClient;
import api.common.GameCommon;
import api.common.GameServer;
import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.PlayerControllable;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.controller.rails.RailRelation;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.meta.BlueprintMetaItem;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.controller.EntityAlreadyExistsException;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.SegmentControllerSpawnCallbackDirect;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.settings.StateParameterNotFoundException;
import thederpgamer.edencore.EdenCore;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/20/2021]
 */
public class EntityUtils {

	public static void warpPlayerIntoEntity(SegmentController entity) {
		SegmentPiece toEnter = null;
		if(entity == null || entity.getSegmentBuffer() == null) return; // No segment controller or segment buffer
		if(entity.getType() == SimpleTransformableSendableObject.EntityType.SHIP) toEnter = entity.getSegmentBuffer().getPointUnsave(Ship.core);
		else if(entity.getType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION) toEnter = getAvailableBuildBlock(entity);
		if(toEnter != null) {
			if(GameCommon.isOnSinglePlayer() || GameCommon.isClientConnectedToServer()) {
				GameClient.getClientPlayerState().getControllerState().forcePlayerOutOfSegmentControllers();
				GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getInShipControlManager().setEntered(toEnter);
				GameClient.getClientState().getController().requestControlChange(GameClient.getClientPlayerState().getAssingedPlayerCharacter(), (PlayerControllable) entity, new Vector3i(), toEnter.getAbsolutePos(new Vector3i()), true);
			}
		}
	}

	public static SegmentPiece getAvailableBuildBlock(SegmentController segmentController) {
		ManagerContainer<?> managerContainer = null;
		if(segmentController.getType() == SimpleTransformableSendableObject.EntityType.SHIP) managerContainer = ((Ship) segmentController).getManagerContainer();
		else if(segmentController.getType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION) managerContainer = ((SpaceStation) segmentController).getManagerContainer();
		if(managerContainer != null && !managerContainer.getBuildBlocks().isEmpty()) return segmentController.getSegmentBuffer().getPointUnsave(managerContainer.getBuildBlocks().toLongArray()[0]);
		else return null;
	}

	public static float distance(Vector3f vectorA, Vector3f vectorB) {
		return (new Vector3f(Math.abs(vectorA.x - vectorB.x), Math.abs(vectorA.y - vectorB.y), Math.abs(vectorA.z - vectorB.z))).length();
	}

	public static SegmentController spawnEntry(PlayerState owner, BlueprintEntry entry, String spawnName, int factionId) {
		return spawnEntryOnDock(owner, entry, spawnName, factionId, null);
	}

	public static SegmentController spawnEntryOnDock(PlayerState owner, BlueprintEntry entry, String spawnName, int factionId, SegmentPiece spawnOnBlock) {
		Transform transform = new Transform();
		transform.setIdentity();
		transform.origin.set(owner.getFirstControlledTransformableWOExc().getWorldTransform().origin);
		Vector3f forward = GlUtil.getForwardVector(new Vector3f(), transform);
		Vector3f size = entry.getBb().calculateHalfSize(new Vector3f());
		size.scale(0.5f);
		forward.scaleAdd(1.15f, size);
		transform.origin.set(forward);
		try {
			SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(GameServerState.instance, entry.getName(), spawnName, transform, -1, factionId, owner.getCurrentSector(), owner.getName(), PlayerState.buffer, spawnOnBlock, false, new ChildStats(false));
			return outline.spawn(owner.getCurrentSector(), false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServer.getServerState(), owner.getCurrentSector()) {
				@Override
				public void onNoDocker() {
				}
			});
		} catch(EntityNotFountException | IOException | EntityAlreadyExistsException | StateParameterNotFoundException exception) {
			EdenCore.getInstance().logException("Failed to spawn entity from blueprint entry", exception);
		}
		return null;
	}

	public static ManagerContainer<?> getManagerContainer(SegmentController entity) {
		if(entity.getType() == SimpleTransformableSendableObject.EntityType.SHIP) return ((Ship) entity).getManagerContainer();
		else if(entity.getType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION) return ((SpaceStation) entity).getManagerContainer();
		return null;
	}

	public static void toggleAI(SegmentController entity, boolean toggle) {
		try {
			SegmentController root = entity.railController.getRoot();
			if(root != null) toggleAIRecursively(root, toggle);
		} catch(Exception exception) {
			EdenCore.getInstance().logException("Failed to toggle AI on entity", exception);
		}
	}
	
	private static void toggleAIRecursively(SegmentController entity, boolean toggle) {
		try {
			if(entity.getType() == SimpleTransformableSendableObject.EntityType.SHIP) ((Ship) entity).getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(toggle), true);
			else if(entity.getType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION) ((SpaceStation) entity).getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(toggle), true);
		} catch(Exception exception) {
			EdenCore.getInstance().logException("Failed to toggle AI on entity", exception);
		}
		for(RailRelation relation : entity.railController.next) toggleAIRecursively(relation.docked.getSegmentController(), toggle);
	}
	
	public static void delete(SegmentController segmentController) {
		try {
			segmentController.setMarkedForDeletePermanentIncludingDocks(true);
			segmentController.setMarkedForDeleteVolatileIncludingDocks(true);
			segmentController.markForPermanentDelete(true);
			for(RailRelation relation : segmentController.railController.next) deleteRecursively(relation.docked.getSegmentController());
		} catch(Exception exception) {
			EdenCore.getInstance().logException("Failed to delete entity", exception);
		}
	}
	
	private static void deleteRecursively(SegmentController entity) {
		try {
			entity.setMarkedForDeletePermanentIncludingDocks(true);
			entity.setMarkedForDeleteVolatileIncludingDocks(true);
			entity.markForPermanentDelete(true);
			for(RailRelation relation : entity.railController.next) deleteRecursively(relation.docked.getSegmentController());
		} catch(Exception exception) {
			EdenCore.getInstance().logException("Failed to delete entity", exception);
		}
	}
	
	public static BlueprintEntry getFromCatalog(CatalogPermission permission) {
		ArrayList<BlueprintEntry> entries = new ArrayList<>(BluePrintController.active.readBluePrints());
		for(BlueprintEntry entry : entries) {
			if(entry.getName().equals(permission.getUid())) return entry;
		}
		return null;
	}
}
