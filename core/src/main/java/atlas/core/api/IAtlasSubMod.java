package atlas.core.api;

import api.listener.events.player.PlayerJoinWorldEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarMod;
import org.schema.game.client.view.gui.newgui.GUITopBar;

/**
 * Interface implemented by all Atlas sub-mods. Sub-mods register themselves with
 * {@link SubModRegistry} during their {@code onEnable()} and receive a callback
 * via {@link #onAtlasCoreReady()} once AtlasCore has fully initialized.
 */
public interface IAtlasSubMod {

    /** Unique identifier for this sub-mod (e.g. "atlas_guide", "atlas_banking"). */
    String getModId();

    /** The StarMod instance for this sub-mod. */
    StarMod getMod();

    /**
     * Called by AtlasCore once it has completed initialization and all sub-mods have
     * registered. Sub-mods should register data types, action handlers, elements,
     * commands, and keybinds here.
     */
    void onAtlasCoreReady();

    /**
     * Called during {@code GUITopBarCreateEvent} so sub-mods can add their own buttons
     * to the player dropdown without coupling to AtlasCore's EventManager.
     */
    default void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {}

    /**
     * Called during {@code PlayerSpawnEvent} on the server so sub-mods can send their
     * own data to the spawning player.
     */
    default void onPlayerSpawn(PlayerSpawnEvent event) {}

    /**
     * Called during {@code PlayerJoinWorldEvent} on the server so sub-mods can
     * initialize missing data for newly joined players.
     */
    default void onPlayerJoinWorld(PlayerJoinWorldEvent event) {}

    /**
     * Called during {@code KeyPressEvent} for each keybinding registered by this
     * sub-mod. Sub-mods should check {@code bindingName} and act accordingly.
     */
    default void onKeyPress(String bindingName) {}
}
