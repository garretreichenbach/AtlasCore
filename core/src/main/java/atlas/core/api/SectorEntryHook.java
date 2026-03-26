package atlas.core.api;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension point that lets sub-mods gate sector entry server-side.
 *
 * <p>Sub-mods register a {@link Validator} in {@code onAtlasCoreReady()}. AtlasCore's
 * {@code MixinSectorSwitch} calls every registered validator before any sector transition
 * (regardless of jump type). If any validator denies entry the transition is cancelled and
 * the supplied message array's first element (index 0) is sent to the player.
 *
 * <p>Example registration in AtlasBuildSectors:
 * <pre>{@code
 * SectorEntryHook.register((player, dest, msg) -> {
 *     BuildSectorData sector = BuildSectorDataManager.getInstance(true).getFromSector(dest, true);
 *     if (sector == null) return true;  // not a build sector
 *     boolean ok = sector.getOwner().equals(player.getName())
 *                  || sector.getPermissionsForUser(player.getName()) != null;
 *     if (!ok) msg[0] = "This is a private build sector — use the Build Sector menu to enter.";
 *     return ok;
 * });
 * }</pre>
 */
public final class SectorEntryHook {

    public interface Validator {
        /**
         * Returns {@code true} to permit entry, {@code false} to block it.
         *
         * @param player  the player attempting to enter
         * @param dest    destination sector coordinates
         * @param message single-element array; set {@code message[0]} to the denial reason
         */
        boolean canEnter(PlayerState player, Vector3i dest, String[] message);
    }

    private static final List<Validator> validators = new ArrayList<>();

    private SectorEntryHook() {}

    /** Registers a validator. Call from {@link IAtlasSubMod#onAtlasCoreReady()}. */
    public static void register(Validator validator) {
        validators.add(validator);
    }

    /**
     * Runs all registered validators for the given player and destination.
     *
     * @param message single-element array populated with the denial reason if any validator rejects
     * @return {@code true} if all validators permit entry
     */
    public static boolean checkAll(PlayerState player, Vector3i dest, String[] message) {
        for (Validator v : validators) {
            if (!v.canEnter(player, dest, message)) return false;
        }
        return true;
    }
}
