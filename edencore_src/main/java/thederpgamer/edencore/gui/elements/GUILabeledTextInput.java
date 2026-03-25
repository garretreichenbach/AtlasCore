package thederpgamer.edencore.gui.elements;

import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUITextInput;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class GUILabeledTextInput extends GUITextInput {
	
	public static final int LEFT = 0;
	public static final int TOP = 1;
	public static final int RIGHT = 2;
	public static final int BOTTOM = 3;
	
	private Object labelText;
	private int labelPosition;
	private GUITextOverlay label;
	private boolean needsUpdate;
	
	public GUILabeledTextInput(int width, int height, InputState state, Object labelText, int labelPosition) {
		super(width, height, state);
		this.labelText = labelText;
		this.labelPosition = labelPosition;
	}

	public GUILabeledTextInput(int width, int height, InputState state, boolean mouseActive, Object labelText, int labelPosition) {
		super(width, height, state, mouseActive);
		this.labelText = labelText;
		this.labelPosition = labelPosition;
	}

	public GUILabeledTextInput(int width, int height, FontLibrary.FontSize font, InputState state, Object labelText, int labelPosition) {
		super(width, height, font, state);
		this.labelText = labelText;
		this.labelPosition = labelPosition;
	}

	public GUILabeledTextInput(int width, int height, FontLibrary.FontSize font, InputState state, boolean mouseActive, Object labelText, int labelPosition) {
		super(width, height, font, state, mouseActive);
		this.labelText = labelText;
		this.labelPosition = labelPosition;
	}
	
	@Override
	public void onInit() {
		super.onInit();
		label = new GUITextOverlay(10, 10, getState());
		label.onInit();
		label.setTextSimple(labelText);
		attach(label);
		needsUpdate = true;
	}
	
	@Override
	public void draw() {
		super.draw();
		if(needsUpdate) {
			switch(labelPosition) {
				case LEFT:
					label.getPos().x = getPos().x - label.getWidth() - 5;
					label.getPos().y = getPos().y + (getHeight() / 2) - (label.getHeight() / 2);
					setPos(getPos().x - label.getWidth() - 5, getPos().y, getPos().z);
					break;
				case TOP:
					label.getPos().x = getPos().x;
					label.getPos().y = getPos().y - label.getHeight() - 5;
					setPos(getPos().x, getPos().y - label.getHeight() - 5, getPos().z);
					break;
				case RIGHT:
					label.getPos().x = getPos().x + getWidth() + 5;
					label.getPos().y = getPos().y + (getHeight() / 2) - (label.getHeight() / 2);
					break;
				case BOTTOM:
					label.getPos().x = getPos().x;
					label.getPos().y = getPos().y + getHeight() + 5;
					break;
			}
			needsUpdate = false;
		}
	}
	
	public Object getLabelText() {
		return labelText;
	}
	
	public void setLabelText(Object labelText) {
		this.labelText = labelText;
		label.setTextSimple(labelText);
		needsUpdate = true;
	}
	
	public int getLabelPosition() {
		return labelPosition;
	}
	
	public void setLabelPosition(int labelPosition) {
		this.labelPosition = labelPosition;
		needsUpdate = true;
	}
}
