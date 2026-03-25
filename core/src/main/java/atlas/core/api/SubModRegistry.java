package atlas.core.api;

import atlas.core.AtlasCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for Atlas sub-mods. Each sub-mod calls {@link #register(IAtlasSubMod)}
 * from its {@code onEnable()} so AtlasCore can discover and initialize it.
 */
public final class SubModRegistry {

    private static final List<IAtlasSubMod> registered = new ArrayList<>();

    private SubModRegistry() {}

    /** Registers a sub-mod. Must be called from the sub-mod's {@code onEnable()}. */
    public static void register(IAtlasSubMod subMod) {
        registered.add(subMod);
        AtlasCore.getInstance().logDebug("Registered sub-mod: " + subMod.getModId());
    }

    public static List<IAtlasSubMod> getAll() {
        return Collections.unmodifiableList(registered);
    }

    public static boolean isLoaded(String modId) {
        for (IAtlasSubMod m : registered) {
            if (m.getModId().equals(modId)) return true;
        }
        return false;
    }

    /** Called by AtlasCore in {@code onBlockConfigLoad()} after all mods have enabled. */
    public static void fireAtlasCoreReady() {
        for (IAtlasSubMod m : registered) {
            AtlasCore.getInstance().logDebug("Firing onAtlasCoreReady for: " + m.getModId());
            m.onAtlasCoreReady();
        }
    }
}
