package atlas.buildsectors.mixin;

import atlas.buildsectors.data.BuildSectorData;
import atlas.buildsectors.data.BuildSectorDataManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.PlayerControllable;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.data.blockeffects.PullEffect;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.controller.SectorSwitch;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.server.ServerMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Vector3f;
import java.io.IOException;
import java.util.List;

/**
 * Enforces build-sector entry permissions for <em>all</em> sector transition types.
 *
 * <p>StarMade's vanilla {@code LOCK_NO_ENTER} flag only blocks {@code TRANS_LOCAL}
 * (organic boundary-crossings). Any force-teleport ({@code TRANS_JUMP}, admin commands,
 * warp-gates from other mods) bypasses it entirely. This mixin intercepts every
 * {@code SectorSwitch.execute()} call and rejects transitions into build sectors for
 * players who have not been granted access.
 *
 * <p>Authorised entry (e.g. the {@code ENTER_BUILD_SECTOR} server action) still goes
 * through {@code forcePlayerIntoEntity} / {@code TRANS_JUMP}, but the player will already
 * be in the sector's permissions map at that point, so the check passes.
 *
 * <p>Registered in {@code atlasbuildsectors.mixins.json} under {@code "server"}.
 */
@Mixin(value = SectorSwitch.class, remap = false)
public abstract class MixinSectorSwitch {

	@Shadow
	private SimpleTransformableSendableObject<?> o;
	@Shadow
	private Vector3i belogingVector;

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private void onExecuteHead(GameServerState state, CallbackInfo ci) throws IOException {
		if(!(o instanceof PlayerControllable)) return;

		List<PlayerState> players = ((PlayerControllable) o).getAttachedPlayers();
		if(players == null || players.isEmpty()) return;

		BuildSectorDataManager mgr = BuildSectorDataManager.getInstance(true);
		BuildSectorData sector = mgr.getFromSector(belogingVector, true);
		if(sector == null) return; // not a build sector — let StarMade handle it normally

		for(PlayerState player : players) {
			String name = player.getName();
			boolean permitted = sector.getOwner().equals(name) || sector.getPermissionsForUser(name) != null;

			if(!permitted) {
				player.sendServerMessage(new ServerMessage(new Object[]{"Cannot enter build sector: you have not been invited. " + "Ask " + sector.getOwner() + " to add you via the Build Sector menu."}, ServerMessage.MESSAGE_TYPE_ERROR, player.getId()));

				if(o instanceof SendableSegmentController) {
					Vector3f force = new Vector3f(o.getWorldTransform().origin);
					float pullForce = force.length();
					force.normalize();
					((SendableSegmentController) o).getBlockEffectManager().addEffect(new PullEffect((SendableSegmentController) o, force, pullForce, false, 5));
				} else {
					o.warpTransformable(0, 0, 0, true, null);
				}

				ci.cancel();
				return;
			}
		}
	}
}
