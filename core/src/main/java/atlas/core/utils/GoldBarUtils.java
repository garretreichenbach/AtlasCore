package atlas.core.utils;

import api.utils.element.Blocks;
import api.utils.game.inventory.InventoryUtils;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.network.server.ServerMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Shared Gold Bar currency helpers for Atlas sub-mods. Gold Bars are a physical inventory
 * block ({@link Blocks#GOLD_BAR}); this centralizes balance checks, atomic deduction, refunds,
 * and the offline/full-inventory pending-delivery retry used by both AtlasExchange and
 * AtlasContracts.
 *
 * <p>Pending (owed) bars are persisted on {@link PlayerData#getPendingExchangeCredits()} — a shared
 * pool, so bars owed from multiple sources accumulate and are delivered together on login.</p>
 */
public final class GoldBarUtils {

	private static final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Atlas-GoldBarRetry");
		t.setDaemon(true);
		return t;
	});

	private GoldBarUtils() {}

	/** The Gold Bar block id, or {@code -1} if the currency block is unavailable. */
	public static short id() {
		return Blocks.GOLD_BAR.getId();
	}

	/** Whether the Gold Bar currency block is available in this configuration. */
	public static boolean available() {
		return id() != -1;
	}

	/** Returns how many Gold Bars {@code player} currently holds (0 if currency unavailable). */
	public static int getBalance(PlayerState player) {
		short id = id();
		if(id == -1) return 0;
		return InventoryUtils.getItemAmount(player.getInventory(), id);
	}

	public static boolean hasEnough(PlayerState player, int amount) {
		return amount > 0 && getBalance(player) >= amount;
	}

	/**
	 * Server-side: validates {@code player} holds at least {@code amount} Gold Bars and consumes them.
	 * Returns {@code false} (and changes nothing) on any failure — never hands out goods for free.
	 */
	public static boolean deduct(PlayerState player, int amount) {
		if(amount <= 0) return false;
		short id = id();
		if(id == -1) return false;
		if(InventoryUtils.getItemAmount(player.getInventory(), id) < amount) return false;
		InventoryUtils.consumeItems(player.getInventory(), id, amount);
		return true;
	}

	/** Best-effort immediate refund of {@code amount} Gold Bars into the player's inventory. */
	public static void refund(PlayerState player, int amount) {
		short id = id();
		if(id != -1 && amount > 0) InventoryUtils.addItem(player.getInventory(), id, amount);
	}

	/**
	 * Server-side: credits {@code amount} Gold Bars to the named player. The amount is added to their
	 * persisted pending pool and delivered immediately if they are online (with a 15-minute retry when
	 * their inventory is full). Offline players receive the bars on their next login via
	 * {@link #deliverPending(PlayerState)}.
	 */
	public static void pay(String playerName, int amount) {
		if(amount <= 0 || id() == -1) return;
		PlayerDataManager pdm = PlayerDataManager.getInstance(true);
		PlayerData data = pdm.getFromName(playerName, true);
		if(data == null) return;
		data.setPendingExchangeCredits(data.getPendingExchangeCredits() + amount);
		pdm.updateData(data, true);
		PlayerState player = null;
		try {
			player = GameServerState.instance.getPlayerFromName(playerName);
		} catch(PlayerNotFountException ignored) {
		}
		if(player != null) deliverPending(player);
	}

	/**
	 * Server-side: delivers all pending Gold Bars owed to {@code player}. If their inventory is full,
	 * informs them and reschedules a retry in 15 minutes. On success the pending pool is zeroed and saved.
	 * Safe to call on every spawn.
	 */
	public static void deliverPending(PlayerState player) {
		PlayerDataManager pdm = PlayerDataManager.getInstance(true);
		PlayerData data = pdm.getFromName(player.getName(), true);
		if(data == null) return;
		int pending = data.getPendingExchangeCredits();
		if(pending <= 0) return;
		short id = id();
		if(id == -1) return;
		if(!player.getInventory().hasFreeSlot()) {
			player.sendServerMessage(new ServerMessage(new Object[]{"[Atlas] You have " + pending + " Gold Bar(s) waiting! Clear some inventory space — we will try again in 15 minutes."}, ServerMessage.MESSAGE_TYPE_WARNING));
			String name = player.getName();
			retryScheduler.schedule(() -> {
				PlayerState s = null;
				try {
					s = GameServerState.instance.getPlayerFromName(name);
				} catch(PlayerNotFountException ignored) {
				}
				if(s != null) deliverPending(s);
			}, 15, TimeUnit.MINUTES);
			return;
		}
		InventoryUtils.addItem(player.getInventory(), id, pending);
		data.setPendingExchangeCredits(0);
		pdm.updateData(data, true);
	}
}
