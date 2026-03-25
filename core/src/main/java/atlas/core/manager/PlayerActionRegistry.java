package atlas.core.manager;

import atlas.core.AtlasCore;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for player action handlers. Replaces the old hardcoded
 * {@code PlayerActionManager} switch statement so sub-mods can register their
 * own action types.
 *
 * <p>Usage — in a sub-mod's {@code onAtlasCoreReady()}:
 * <pre>
 *   public static final int MY_ACTION = PlayerActionRegistry.register(args -> { ... });
 * </pre>
 * Then send {@code new PlayerActionCommandPacket(MY_ACTION, ...)} to trigger it.
 */
public final class PlayerActionRegistry {

    public interface ActionHandler {
        void process(String[] args);
    }

    private static final Map<Integer, ActionHandler> handlers = new HashMap<>();
    private static int nextId = 0;

    private PlayerActionRegistry() {}

    /**
     * Registers an action handler and returns the integer type ID that should be
     * stored as a constant and passed to {@code PlayerActionCommandPacket}.
     */
    public static int register(ActionHandler handler) {
        int id = nextId++;
        handlers.put(id, handler);
        return id;
    }

    public static void process(int type, String[] args) {
        ActionHandler handler = handlers.get(type);
        if (handler != null) {
            handler.process(args);
        } else {
            AtlasCore.getInstance().logWarning("No handler registered for action type " + type);
        }
    }
}
