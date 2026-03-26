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
import atlas.core.utils.EntityUtils;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
import atlas.exchange.element.ElementRegistry;
import atlas.exchange.gui.ExchangeDialog;
import atlas.exchange.tests.ExchangeDataTest;
import com.bulletphysics.linearmath.Transform;
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
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
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
	 * Returns the staging sector position for {@code playerName}.
	 *
	 * <p>Each player gets a unique sector at extreme coordinates that will never
	 * be naturally loaded by StarMade (no players or generation activity will
	 * occur there). Virtual blueprint entities are stored here between purchase
	 * and the buyer loading them into a shipyard.</p>
	 */
	private static Vector3i getStagingSector(String playerName) {
		int hash = Math.abs(playerName.hashCode()) % 1000000;
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
	private static void giveDesignItem(PlayerState buyer, String catalogName) {
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
				return;
			}
			SegmentController entity = outline.spawn(stagingPos, false, new ChildStats(false), new SegmentControllerSpawnCallbackDirect(GameServerState.instance, stagingPos) {
				@Override
				public void onNoDocker() {
				}
			});
			if(entity == null) {
				buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] Failed to create design entity."}, ServerMessage.MESSAGE_TYPE_ERROR));
				return;
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
				return;
			}
			buyer.getInventory().put(slot, meta);
			buyer.getInventory().sendInventoryModification(slot);

			// Record the new design UID so the next cleanup pass won't delete it.
			if(playerData != null) {
				playerData.setPendingExchangeDesignUID(entity.getUniqueIdentifier());
				pdm.updateData(playerData, true);
			}
		} catch(Exception e) {
			instance.logException("[Exchange] Failed to create design item for " + playerName, e);
			buyer.sendServerMessage(new ServerMessage(new Object[]{"[Exchange] An error occurred creating your design. Please contact an admin."}, ServerMessage.MESSAGE_TYPE_ERROR));
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
