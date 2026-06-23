package atlas.contracts.gui.contract.contractlist;

import api.common.GameClient;
import api.utils.gui.SimplePopup;
import atlas.contracts.gui.contract.newcontract.NewContractDialog;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;

public class ContractsTab extends GUIContentPane {

	private final GUIMainWindow window;
	private ContractsScrollableList contractsScrollableList;
	private final int offset = 139;

	public ContractsTab(GUIMainWindow window) {
		super(window.getState(), window, "CONTRACTS");
		this.window = window;
	}

	@Override
	public void onInit() {
		super.onInit();
		createTab();
	}

	@Override
	public void draw() {
		super.draw();
		setTextBoxHeight(0, (int) (window.getHeight() - offset));
	}

	public void flagForRefresh() {
		if(contractsScrollableList != null) contractsScrollableList.flagDirty();
	}

	private void createTab() {
		setTextBoxHeightLast((int) (window.getHeight() - offset));
		contractsScrollableList = new ContractsScrollableList(getState(), window, getContent(0));
		contractsScrollableList.onInit();
		final PlayerState player = GameClient.getClientPlayerState();

		addNewTextBox(offset);
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, getContent(1));
		setTextBoxHeightLast(offset);
		buttonPane.onInit();

		buttonPane.addButton(0, 0, "ADD CONTRACT", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					if(player.getFactionId() != 0) {
						getState().getController().queueUIAudio("0022_menu_ui - enter");
						(new NewContractDialog(GameClient.getClientState())).activate();
					} else (new SimplePopup(getState(), "Cannot Add Contract", "You must be in a faction to add new contracts!")).activate();
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
		getContent(0).attach(contractsScrollableList);
		getContent(1).attach(buttonPane);
	}
}
