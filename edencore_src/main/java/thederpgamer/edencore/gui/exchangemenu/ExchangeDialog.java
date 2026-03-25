package thederpgamer.edencore.gui.exchangemenu;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.exchangedata.ExchangeData;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;

import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ExchangeDialog extends PlayerInput {

	public static final int SHIPS = 0;
	public static final int STATIONS = 1;
	public static final int ITEMS = 2;
	public static final int WEAPONS = 3;

	private final ExchangePanel panel;

	public ExchangeDialog() {
		super(GameClient.getClientState());
		(panel = new ExchangePanel(getState(), this)).onInit();
	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public ExchangePanel getInputPanel() {
		return panel;
	}

	public static Set<ExchangeData> getShipList() {
		return ExchangeDataManager.getCategory(ExchangeData.ExchangeDataCategory.SHIP);
	}

	public static Set<ExchangeData> getStationList() {
		return ExchangeDataManager.getCategory(ExchangeData.ExchangeDataCategory.STATION);
	}
	
	public static Set<ExchangeData> getItemsList() {
		return ExchangeDataManager.getCategory(ExchangeData.ExchangeDataCategory.ITEM);
	}
	
	public static Set<ExchangeData> getWeaponsList() {
		return ExchangeDataManager.getCategory(ExchangeData.ExchangeDataCategory.WEAPON);
	}

	public static class ExchangePanel extends GUIInputPanel {

		private GUITabbedContent tabbedContent;

		public ExchangePanel(InputState state, GUICallback guiCallback) {
			super("ExchangePanel", state, 800, 500, guiCallback, "", "");
		}

		private static boolean isObscured() {
			for(DialogInterface dialogInterface : GameClient.getClientController().getPlayerInputs()) {
				if(dialogInterface instanceof AddExchangeItemDialog) return true;
			}
			return GameClient.getClientPlayerState().getFactionId() == 0 && !GameClient.getClientPlayerState().isAdmin();
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
			final PlayerState playerState = ((GameClientState) getState()).getPlayer();

			GUIContentPane shipsTab = tabbedContent.addTab(Lng.str("SHIPS"));
			shipsTab.setTextBoxHeightLast(28);
			GUIHorizontalButtonTablePane shipsAddButton = new GUIHorizontalButtonTablePane(getState(), 1, 1, shipsTab.getContent(0));
			shipsAddButton.onInit();
			shipsAddButton.addButton(0, 0, Lng.str("ADD BLUEPRINT"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new AddExchangeItemDialog(GameClient.getClientState(), SHIPS)).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return playerState.getFactionId() == 0 && !playerState.isAdmin();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return playerState.getFactionId() != 0 || playerState.isAdmin();
				}
			});
			shipsTab.getContent(0).attach(shipsAddButton);

			shipsTab.addNewTextBox(300);
			ExchangeItemScrollableList shipsList = new ExchangeItemScrollableList(getState(), shipsTab.getContent(1), SHIPS);
			shipsList.onInit();
			shipsTab.getContent(1).attach(shipsList);

			GUIContentPane stationsTab = tabbedContent.addTab(Lng.str("STATIONS"));
			stationsTab.setTextBoxHeightLast(28);
			GUIHorizontalButtonTablePane stationsAddButton = new GUIHorizontalButtonTablePane(getState(), 1, 1, stationsTab.getContent(0));
			stationsAddButton.onInit();
			stationsAddButton.addButton(0, 0, Lng.str("ADD BLUEPRINT"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new AddExchangeItemDialog(GameClient.getClientState(), STATIONS)).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return playerState.getFactionId() == 0 && !playerState.isAdmin();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return playerState.getFactionId() != 0 || playerState.isAdmin();
				}
			});
			stationsTab.getContent(0).attach(stationsAddButton);

			stationsTab.addNewTextBox(300);
			ExchangeItemScrollableList stationsList = new ExchangeItemScrollableList(getState(), stationsTab.getContent(1), STATIONS);
			stationsList.onInit();
			stationsTab.getContent(1).attach(stationsList);
			
			GUIContentPane itemsTab = tabbedContent.addTab(Lng.str("ITEMS"));
			if(GameClient.getClientPlayerState().isAdmin()) { //Only admins can add new items
				itemsTab.setTextBoxHeightLast(28);
				GUIHorizontalButtonTablePane itemsAddButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, itemsTab.getContent(0));
				itemsAddButtonPane.onInit();
				itemsAddButtonPane.addButton(0, 0, Lng.str("ADD ITEM"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							// Open the dialog to add a new exchange item
							(new AddExchangeItemDialog(GameClient.getClientState(), ITEMS)).activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return isObscured();
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return GameClient.getClientPlayerState().isAdmin(); // Only active for admins
					}
				});

				itemsTab.getContent(0).attach(itemsAddButtonPane); // Attach the add button pane to the content pane

				itemsTab.addNewTextBox(300); // Add a text box for the scrollable list
				ExchangeItemScrollableList itemsList = new ExchangeItemScrollableList(getState(), itemsTab.getContent(1), ITEMS);
				itemsList.onInit();
				itemsTab.getContent(1).attach(itemsList); // Attach the scrollable list to the content pane
			} else {
				itemsTab.setTextBoxHeightLast(300);
				ExchangeItemScrollableList itemsList = new ExchangeItemScrollableList(getState(), itemsTab.getContent(0), ITEMS);
				itemsList.onInit(); // Initialize the scrollable list
				itemsTab.getContent(0).attach(itemsList); // Attach the scrollable list to the content pane
			}

			GUIContentPane weaponsTab = tabbedContent.addTab(Lng.str("WEAPONS"));
			if(GameClient.getClientPlayerState().isAdmin()) { //Only admins can add new weapons
				weaponsTab.setTextBoxHeightLast(28);
				GUIHorizontalButtonTablePane weaponsAddButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, weaponsTab.getContent(0));
				weaponsAddButtonPane.onInit();
				weaponsAddButtonPane.addButton(0, 0, Lng.str("ADD WEAPON"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							(new AddExchangeItemDialog(GameClient.getClientState(), WEAPONS)).activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return isObscured();
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState inputState) {
						return true; // Always visible for admins
					}

					@Override
					public boolean isActive(InputState inputState) {
						return GameClient.getClientPlayerState().isAdmin(); // Only active for admins
					}
				});
				weaponsTab.getContent(0).attach(weaponsAddButtonPane); // Attach the add button pane to the content pane

				weaponsTab.addNewTextBox(300); // Add a text box for the scrollable list
				ExchangeItemScrollableList weaponsList = new ExchangeItemScrollableList(getState(), weaponsTab.getContent(1), WEAPONS);
				weaponsList.onInit(); // Initialize the scrollable list
				weaponsTab.getContent(1).attach(weaponsList); // Attach the scrollable list to the content pane
			} else {
				weaponsTab.setTextBoxHeightLast(300); // Set the height for non-admins
				ExchangeItemScrollableList weaponsList = new ExchangeItemScrollableList(getState(), weaponsTab.getContent(0), WEAPONS);
				weaponsList.onInit(); // Initialize the scrollable list
				weaponsTab.getContent(0).attach(weaponsList); // Attach the scrollable list to the content pane
			}

			tabbedContent.setSelectedTab(lastTab);
			contentPane.getContent(0).attach(tabbedContent);
		}
	}
}
