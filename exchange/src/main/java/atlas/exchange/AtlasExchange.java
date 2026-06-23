package atlas.exchange;

import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.element.Blocks;
import api.utils.game.inventory.InventoryUtils;
import api.utils.textures.StarLoaderTexture;
import atlas.core.api.IAtlasSubMod;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import atlas.core.manager.PlayerActionRegistry;
import atlas.core.utils.EntityUtils;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
import atlas.exchange.gui.ExchangeDialog;
import atlas.exchange.tests.ExchangeDataTest;
import com.bulletphysics.linearmath.Transform;
import org.schema.game.client.view.mainmenu.GuidesRegistry;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.element.meta.BlueprintMetaItem;
import org.schema.game.common.data.element.meta.MetaObjectManager;
import org.schema.game.common.data.element.meta.VirtualBlueprintMetaItem;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.NoSlotFreeException;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.SegmentControllerSpawnCallbackDirect;
import org.schema.game.server.test.TestRegistry;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import org.schema.schine.input.KeyboardContext;
import org.schema.schine.input.KeyboardMappings;
import org.schema.schine.network.server.ServerMessage;

import java.util.ArrayList;
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
	 * Serializes the buy transaction so concurrent / rapid purchases cannot
	 * double-spend (deduct + give + credit must be atomic per the whole exchange).
	 */
	private static final Object EXCHANGE_LOCK = new Object();
	/**
	 * Purchases an ITEM/WEAPON listing: deducts Gold Bars and gives the listed item.
	 * args: [listingUUID]. All price/item/seller data comes from the server-side listing.
	 */
	public static String BUY_ITEM;
	/**
	 * Purchases a blueprint listing: deducts Gold Bars from buyer, gives an
	 * empty {@link BlueprintMetaItem} (goal filled, progress empty), and
	 * credits the seller. args: [listingUUID].
	 */
	public static String BUY_BLUEPRINT;
	/**
	 * Purchases a blueprint listing as a shipyard design item. args: [listingUUID].
	 */
	public static String BUY_DESIGN;
	/**
	 * Creates a marketplace listing. args: [serializedExchangeDataJSON].
	 * The server forces {@code producer = sender}, clamps price/count, and
	 * requires the sender to be in a faction or be an admin.
	 */
	public static String ADD_LISTING;
	/**
	 * Removes a marketplace listing. args: [listingUUID]. Only the listing's
	 * producer or an admin may remove it.
	 */
	public static String REMOVE_LISTING;
	private static AtlasExchange instance;
	private KeyboardMappings openExchangeKey;

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
		if(listing.getPrice() <= 0) return false; // invalid/listing-manipulated price
		if(!buyer.getInventory().hasFreeSlot()) return false;
		short goldBarId = Blocks.GOLD_BAR.getId();
		// If the currency block is unavailable we must NOT hand out the item for free.
		if(goldBarId == -1) return false;
		if(InventoryUtils.getItemAmount(buyer.getInventory(), goldBarId) < listing.getPrice()) return false;
		InventoryUtils.consumeItems(buyer.getInventory(), goldBarId, listing.getPrice());
		return true;
	}

	/** Refunds {@code price} Gold Bars to the buyer when a purchase fails after deduction. */
	private static void refundGoldBars(PlayerState buyer, int price) {
		short goldBarId = Blocks.GOLD_BAR.getId();
		if(goldBarId != -1 && price > 0) InventoryUtils.addItem(buyer.getInventory(), goldBarId, price);
	}

	/** Gives the buyer the item/weapon described by the server-side listing. */
	private static boolean giveListingItem(PlayerState buyer, ExchangeData listing) {
		int count = (listing.getCategory() == ExchangeData.ExchangeDataCategory.WEAPON) ? 1 : listing.getItemCount();
		if(count <= 0) return false;
		InventoryUtils.addItem(buyer.getInventory(), listing.getItemId(), count);
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
	private static boolean giveBlueprintItem(PlayerState buyer, String catalogName) {
		BlueprintMetaItem meta = (BlueprintMetaItem) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.BLUEPRINT.type, (short) -1, true);
		meta.blueprintName = catalogName;
		if(meta.goal == null) meta.goal = new ElementCountMap();
		if(meta.progress == null) meta.progress = new ElementCountMap();
		int slot;
		try {
			slot = buyer.getInventory().getFreeSlot();
		} catch(NoSlotFreeException e) {
			return false; // caller refunds
		}
		buyer.getInventory().put(slot, meta);
		buyer.getInventory().sendInventoryModification(slot);
		return true;
	}

	/**
	 * Returns the staging sector position for {@code playerName}.
	 *
	 * <p>Each player gets a unique sector at extreme coordinates that will never
	 * be naturally loaded by StarMade (no players or generation activity will
	 * occur there). Virtual blueprint entities are stored here between purchase
	 * and the buyer loading them into a shipyard.</p>
	 */
	private static Vector3i getStagingSector(String playerName) {
		int hash = Math.abs(playerName.hashCode()) % 875800;
		return new Vector3i(1500000 + hash, 0, 0);
	}

	/**
	 * Spawns a virtual blueprint entity from {@code catalogName} in the buyer's
	 * dedicated staging sector, persists it to the database, removes it from the
	 * active sector, and gives the buyer a {@link VirtualBlueprintMetaItem}.
	 *
	 * <p>The staging sector sits at extreme coordinates and is never naturally
	 * loaded by the engine, so the entity safely lives in the database until the
	 * buyer loads it into a shipyard via LOAD_DESIGN. The only time the sector is
	 * activated is when we explicitly open it for the next purchase, at which
	 * point orphaned virtual entities from previous errors are cleaned up first.
	 * The player's current valid design (tracked in {@link PlayerData}) is always
	 * skipped during cleanup so it cannot be accidentally invalidated.</p>
	 */
	private static boolean giveDesignItem(PlayerState buyer, String catalogName) {
		String playerName = buyer.getName();
		try {
			Vector3i stagingPos = getStagingSector(playerName);
			Sector stagingSector = GameServerState.instance.getUniverse().getSector(stagingPos);

			// Find the UID the player currently owns so we don't delete it during cleanup.
			PlayerDataManager pdm = PlayerDataManager.getInstance(true);
			PlayerData playerData = pdm.getFromName(playerName, true);
			String protectedUID = (playerData != null) ? playerData.getPendingExchangeDesignUID() : null;

			// Permanently delete any orphaned virtual entities left by previous errors.
			// Real entities (asteroids etc. from sector generation) are left untouched.
			for(SimpleTransformableSendableObject<?> e : new ArrayList<>(stagingSector.getEntities())) {
				if(e instanceof SegmentController && ((SegmentController) e).isVirtualBlueprint()) {
					String uid = e.getUniqueIdentifier();
					if(protectedUID == null || !protectedUID.equals(uid)) {
						EntityUtils.delete((SegmentController) e);
					}
				}
			}

			// Spawn the blueprint entity into the staging sector.
			Transform tr = new Transform();
			tr.setIdentity();
			String spawnName = "EXCHANGE_" + playerName + "_" + System.currentTimeMillis();
			SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(GameServerState.instance, catalogName, spawnName, tr, -1, 0, stagingPos, playerName, PlayerState.buffer, null, false, new ChildStats(false));
			if(outline == null) {
				buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] Failed to load blueprint for design."}, ServerMessage.MESSAGE_TYPE_ERROR));
				return false;
			}
			SegmentController entity = outline.spawn(stagingPos, false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServerState.instance, stagingPos) {
				@Override
				public void onNoDocker() {
				}
			});
			if(entity == null) {
				buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] Failed to create design entity."}, ServerMessage.MESSAGE_TYPE_ERROR));
				return false;
			}

			// Mark virtual, persist to DB, then remove from the active sector.
			// Because the staging sector is never naturally loaded, the entity will
			// not be subject to the 60-second virtual-blueprint auto-delete check
			// until the sector is next activated (our next purchase call), at which
			// point the cleanup above handles it before the timer can fire.
			entity.setVirtualBlueprintRecursive(true);
			GameServerState.instance.getController().writeSingleEntityWithDock(entity);
			entity.setMarkedForDeleteVolatileIncludingDocks(true);

			VirtualBlueprintMetaItem meta = (VirtualBlueprintMetaItem) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.VIRTUAL_BLUEPRINT.type, (short) -1, true);
			meta.UID = entity.getUniqueIdentifier();
			meta.virtualName = spawnName;

			int slot;
			try {
				slot = buyer.getInventory().getFreeSlot();
			} catch(NoSlotFreeException ex) {
				buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] No free inventory slot — design was saved. Contact an admin to recover."}, ServerMessage.MESSAGE_TYPE_ERROR));
				return false;
			}
			buyer.getInventory().put(slot, meta);
			buyer.getInventory().sendInventoryModification(slot);

			// Record the new design UID so the next cleanup pass won't delete it.
			if(playerData != null) {
				playerData.setPendingExchangeDesignUID(entity.getUniqueIdentifier());
				pdm.updateData(playerData, true);
			}
			return true;
		} catch(Exception e) {
			instance.logException("[Exchange] Failed to create design item for " + playerName, e);
			buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] An error occurred creating your design. Please contact an admin."}, ServerMessage.MESSAGE_TYPE_ERROR));
			return false;
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
		if(amount <= 0 || Blocks.GOLD_BAR.getId() == -1) return;
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
		short goldBarId = Blocks.GOLD_BAR.getId();
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
		// Registered through StarMade's official keybinding API; appears in the
		// in-game controls menu and is player-remappable.
		openExchangeKey = KeyboardMappings.registerMapping(this, "Open Exchange Menu", GLFW.GLFW_KEY_J, KeyboardContext.GENERAL);
		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent keyEvent) {
				if(keyEvent.isKeyDown() && keyEvent.isMapping(openExchangeKey)) openExchange();
			}
		}, this);
	}

	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(ExchangeDataTest.class);
	}

	@Override
	public void onRegisterGuides(GuidesRegistry.ModGuideRegistrar registrar) {
		registrar.registerFromResource("atlas", "Atlas", "AtlasExchange", "guides/atlas-exchange.md", this);
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
	public void onDisable() {
		retryScheduler.shutdownNow();
	}

	@Override
	public void onAtlasCoreReady() {
		registerExchangeDataType();

		BUY_ITEM = PlayerActionRegistry.register("atlas_exchange:buy_item", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			handleBuy(sender, args[0], BuyKind.ITEM);
		});

		BUY_BLUEPRINT = PlayerActionRegistry.register("atlas_exchange:buy_blueprint", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			handleBuy(sender, args[0], BuyKind.BLUEPRINT);
		});

		BUY_DESIGN = PlayerActionRegistry.register("atlas_exchange:buy_design", (args, sender) -> {
			if(sender == null || args.length < 1) return;
			handleBuy(sender, args[0], BuyKind.DESIGN);
		});

		ADD_LISTING = PlayerActionRegistry.register("atlas_exchange:add_listing", (args, sender) -> {
			// Server-only. args: [serializedExchangeDataJSON].
			if(sender == null || args.length < 1) return;
			// Authorisation mirrors the client gate: faction member or admin.
			if(sender.getFactionId() == 0 && !sender.isAdmin()) {
				logWarning("[Exchange] ADD_LISTING: " + sender.getName() + " is not in a faction and not admin");
				return;
			}
			ExchangeData listing;
			try {
				listing = new ExchangeData(new org.json.JSONObject(args[0]));
			} catch(Exception e) {
				logWarning("[Exchange] ADD_LISTING: malformed listing data from " + sender.getName());
				return;
			}
			// Sanitize client-supplied fields: the seller is always the sender, and
			// price/count are clamped so listings can't be manipulated.
			listing.setProducer(sender.getName());
			if(listing.getPrice() < 1) listing.setPrice(1);
			if((listing.getCategory() == ExchangeData.ExchangeDataCategory.ITEM
				|| listing.getCategory() == ExchangeData.ExchangeDataCategory.WEAPON)
				&& listing.getItemCount() < 1) listing.setItemCount(1);
			if(listing.getName() == null || listing.getName().trim().isEmpty()) return;
			ExchangeDataManager mgr = ExchangeDataManager.getInstance(true);
			for(ExchangeData existing : mgr.getServerCache()) {
				if(existing.getName().equalsIgnoreCase(listing.getName())) {
					logWarning("[Exchange] ADD_LISTING: duplicate name '" + listing.getName() + "'");
					return;
				}
			}
			mgr.addData(listing, true);
		});

		REMOVE_LISTING = PlayerActionRegistry.register("atlas_exchange:remove_listing", (args, sender) -> {
			// Server-only. args: [listingUUID]. Only the producer or an admin may remove.
			if(sender == null || args.length < 1) return;
			ExchangeDataManager mgr = ExchangeDataManager.getInstance(true);
			ExchangeData listing = mgr.getFromUUID(args[0], true);
			if(listing == null) return;
			if(!sender.isAdmin() && !listing.getProducer().equals(sender.getName())) {
				logWarning("[Exchange] REMOVE_LISTING: " + sender.getName() + " may not remove listing owned by " + listing.getProducer());
				return;
			}
			mgr.removeData(listing, true);
		});
	}

	private enum BuyKind {ITEM, BLUEPRINT, DESIGN}

	/**
	 * Executes a purchase atomically: resolve the listing by UUID (server-authoritative),
	 * deduct payment, deliver the goods, and credit the seller — refunding if delivery
	 * fails. Synchronized so concurrent/rapid purchases cannot double-spend.
	 */
	private static void handleBuy(PlayerState buyer, String listingUUID, BuyKind kind) {
		synchronized(EXCHANGE_LOCK) {
			ExchangeData listing = ExchangeDataManager.getInstance(true).getFromUUID(listingUUID, true);
			if(listing == null) return;
			if(!validateAndDeduct(buyer, listing)) return;
			boolean delivered;
			try {
				switch(kind) {
					case ITEM:
						delivered = giveListingItem(buyer, listing);
						break;
					case BLUEPRINT:
						delivered = giveBlueprintItem(buyer, listing.getCatalogName());
						break;
					case DESIGN:
						delivered = giveDesignItem(buyer, listing.getCatalogName());
						break;
					default:
						delivered = false;
				}
			} catch(Exception e) {
				delivered = false;
				instance.logException("[Exchange] purchase delivery failed for " + buyer.getName(), e);
			}
			if(!delivered) {
				refundGoldBars(buyer, listing.getPrice());
				return;
			}
			// Seller is the listing's producer — never a client-supplied argument.
			creditSeller(listing.getProducer(), listing.getPrice());
		}
	}

	@Override
	public void onPlayerSpawn(PlayerSpawnEvent event) {
		if(event.getPlayer().isOnServer()) {
			PlayerState player = event.getPlayer().getOwnerState();
			ExchangeDataManager.getInstance(true).sendAllDataToPlayer(player);
			// Grant the daily login reward (no-op if already claimed today), then
			// deliver any Gold Bars queued while the player was offline.
			grantDailyReward(player);
			attemptPendingCredit(player);
		}
	}

	/**
	 * Grants a once-per-day login reward of 1–3 Gold Bars. Tracked per player by
	 * UTC epoch-day in {@link PlayerData}, so it fires only on the first spawn of a
	 * new day (respawns after death don't re-trigger it). Delivery reuses the
	 * pending-credit system, which handles a full inventory gracefully.
	 *
	 * <p>This is a placeholder economy faucet until a proper contract/mission system exists.</p>
	 */
	private static void grantDailyReward(PlayerState player) {
		if(Blocks.GOLD_BAR.getId() == -1) return;
		PlayerDataManager pdm = PlayerDataManager.getInstance(true);
		PlayerData data = pdm.getFromName(player.getName(), true);
		if(data == null) return;
		long today = System.currentTimeMillis() / 86_400_000L; // UTC epoch-day
		if(data.getLastDailyRewardDay() == today) return; // already claimed today
		int reward = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 4); // 1..3
		data.setLastDailyRewardDay(today);
		pdm.updateData(data, true);
		creditSeller(player.getName(), reward); // queue + deliver via the pending-credit path
		player.sendServerMessage(new ServerMessage(new Object[]{"Daily login reward: " + reward + " Gold Bar(s)! Spend them on the Exchange."}, ServerMessage.MESSAGE_TYPE_INFO));
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
