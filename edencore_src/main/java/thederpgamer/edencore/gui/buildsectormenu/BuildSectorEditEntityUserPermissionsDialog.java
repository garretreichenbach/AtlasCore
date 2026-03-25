package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.schine.common.language.Lng;
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
public class BuildSectorEditEntityUserPermissionsDialog extends PlayerInput {
	
	private final BuildSectorEditEntityUserPermissionsPanel panel;
	
	public BuildSectorEditEntityUserPermissionsDialog(String entityUID, String username, BuildSectorData buildSectorData) {
		super(GameClient.getClientState());
		(panel = new BuildSectorEditEntityUserPermissionsPanel(getState(), this, username, buildSectorData, entityUID)).onInit();
	}

	@Override
	public void onDeactivate() {
		panel.cleanUp();
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public BuildSectorEditEntityUserPermissionsPanel getInputPanel() {
		return panel;
	}

	public static class BuildSectorEditEntityUserPermissionsPanel extends GUIInputPanel {

		private final BuildSectorData buildSectorData;
		private final String username;
		private final String entityUID;

		public BuildSectorEditEntityUserPermissionsPanel(InputState state, GUICallback guiCallback, String username, BuildSectorData buildSectorData, String entityUID) {
			super("BuildSectorEditEntityUserPermissionsPanel", state, guiCallback, Lng.str("Edit User Permissions"), "");
			this.username = username;
			this.buildSectorData = buildSectorData;
			this.entityUID = entityUID;
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast((int) (getHeight() - 50));
			BuildSectorPermissionsScrollableList permissionsScrollableList = new BuildSectorPermissionsScrollableList(getState(), contentPane.getContent(0), username, buildSectorData, entityUID);
			permissionsScrollableList.onInit();
			contentPane.getContent(0).attach(permissionsScrollableList);
		}
	}
}
