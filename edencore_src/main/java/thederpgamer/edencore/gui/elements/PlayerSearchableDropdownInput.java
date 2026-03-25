package thederpgamer.edencore.gui.elements;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUISearchBar;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class PlayerSearchableDropdownInput extends PlayerGameDropDownInput {

	private final Object info;
	protected GUISearchBar searchBar;
	protected final ObjectArrayList<GUIElement> elements;

	protected PlayerSearchableDropdownInput(String windowId, GameClientState state, Object info, ObjectArrayList<GUIElement> elements) {
		super(windowId, state, 480, 180, info, 32);
		this.info = info;
		this.elements = elements;
		searchBar = new GUISearchBar(state, Lng.str("FILTER DROPDOWN"), ((GUIDialogWindow) getInputPanel().getBackground()).getMainContentPane().getContent(0), new TextCallback() {

			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {
			}

			@Override
			public void onFailedTextCheck(String msg) {
			}

			@Override
			public void newLine() {
			}

			@Override
			public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
				return null;
			}

		}, new OnInputChangedCallback() {

			@Override
			public String onInputChanged(String t) {
				updateDropdown(t);
				return t;
			}
		});

		searchBar.setPos(0, 24, 0);
		((GUIDialogWindow) getInputPanel().getBackground()).getMainContentPane().getContent(0).attach(searchBar);
		updateDropdown("");
	}

	private void updateDropdown(String text) {
		update(getState(), info, 32, "", filterElements(text));
	}
	
	public abstract ObjectArrayList<GUIElement> filterElements(String text);
}
