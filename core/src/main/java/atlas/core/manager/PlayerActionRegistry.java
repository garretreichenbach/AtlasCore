package atlas.core.manager;

import atlas.core.AtlasCore;
import org.schema.game.common.data.player.PlayerState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for player action handlers. Replaces the old hardcoded
 * {@code PlayerActionManager} switch statement so sub-mods can register their
 * own action types.
 *
 * <p>Usage — in a sub-mod's {@code onAtlasCoreReady()}:
 * <pre>
 *   public static final String MY_ACTION =
 *       PlayerActionRegistry.register("atlas_mymod:my_action", (args, sender) -> { ... });
 * </pre>
 * Then send {@code new PlayerActionCommandPacket(MY_ACTION, ...)} to trigger it.
 *
 * <p><b>Why a String key, not an int</b>: action keys are sent over the wire and
 * looked up on the receiving side. The old design handed out sequential int ids
 * ({@code nextId++}) in sub-mod registration order, so the same int meant
 * different actions on a client and server whenever their installed module set or
 * load order differed (which is explicitly supported — modules are independently
 * installable). That silently dispatched the wrong handler. A stable string key
 * is resolved by identity, so it is correct regardless of which modules are
 * present or in what order they registered.
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

	private static final Map<String, ActionHandler> handlers = new ConcurrentHashMap<>();

	private PlayerActionRegistry() {
	}

	/**
	 * Registers an action handler under a stable string key and returns that key,
	 * so callers can store it as a constant and pass it to
	 * {@code PlayerActionCommandPacket}.
	 *
	 * @param key a stable, globally-unique key (convention: {@code "modId:action"}).
	 *            Must be identical on client and server for the action to dispatch.
	 * @throws IllegalArgumentException if {@code key} is null/empty or already registered.
	 */
	public static String register(String key, ActionHandler handler) {
		if(key == null || key.isEmpty()) throw new IllegalArgumentException("Action key must be non-empty");
		if(handler == null) throw new IllegalArgumentException("Action handler must be non-null");
		ActionHandler previous = handlers.putIfAbsent(key, handler);
		if(previous != null) throw new IllegalArgumentException("Duplicate player action key: " + key);
		return key;
	}

	/**
	 * Dispatches an action. Called by {@code PlayerActionCommandPacket}.
	 *
	 * @param sender {@code null} on the client, non-null on the server.
	 */
	public static void process(String type, String[] args, PlayerState sender) {
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
