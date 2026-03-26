package atlas.exchange;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarMod;
import api.utils.game.inventory.InventoryUtils;
import api.utils.textures.StarLoaderTexture;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import atlas.core.manager.PlayerActionRegistry;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
import atlas.exchange.element.ElementRegistry;
import atlas.exchange.gui.ExchangeDialog;
import atlas.exchange.tests.ExchangeDataTest;
import com.bulletphysics.linearmath.Transform;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.element.meta.BlueprintMetaItem;
import org.schema.game.common.data.element.meta.MetaObjectManager;
import org.schema.game.common.data.element.meta.VirtualBlueprintMetaItem;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.NoSlotFreeException;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.SegmentControllerSpawnCallbackDirect;
import org.schema.game.server.test.TestRegistry;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.network.server.ServerMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AtlasExchange — player trading and blueprint marketplace.
 * Depends on AtlasCore.
 */
public class AtlasExchange extends StarMod implements IAtlasSubMod {

	/**
	 * Single-thread scheduler used to retry Gold Bar delivery to sellers.
	 */
	private static final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "AtlasExchange-CreditRetry");
		t.setDaemon(true);
		return t;
	});
	/**
	 * Gives a plain item to the authenticated sender (server-side).
	 */
	public static int GIVE_ITEM = -1;
	/**
	 * Credits Gold Bars to a named player (server-side).
	 */
	public static int ADD_BARS = -1;
	/**
	 * Purchases a blueprint listing: deducts Gold Bars from buyer, gives an
	 * empty {@link BlueprintMetaItem} (goal filled, progress empty), and
	 * credits the seller. Replaces the old client-side directBuy spawn.
	 */
	public static int BUY_BLUEPRINT = -1;
	/**
	 * Purchases a blueprint listing as a shipyard design item.
	 * Same transaction as {@link #BUY_BLUEPRINT} but gives a
	 * {@link VirtualBlueprintMetaItem} so the buyer can load it into a
	 * shipyard directly via LOAD_DESIGN.
	 */
	public static int BUY_DESIGN = -1;
	private static AtlasExchange instance;

	public AtlasExchange() {
		instance = this;
	}

	public static AtlasExchange getInstance() {
		return instance;
	}

	// ── StarMod lifecycle ────────────────────────────────────────────────────

	/**
	 * Validates the buyer has enough Gold Bars and a free inventory slot,
	 * then deducts the Gold Bars. Returns false (and takes no action) on failure.
	 */
	private static boolean validateAndDeduct(PlayerState buyer, ExchangeData listing) {
		if(!buyer.getInventory().hasFreeSlot()) return false;
		short goldBarId = ElementRegistry.GOLD_BAR.getId();
		if(goldBarId != -1) {
			if(InventoryUtils.getItemAmount(buyer.getInventory(), goldBarId) < listing.getPrice()) return false;
			InventoryUtils.consumeItems(buyer.getInventory(), goldBarId, listing.getPrice());
		}
		return true;
	}

	/**
	 * Creates an empty {@link BlueprintMetaItem} for {@code catalogName} and
	 * places it in the buyer's inventory.
	 *
	 * <p>The item's {@code blueprintName} is set so the game can look up its
	 * block requirements from the catalog. {@code progress} is intentionally
	 * left empty — the buyer must gather the required materials themselves.</p>
	 */
	private static void giveBlueprintItem(PlayerState buyer, String catalogName) {
		BlueprintMetaItem meta = (BlueprintMetaItem) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.BLUEPRINT.type, (short) -1, true);
		meta.blueprintName = catalogName;
		if(meta.goal == null) meta.goal = new ElementCountMap();
		if(meta.progress == null) meta.progress = new ElementCountMap();
		int slot = 0;
		try {
			slot = buyer.getInventory().getFreeSlot();
		} catch(NoSlotFreeException e) {
			throw new RuntimeException(e);
		}
		buyer.getInventory().put(slot, meta);
		buyer.getInventory().sendInventoryModification(slot);
	}

	/**
	 * Spawns a virtual blueprint entity from {@code catalogName} in the buyer's
	 * sector, immediately persists it to the database, removes it from the active
	 * sector, and gives the buyer a {@link VirtualBlueprintMetaItem} pointing to
	 * that entity's UID. The buyer can then load it into any shipyard via
	 * LOAD_DESIGN without needing a pre-built ship.
	 */
	private static void giveDesignItem(PlayerState buyer, String catalogName) {
		try {
			Transform tr = new Transform();
			tr.setIdentity();
			String spawnName = "EXCHANGE_" + buyer.getName() + "_" + System.currentTimeMillis();
			SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(
					GameServerState.instance,
					catalogName,
					spawnName,
					tr,
					-1,
					0, // neutral faction — design ownership is via the item, not the entity
					buyer.getCurrentSector(),
					buyer.getName(),
					PlayerState.buffer,
					null,
					false,
					new ChildStats(false));
			if(outline == null) {
				buyer.sendServerMessage("[Exchange] Failed to load blueprint for design.");
				return;
			}
			SegmentController entity = outline.spawn(buyer.getCurrentSector(), false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServerState.instance, buyer.getCurrentSector()) {
				@Override
				public void onNoDocker() {
				}
			});
			if(entity == null) {
				buyer.sendServerMessage("[Exchange] Failed to create design entity.");
				return;
			}
			// Mark as virtual, persist to DB, then remove from the active sector.
			// After this sequence the entity lives only in the database; the
			// shipyard's loadDesign() will fetch it by UID when the player uses
			// the item.
			entity.setVirtualBlueprintRecursive(true);
			GameServerState.instance.getController().writeSingleEntityWithDock(entity);
			entity.setMarkedForDeleteVolatileIncludingDocks(true);

			VirtualBlueprintMetaItem meta = (VirtualBlueprintMetaItem) MetaObjectManager.instantiate(
					MetaObjectManager.MetaObjectType.VIRTUAL_BLUEPRINT.type, (short) -1, true);
			meta.UID = entity.getUniqueIdentifier();
			meta.virtualName = spawnName;

			int slot;
			try {
				slot = buyer.getInventory().getFreeSlot();
			} catch(NoSlotFreeException e) {
				// Inventory filled up between validation and here — entity is already
				// persisted so it won't be lost, but we can't deliver the item.
				buyer.sendServerMessage("[Exchange] No free inventory slot — design was saved but could not be delivered. Contact an admin.");
				return;
			}
			buyer.getInventory().put(slot, meta);
			buyer.getInventory().sendInventoryModification(slot);
		} catch(Exception e) {
			instance.logException("[Exchange] Failed to create design item for " + buyer.getName(), e);
			buyer.sendServerMessage("[Exchange] An error occurred creating your design. Please contact an admin.");
		}
	}

	/**
	 * Queues {@code amount} Gold Bars for {@code sellerName} and immediately
	 * attempts delivery if the seller is online. If offline, delivery is
	 * deferred until their next login via {@link #onPlayerSpawn}. The pending
	 * amount is persisted in the seller's {@link PlayerData} so it survives
	 * server restarts.
	 */
	private static void creditSeller(String sellerName, int amount) {
		if(amount <= 0 || ElementRegistry.GOLD_BAR.getId() == -1) return;
		PlayerDataManager pdm = PlayerDataManager.getInstance(true);
		PlayerData data = pdm.getFromName(sellerName, true);
		if(data == null) return;
		data.setPendingExchangeCredits(data.getPendingExchangeCredits() + amount);
		pdm.updateData(data, true);
		PlayerState seller = null;
		try {
			seller = GameServerState.instance.getPlayerFromName(sellerName);
		} catch(PlayerNotFountException ignored) {
		}
		if(seller != null) attemptPendingCredit(seller);
	}

	/**
	 * Attempts to deliver all pending Gold Bars to {@code seller}.
	 *
	 * <ul>
	 *   <li>If the seller's personal inventory (used regardless of creative mode)
	 *       has no free slot, they are informed via server message and a retry is
	 *       scheduled for 15 minutes later.</li>
	 *   <li>On success the pending amount in {@link PlayerData} is zeroed and saved.</li>
	 * </ul>
	 */
	private static void attemptPendingCredit(PlayerState seller) {
		PlayerDataManager pdm = PlayerDataManager.getInstance(true);
		PlayerData data = pdm.getFromName(seller.getName(), true);
		if(data == null) return;
		int pending = data.getPendingExchangeCredits();
		if(pending <= 0) return;
		short goldBarId = ElementRegistry.GOLD_BAR.getId();
		if(goldBarId == -1) return;
		// getInventory() returns the player's personal inventory regardless of
		// creative mode — StarMade creative mode does not use a separate inventory.
		if(!seller.getInventory().hasFreeSlot()) {
			seller.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] You have " + pending + " Gold Bar(s) waiting from a sale! " + "Clear some inventory space — we will try again in 15 minutes."}, ServerMessage.MESSAGE_TYPE_WARNING));
			String name = seller.getName();
			retryScheduler.schedule(() -> {
				PlayerState s = null;
				try {
					s = GameServerState.instance.getPlayerFromName(name);
				} catch(PlayerNotFountException ignored) {

				}
				if(s != null) attemptPendingCredit(s);
			}, 15, TimeUnit.MINUTES);
			return;
		}
		InventoryUtils.addItem(seller.getInventory(), goldBarId, pending);
		data.setPendingExchangeCredits(0);
		pdm.updateData(data, true);
	}

	private static void openExchange() {
		StarLoaderTexture.runOnGraphicsThread(() -> new ExchangeDialog().activate());
	}

	// ── IAtlasSubMod ─────────────────────────────────────────────────────────

	@Override
	public void onEnable() {
		SubModRegistry.register(this);
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		ControlBindingData.load(this);
		ControlBindingData.registerBinding(this, "Open Exchange Menu", "Opens the Exchange menu.", 74 /* J */);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
	}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(ExchangeDataTest.class);
	}

	@Override
	public String getModId() {
		return "atlas_exchange";
	}

	@Override
	public StarMod getMod() {
		return this;
	}

	// ── private helpers ───────────────────────────────────────────────────────

	@Override
	public void registerTopBarButtons(GUITopBar.ExpandedButton playerDropdown) {
		playerDropdown.addExpandedButton("EXCHANGE", new GUICallback() {
			@Override
			public void callback(GUIElement e, MouseEvent event) {
				if(event.pressedLeftMouse()) openExchange();
			}

			@Override
			public boolean isOccluded() {
				return false;
			}
		}, new GUIActivationHighlightCallback() {
			@Override
			public boolean isHighlighted(InputState s) {
				return false;
			}

			@Override
			public boolean isVisible(InputState s) {
				return true;
			}

			@Override
			public boolean isActive(InputState s) {
				return true;
			}
		});
	}

	@Override
	public void onKeyPress(String bindingName) {
		if("Open Exchange Menu".equals(bindingName)) openExchange();
	}

	@Override
	public void onDisable() {
		retryScheduler.shutdownNow();
	}

	@Override
	public void onAtlasCoreReady() {
		registerExchangeDataType();

		GIVE_ITEM = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args: [itemId, count, meta]
			if(sender == null || args.length < 3) return;
			short itemId;
			int count;
			try {
				itemId = Short.parseShort(args[0]);
				count = Integer.parseInt(args[1]);
			} catch(NumberFormatException e) {
				return;
			}
			InventoryUtils.addItem(sender.getInventory(), itemId, count);
		});

		ADD_BARS = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args: [sellerName, amount]
			if(sender == null || args.length < 2) return;
			int amount;
			try {
				amount = Integer.parseInt(args[1]);
			} catch(NumberFormatException e) {
				return;
			}
			creditSeller(args[0], amount);
		});

		BUY_BLUEPRINT = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args: [catalogName, sellerName]
			if(sender == null || args.length < 2) return;
			ExchangeData listing = ExchangeDataManager.getInstance(true).findByCatalogName(args[0]);
			if(listing == null) return;
			if(!validateAndDeduct(sender, listing)) return;
			giveBlueprintItem(sender, args[0]);
			creditSeller(args[1], listing.getPrice());
		});

		BUY_DESIGN = PlayerActionRegistry.register((args, sender) -> {
			// Server-only. args: [catalogName, sellerName]
			if(sender == null || args.length < 2) return;
			ExchangeData listing = ExchangeDataManager.getInstance(true).findByCatalogName(args[0]);
			if(listing == null) return;
			if(!validateAndDeduct(sender, listing)) return;
			giveDesignItem(sender, args[0]);
			creditSeller(args[1], listing.getPrice());
		});
	}

	@Override
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if(event.getPlayer().isOnServer()) {
			PlayerState player = event.getPlayer().getOwnerState();
			ExchangeDataManager.getInstance(true).sendAllDataToPlayer(player);
			// Deliver any Gold Bars that were queued while the player was offline.
			attemptPendingCredit(player);
		}
	}

	private void registerExchangeDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() {
				return "EXCHANGE_DATA";
			}

			@Override
			public atlas.core.data.SerializableData deserializeNetwork(api.network.PacketReadBuffer buf) throws java.io.IOException {
				return new ExchangeData(buf);
			}

			@Override
			public atlas.core.data.SerializableData deserializeJSON(org.json.JSONObject obj) {
				return new ExchangeData(obj);
			}

			@Override
			public atlas.core.data.DataManager<?> getManager(boolean server) {
				return ExchangeDataManager.getInstance(server);
			}
		});
	}
}
