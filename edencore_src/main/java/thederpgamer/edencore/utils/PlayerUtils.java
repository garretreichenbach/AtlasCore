package thederpgamer.edencore.utils;

import api.common.GameClient;
import api.common.GameServer;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.physics.CubeRayCastResult;
import org.schema.game.common.data.physics.PhysicsExt;
import org.schema.game.common.data.player.AbstractCharacter;
import org.schema.game.common.data.player.PlayerControlledTransformableNotFound;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.playermessage.PlayerMessageController;
import org.schema.game.common.data.player.playermessage.ServerPlayerMessager;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.resource.FileExt;
import org.schema.schine.resource.tag.Tag;
import thederpgamer.edencore.EdenCore;

import javax.vecmath.Vector3f;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerUtils {

	public static void sendMail(String from, String to, String topic, String message, boolean onSever) {
		if(onSever) {
			GameServer.getServerState().getServerPlayerMessager().send(from, to, topic, message);
		} else {
			GameClient.getClientState().getController().getClientChannel().getPlayerMessageController().clientSend(from, to, topic, message);
		}
	}

	public static PlayerState getPlayerFromDB(String playerName) {
		PlayerState player = null;
		try {
			File playerFile = new FileExt(GameServerState.ENTITY_DATABASE_PATH + File.separator + "ENTITY_PLAYERSTATE_" + playerName.trim() + ".ent");
			Tag tag;
			tag = Tag.readFrom(new BufferedInputStream(Files.newInputStream(playerFile.toPath())), true, false);
			player = new PlayerState(GameServer.getServerState());
			player.initialize();
			player.fromTagStructure(tag);
			String fName = playerFile.getName();
			player.setName(fName.substring("ENTITY_PLAYERSTATE_".length(), fName.lastIndexOf(".")));
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return player;
	}

	public static SegmentPiece getBlockLookingAt(PlayerState player) throws PlayerControlledTransformableNotFound {
		Vector3f pos;
		Vector3f to;
		SimpleTransformableSendableObject<?> firstControlledTransformable = player.getFirstControlledTransformable();
		if(player.getNetworkObject().isInBuildMode.getBoolean()) {
			pos = new Vector3f(player.getBuildModePosition().getWorldTransform().origin);
			Vector3f forw = GlUtil.getForwardVector(new Vector3f(), player.getBuildModePosition().getWorldTransform());
			forw.scale(5000);
			to = new Vector3f(pos);
			to.add(forw);
		} else {
			if(firstControlledTransformable instanceof AbstractCharacter<?>)
				pos = new Vector3f(((AbstractCharacter<?>) firstControlledTransformable).getHeadWorldTransform().origin);
			else pos = new Vector3f(firstControlledTransformable.getWorldTransform().origin);
			to = new Vector3f(pos);
			Vector3f forw = player.getForward(new Vector3f());
			forw.scale(5000);
			to.add(forw);
		}
		Sector sector = GameServer.getUniverse().getSector(firstControlledTransformable.getSectorId());
		CollisionWorld.ClosestRayResultCallback testRayCollisionPoint = ((PhysicsExt) sector.getPhysics()).testRayCollisionPoint(pos, to, false, null, null, false, true, false);
		if(testRayCollisionPoint.hasHit() && testRayCollisionPoint instanceof CubeRayCastResult && ((CubeRayCastResult) testRayCollisionPoint).getSegment() != null) {
			CubeRayCastResult c = (CubeRayCastResult) testRayCollisionPoint;
			SegmentPiece p = new SegmentPiece(c.getSegment(), c.getCubePos());
			if(p.isValid() && p.getInfo().isRailDockable()) return p;
		}
		return null;
	}
}
