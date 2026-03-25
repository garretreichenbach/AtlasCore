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
public class BuildSectorEditEntityPermissionsDialog extends PlayerInput {

	private final EditBuildSectorEntityPermissionsPanel panel;

	public BuildSectorEditEntityPermissionsDialog(BuildSectorData.BuildSectorEntityData entityData, BuildSectorData buildSectorData) {
		super(GameClient.getClientState());
		(panel = new EditBuildSectorEntityPermissionsPanel(getState(), this, entityData, buildSectorData)).onInit();
	}

	@Override
	public void onDeactivate() {
		panel.cleanUp();
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public EditBuildSectorEntityPermissionsPanel getInputPanel() {
		return panel;
	}

	public static class EditBuildSectorEntityPermissionsPanel extends GUIInputPanel {

		private final BuildSectorData.BuildSectorEntityData entityData;
		private final BuildSectorData buildSectorData;
		
		public EditBuildSectorEntityPermissionsPanel(InputState state, GUICallback guiCallback, BuildSectorData.BuildSectorEntityData entityData, BuildSectorData buildSectorData) {
			super("EditBuildSectorEntityPermissionsPanel", state, guiCallback, Lng.str("Edit Permissions"), "");
			this.entityData = entityData;
			this.buildSectorData = buildSectorData;
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(300);
			BuildSectorUserScrollableList userScrollableList = new BuildSectorUserScrollableList(getState(), contentPane.getContent(0), buildSectorData, entityData.getEntityUID());
			userScrollableList.onInit();
			contentPane.getContent(0).attach(userScrollableList);
		}
	}
}
