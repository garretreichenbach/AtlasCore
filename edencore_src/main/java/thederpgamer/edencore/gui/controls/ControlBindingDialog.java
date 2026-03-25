package thederpgamer.edencore.gui.controls;

import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.client.view.mainmenu.DialogInput;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.input.KeyEventInterface;
import org.schema.schine.input.KeyboardMappings;
import org.schema.schine.input.Mouse;
import thederpgamer.edencore.data.misc.ControlBindingData;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ControlBindingDialog extends DialogInput {

	private boolean pressed;
	private final ControlBindingData mapping;
	private final GUIInputPanel input;

	public ControlBindingDialog(InputState state, ControlBindingData mapping) {
		super(state);
		this.mapping = mapping;
		input = new GUIInputPanel("KEY_ASSIGN", getState(), this, Lng.str("Assign New Key to %s", mapping.getDescription()), Lng.str("Press a Key to assign it to \n\n <%s> \n\nor press ESC to cancel.", mapping.getDescription()));
		input.setOkButton(false);
		input.setCallback(this);
	}

	@Override
	public void callback(GUIElement callingGuiElement, MouseEvent event) {
		if(Mouse.isButtonDown(0) && !pressed) {
			if(callingGuiElement.getUserPointer().equals("CANCEL") || callingGuiElement.getUserPointer().equals("X")) {
				System.err.println("CANCEL");
				cancel();
			}
		}
		pressed = Mouse.isButtonDown(0);
	}

	@Override
	public void handleKeyEvent(KeyEventInterface e) {
		if(KeyboardMappings.getEventKeyState(e, getState())) {
			if(KeyboardMappings.getEventKeyRaw(e) == GLFW.GLFW_KEY_ESCAPE) mapping.setBinding(0);
			else mapping.setBinding(KeyboardMappings.getEventKeyRaw(e));
			deactivate();
		}
	}

	@Override
	public GUIElement getInputPanel() {
		return input;
	}

	@Override
	protected void initialize() {
	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public boolean isOccluded() {
		return false;
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
	}
}
