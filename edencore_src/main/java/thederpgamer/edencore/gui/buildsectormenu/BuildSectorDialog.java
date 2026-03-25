package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import api.utils.game.PlayerUtils;
import api.utils.gui.SimplePlayerTextInput;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class BuildSectorDialog extends PlayerInput {
	
	private final BuildSectorPanel panel;
	private static BuildSectorDialog instance;

	public static BuildSectorDialog getInstance() {
		return instance;
	}
	
	public BuildSectorDialog() {
		super(GameClient.getClientState());
		instance = this;
		(panel = new BuildSectorPanel(getState(), this)).onInit();
	}

	@Override
	public void onDeactivate() {
		
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public BuildSectorPanel getInputPanel() {
		return panel;
	}

	public static class BuildSectorPanel extends GUIInputPanel {
		
		private GUITabbedContent tabbedContent;

		public BuildSectorPanel(InputState state, GUICallback guiCallback) {
			super("BuildSectorPanel", state, 1000, 650, guiCallback, "", "");
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(300);
			int lastTab = 0;
			if(tabbedContent != null) {
				lastTab = tabbedContent.getSelectedTab();
				tabbedContent.clearTabs();
			}
			tabbedContent = new GUITabbedContent(getState(), contentPane.getContent(0));
			tabbedContent.onInit();
			
			createMainTab(tabbedContent.addTab("SECTORS"));
			createEntitiesTab(tabbedContent.addTab("ENTITIES"));
			createPermissionsTab(tabbedContent.addTab("PERMISSIONS"));
			
			tabbedContent.setSelectedTab(lastTab);
			contentPane.getContent(0).attach(tabbedContent);
		}
		
		private BuildSectorData getBuildSectorData() {
			if(BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) return BuildSectorDataManager.getInstance(false).getCurrentBuildSector(GameClient.getClientPlayerState());
			else return BuildSectorDataManager.getInstance(false).getFromPlayerName(GameClient.getClientPlayerState().getName(), false);
		}
		
		private void createMainTab(GUIContentPane contentPane) {
			contentPane.setTextBoxHeightLast(400);
			BuildSectorScrollableList buildSectorList = new BuildSectorScrollableList(getState(), contentPane.getContent(0));
			buildSectorList.onInit();
			contentPane.getContent(0).attach(buildSectorList);
		}
		
		private void createEntitiesTab(GUIContentPane contentPane) {
			contentPane.setTextBoxHeightLast(400);
			BuildSectorEntityScrollableList entityList = new BuildSectorEntityScrollableList(getState(), contentPane.getContent(0), getBuildSectorData());
			entityList.onInit();
			contentPane.getContent(0).attach(entityList);
		}
		
		private void createPermissionsTab(GUIContentPane contentPane) {
			contentPane.setTextBoxHeightLast(28);
			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, contentPane.getContent(0));
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "ADD USER", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new SimplePlayerTextInput("Add User", "") {
							@Override
							public boolean onInput(String s) {
								if(PlayerDataManager.getInstance(false).getFromName(s.trim(), false) != null) {
									getBuildSectorData().addPlayer(s.trim(), BuildSectorData.FRIEND, false);
									return true;
								} else {
									PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Player not found");
									return false;
								}
							}
						}).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});
			contentPane.getContent(0).attach(buttonPane);

			contentPane.addNewTextBox(300);
			BuildSectorUserScrollableList userScrollableList = new BuildSectorUserScrollableList(getState(), contentPane.getContent(1), getBuildSectorData());
			userScrollableList.onInit();
			contentPane.getContent(1).attach(userScrollableList);
		}
	}
}
