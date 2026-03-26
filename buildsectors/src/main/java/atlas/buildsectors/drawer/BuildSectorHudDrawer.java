package atlas.buildsectors.drawer;

import api.common.GameClient;
import api.utils.draw.ModWorldDrawer;
import atlas.buildsectors.AtlasBuildSectors;
import atlas.buildsectors.data.BuildSectorDataManager;
import org.schema.game.client.view.gui.shiphud.newhud.Hud;
import org.schema.schine.graphicsengine.core.Timer;

/**
 * Modifies the HUD while the client is in a build sector to hide real coordinates.
 *
 * @author TheDerpGamer
 */
public class BuildSectorHudDrawer extends ModWorldDrawer {

	private boolean wasInBuildSectorLastFrame;

	@Override
	public void onInit() {
	}

	@Override
	public void draw() {
		Hud hud = GameClient.getClientState().getWorldDrawer().getGuiDrawer().getHud();
		BuildSectorDataManager mgr = BuildSectorDataManager.getInstance(false);
		if(mgr != null) {
			if(mgr.isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
				try {
					if(!wasInBuildSectorLastFrame) {
						hud.getRadar().getLocation().setTextSimple("<Build Sector>");
						hud.getIndicator().drawSectorIndicators = false;
						hud.getIndicator().drawWaypoints = false;
						if(GameClient.getClientState().getController().getClientGameData().getWaypoint() != null) {
							GameClient.getClientState().getController().getClientGameData().setWaypoint(null);
						}
					}
				} catch(Exception exception) {
					AtlasBuildSectors.getInstance().logException("An error occurred while updating the HUD", exception);
				}
				wasInBuildSectorLastFrame = true;
			} else {
				try {
					if(wasInBuildSectorLastFrame) {
						hud.getRadar().getLocation().setTextSimple(GameClient.getClientPlayerState().getCurrentSector().toStringPure());
						hud.getIndicator().drawSectorIndicators = true;
						hud.getIndicator().drawWaypoints = true;
					}
				} catch(Exception exception) {
					AtlasBuildSectors.getInstance().logException("An error occurred while updating the HUD", exception);
				}
				wasInBuildSectorLastFrame = false;
			}
		}
	}

	@Override
	public void update(Timer timer) {
	}

	@Override
	public void cleanUp() {
	}

	@Override
	public boolean isInvisible() {
		return false;
	}
}
