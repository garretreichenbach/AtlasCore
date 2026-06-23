package atlas.contracts.gui.contract.newcontract;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.KeyEventInterface;
import org.schema.schine.input.KeyboardMappings;
import atlas.contracts.data.contract.ContractData;

public class NewContractDialog extends PlayerInput {

	private final NewContractPanel panel;
	private ContractData.ContractType selectedType;

	public NewContractDialog(GameClientState gameClientState) {
		super(gameClientState);
		panel = new NewContractPanel(getState(), this);
		panel.onInit();
		selectedType = ContractData.ContractType.BOUNTY;
		panel.drawBountyPanel();
	}

	@Override
	public void onDeactivate() {
		panel.cleanUp();
	}

	@Override
	public void handleKeyEvent(KeyEventInterface event) {
		if(KeyboardMappings.getEventKeyState(event, getState())) {
			if(KeyboardMappings.getEventKeyRaw(event) == GLFW.GLFW_KEY_ESCAPE) deactivate();
		}
	}

	@Override
	public void handleMouseEvent(MouseEvent event) {
	}

	@Override
	public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
		if(!mouseEvent.pressedLeftMouse()) return;
		if(guiElement == null || guiElement.getUserPointer() == null) return;
		Object pointer = guiElement.getUserPointer();
		if(pointer.equals("OK")) {
			boolean ok;
			switch(selectedType) {
				case BOUNTY:
					ok = panel.createBountyContract();
					break;
				case ITEMS:
					ok = panel.createItemsContract();
					break;
				case ESCORT:
					ok = panel.createEscortContract();
					break;
				default:
					ok = false;
			}
			if(ok) deactivate();
		} else if(pointer.equals("CANCEL") || pointer.equals("X")) {
			deactivate();
		} else if(pointer.equals("BOUNTY")) {
			selectedType = ContractData.ContractType.BOUNTY;
			panel.drawBountyPanel();
		} else if(pointer.equals("ITEMS")) {
			selectedType = ContractData.ContractType.ITEMS;
			panel.drawItemsPanel();
		} else if(pointer.equals("ESCORT")) {
			selectedType = ContractData.ContractType.ESCORT;
			panel.drawEscortPanel();
		}
	}

	@Override
	public NewContractPanel getInputPanel() {
		return panel;
	}
}
