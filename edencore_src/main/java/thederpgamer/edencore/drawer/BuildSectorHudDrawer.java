package thederpgamer.edencore.drawer;

import api.common.GameClient;
import api.utils.draw.ModWorldDrawer;
import org.schema.game.client.view.gui.shiphud.newhud.Hud;
import org.schema.schine.graphicsengine.core.Timer;
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;

/**
 * Modifies the Hud Drawer while the client is in a build sector in order to hide the actual coordinates.
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
		if(BuildSectorDataManager.getInstance(false) != null) {
			if(BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
				try {
					if(!wasInBuildSectorLastFrame) {
						hud.getRadar().getLocation().setTextSimple("<Build Sector>");
						hud.getIndicator().drawSectorIndicators = false;
						hud.getIndicator().drawWaypoints = false;
						if(GameClient.getClientState().getController().getClientGameData().getWaypoint() != null) GameClient.getClientState().getController().getClientGameData().setWaypoint(null);
					}
				} catch(Exception exception) {
					EdenCore.getInstance().logException("An error occurred while updating the HUD", exception);
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
					EdenCore.getInstance().logException("An error occurred while updating the HUD", exception);
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
