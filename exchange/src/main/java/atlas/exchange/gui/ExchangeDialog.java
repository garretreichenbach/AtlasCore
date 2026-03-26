package atlas.exchange.gui;

import api.common.GameClient;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
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

import java.util.Set;

/**
 * Four-tabbed marketplace dialog: Ships, Stations, Items, Weapons.
 *
 * @author TheDerpGamer
 */
public class ExchangeDialog extends PlayerInput {

    public static final int SHIPS    = ExchangeData.SHIPS;
    public static final int STATIONS = ExchangeData.STATIONS;
    public static final int ITEMS    = ExchangeData.ITEMS;
    public static final int WEAPONS  = ExchangeData.WEAPONS;

    private final ExchangePanel panel;

    public ExchangeDialog() {
        super(GameClient.getClientState());
        (panel = new ExchangePanel(getState(), this)).onInit();
    }

    @Override
    public void onDeactivate() {}

    @Override
    public void handleMouseEvent(MouseEvent mouseEvent) {}

    @Override
    public ExchangePanel getInputPanel() { return panel; }

    public static Set<ExchangeData> getShipList() {
        return ExchangeDataManager.getByCategory(ExchangeData.ExchangeDataCategory.SHIP);
    }

    public static Set<ExchangeData> getStationList() {
        return ExchangeDataManager.getByCategory(ExchangeData.ExchangeDataCategory.STATION);
    }

    public static Set<ExchangeData> getItemsList() {
        return ExchangeDataManager.getByCategory(ExchangeData.ExchangeDataCategory.ITEM);
    }

    public static Set<ExchangeData> getWeaponsList() {
        return ExchangeDataManager.getByCategory(ExchangeData.ExchangeDataCategory.WEAPON);
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
            return GameClient.getClientPlayerState().getFactionId() == 0
                    && !GameClient.getClientPlayerState().isAdmin();
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

            // ── Ships tab ─────────────────────────────────────────────────
            GUIContentPane shipsTab = tabbedContent.addTab(Lng.str("SHIPS"));
            shipsTab.setTextBoxHeightLast(28);
            GUIHorizontalButtonTablePane shipsAdd = new GUIHorizontalButtonTablePane(getState(), 1, 1, shipsTab.getContent(0));
            shipsAdd.onInit();
            shipsAdd.addButton(0, 0, Lng.str("ADD BLUEPRINT"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                @Override
                public void callback(GUIElement e, MouseEvent event) {
                    if(event.pressedLeftMouse()) (new AddExchangeItemDialog(GameClient.getClientState(), SHIPS)).activate();
                }
                @Override
                public boolean isOccluded() {
                    return isObscured() || (playerState.getFactionId() == 0 && !playerState.isAdmin());
                }
            }, new GUIActivationCallback() {
                @Override public boolean isVisible(InputState s) { return true; }
                @Override public boolean isActive(InputState s) { return playerState.getFactionId() != 0 || playerState.isAdmin(); }
            });
            shipsTab.getContent(0).attach(shipsAdd);
            shipsTab.addNewTextBox(300);
            ExchangeItemScrollableList shipsList = new ExchangeItemScrollableList(getState(), shipsTab.getContent(1), SHIPS);
            shipsList.onInit();
            shipsTab.getContent(1).attach(shipsList);

            // ── Stations tab ───────────────────────────────────────────────
            GUIContentPane stationsTab = tabbedContent.addTab(Lng.str("STATIONS"));
            stationsTab.setTextBoxHeightLast(28);
            GUIHorizontalButtonTablePane stationsAdd = new GUIHorizontalButtonTablePane(getState(), 1, 1, stationsTab.getContent(0));
            stationsAdd.onInit();
            stationsAdd.addButton(0, 0, Lng.str("ADD BLUEPRINT"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                @Override
                public void callback(GUIElement e, MouseEvent event) {
                    if(event.pressedLeftMouse()) (new AddExchangeItemDialog(GameClient.getClientState(), STATIONS)).activate();
                }
                @Override
                public boolean isOccluded() {
                    return isObscured() || (playerState.getFactionId() == 0 && !playerState.isAdmin());
                }
            }, new GUIActivationCallback() {
                @Override public boolean isVisible(InputState s) { return true; }
                @Override public boolean isActive(InputState s) { return playerState.getFactionId() != 0 || playerState.isAdmin(); }
            });
            stationsTab.getContent(0).attach(stationsAdd);
            stationsTab.addNewTextBox(300);
            ExchangeItemScrollableList stationsList = new ExchangeItemScrollableList(getState(), stationsTab.getContent(1), STATIONS);
            stationsList.onInit();
            stationsTab.getContent(1).attach(stationsList);

            // ── Items tab ──────────────────────────────────────────────────
            GUIContentPane itemsTab = tabbedContent.addTab(Lng.str("ITEMS"));
            if(GameClient.getClientPlayerState().isAdmin()) {
                itemsTab.setTextBoxHeightLast(28);
                GUIHorizontalButtonTablePane itemsAdd = new GUIHorizontalButtonTablePane(getState(), 1, 1, itemsTab.getContent(0));
                itemsAdd.onInit();
                itemsAdd.addButton(0, 0, Lng.str("ADD ITEM"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                    @Override
                    public void callback(GUIElement e, MouseEvent event) {
                        if(event.pressedLeftMouse()) (new AddExchangeItemDialog(GameClient.getClientState(), ITEMS)).activate();
                    }
                    @Override
                    public boolean isOccluded() { return isObscured(); }
                }, new GUIActivationCallback() {
                    @Override public boolean isVisible(InputState s) { return true; }
                    @Override public boolean isActive(InputState s) { return GameClient.getClientPlayerState().isAdmin(); }
                });
                itemsTab.getContent(0).attach(itemsAdd);
                itemsTab.addNewTextBox(300);
                ExchangeItemScrollableList itemsList = new ExchangeItemScrollableList(getState(), itemsTab.getContent(1), ITEMS);
                itemsList.onInit();
                itemsTab.getContent(1).attach(itemsList);
            } else {
                itemsTab.setTextBoxHeightLast(300);
                ExchangeItemScrollableList itemsList = new ExchangeItemScrollableList(getState(), itemsTab.getContent(0), ITEMS);
                itemsList.onInit();
                itemsTab.getContent(0).attach(itemsList);
            }

            // ── Weapons tab ────────────────────────────────────────────────
            GUIContentPane weaponsTab = tabbedContent.addTab(Lng.str("WEAPONS"));
            if(GameClient.getClientPlayerState().isAdmin()) {
                weaponsTab.setTextBoxHeightLast(28);
                GUIHorizontalButtonTablePane weaponsAdd = new GUIHorizontalButtonTablePane(getState(), 1, 1, weaponsTab.getContent(0));
                weaponsAdd.onInit();
                weaponsAdd.addButton(0, 0, Lng.str("ADD WEAPON"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                    @Override
                    public void callback(GUIElement e, MouseEvent event) {
                        if(event.pressedLeftMouse()) (new AddExchangeItemDialog(GameClient.getClientState(), WEAPONS)).activate();
                    }
                    @Override
                    public boolean isOccluded() { return isObscured(); }
                }, new GUIActivationCallback() {
                    @Override public boolean isVisible(InputState s) { return true; }
                    @Override public boolean isActive(InputState s) { return GameClient.getClientPlayerState().isAdmin(); }
                });
                weaponsTab.getContent(0).attach(weaponsAdd);
                weaponsTab.addNewTextBox(300);
                ExchangeItemScrollableList weaponsList = new ExchangeItemScrollableList(getState(), weaponsTab.getContent(1), WEAPONS);
                weaponsList.onInit();
                weaponsTab.getContent(1).attach(weaponsList);
            } else {
                weaponsTab.setTextBoxHeightLast(300);
                ExchangeItemScrollableList weaponsList = new ExchangeItemScrollableList(getState(), weaponsTab.getContent(0), WEAPONS);
                weaponsList.onInit();
                weaponsTab.getContent(0).attach(weaponsList);
            }

            tabbedContent.setSelectedTab(lastTab);
            contentPane.getContent(0).attach(tabbedContent);
        }
    }
}
