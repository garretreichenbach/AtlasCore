package thederpgamer.edencore.manager;

import api.common.GameClient;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.gui.ModGUIHandler;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;
import thederpgamer.edencore.data.misc.ControlBindingData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;
import thederpgamer.edencore.gui.bankingmenu.BankingDialog;
import thederpgamer.edencore.gui.controls.ControlBindingsScrollableList;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class EventManager {

	public static final String[] disabledTabs = {"FLEETS", "SHOP", "REPAIRS", "TRADE", "SET PRICES"};
	public static final String[] disabledWindows = {"Fleet", "ShopPanelNew", "FactionPanelNew"};
	public static final short[] disabledBlocks = {
			347, //Shop Module
			291, //Faction Module
			667, //Shipyard Computer
			683, //Race Gate Controller
			542 //Warp Gate Computer
	};

	public static void initialize(final EdenCore instance) {
		StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
			@Override
			public void onEvent(MainWindowTabAddEvent event) {
				if(event.getTitleAsString().equals(Lng.str("Keyboard"))) { //Fix for the tab name being lowercase for some reason
					event.getPane().getTabNameText().setTextSimple(Lng.str("KEYBOARD"));
				} else if(event.getTitleAsString().equals(Lng.str("CONTROLS")) && event.getWindow().getTabs().size() == 2) { //Make sure we aren't adding a duplicate tab
					GUIContentPane modControlsPane = event.getWindow().addTab(Lng.str("MOD CONTROLS")); //Todo: StarLoader will support mod controls and settings in these menus next update, so we can remove this later
					GUITabbedContent tabbedContent = new GUITabbedContent(modControlsPane.getState(), modControlsPane.getContent(0));
					tabbedContent.activationInterface = event.getWindow().activeInterface;
					tabbedContent.onInit();
					tabbedContent.setPos(0, 2, 0);
					modControlsPane.getContent(0).attach(tabbedContent);

					for(StarMod mod : ControlBindingData.getBindings().keySet()) {
						ArrayList<ControlBindingData> modBindings = ControlBindingData.getBindings().get(mod);
						if(!modBindings.isEmpty()) {
							GUIContentPane modTab = tabbedContent.addTab(mod.getName().toUpperCase(Locale.ENGLISH));
							ControlBindingsScrollableList scrollableList = new ControlBindingsScrollableList(modTab.getState(), modTab.getContent(0), mod);
							scrollableList.onInit();
							modTab.getContent(0).attach(scrollableList);
						}
					}
				}

				if(GameClient.getClientPlayerState() != null && BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
					for(String disabledTab : disabledTabs) {
						if(event.getTitleAsString().equals(Lng.str(disabledTab))) {
							event.setCanceled(true);
							event.getWindow().cleanUp();
						}
					}
				}
			}
		}, instance);

		StarLoader.registerListener(PlayerSpawnEvent.class, new Listener<PlayerSpawnEvent>() {
			@Override
			public void onEvent(PlayerSpawnEvent event) {
				if(event.getPlayer().isOnServer()) {
					PlayerDataManager.getInstance(event.getPlayer().isOnServer()).sendAllDataToPlayer(event.getPlayer().getOwnerState());
					BuildSectorDataManager.getInstance(event.getPlayer().isOnServer()).sendAllDataToPlayer(event.getPlayer().getOwnerState());
					ExchangeDataManager.getInstance(event.getPlayer().isOnServer()).sendAllDataToPlayer(event.getPlayer().getOwnerState());
				}
			}
		}, instance);

		/*StarLoader.registerListener(PlayerChangeSectorEvent.class, new Listener<PlayerChangeSectorEvent>() {
			@Override
			public void onEvent(PlayerChangeSectorEvent event) {
				try {
					int oldSectorId = event.getOldSectorId();
					int newSectorId = event.getNewSectorId();
					if(oldSectorId == newSectorId) return;
					if(!event.getPlayerState().isOnServer()) {
						RemoteSector oldSector = (RemoteSector) event.getPlayerState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(oldSectorId);
						RemoteSector newSector = (RemoteSector) event.getPlayerState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(newSectorId);
						if(oldSector == null || newSector == null) return;
						if(BuildSectorDataManager.getInstance(false).isBuildSector(oldSector.clientPos())) {
							event.getPlayerState().getControllerState().forcePlayerOutOfSegmentControllers();
							event.getPlayerState().getControllerState().forcePlayerOutOfShips();
							if(!event.getPlayerState().isAdmin()) event.getPlayerState().setHasCreativeMode(false);
							event.getPlayerState().setUseCreativeMode(false);
						}
					} else {
						Sector oldSector = GameServer.getServerState().getUniverse().getSector(oldSectorId);
						Sector newSector = GameServer.getServerState().getUniverse().getSector(newSectorId);
						if(oldSector == null || newSector == null) return;
						if(BuildSectorDataManager.getInstance(true).isBuildSector(oldSector.pos)) {
							event.getPlayerState().getControllerState().forcePlayerOutOfSegmentControllers();
							event.getPlayerState().getControllerState().forcePlayerOutOfShips();
							if(!event.getPlayerState().isAdmin()) event.getPlayerState().setHasCreativeMode(false);
							event.getPlayerState().setUseCreativeMode(false);
						}
					}
				} catch(Exception exception) {
					instance.logException("Failed to retrieve sectors during PlayerChangeSectorEvent for player " + event.getPlayerState().getName(), exception);
				}
			}
		}, instance);*/

		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(final PlayerJoinWorldEvent event) {
				if(event.isServer()) {
					(new Thread("EdenCore_Player_Join_World_Thread") {
						@Override
						public void run() {
							try {
								sleep(5000);
								PlayerDataManager.getInstance(event.getPlayerState().isOnServer()).createMissingData(event.getPlayerState().getName()); // Create missing player data if it doesn't exist
//								BuildSectorDataManager.getInstance(event.getPlayerState().isOnServer()).createMissingData(event.getPlayerState().getName()); // Create missing build sector data if it doesn't exist
							} catch(Exception exception) {
								instance.logException("Failed to create missing data for player " + event.getPlayerState().getName(), exception);
							}
						}
					}).start();

					/*(new Thread("EdenCore_Player_Login_Reward_Timer") {
						@Override
						public void run() {
							try {
								sleep(ConfigManager.getMainConfig().getLong("player_login_reward_timer"));
								PlayerState playerState = event.getPlayerState();
								if(playerState != null && playerState.spawnedOnce) {
									Inventory inventory = playerState.getInventory();
									if(inventory.isInfinite()) { //They are in creative mode, we need to get their survival inventory specifically
										Field infiniteField = AbstractOwnerState.class.getDeclaredField("inventory");
										infiniteField.setAccessible(true);
										inventory = (Inventory) infiniteField.get(playerState);
									}
									int prizeBarCount = 1; //Todo: Donators get extra bars
									InventoryUtils.addItem(inventory, ElementManager.getItem("Gold Bar").getId(), prizeBarCount);
									PlayerUtils.sendMessage(playerState, "You have received " + prizeBarCount + " Gold Bars for logging in today! Thanks for playing!");
								}
							} catch(Exception exception) {
								instance.logException("Failed to start player login reward timer for player " + event.getPlayerState().getName(), exception);
							}
						}
					}).start();*/
				}
			}
		}, instance);

		StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
			@Override
			public void onEvent(GUITopBarCreateEvent event) {
				BuildSectorDataManager.getInstance(false);
				PlayerDataManager.getInstance(false);
				ExchangeDataManager.getInstance(false);

				GUITopBar.ExpandedButton dropDownButton = event.getDropdownButtons().get(event.getDropdownButtons().size() - 1);
				dropDownButton.addExpandedButton("BANKING", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) (new BankingDialog()).activate();
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return false;
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});
				dropDownButton.addExpandedButton("GUIDE", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse() && ModGUIHandler.getGUIControlManager("glossarPanel") != null) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
							ModGUIHandler.getGUIControlManager("glossarPanel").setActive(true);
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return false;
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});
				/*dropDownButton.addExpandedButton("BUILD SECTOR", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
							(new BuildSectorDialog()).activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return false;
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});*/
			/*	dropDownButton.addExpandedButton("EXCHANGE", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
							(new ExchangeDialog()).activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return false;
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});*/
				dropDownButton.addExpandedButton("DISCORD", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							try {
								String discordURL = "https://discord.gg/" + ConfigManager.getMainConfig().getConfigurableValue("discord_invite_code", "kcb84yRwHU");
								//Open in the default browser
								if(!discordURL.isEmpty()) {
									Desktop.getDesktop().browse(URI.create(discordURL));
								} else {
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - error");
									PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Discord link is not set in the config file! Notify an admin!");
								}
							} catch(Exception exception) {
								instance.logException("Failed to open Discord link", exception);
							}
						}
					}

					@Override
					public boolean isOccluded() {
						return false;
					}
				}, new GUIActivationHighlightCallback() {
					@Override
					public boolean isHighlighted(InputState inputState) {
						return false;
					}

					@Override
					public boolean isVisible(InputState inputState) {
						return true;
					}

					@Override
					public boolean isActive(InputState inputState) {
						return true;
					}
				});
			}
		}, instance);

		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent event) {
				if(event.getKey() != 0 && event.isKeyDown()) {
					for(ControlBindingData bindingData : ControlBindingData.getModBindings(instance)) {
						if(event.getKey() == bindingData.getBinding()) {
							switch(bindingData.getName()) {
								case "Open Guide":
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
									GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
									ModGUIHandler.getGUIControlManager("glossarPanel").setActive(true);
									return;
								case "Open Banking Menu":
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
									GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
									(new BankingDialog()).activate();
									return;
								/*case "Open Exchange Menu":
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
									GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
									(new ExchangeDialog()).activate();
									return;*/
								/*case "Open Build Sector Menu":
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
									GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().deactivateAll();
									(new BuildSectorDialog()).activate();
									return;*/
							}
						}
					}
				}
			}
		}, instance);

		/*StarLoader.registerListener(GUIElementInstansiateEvent.class, new Listener<GUIElementInstansiateEvent>() {
			@Override
			public void onEvent(GUIElementInstansiateEvent event) {
				if(event.getGUIElement() instanceof GUIResizableGrabbableWindow) {
					GUIResizableGrabbableWindow window = (GUIResizableGrabbableWindow) event.getGUIElement();
					if(GameClient.getClientState() != null && GameClient.getClientPlayerState() != null) {
						if(BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(GameClient.getClientPlayerState())) {
							for(String windowID : disabledWindows) {
								if(window.getWindowId() == null) continue;
								if(window.getWindowId().equals(windowID) || window.getWindowId().equals(Lng.str(windowID))) {
									window.cleanUp();
									event.setCanceled(true);
								}
							}
						}
					}
				}
			}
		}, instance);*/

		/*StarLoader.registerListener(SimulationJobExecuteEvent.class, new Listener<SimulationJobExecuteEvent>() {
			@Override
			public void onEvent(SimulationJobExecuteEvent event) {
				if(event.getSimulationJob() instanceof SpawnPiratePatrolPartyJob) {
					SpawnPiratePatrolPartyJob job = (SpawnPiratePatrolPartyJob) event.getSimulationJob();
					Vector3i from = (Vector3i) ClassUtils.getField(job, "from");
					Vector3i to = (Vector3i) ClassUtils.getField(job, "to");
					if(BuildSectorDataManager.getInstance(false).isBuildSector(from) || BuildSectorDataManager.getInstance(false).isBuildSector(to)) event.setCanceled(true);
				} else if(BuildSectorDataManager.getInstance(false).isBuildSector(event.getStartLocation())) event.setCanceled(true);
			}
		}, instance);*/

		/*StarLoader.registerListener(RegisterWorldDrawersEvent.class, new Listener<RegisterWorldDrawersEvent>() {
			@Override
			public void onEvent(RegisterWorldDrawersEvent event) {
				event.getModDrawables().add(new BuildSectorHudDrawer());
			}
		}, instance);*/

		/*StarLoader.registerListener(SegmentPieceActivateEvent.class, new Listener<SegmentPieceActivateEvent>() {
			@Override
			public void onEvent(SegmentPieceActivateEvent event) {
				try {
					if(BuildSectorDataManager.getInstance(false).isBuildSector(event.getSegmentPiece().getSegmentController().getSector(new Vector3i()))) {
						for(short id : disabledBlocks) {
							if(event.getSegmentPiece().getType() == id) {
								event.setCanceled(true);
								return;
							}
						}
					}
				} catch(Exception exception) {
					instance.logException("Encountered an exception while trying to enforce permissions in a build sector", exception);
				}
			}
		}, instance);*/

		/*StarLoader.registerListener(SegmentPieceActivateByPlayer.class, new Listener<SegmentPieceActivateByPlayer>() {
			@Override
			public void onEvent(SegmentPieceActivateByPlayer event) {
				try {
					if(BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(event.getPlayer())) {
						for(short id : disabledBlocks) {
							if(event.getSegmentPiece().getType() == id && !event.getPlayer().isAdmin()) {
								event.setCanceled(true);
								return;
							}
						}
					}
				} catch(Exception exception) {
					instance.logException("Encountered an exception while trying to enforce permissions in a build sector", exception);
				}
			}
		}, instance);*/
	}
}
