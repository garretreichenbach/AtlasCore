package thederpgamer.edencore.utils;

import api.common.GameClient;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.physics.CubeRayCastResult;
import org.schema.game.common.data.physics.PhysicsExt;
import org.schema.game.common.data.player.AbstractCharacter;
import org.schema.schine.graphicsengine.core.Controller;

import javax.vecmath.Vector3f;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SegmentPieceUtils {
	public static SegmentPiece getLookingAt(AbstractCharacter<?> playerCharacter) {
		try {
			PlayerInteractionControlManager playerInteractionControlManager = GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager();
			Vector3f camPos = new Vector3f(Controller.getCamera().getPos());
			if(playerInteractionControlManager.isInAnyCharacterBuildMode()) camPos.set(playerCharacter.getHeadWorldTransform().origin);
			Vector3f camTo = new Vector3f(camPos);
			Vector3f forw = new Vector3f(Controller.getCamera().getForward());
			if(PlayerInteractionControlManager.isAdvancedBuildMode(GameClient.getClientState())) {
				Vector3f mouse = new Vector3f(GameClient.getClientState().getWorldDrawer().getAbsoluteMousePosition());
				forw.sub(mouse, camPos);
				forw.normalize();
			}
			forw.scale(100);
			camTo.add(forw);
			CollisionWorld.ClosestRayResultCallback testRayCollisionPoint = ((PhysicsExt) GameClient.getClientState().getPhysics()).testRayCollisionPoint(camPos, camTo, false, null, null, false, true, false);
			if(testRayCollisionPoint.hasHit() && testRayCollisionPoint instanceof CubeRayCastResult) {
				CubeRayCastResult c = (CubeRayCastResult) testRayCollisionPoint;
				if(c.getSegment() != null) return new SegmentPiece(c.getSegment(), c.getCubePos());
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}
}
