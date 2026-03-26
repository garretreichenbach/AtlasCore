package atlas.core.manager;

import atlas.core.AtlasCore;
import org.schema.game.common.data.player.PlayerState;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for player action handlers. Replaces the old hardcoded
 * {@code PlayerActionManager} switch statement so sub-mods can register their
 * own action types.
 *
 * <p>Usage — in a sub-mod's {@code onAtlasCoreReady()}:
 * <pre>
 *   public static final int MY_ACTION = PlayerActionRegistry.register((args, sender) -> { ... });
 * </pre>
 * Then send {@code new PlayerActionCommandPacket(MY_ACTION, ...)} to trigger it.
 *
 * <p><b>Security note</b>: when an action is processed server-side, {@code sender}
 * is the authenticated {@link PlayerState} of the player who sent the packet.
 * Server-side handlers must use {@code sender} as the authoritative identity and
 * must <em>never</em> trust a player name passed in {@code args} for permission
 * checks — args are fully client-controlled and can be spoofed.
 *
 * <p>When processed client-side (e.g. a packet sent back from the server),
 * {@code sender} is {@code null}.
 */
public final class PlayerActionRegistry {

	private static final Map<Integer, ActionHandler> handlers = new HashMap<>();
	private static int nextId;

	private PlayerActionRegistry() {
	}

	/**
	 * Registers an action handler and returns the integer type ID that should be
	 * stored as a constant and passed to {@code PlayerActionCommandPacket}.
	 */
	public static int register(ActionHandler handler) {
		int id = nextId;
		nextId++;
		handlers.put(id, handler);
		return id;
	}

	/**
	 * Dispatches an action. Called by {@code PlayerActionCommandPacket}.
	 *
	 * @param sender {@code null} on the client, non-null on the server.
	 */
	public static void process(int type, String[] args, PlayerState sender) {
		ActionHandler handler = handlers.get(type);
		if(handler != null) {
			handler.process(args, sender);
		} else {
			AtlasCore.getInstance().logWarning("No handler registered for action type " + type);
		}
	}

	@FunctionalInterface
	public interface ActionHandler {
		/**
		 * @param args   Packet arguments (client-supplied; do not trust for identity).
		 * @param sender The authenticated sending player, or {@code null} when
		 *               executing on the client side.
		 */
		void process(String[] args, PlayerState sender);
	}
}
