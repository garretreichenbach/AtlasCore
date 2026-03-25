package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class BuildSectorEditUserPermissionsDialog extends PlayerInput {
	
	private final BuildSectorEditUserPermissionsPanel panel;
	
	public BuildSectorEditUserPermissionsDialog(String username, BuildSectorData buildSectorData) {
		super(GameClient.getClientState());
		(panel = new BuildSectorEditUserPermissionsPanel(getState(), this, username, buildSectorData)).onInit();
	}

	@Override
	public void onDeactivate() {
		panel.cleanUp();
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public BuildSectorEditUserPermissionsPanel getInputPanel() {
		return panel;
	}
	
	public static class BuildSectorEditUserPermissionsPanel extends GUIInputPanel {

		private final BuildSectorData buildSectorData;
		private final String username;
		
		public BuildSectorEditUserPermissionsPanel(InputState state, GUICallback guiCallback, String username, BuildSectorData buildSectorData) {
			super("BuildSectorEditUserPermissionsPanel", state, guiCallback, Lng.str("Edit Permissions"), "");
			this.username = username;
			this.buildSectorData = buildSectorData;
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast((int) (getHeight() - 50));
			BuildSectorPermissionsScrollableList permissionsList = new BuildSectorPermissionsScrollableList(getState(), contentPane.getContent(0), username, buildSectorData);
			permissionsList.onInit();
			contentPane.getContent(0).attach(permissionsList);
		}
	}
}
