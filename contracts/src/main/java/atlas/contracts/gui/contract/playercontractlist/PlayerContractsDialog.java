package atlas.contracts.gui.contract.playercontractlist;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.gui.GUIInputInterface;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;

public class PlayerContractsDialog extends PlayerInput {

	private PlayerContractsPanel panel;

	public PlayerContractsDialog() {
		super(GameClient.getClientState());
	}

	@Override
	protected void initialize() {
		super.initialize();
		(panel = new PlayerContractsPanel()).onInit();
		panel.setCallback(this);
	}

	@Override
	public void activate() {
		GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerInteractionManager().deactivateAll();
		super.activate();
	}

	@Override
	public PlayerContractsPanel getInputPanel() {
		return panel;
	}

	@Override
	public void onDeactivate() {
		panel.cleanUp();
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
	}

	public static class PlayerContractsPanel extends GUIMainWindow implements GUIInputInterface {

		public PlayerContractsPanel() {
			super(GameClient.getClientState(), (int) (GLFrame.getWidth() / 1.5f), (int) (GLFrame.getHeight() / 1.5f), "player_contracts_panel");
		}

		@Override
		public void onInit() {
			super.onInit();
			recreateTabs();
		}

		public void recreateTabs() {
			int lastTab = getSelectedTab();
			if(!getTabs().isEmpty()) clearTabs();

			GUIContentPane contractsPane = addTab(Lng.str("CONTRACTS"));
			contractsPane.setTextBoxHeightLast((int) getHeight());
			PlayerContractsScrollableList playerContractsList = new PlayerContractsScrollableList(GameClient.getClientState(), this, contractsPane.getContent(0));
			playerContractsList.onInit();
			contractsPane.getContent(0).attach(playerContractsList);

			setSelectedTab(lastTab);
		}
	}
}
