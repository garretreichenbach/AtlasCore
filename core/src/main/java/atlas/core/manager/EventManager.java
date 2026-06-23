package atlas.core.manager;

import api.common.GameClient;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.utils.game.PlayerUtils;
import atlas.core.AtlasCore;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.player.PlayerDataManager;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;

import java.awt.*;
import java.net.URI;

/**
 * AtlasCore event manager. Handles only core-level events; sub-mod-specific
 * behavior is delegated via {@link IAtlasSubMod} callbacks on {@link SubModRegistry}.
 *
 * <p>Key bindings are no longer managed here — sub-mods register their own bindings
 * through StarMade's official {@code KeyboardMappings} API and listen for
 * {@code KeyPressEvent} directly, so they appear in the game's own controls menu.
 */
public class EventManager {

	public static void initialize(final AtlasCore instance) {

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
	}
}
