package atlas.core.manager;

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
import atlas.core.AtlasCore;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.data.player.PlayerDataManager;
import atlas.core.gui.controls.ControlBindingsScrollableList;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.input.InputState;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

/**
 * AtlasCore event manager. Handles only core-level events; sub-mod-specific
 * behavior is delegated via {@link IAtlasSubMod} callbacks on {@link SubModRegistry}.
 */
public class EventManager {

	public static void initialize(final AtlasCore instance) {

		// MOD CONTROLS tab in the settings/controls window
		StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
			@Override
			public void onEvent(MainWindowTabAddEvent event) {
				if(event.getTitleAsString().equals(Lng.str("Keyboard"))) {
					event.getPane().getTabNameText().setTextSimple(Lng.str("KEYBOARD"));
				} else if(event.getTitleAsString().equals(Lng.str("CONTROLS")) && event.getWindow().getTabs().size() == 2) {
					GUIContentPane modControlsPane = event.getWindow().addTab(Lng.str("MOD CONTROLS"));
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
			}
		}, instance);

		// Sync AtlasCore's own PLAYER_DATA; then delegate to sub-mods
		StarLoader.registerListener(PlayerSpawnEvent.class, new Listener<PlayerSpawnEvent>() {
			@Override
			public void onEvent(PlayerSpawnEvent event) {
				if(event.getPlayer().isOnServer()) {
					PlayerDataManager.getInstance(true).sendAllDataToPlayer(event.getPlayer().getOwnerState());
					for(IAtlasSubMod subMod : SubModRegistry.getAll()) {
						subMod.onPlayerSpawn(event);
					}
				}
			}
		}, instance);

		// Create missing PlayerData on join; then delegate to sub-mods
		StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
			@Override
			public void onEvent(final PlayerJoinWorldEvent event) {
				if(event.isServer()) {
					(new Thread("AtlasCore_Player_Join_World_Thread") {
						@Override
						public void run() {
							try {
								sleep(5000);
								PlayerDataManager.getInstance(true).createMissingData(event.getPlayerState().getName());
								for(IAtlasSubMod subMod : SubModRegistry.getAll()) {
									subMod.onPlayerJoinWorld(event);
								}
							} catch(Exception exception) {
								instance.logException("Failed to initialize data for player " + event.getPlayerState().getName(), exception);
							}
						}
					}).start();
				}
			}
		}, instance);

		// Top bar: sub-mods add their buttons; DISCORD is always added by AtlasCore last
		StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
			@Override
			public void onEvent(GUITopBarCreateEvent event) {
				GUITopBar.ExpandedButton dropDownButton = event.getDropdownButtons().get(event.getDropdownButtons().size() - 1);

				for(IAtlasSubMod subMod : SubModRegistry.getAll()) {
					subMod.registerTopBarButtons(dropDownButton);
				}

				// DISCORD — always present as a core button
				dropDownButton.addExpandedButton("DISCORD", new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
							try {
								String discordURL = "https://discord.gg/" + ConfigManager.getDiscordInviteCode();
								if(!discordURL.isEmpty()) {
									Desktop.getDesktop().browse(URI.create(discordURL));
								} else {
									GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - error");
									PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Discord link is not configured!");
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
					public boolean isHighlighted(InputState inputState) { return false; }
					@Override
					public boolean isVisible(InputState inputState) { return true; }
					@Override
					public boolean isActive(InputState inputState) { return true; }
				});
			}
		}, instance);

		// KeyPress: delegate to sub-mods by their registered binding names
		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent event) {
				if(event.getKey() != 0 && event.isKeyDown()) {
					for(IAtlasSubMod subMod : SubModRegistry.getAll()) {
						for(ControlBindingData bindingData : ControlBindingData.getModBindings(subMod.getMod())) {
							if(event.getKey() == bindingData.getBinding()) {
								subMod.onKeyPress(bindingData.getName());
								return;
							}
						}
					}
				}
			}
		}, instance);
	}
}
