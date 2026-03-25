package org.schema.game.common.data.player;

import api.listener.events.Event;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.utils.gui.ModGUIHandler;
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.bulletphysics.linearmath.Transform;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.opengl.Display;
import org.schema.common.LogUtil;
import org.schema.common.util.StringTools;
import org.schema.common.util.linAlg.Vector3b;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.ClientChannel;
import org.schema.game.client.controller.GameClientController;
import org.schema.game.client.controller.RoundEndMenu;
import org.schema.game.client.controller.manager.ingame.InventoryControllerManager;
import org.schema.game.client.controller.tutorial.states.TeleportToTutorialSector;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.data.GameStateInterface;
import org.schema.game.client.data.PlayerControllable;
import org.schema.game.client.view.camera.InShipCamera;
import org.schema.game.client.view.camera.ObjectViewerCamera;
import org.schema.game.client.view.cubes.shapes.BlockShapeAlgorithm;
import org.schema.game.client.view.cubes.shapes.orientcube.Oriencube;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.catalog.CatalogEntryPanel;
import org.schema.game.client.view.gui.navigation.navigationnew.SavedCoordinatesScrollableListNew;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.Starter;
import org.schema.game.common.controller.*;
import org.schema.game.common.controller.ai.AIGameCreatureConfiguration;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.controller.ai.UnloadedAiContainer;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.controller.database.DatabaseIndex;
import org.schema.game.common.controller.database.FogOfWarReceiver;
import org.schema.game.common.controller.database.tables.FTLTable;
import org.schema.game.common.controller.elements.ActivationManagerInterface;
import org.schema.game.common.controller.elements.Cockpit;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.controller.elements.StationaryManagerContainer;
import org.schema.game.common.controller.elements.warpgate.WarpgateCollectionManager;
import org.schema.game.common.controller.rails.RailRelation;
import org.schema.game.common.controller.rails.RailRequest;
import org.schema.game.common.controller.rules.rules.PlayerRuleEntityManager;
import org.schema.game.common.data.*;
import org.schema.game.common.data.creature.AICreature;
import org.schema.game.common.data.creature.AIPlayer;
import org.schema.game.common.data.element.*;
import org.schema.game.common.data.element.meta.*;
import org.schema.game.common.data.element.meta.weapon.Weapon;
import org.schema.game.common.data.missile.Missile;
import org.schema.game.common.data.physics.PhysicsExt;
import org.schema.game.common.data.player.catalog.PlayerCatalogManager;
import org.schema.game.common.data.player.dialog.PlayerConversationManager;
import org.schema.game.common.data.player.faction.*;
import org.schema.game.common.data.player.faction.config.FactionPointsGeneralConfig;
import org.schema.game.common.data.player.inventory.*;
import org.schema.game.common.data.world.*;
import org.schema.game.common.updater.FileUtil;
import org.schema.game.network.objects.CreateDockRequest;
import org.schema.game.network.objects.DragDrop;
import org.schema.game.network.objects.NetworkPlayer;
import org.schema.game.network.objects.remote.*;
import org.schema.game.server.controller.*;
import org.schema.game.server.data.*;
import org.schema.game.server.data.blueprint.BluePrintSpawnQueueElement;
import org.schema.game.server.data.blueprint.BluePrintWriteQueueElement;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import org.schema.schine.common.JoystickAxisMapping;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.*;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.core.settings.EngineSettings;
import org.schema.schine.graphicsengine.core.settings.StateParameterNotFoundException;
import org.schema.schine.graphicsengine.forms.BoundingBox;
import org.schema.schine.graphicsengine.forms.gui.newgui.DialogInterface;
import org.schema.schine.input.JoystickMappingFile;
import org.schema.schine.input.KeyboardMappings;
import org.schema.schine.network.RegisteredClientOnServer;
import org.schema.schine.network.StateInterface;
import org.schema.schine.network.client.ClientController;
import org.schema.schine.network.client.ClientStateInterface;
import org.schema.schine.network.objects.NetworkEntity;
import org.schema.schine.network.objects.NetworkObject;
import org.schema.schine.network.objects.Sendable;
import org.schema.schine.network.objects.remote.RemoteIntegerArray;
import org.schema.schine.network.objects.remote.RemoteString;
import org.schema.schine.network.objects.remote.RemoteVector3i;
import org.schema.schine.network.server.ServerMessage;
import org.schema.schine.network.server.ServerStateInterface;
import org.schema.schine.resource.DiskWritable;
import org.schema.schine.resource.FileExt;
import org.schema.schine.resource.UniqueInterface;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.ListSpawnObjectCallback;
import org.schema.schine.resource.tag.Tag;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipException;

public final class PlayerState extends AbstractOwnerState implements Sendable, DiskWritable, FogOfWarReceiver, RuleEntityContainer {
	public static final float MAX_HEALTH = 120;
	private static final long DEFAULT_BLUEPRINT_DELAY = 10000;
	public static ByteBuffer buffer = ByteBuffer.allocate(10 * 1024);
	public static Vector3i NO_WAYPOINT = new Vector3i(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE);
	public final PlayerStateSpawnData spawnData;
	private final Vector3f lastForward = new Vector3f(0, 0, 1);
	private final Vector3f lastRight = new Vector3f(1, 0, 0);
	private final Vector3f lastUp = new Vector3f(0, 1, 0);
	private final ByteArrayList keyboardEvents = new ByteArrayList();
	private final ControllerState controllerState;
	private final UploadController shipUploadController;
	private final boolean onServer;
	private final PlayerConversationManager playerConversationManager;
	private final Vector3i currentSector = new Vector3i(Sector.DEFAULT_SECTOR);
	private final Set<ShopInterface> shopsInDistance = new HashSet<ShopInterface>();
	private final PlayerCatalogManager catalog;
	private final ArrayList<Damager> dieList = new ArrayList<Damager>();
	private final ObjectArrayList<SavedCoordinate> savedCoordinates = new ObjectArrayList<SavedCoordinate>();
	private final ObjectArrayFIFOQueue<SavedCoordinate> savedCoordinatesToAdd = new ObjectArrayFIFOQueue<SavedCoordinate>();
	private final SkinUploadController skinUploadController;
	private final SkinManager skinManager;
	private final PlayerFactionController factionController;
	private final long creationTime;
	private final Transform remoteCam = new Transform();
	private final IntArrayList toDrop = new IntArrayList();
	private final Vector3f min = new Vector3f();
	private final Vector3f max = new Vector3f();
	private final Vector3f tmpmin = new Vector3f();
	private final Vector3f tmpmax = new Vector3f();
	private boolean hasCreativeMode;
	private boolean useCreativeMode;
	private final IntArrayFIFOQueue killerIds = new IntArrayFIFOQueue();
	private final ObjectArrayFIFOQueue<InventoryMultMod> queuedModifactions = new ObjectArrayFIFOQueue<InventoryMultMod>();
	private final ObjectArrayFIFOQueue<SimpleCommand<?>> simpleCommandQueue = new ObjectArrayFIFOQueue<SimpleCommand<?>>();
	private final ObjectArrayFIFOQueue<CreatureSpawn> requestedreatureSpawns = new ObjectArrayFIFOQueue<CreatureSpawn>();
	private final PlayerAiManager playerAiManager;
	private final Vector3i currentSystem = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
	private final List<ScanData> scanHistory = new ObjectArrayList<ScanData>();
	private final PlayerChannelManager playerChannelManager;
	public boolean spawnedOnce;
	public long lastSectorProtectedMsgSent;
	public Tag mainInventoryBackup;
	public Tag capsuleRefinerInventoryBackup;
	public Tag microInventoryBackup;
	public Tag factoryInventoryBackup;
	public long inControlTransition;
	public Vector3i personalSector;
	public int friendlyFireStrikes;
	public long lastFriendlyFireStrike;
	public SavedCoordinatesScrollableListNew savedCoordinatesList;
	public boolean hasSpawnWait;
	boolean wasKeyDown[] = new boolean[KeyboardMappings.remoteMappings.length];
	boolean wasMouseEvent[] = new boolean[4];
	boolean wasMouseDown[] = new boolean[4];
	long lastHitConfirm;
	long lastHelmetCommand;
	private float health = MAX_HEALTH;
	private long credits;
	private int id;
	private int helmetSlot = -1;
	private NetworkPlayer networkPlayerObject;
	private StateInterface state;
	private InventoryController inventoryController;
	private String name;
	private int clientId;
	private boolean markedForDelete;
	private int ping;
	private ArrayList<BluePrintSpawnQueueElement> bluePrintSpawnQueue = new ArrayList<BluePrintSpawnQueueElement>();
	private ArrayList<BluePrintWriteQueueElement> bluePrintWriteQueue = new ArrayList<BluePrintWriteQueueElement>();
	private List<String> ignored = new ArrayList<String>();
	private int currentSectorId;

	private int kills;
	private int deaths;
	private boolean alive;
	private boolean markedForDeleteSent;
	private SimpleTransformableSendableObject<?> aquiredTarget;
	private PlayerCharacter playerCharacter;
	private RegisteredClientOnServer serverClient;
	private ArrayList<ServerMessage> serverToSendMessages = new ArrayList<ServerMessage>();
	private LongArrayList creditModifications = new LongArrayList();
	private ClientProximitySector proximitySector;
	private boolean sectorChanged = true;
	private ClientProximitySystem proximitySystem;
	private long sectorBlackHoleEffectStart = -1;
	private long lastInventoryFullMsgSent;
	private boolean markedForPermanentDelete;
	private long lastDeathTime;
	private String ip;
	private long lastBlueprintSpawn;
	private long blueprintDelay = DEFAULT_BLUEPRINT_DELAY;
	private float lastHP = MAX_HEALTH;
	private boolean checkForOvercap = true;
	private Transform transTmp = new Transform();
	private boolean godMode;
	private boolean invisibilityMode;
	private String lastEnteredEntity;
	private String starmadeName;
	private boolean upgradedAccount;
	private byte clientHitNotifaction;
	private String giveMetaBlueprint;
	private long lastSetProtectionMsg;
	private boolean factionPointProtected;
	private String tutorialCallClient;
	private int oldSectorId = -1;
	private SectorInformation.SectorType currentSectorType = SectorInformation.SectorType.ASTEROID;
	private boolean basicTutorialStarted;
	private long lastMessage;
	private ClientChannel clientChannel;
	private boolean newPlayerOnServer;
	private long lastDeathNotSuicide;
	private String lastDiedMessage = "";
	private boolean createdClientChannelOnServer;
	private boolean flagSendUseCreative;
	public Vector3i testSector;
	private final ObjectArrayFIFOQueue<CreateDockRequest> createDockRequests = new ObjectArrayFIFOQueue<CreateDockRequest>();
	private long lastLagReceived;
	private long currentLag;
	private boolean useCargoInventory;
	private int flagRequestCargoInv = -1;
	private VoidUniqueSegmentPiece cargoInventoryBlock;
	private byte shipControllerSlotClient;
	private float slotChangeUpdateDelay;
	private Vector3i lastSector = new Vector3i();
	private ObjectArrayFIFOQueue<Vector3i> resetFowSystem = new ObjectArrayFIFOQueue<Vector3i>();
	private ObjectArrayFIFOQueue<DragDrop> dropsIntoSpace = new ObjectArrayFIFOQueue<DragDrop>();
	private int inputBasedSeed;
	private boolean lastCanKeyPressCheckResult;
	private short lastCanKeyPressCheck;
	private ByteArrayList mouseEvents = new ByteArrayList();
	private long lastSentHitDeniedMessage;
	private final BuildModePosition buildModePosition;

	//INSERTED CODE:
	public void activateModControlManager(String windowName) {
		ModGUIHandler.getGUIControlManager(windowName).setActive(true);
	}

	public void activateModInputDialog(String dialogName) {
		ModGUIHandler.getInputDialog(dialogName).activate();
	}
	//

	public PlayerState(StateInterface state) {
		this.setState(state);
		buildModePosition = new BuildModePosition(this);
		spawnData = new PlayerStateSpawnData(this);

		playerConversationManager = new PlayerConversationManager(this);
		creationTime = System.currentTimeMillis();
		onServer = state instanceof ServerStateInterface;
		this.controllerState = new ControllerState(this);

		this.playerAiManager = new PlayerAiManager(this);
		this.catalog = new PlayerCatalogManager(this);
		proximitySector = new ClientProximitySector(this);
		proximitySystem = new ClientProximitySystem(this);

		shipUploadController = new ShipUploadController(this);
		skinUploadController = new SkinUploadController(this);
		skinManager = new SkinManager(this);
		this.factionController = (new PlayerFactionController(this));

		setInventory(new PlayerInventory(this, NORM_INV));
		this.creativeInventory = new CreativeModeInventory(this, NORM_INV);
		this.virtualCreativeInventory = new VirtualCreativeModeInventory(this, NORM_INV);
		setPersonalFactoryInventoryCapsule(new PersonalFactoryInventory(this, CAPSULE_INV, ElementKeyMap.FACTORY_CAPSULE_ASSEMBLER_ID));
		setPersonalFactoryInventoryMicro(new PersonalFactoryInventory(this, MICRO_INV, ElementKeyMap.FACTORY_MICRO_ASSEMBLER_ID));
		setPersonalFactoryInventoryMacroBlock(new PersonalFactoryInventory(this, MACRO_BLOCK_INV, ElementKeyMap.FACTORY_BASIC_ID));

		inventoryController = new InventoryController(this);

		playerChannelManager = new PlayerChannelManager(this);

		ruleEntityManager = new PlayerRuleEntityManager(this);

	}

	@Override
	public boolean isCreativeModeEnabled() {
		return isUseCreativeMode() && isHasCreativeMode();
	}

	@Override
	public Inventory getInventory() {
		Inventory cargoInv;
		if((cargoInv = getCargoInventoryIfActive()) != null) {
			return cargoInv;
		} else if(isCreativeModeEnabled() && !isInTutorial()) {
			return creativeInventory;
		} else if(getFirstControlledTransformableWOExc() != null && getFirstControlledTransformableWOExc() instanceof SegmentController && ((SegmentController) getFirstControlledTransformableWOExc()).isVirtualBlueprint()) {
			return virtualCreativeInventory;
		} else if(BuildSectorDataManager.getInstance(onServer).isPlayerInAnyBuildSector(this)) {
			return virtualCreativeInventory;
		}
		return inventory;
	}

	/**
	 * Checks the player's inventory for any illegal items (i.e. infinite stacks) and removes them.
	 * <br/>Todo: This happens often when the player leaves a build sector, I don't know why.
	 */
	public void removeBadItems() {
		if(useCreativeMode) return;
		for(Integer slotId : inventory.getAllSlots()) {
			InventorySlot slot = inventory.getMap().get(slotId);
			if(slot != null && !slot.isEmpty()) {
				if(slot.isInfinite() || slot.count() >= 9999999) {
					slot.setInfinite(false);
					inventory.removeSlot(slotId, false);
				}
			}
		}
		inventory.sendAll();
	}

	public int getSelectedEntityId() {
		return getNetworkObject().selectedEntityId.getInt();
	}

	public void announceKill(Damager killerEntity) {

		if(isOnServer()) {
			//don't hide. it might fuck up client as he tried to update a hidden kinematic controller
			//while object is still in physics
			getControllerState().removeAllUnitsFromPlayer(this, false);

			System.err.println(getState() + " " + this + " Announcing kill: " + killerEntity + " killed " + this);

			int id = -1;
			if((killerEntity != null && killerEntity instanceof Sendable)) {
				id = ((Sendable) killerEntity).getId();
			}
			if(killerEntity instanceof Missile) {
				((Missile) killerEntity).getOwner();
				id = ((Sendable) ((Missile) killerEntity).getOwner()).getId();
			}

			((GameServerState) getState()).getGameState().announceKill(this, id);

			sendKill(id);

		} else {
			throw new IllegalArgumentException("Clients may not be here");
		}

	}

	public void putAllInventoryInSpace() {
		putInventoryInSpace(NORM_FORCE); //personal main inventory (not any other like personal cargo)
		putInventoryInSpace(CAPSULE_INV);
		putInventoryInSpace(MICRO_INV);
		putInventoryInSpace(MACRO_BLOCK_INV);
	}

	public void putInventoryInSpace(long parameter) {
		SimpleTransformableSendableObject<?> c;
		try {
			Inventory inv = getInventory(parameter);
			c = getFirstControlledTransformable();
			IntOpenHashSet mod = new IntOpenHashSet(inv.getMap().size());
			inv.spawnInSpace(c, ElementCollection.getIndex(SegmentData.SEG_HALF, SegmentData.SEG_HALF, SegmentData.SEG_HALF), mod);

			sendInventoryModification(mod, parameter);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void checkIfDiedOnServer() {

		if(!dieList.isEmpty()) {
			Damager from = dieList.get(dieList.size() - 1);
			//INSERTED CODE @475
			PlayerDeathEvent event = new PlayerDeathEvent(this, from);
			StarLoader.fireEvent(PlayerDeathEvent.class, event, this.isOnServer());
			///

			System.err.println("[SERVER] PLAYER " + this + " died, removing ALL control units");

			Sector sector = ((GameServerState) getState()).getUniverse().getSector(getCurrentSectorId());
			if(sector != null) {

				if(isSpawnProtected()) {
					sendServerMessage(new ServerMessage(Lng.astr("No punishment: spawn protection"), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
				} else if(!sector.isPeace()) {

					float cMult = (Float) ServerConfig.PLAYER_DEATH_CREDIT_PUNISHMENT.getCurrentState();
					int secsUntilPunish = (Integer) ServerConfig.PLAYER_DEATH_PUNISHMENT_TIME.getCurrentState();
					if(lastSpawnedThisSession == 0 || System.currentTimeMillis() - lastSpawnedThisSession > secsUntilPunish * 1000) {
						if(cMult < 1f) {
							int lost = (int) (getCredits() * cMult);

							if(ServerConfig.PLAYER_DEATH_CREDIT_DROP.isOn()) {
								sendServerMessage(new ServerMessage(Lng.astr("You dropped %s credits!", lost), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
								dropCreditsIntoSpace(lost);
							} else {
								sendServerMessage(new ServerMessage(Lng.astr("You lost %s credits!", lost), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
								modCreditsServer(-(lost));
							}
						}
						if(ServerConfig.PLAYER_DEATH_BLOCK_PUNISHMENT.isOn()) {
							sendServerMessage(new ServerMessage(Lng.astr("Your inventory dropped into space!"), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
							putAllInventoryInSpace();
						}

					} else {
						long s = (getState().getUpdateTime() - lastSpawnedThisSession) / 1000;
						sendServerMessage(new ServerMessage(Lng.astr("No punishment: respawn protection still on!\n%s / %s seconds", s, secsUntilPunish), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
					}
				} else {
					sendServerMessage(new ServerMessage(Lng.astr("No punishment: Sector set to peace!"), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
				}
			}
			lastDeathTime = getState().getUpdateTime();

			announceKill(from);

			synchronized(getState().getLocalAndRemoteObjectContainer().getLocalObjects()) {
				for(Sendable s : getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
					if(s instanceof AbstractCharacter<?>) {
						AbstractCharacter<?> p = (AbstractCharacter<?>) s;
						if(p.getOwnerState() == this) {
							//							if (p instanceof PlayerCharacter && ((PlayerCharacter)p).getClientOwnerId() == this.clientId) {
							p.destroy(from);
							break;
							//							}else if(p instanceof AICreature<?>){
							//								p.destroy(from);
							//								break;
							//							}
						}

					}
				}
			}
			dieList.clear();
		}

	}

	public boolean checkItemInReach(FreeItem item, Object2ObjectOpenHashMap<PlayerState, IntOpenHashSet> changedSlots) {
		assert (isOnServer());
		SimpleTransformableSendableObject t;
		try {
			t = getFirstControlledTransformable();
			if(t != null) {
				boolean check = false;
				Sector s = ((GameServerState) state).getUniverse().getSector(getCurrentSectorId());
				if(t instanceof SegmentController) {

					Vector3f v = new Vector3f(item.getPos());
					v.sub(new Vector3f(0, 0.5f, 0));
					Vector3f v2 = new Vector3f(item.getPos());
					v2.add(new Vector3f(0, 0.5f, 0));

					ClosestRayResultCallback result = ((PhysicsExt) s.getPhysics()).testRayCollisionPoint(v, v2, false, null, (SegmentController) t, false, true, false);
					check = result.hasHit();
				} else {
					t.getTransformedAABB(min, max, 0.5f, tmpmin, tmpmax, null);
					check = BoundingBox.testPointAABB(item.getPos(), min, max);
				}
				if(check) {
					//INSERTED CODE
					PlayerPickupFreeItemEvent event = new PlayerPickupFreeItemEvent(this, item, Event.Condition.PRE);
					StarLoader.fireEvent(event, true);
					if(event.isCanceled()) return false;
					///
					if(item.getType() == FreeItem.CREDITS_TYPE) {
						modCreditsServer(item.getCount());
						sendServerMessage(new ServerMessage(Lng.astr("Picked up %s credits", item.getCount()), ServerMessage.MESSAGE_TYPE_INFO, this.getId()));
						item.setCount(0);
					} else {
						int countMax = item.getCount();
						if((countMax = getInventory().canPutInHowMuch(item.getType(), countMax, item.getMetaId())) > 0) {
							System.err.println("[SERVER][PLAYERSTATE] Picked up: Type " + item.getType() + "; Count " + countMax + "; Meta " + item.getMetaId());
							int slot = getInventory(null).incExistingOrNextFreeSlotWithoutException(item.getType(), countMax, item.getMetaId());
							//							sendInventoryModification(slot, null);
							IntOpenHashSet intOpenHashSet = changedSlots.get(this);
							if(intOpenHashSet == null) {
								intOpenHashSet = new IntOpenHashSet(24);
								changedSlots.put(this, intOpenHashSet);
							}
							item.setCount(item.getCount() - countMax);
							intOpenHashSet.add(slot);
							if(item.getType() > 0) {
								sendServerMessage(new ServerMessage(Lng.astr("Picked up %s\n%s", countMax, ElementKeyMap.getInfo(item.getType()).getName()), ServerMessage.MESSAGE_TYPE_INFO, this.getId()));
							} else {
								MetaObject object = ((MetaObjectState) state).getMetaObjectManager().getObject(item.getMetaId());
								if(object != null) {
									sendServerMessage(new ServerMessage(Lng.astr("Picked up %s", object.getName()), ServerMessage.MESSAGE_TYPE_INFO, this.getId()));
								}

							}
							//INSERTED CODE
							event = new PlayerPickupFreeItemEvent(this, item, Event.Condition.POST);
							StarLoader.fireEvent(event, true);
							///
						} else {
							System.err.println("[SERVER] " + this + " Cannot pick up item: inventory is full");
							if(System.currentTimeMillis() - lastInventoryFullMsgSent > 7000) {
								sendServerMessage(new ServerMessage(Lng.astr("Inventory full!"), ServerMessage.MESSAGE_TYPE_WARNING, this.getId()));
								lastInventoryFullMsgSent = System.currentTimeMillis();
							}
							//INSERTED CODE
							PlayerInventoryTooFullToPickupFreeItemEvent event2 = new PlayerInventoryTooFullToPickupFreeItemEvent(this, item);
							StarLoader.fireEvent(event2, true);
							///
							return false;
						}
					}
					s.getRemoteSector().removeItem(item.getId());
					if(item.getCount() > 0) {
						s.getRemoteSector().addItem(item.getPos(), item.getType(), item.getMetaId(), item.getCount());
					}
					return true;

				}
			}
		} catch(PlayerControlledTransformableNotFound e) {
		}
		return false;
	}

	@Override
	public void cleanUpOnEntityDelete() {
		// detach player from all
		getControllerState().removeAllUnitsFromPlayer(this, true);

		System.err.println("[PLAYER][CLEANUP] " + this + " removed controlled entities");
		if(!isOnServer()) {
			if(this == ((GameClientState) state).getPlayer()) {
				System.err.println("PLAYER SET TO NULL ON " + state);
				((GameClientState) state).setPlayer(null);
			}
		}

		factionController.cleanUp();
		catalog.cleanUp();
		System.err.println("[PLAYER][CLEANUP] " + this + " notified team change");

	}

	@Override
	public void sendInventoryErrorMessage(Object[] astr, Inventory inv) {
		sendServerMessagePlayerError(astr);
	}

	@Override
	public void destroyPersistent() {
		assert (isOnServer());
		String uName = "ENTITY_PLAYERSTATE_" + getName();
		File f = new FileExt(GameServerState.ENTITY_DATABASE_PATH + uName + ".ent");
		f.delete();
	}

	private boolean canAccessProtectedFields() {
		try {
			return onServer || state == null || ((GameClientState) state).getPlayer() == null || isClientOwnPlayer() || (((GameClientState) state).getPlayer() != null && ((GameClientState) state).getPlayer().getFactionId() == getFactionId() && getFactionId() != 0);
		} catch(NullPointerException e) {
			e.printStackTrace();
			return true;
		}
	}

	@Override
	public final void initFromNetworkObject(NetworkObject o) {
		NetworkPlayer p = (NetworkPlayer) o;
		setId(p.id.getInt());
		setClientId(p.clientId.getInt());
		setCredits(p.credits.getLong());
		if(canAccessProtectedFields()) {
			setCurrentSector(p.sectorPos.getVector());
			currentSectorId = p.sectorId.getInt();
			currentSectorType = SectorInformation.SectorType.values()[p.currentSectorType.getInt()];
			personalSector = p.personalSector.getVector();
			testSector = p.testSector.getVector();
		}
		handleServerHealthAndCheckAliveOnServer(p.health.getFloat(), null);
		kills = p.kills.get();
		deaths = p.deaths.get();
		lastDeathNotSuicide = p.lastDeathNotSuicide.get();
		name = p.playerName.get();

		dbId = networkPlayerObject.dbId.getLong();
		upgradedAccount = networkPlayerObject.upgradedAccount.get();

		spawnData.initFromNetworkObject();

		factionController.initFromNetworkObject(p);
		playerAiManager.initFromNetworkObject(p);
		skinManager.initFromNetworkObject();

		ping = p.ping.get();

		sittingOnId = (int) networkPlayerObject.sittingState.getLongArray()[0];
		ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[1], sittingPos);
		ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[2], sittingPosTo);
		ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[3], sittingPosLegs);

		assert (getId() >= 0);
		assert (getClientId() >= 0);
		assert (getState().getId() >= 0);

		if(!isOnServer()) {
			lastSpawnedThisSession = getNetworkObject().lastSpawnedThisSession.getLong();
			for(int i = 0; i < getNetworkObject().cargoInventoryChange.getReceiveBuffer().size(); i++) {
				RemoteSegmentControllerBlock remoteSegmentControllerBlock = getNetworkObject().cargoInventoryChange.getReceiveBuffer().get(i);
				this.cargoInventoryBlock = remoteSegmentControllerBlock.get();
			}
			this.infiniteInventoryVolume = getNetworkObject().infiniteInventoryVolume.getBoolean();
			getNetworkObject().tint.getVector(getTint());
			setInvisibilityMode(p.invisibility.get());
			if(getClientId() == state.getId()) {
				GameClientState s = (GameClientState) state;

				s.setPlayer(this);
				System.out.println("[PlayerState] Client successfully received player state " + getState() + ", owner: " + getClientId());
				//				clientRequestProxmitySectors();
			} else {
				// System.err.println("[PlayerState] Client received player state "+getState()+", owner: "+getClientId());
			}
		}
		buildModePosition.initFromNetworkObject();
		// System.err.println("[PLAYER] "+getState()+" initializing data from network object "+getId()+", credits "+getCredits());

	}

	@Override
	public void initialize() {
		/*
		 * This is secure: not accesible to change for a client to a remote server
		 * Even if a hacked client manages to set the hasCreative flag, the server will
		 * assume the player is not in creative, so if that player places a block with
		 * his fancy creative mode inventory, it will be denied by the server.
		 */
		if(isOnServer()) {
			if(GameClientState.singleplayerCreativeMode == GameClientState.CREATIVE_MODE_ON) {
				setHasCreativeMode(true);
			} else if(GameClientState.singleplayerCreativeMode == GameClientState.CREATIVE_MODE_OFF) {
				setHasCreativeMode(false);
			}
		}
	}

	@Override
	public boolean isMarkedForDeleteVolatile() {
		return markedForDelete;
	}

	@Override
	public void setMarkedForDeleteVolatile(boolean markedForDelete) {
		this.markedForDelete = markedForDelete;
	}

	@Override
	public boolean isMarkedForDeleteVolatileSent() {
		return markedForDeleteSent;
	}

	@Override
	public void setMarkedForDeleteVolatileSent(boolean b) {
		markedForDeleteSent = b;

	}

	@Override
	public boolean isMarkedForPermanentDelete() {
		return markedForPermanentDelete;
	}

	@Override
	public boolean isOkToAdd() {
		return true;
	}

	@Override
	public boolean isUpdatable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.schema.schine.network.objects.Sendable#markedForPermanentDelete(boolean)
	 */
	@Override
	public void markForPermanentDelete(boolean mark) {
		this.markedForPermanentDelete = mark;
	}

	@Override
	public void newNetworkObject() {
		setNetworkPlayerObject(new NetworkPlayer(getState(), this));
	}

	@Override
	public void updateFromNetworkObject(NetworkObject o, int senderId) {
		NetworkPlayer p = (NetworkPlayer) o;

		skinManager.updateFromNetworkObject();

		if(!isClientOwnPlayer()) {
			if(!isOnServer()) {
				//other players have their seed set by the server
				this.inputBasedSeed = p.inputSeed.getInt();
			}
			if(senderId != 0 && senderId != clientId) {
				System.err.println(getState() + " " + this + " WARNING: Possible attempt to hack controls of another player. sender: " + senderId + " but was: " + clientId);
				System.err.println(getState() + " " + this + " debugging what was sent: " + o.lastDecoded);
			}
			getNetworkObject().camOrientation.getMatrix(remoteCam.basis);
		}
		for(int i = 0; i < getNetworkObject().cargoInventoryChange.getReceiveBuffer().size(); i++) {
			RemoteSegmentControllerBlock remoteSegmentControllerBlock = getNetworkObject().cargoInventoryChange.getReceiveBuffer().get(i);
			this.cargoInventoryBlock = remoteSegmentControllerBlock.get();

			if(isOnServer()) {
				getNetworkObject().cargoInventoryChange.add(remoteSegmentControllerBlock);
			}
		}
		for(int i = 0; i < getNetworkObject().resetFowBuffer.getReceiveBuffer().size(); i++) {
			resetFowSystem.enqueue(getNetworkObject().resetFowBuffer.getReceiveBuffer().get(i).getVector());
		}
		ruleEntityManager.receive(getNetworkObject());
		if(!isOnServer()) {
			lastSpawnedThisSession = getNetworkObject().lastSpawnedThisSession.getLong();

			mineAutoArmSecs = getNetworkObject().mineArmTimer.getInt();

		} else {
			for(int i = 0; i < getNetworkObject().mineArmTimerRequests.getReceiveBuffer().size(); i++) {
				mineAutoArmSecs = getNetworkObject().mineArmTimerRequests.getReceiveBuffer().get(i);
			}
		}

		buildModePosition.updateFromNetworkObject();

		playerConversationManager.updateFromNetworkObject();
		if(!isOnServer()) {
			this.infiniteInventoryVolume = getNetworkObject().infiniteInventoryVolume.getBoolean();
			this.setUseCargoInventory(getNetworkObject().useCargoMode.getBoolean());

			this.setHasCreativeMode(getNetworkObject().hasCreativeMode.getBoolean());
			if(!isClientOwnPlayer()) {
				this.setUseCreativeMode(getNetworkObject().useCreativeMode.getBoolean());
			}

			for(int i = 0; i < getNetworkObject().blockCountMapBuffer.getReceiveBuffer().size(); i++) {
				RemoteBlockCountMap remoteBlockCountMap = getNetworkObject().blockCountMapBuffer.getReceiveBuffer().get(i);
				if(isClientOwnPlayer()) {
					CatalogEntryPanel.currentRequestedBlockMap = remoteBlockCountMap.get();
				}
			}

			lastDeathNotSuicide = getNetworkObject().lastDeathNotSuicide.get();

			if(canAccessProtectedFields()) currentSectorType = SectorInformation.SectorType.values()[p.currentSectorType.getInt()];

			sittingOnId = (int) getNetworkObject().sittingState.getLongArray()[0];
			ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[1], sittingPos);
			ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[2], sittingPosTo);
			ElementCollection.getPosFromIndex(getNetworkObject().sittingState.getLongArray()[3], sittingPosLegs);

			getNetworkObject().tint.getVector(getTint());
			setInvisibilityMode(p.invisibility.get());

			boolean changedCred = false;
			if(credits != p.credits.getLong()) {
				changedCred = true;
			}

			setCredits(p.credits.getLong());

			if(changedCred) {
				Starter.modManager.onPlayerCreditsChanged(this);
			}

			setHelmetSlot(p.helmetSlot.getInt());

			//			if(p.health.get() < this.health && isClientOwnPlayer()){
			//				SimpleTransformableSendableObject c = ((GameClientState)getState()).getCurrentPlayerObject();
			//				if(c != null && c instanceof PlayerControllable){
			//					PlayerControllable pc = (PlayerControllable)c;
			//					for(int i = 0; i < pc.getAttachedPlayers().size(); i++){
			//						if(pc.isClientOwnObject()){
			//							pc.getAttachedPlayers().get(i).onVesselHit(this);
			//						}
			//					}
			//				}
			//			}
			this.health = p.health.getFloat();
			if(canAccessProtectedFields()) {
				int old = getCurrentSectorId();
				if(p.sectorId.getInt() != currentSectorId) {
//				System.err.println("[CLIENT] received playerstate sector change: "+currentSectorId+"("+getCurrentSector()+") -> "+p.sectorId.get()+" ("+p.sectorPos.getVector()+")");
					if(((GameClientState) getState()).getPlayer() == this) {
						((GameClientState) getState()).flagWarped();
					}
				}

				setCurrentSector(p.sectorPos.getVector());
				currentSectorId = p.sectorId.getInt();
				if(old != getCurrentSectorId() && isClientOwnPlayer()) {
					((GameClientState) getState()).setFlagSectorChange(old);
				}
			}

			for(int i = 0; i < p.tutorialCalls.getReceiveBuffer().size(); i++) {
				tutorialCallClient = p.tutorialCalls.getReceiveBuffer().get(i).get();

			}
			for(int i = 0; i < p.messages.getReceiveBuffer().size(); i++) {
				ServerMessage fM = p.messages.getReceiveBuffer().get(i).get();
				((GameClientState) getState()).getServerMessages().add(fM);
			}
		}

		getShipUploadController().handleUploadNT(senderId);

		getSkinUploadController().handleUploadNT(senderId);

		kills = p.kills.get();
		deaths = p.deaths.get();

		factionController.updateFromNetworkObject(p);

		playerAiManager.updateFromNetworkObject(p);

		ping = p.ping.get();

		// System.err.println("player "+getId()+" on "+getState()+" received CREDITS: "+credits);
		// System.err.println("UPDATE BUY PLAYER FROMNT "+p.buyBuffer.getReceiveBuffer().size());
//		getSpawnPoint().set(getNetworkObject().spawnPoint.getVector());
		spawnData.fromNetworkObject();
		if(!isClientOwnPlayer()) {
			for(int i = 0; i < p.keyboardOfControllerBuffer.getReceiveBuffer().size(); i++) {
				keyboardEvents.add(p.keyboardOfControllerBuffer.getReceiveBuffer().getByte(i));
			}
		}
		if(isOnServer()) {
			for(int i = 0; i < p.mouseOfControllerBuffer.getReceiveBuffer().size(); i++) {
				//server has its seeds set by transmitted mouse input from client
				inputBasedSeed++;
				this.mouseEvents.add(p.mouseOfControllerBuffer.getReceiveBuffer().getByte(i));
			}
		}
		handleInventoryStateFromNT(p);
		super.handleInventoryNT();
		handleSpawnRequestFromNT(p);

		controllerState.handleControllerStateFromNT(p);
		handleKilledFromNT(p);
		handleRoundEndFromNT(p);

		if(isOnServer()) {
			boolean befUse = this.isUseCreativeMode();
			this.setUseCreativeMode(getNetworkObject().useCreativeMode.getBoolean());
			if(befUse != this.isUseCreativeMode()) {
				flagSendUseCreative = true;
			}

			for(int i = 0; i < getNetworkObject().requestCargoMode.getReceiveBuffer().size(); i++) {
				int r = getNetworkObject().requestCargoMode.getReceiveBuffer().getInt(i);
				flagRequestCargoInv = r;
			}

			for(int i = 0; i < getNetworkObject().creatureSpawnBuffer.getReceiveBuffer().size(); i++) {
				RemoteCreatureSpawnRequest r = getNetworkObject().creatureSpawnBuffer.getReceiveBuffer().get(i);
				synchronized(requestedreatureSpawns) {
					requestedreatureSpawns.enqueue(r.get());
				}
			}

			for(int i = 0; i < getNetworkObject().simpleCommandQueue.getReceiveBuffer().size(); i++) {
				SimpleCommand<?> r = getNetworkObject().simpleCommandQueue.getReceiveBuffer().get(i).get();
				synchronized(simpleCommandQueue) {
					simpleCommandQueue.enqueue(r);
				}
			}

			for(int i = 0; i < getNetworkObject().creditsDropBuffer.getReceiveBuffer().size(); i++) {
				int credits = getNetworkObject().creditsDropBuffer.getReceiveBuffer().get(i);
				synchronized(toDrop) {
					this.toDrop.add(credits);
				}

			}

			if(getState().getLocalAndRemoteObjectContainer().getLocalObjects().containsKey(p.aquiredTargetId.get())) {
				aquiredTarget = (SimpleTransformableSendableObject) getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(p.aquiredTargetId.get());
				//INSERTED CODE
				PlayerAcquireTargetEvent event = new PlayerAcquireTargetEvent(this, aquiredTarget);
				StarLoader.fireEvent(event, isOnServer());
				///
			} else {
				aquiredTarget = null;
			}
		}
		for(long s : getNetworkObject().lagAnnouncement.getReceiveBuffer()) {
			currentLag = s;
			lastLagReceived = System.currentTimeMillis();
		}
	}

	@Override
	public void updateToNetworkObject() {

		if(isClientOwnPlayer()) {
			//we set ours own seed.
			//tmp seed counts the actual mouse input made since the last sending
			inputBasedSeed += tempSeed;
			tempSeed = 0;
		}

		skinManager.updateToNetworkObject();
		spawnData.updateToNetworkObject();
		buildModePosition.updateToNetworkObject();
		if(isOnServer()) {
			getNetworkObject().hasCreativeMode.set(isHasCreativeMode());
			if(flagSendUseCreative) {
				getNetworkObject().useCreativeMode.set(isUseCreativeMode(), true);
				flagSendUseCreative = false;
			}
			getNetworkObject().lastSpawnedThisSession.set(lastSpawnedThisSession);
			getNetworkObject().inputSeed.set(inputBasedSeed);
			getNetworkObject().useCargoMode.set(isUseCargoInventory());
			getNetworkObject().invisibility.set(invisibilityMode);
			getNetworkObject().id.set(getId());
//			getNetworkObject().spawnPoint.set(getSpawnPoint());
			getNetworkObject().helmetSlot.set(getHelmetSlot());
			getNetworkObject().infiniteInventoryVolume.set(infiniteInventoryVolume);

			getNetworkObject().currentSectorType.set(currentSectorType.ordinal());
			getNetworkObject().credits.set(getCredits());
			getNetworkObject().tint.set(getTint());

			getNetworkObject().mineArmTimer.set(mineAutoArmSecs);

			getNetworkObject().health.set(getHealth());
			getNetworkObject().kills.set(kills);
			getNetworkObject().deaths.set(deaths);
			getNetworkObject().lastDeathNotSuicide.set(lastDeathNotSuicide);

			getNetworkObject().ping.set(ping);

			getNetworkObject().sectorId.set(currentSectorId);
			getNetworkObject().sectorPos.set(getCurrentSector());
			getNetworkObject().health.set(health);

			boolean changedSit = getNetworkObject().sittingState.getLongArray()[0] != sittingOnId;
			getNetworkObject().sittingState.set(0, sittingOnId);
			getNetworkObject().sittingState.set(1, ElementCollection.getIndex(sittingPos));
			getNetworkObject().sittingState.set(2, ElementCollection.getIndex(sittingPosTo));
			getNetworkObject().sittingState.set(3, ElementCollection.getIndex(sittingPosLegs));
			assert (!changedSit || (getNetworkObject().sittingState.hasChanged() && getNetworkObject().isChanged()));

			getNetworkObject().isAdminClient.set(((GameServerState) getState()).getController().isAdmin(getName()));

		}
		playerAiManager.updateToNetworkObject();
		factionController.updateToNetworkObject();

		if(isClientOwnPlayer()) {
			getNetworkObject().useCreativeMode.set(isUseCreativeMode(), true);
			getNetworkObject().mouseSwitched.forceClientUpdates();
			getNetworkObject().mouseSwitched.set(EngineSettings.C_MOUSE_BUTTON_SWITCH.isOn());
			getNetworkObject().selectedEntityId.forceClientUpdates();
			getNetworkObject().selectedAITargetId.forceClientUpdates();
			getNetworkObject().aquiredTargetId.forceClientUpdates();
			SimpleTransformableSendableObject selectedEntity = ((GameClientState) getState()).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getSelectedEntity();
			SimpleTransformableSendableObject selectedAITarget = ((GameClientState) getState()).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getSelectedAITarget();

			getNetworkObject().selectedEntityId.set(selectedEntity == null ? -1 : selectedEntity.getId());
			getNetworkObject().selectedAITargetId.set(selectedAITarget == null ? -1 : selectedAITarget.getId());
			getNetworkObject().aquiredTargetId.set(aquiredTarget == null ? -1 : aquiredTarget.getId());
		}
	}

	//	public void clientRequestProxmitySectors() {
	//		int[] v = new int[]{getCurrentSector().x,
	//				getCurrentSector().y,
	//				getCurrentSector().z,
	//				0,
	//				0};
	//
	//		RemoteIntegerArray a = new RemoteIntegerArray(5, getNetworkObject());
	//		a.setArray(v);
	//		getNetworkObject().sectorsInProximityBuffer.add(a);
	//
	//	}
	public boolean controls(ControllerStateUnit unit) {
		return controllerState.controls(unit);
	}

	public boolean controls(PlayerControllable controllable, Vector3i parameter) {
		return getControllerState().isOwnerControlling(controllable, parameter);
	}

	@Override
	public void damage(float damage, Destroyable destroyable, Damager from) {
		assert (isOnServer());
		//INSERTED CODE @1085
		PlayerDamageEvent event = new PlayerDamageEvent(damage, destroyable, from, this);
		StarLoader.fireEvent(PlayerDamageEvent.class, event, this.isOnServer());
		if(event.isCanceled()) {
			return;
		}
		///

		// #RM1946 add new method isDamageable(Damager)
		if(!isGodMode() && isVulnerable() && isDamageable(from)) {
			if(damage > 0) {
				from.sendHitConfirm(Damager.CHARACTER);
			}
			handleServerHealthAndCheckAliveOnServer((Math.max(0, getHealth() - damage)), from);
		} else {
			if(from != null && getState().getUpdateTime() > lastSentHitDeniedMessage + 2000) {

				if(isGodMode()) {
					from.sendServerMessage(Lng.astr("Target in god mode"), ServerMessage.MESSAGE_TYPE_ERROR);
					lastSentHitDeniedMessage = getState().getUpdateTime();
				} else if(isSpawnProtected()) {
					from.sendServerMessage(Lng.astr("Target in spawn protection"), ServerMessage.MESSAGE_TYPE_ERROR);
					lastSentHitDeniedMessage = getState().getUpdateTime();
				}
			}
		}

	}

	@Override
	public void heal(float heal, Destroyable destroyable, Damager from) {
		if(!isGodMode() && isVulnerable()) {
			handleServerHealthAndCheckAliveOnServer((Math.min(getMaxHealth(), getHealth() + heal)), from);
		}

	}

	/* (non-Javadoc)
	 * @see org.schema.game.common.data.player.AbstractOwnerState#getMaxHealth()
	 */
	@Override
	public float getMaxHealth() {
		return MAX_HEALTH;
	}

	/**
	 * @return the health
	 */
	@Override
	public float getHealth() {
		return health;
	}

	@Override
	public Vector3f getRight(Vector3f out) {
		if(isOnServer() || !isClientOwnPlayer()) {
			return GlUtil.getRightVector(out, remoteCam);
		} else {

			if(Controller.getCamera() != null && !Controller.getCamera().isStable()) {
				out.set(lastRight);
				return out;
			}
			if(Controller.getCamera() instanceof InShipCamera) {
				return ((InShipCamera) Controller.getCamera()).getHelperCamera().getRight(out);
			} else {
				if(Controller.getCamera() != null && !(Controller.getCamera() instanceof ObjectViewerCamera)) {
					return Controller.getCamera().getRight(out);
				} else {
					out.set(lastRight);
					return out;
				}
			}
		}
	}

	@Override
	public Vector3f getUp(Vector3f out) {
		if(isOnServer() || !isClientOwnPlayer()) {
			return GlUtil.getUpVector(out, remoteCam);
		} else {

			if(Controller.getCamera() != null && !Controller.getCamera().isStable()) {
				out.set(lastUp);
				return out;
			}

			if(Controller.getCamera() instanceof InShipCamera) {
				return ((InShipCamera) Controller.getCamera()).getHelperCamera().getUp(out);
			} else {
				if(Controller.getCamera() != null && !(Controller.getCamera() instanceof ObjectViewerCamera)) {
					return Controller.getCamera().getUp(out);
				} else {
					out.set(lastUp);
					return out;
				}
			}
		}
	}

	@Override
	public Vector3f getForward(Vector3f out) {
		if(isOnServer() || !isClientOwnPlayer()) {

			return GlUtil.getForwardVector(out, remoteCam);
		} else {

			if(Controller.getCamera() != null && !Controller.getCamera().isStable()) {
				out.set(lastForward);
				return out;
			}

			if(Controller.getCamera() instanceof InShipCamera) {
				return ((InShipCamera) Controller.getCamera()).getHelperCamera().getForward(out);
			} else {
				if(Controller.getCamera() != null && !(Controller.getCamera() instanceof ObjectViewerCamera)) {
					return Controller.getCamera().getForward(out);
				} else {
					out.set(lastForward);
					return out;
				}
			}
		}
	}

	/**
	 * @return the invisibilityMode
	 */
	@Override
	public boolean isInvisibilityMode() {
		return invisibilityMode;
	}

	/**
	 * @return the onServer
	 */
	@Override
	public boolean isOnServer() {
		return onServer;
	}

	@Override
	public boolean isFactoryInUse() {
		return true;
	}

	@Override
	public NetworkPlayer getNetworkObject() {
		return networkPlayerObject;
	}

	@Override
	public boolean isHarvestingButton() {

		//FIXME: this will be wrong on server if mouse buttons are switched
		return isMouseButtonDown(EngineSettings.C_MOUSE_BUTTON_SWITCH.isOn() ? 0 : 1);
	}

//	/**
//	 * @return the logoutSpawnPoint
//	 */
//	public Vector3f getLogoutSpawnPoint() {
//		return spawnData.logoutSpawn.abspolutePos;
//	}
//
//	/**
//	 * @return the logoutSpawnSector
//	 */
//	public Vector3i getLogoutSpawnSector() {
//		return spawnData.logoutSpawn.absoluteSector;
//	}

	@Override
	protected void onNoSlotFree(short type, int amount) {
		if(getState().getUpdateTime() - lastMessage > 1000) {
			sendServerMessage(new ServerMessage(Lng.astr("No empty slots\nin inventory!"), ServerMessage.MESSAGE_TYPE_ERROR, getId()));
			lastMessage = getState().getUpdateTime();
		}
	}

	/* (non-Javadoc)
	 * @see org.schema.game.common.data.Damager#sendHitConfirm()
	 */
	@Override
	public void sendHitConfirm(byte damageType) {
		if(isOnServer()) {
			getNetworkObject().hitNotifications.set(damageType);
			lastHitConfirm = getState().getUpdateTime();
		}
	}

	/* (non-Javadoc)
	 * @see org.schema.game.common.data.player.AbstractOwnerState#isVulnerable()
	 */
	@Override
	public boolean isVulnerable() {
		return !isInTutorial() && !isGodMode() && !isSpawnProtected();
	}

	@Override
	public void updateLocal(Timer timer) {
		super.updateLocal(timer);

		ruleEntityManager.update(timer);
		getShopsInDistance().clear();
		SimpleTransformableSendableObject firstControlledTransformable = getFirstControlledTransformableWOExc();
		if(firstControlledTransformable != null && firstControlledTransformable instanceof ShopperInterface) {
			getShopsInDistance().addAll(((ShopperInterface) firstControlledTransformable).getShopsInDistance());
		}

		if(!isOnServer()) {
			for(int i = 0; i < activeManualTrades.size(); i++) {
				activeManualTrades.get(i).updateFromPlayerClient(this);
			}
		}
		while(!dropsIntoSpace.isEmpty()) {
			handleDragDrop(dropsIntoSpace.dequeue());
		}
		while(!isOnServer() && ((GameClientState) getState()).getController().getClientChannel() != null && !resetFowSystem.isEmpty()) {
			Vector3i dequeue = resetFowSystem.dequeue();
			System.err.println("[CLIENT] Received FOW reset for " + getName());
			((GameClientState) getState()).getController().getClientChannel().getGalaxyManagerClient().resetClientVisibilitySystem(dequeue);
		}

		if(!isClientOwnPlayer()) {
			if(Controller.getCamera() instanceof InShipCamera) {
				getNetworkObject().adjustMode.forceClientUpdates();
				getNetworkObject().adjustMode.set(((InShipCamera) Controller.getCamera()).isInAdjustMode());
			}
		}

		if(!isOnServer() && slotChangeUpdateDelay > 0.0F) {
			slotChangeUpdateDelay -= timer.getDelta();
			if(slotChangeUpdateDelay <= 0.0F) {
				getNetworkObject().shipControllerSlot.set(shipControllerSlotClient, true);
			}
		}
		buildModePosition.update(timer);
		handleReceivedSavedCoordinates();
		if(isOnServer()) {
			if(forcedEnterSector != null) {
				if(getFirstControlledTransformableWOExc() == getAssingedPlayerCharacter()) {
					if(getSectorId() != forcedEnterSector.getSectorId()) {
						try {
							SectorSwitch s = new SectorSwitch(getFirstControlledTransformable(), forcedEnterSector.pos, SectorSwitch.TRANS_JUMP);
							s.execute((GameServerState) getState());
						} catch(PlayerControlledTransformableNotFound e) {
							e.printStackTrace();
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
					forcedEnterSector = null;
				}
			}
			if(forcedEnterUID != null && getAssingedPlayerCharacter() != null && getFirstControlledTransformableWOExc() == getAssingedPlayerCharacter()) {
				GameServerState state = ((GameServerState) getState());
				Sendable sendable = state.getLocalAndRemoteObjectContainer().getUidObjectMap().get(forcedEnterUID);
				if(sendable instanceof Ship && ((Ship) sendable).getSectorId() == getSectorId()) {
					Ship s = (Ship) sendable;
					SegmentPiece pointUnsave = s.getSegmentBuffer().getPointUnsave(Ship.core);

					if(pointUnsave != null) {
						getControllerState().requestControlServerAndSend(getAssingedPlayerCharacter(), (PlayerControllable) pointUnsave.getSegment().getSegmentController(), new Vector3i(), pointUnsave.getAbsolutePos(new Vector3i()), true);
						forcedEnterUID = null;
					}
				}
			}

			if(timer.currentTime - lastHitConfirm > 370) {

				getNetworkObject().hitNotifications.set((byte) 0);
			}
			if(createdClientChannelOnServer) {
				onClientChannelCreatedOnServer();
				createdClientChannelOnServer = false;
			}

			handleMetaBlueprintGive();

			handleCreateDockRequests();

		} else {
			this.setClientHitNotifaction(getNetworkObject().hitNotifications.getByte());

		}
		if(flagRequestCargoInv >= 0) {
			if(flagRequestCargoInv == 1) {
				useCargoInventory = true;
				getCargoInventoryIfActive(); //will instantly turn it off if not valid
			} else {
				useCargoInventory = false;
			}
			flagRequestCargoInv = -1;
		}
		spawnData.updateLocal(timer);

		if(clientChannel != null && clientChannel.isConnectionReady()) {
			playerChannelManager.update(timer);
		}
		if(onServer && checkForOvercap && playerCharacter != null) {
			if(getPersonalInventory().isOverCapacity()) {
				BlockStorageMetaItem storage = (BlockStorageMetaItem) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.BLOCK_STORAGE, (short) -1, true);

				addAllToStorageMetaItem(storage, getPersonalInventory());

				int slot;
				try {
					slot = getPersonalInventory().getFreeSlot(10, Integer.MAX_VALUE);
					getPersonalInventory().put(slot, storage);
					getPersonalInventory().sendInventoryModification(slot);
				} catch(NoSlotFreeException e) {
					e.printStackTrace();
				}

				System.err.println("[SERVER][PLAYERSTATE] " + this + " WAS OVER CAPACITY. ADDED INVENTORY ALL TO META ITEM");
				sendServerMessage(new ServerMessage(Lng.astr("Your inventory was over capacity.\nA block storage item has been created\nwhich contains all of your blocks.\n\nThis is done to ensure the over capacity penalty wouldn't hit a player out of the blue.\n\nYou can pull out blocks from there by right clicking on the item."), ServerMessage.MESSAGE_TYPE_DIALOG, id));

			}

			checkForOvercap = false;
		}
		if(isClientOwnPlayer()) {
			if(Controller.getCamera() != null) {
				networkPlayerObject.canRotate.forceClientUpdates();
				networkPlayerObject.canRotate.set(Controller.getCamera().isStable());
			}
			if(tutorialCallClient != null) {
				if("TutorialBasics00Welcome".equals(tutorialCallClient)) {
					if(((GameClientState) state).getController().getTutorialMode() != null && ((GameClientState) state).getController().getTutorialMode().getMachine().getFsm().getCurrentState() instanceof TeleportToTutorialSector) {
						//wait for satisfaction and warp
					} else {
						callTutorialClient(tutorialCallClient);
					}
				} else {
					callTutorialClient(tutorialCallClient);
				}
			}
			Vector3i waypoint = ((GameClientState) state).getController().getClientGameData().getWaypoint();
			networkPlayerObject.waypoint.forceClientUpdates();
			if(waypoint != null) {
				networkPlayerObject.waypoint.set(waypoint);
			} else {
				networkPlayerObject.waypoint.set(NO_WAYPOINT);
			}
		}

		setLastOrientation();

		if(clientChannel != null && clientChannel.getPlayerMessageController() != null) {
			clientChannel.getPlayerMessageController().update();
		}

		if(onServer) {
			skinManager.updateServer();
		} else {
			skinManager.updateOnClient();
			String toAdd;
			// Admins get specific information (including location)
			if(networkPlayerObject.isAdminClient.get()) {
				toAdd = controllerState.getUnits().toString();
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				Iterator<ControllerStateUnit> it = controllerState.getUnits().iterator();
				while(it.hasNext()) {
					ControllerStateUnit csu = it.next();
					sb.append(csu.playerState.getClass().getSimpleName());
					sb.append(" | ");
					sb.append(csu.playerControllable.getClass().getSimpleName());
					if(it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("]");
				toAdd = sb.toString();
			}
			AbstractScene.infoList.add("|CC " + toAdd);
		}

		handleBeingKilled();

		if(isClientOwnPlayer()) {
			if(health < lastHP) {
				onVesselHit(null);
				lastHP = health;
			}

			try {
				handleBluePrintQueuesClient();
			} catch(EntityNotFountException e) {
				e.printStackTrace();
			} catch(IOException e) {
				e.printStackTrace();
			} catch(EntityAlreadyExistsException e) {
				e.printStackTrace();
			}
		}
		if(!simpleCommandQueue.isEmpty()) {
			synchronized(simpleCommandQueue) {
				while(!simpleCommandQueue.isEmpty()) {
					SimpleCommand<?> dequeue = simpleCommandQueue.dequeue();

					executeSimpleCommand(dequeue);
				}
			}
		}
		if(!requestedreatureSpawns.isEmpty()) {
			synchronized(requestedreatureSpawns) {
				while(!requestedreatureSpawns.isEmpty()) {
					CreatureSpawn dequeue = requestedreatureSpawns.dequeue();
					if(onServer) {
						GameServerState s = (GameServerState) state;
						if(s.isAdmin(name)) {
							try {
								dequeue.execute(s);
								sendServerMessage(new ServerMessage(Lng.astr("Spawing creature"), ServerMessage.MESSAGE_TYPE_INFO, id));
							} catch(IOException e) {
								e.printStackTrace();
								sendServerMessage(new ServerMessage(Lng.astr("[SERVER] Error spawning\nPlease send a report"), ServerMessage.MESSAGE_TYPE_ERROR, id));
							}

						} else {
							sendServerMessage(new ServerMessage(Lng.astr("Cannot spawn!\nYou are not an admin!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
						}
					}
				}
			}
		}
		if(!queuedModifactions.isEmpty()) {
			synchronized(queuedModifactions) {
				while(!queuedModifactions.isEmpty()) {
					InventoryMultMod dequeue = queuedModifactions.dequeue();
					System.err.println("[PLAYER] SENDING QUEUED INVENTORY MOD: " + dequeue);
					networkPlayerObject.getInventoryMultModBuffer().add(new RemoteInventoryMultMod(dequeue, networkPlayerObject));
				}
			}
		}
		factionController.update(timer.currentTime);

		playerAiManager.update();

		playerConversationManager.updateOnActive(timer);

		if(onServer) {
			GameServerState s = (GameServerState) state;

			if(helmetSlot >= 0 && getInventory().getType(helmetSlot) != MetaObjectManager.MetaObjectType.HELMET.type) {
				helmetSlot = -1;
			}

			if(s.getUniverse().existsSector(currentSectorId)) {
				s.activeSectors.add(currentSectorId);

			} else {
				System.err.println("[SERVER] Exception (CRITICAL) player " + this + " is in an unloaded sector (" + currentSectorId + ")" + " attempting reload of " + currentSector);
				Sector sector;
				try {
					sector = s.getUniverse().getSector(currentSector);

					if(sector != null) {
						System.err.println("[SERVER] reloaded sector successfully. overwriting sector ids to " + sector.getId());
						for(PlayerState p : s.getPlayerStatesByName().values()) {
							int oldInvalid = currentSectorId;
							if(p.currentSectorId == oldInvalid) {
								System.err.println("[SERVER] reloaded sector successfully. overwriting sector id for " + p);
								p.currentSectorId = sector.getId();
							}
						}
					} else {
						throw new RuntimeException("Failed loading sector");
					}
				} catch(IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}

			}

			if(!toDrop.isEmpty()) {
				synchronized(toDrop) {
					while(!toDrop.isEmpty()) {
						int creditsToDrop = toDrop.removeInt(toDrop.size() - 1);
						dropCreditsIntoSpace(creditsToDrop);
					}
				}
			}

			boolean serverClientFirst = false;
			if(serverClient == null) {
				serverClient = s.getClients().get(clientId);
				serverClientFirst = true;
			}
			if(serverClient != null) {
				ping = (int) serverClient.getProcessor().getPingTime();
				if(serverClientFirst) {
					ip = serverClient.getProcessor().getIp().substring(0, serverClient.getProcessor().getIp().indexOf(':'));
					long time = System.currentTimeMillis();
					getHosts().add(new PlayerInfoHistory(time, ip, serverClient.getStarmadeName()));

					while(getHosts().size() > (Integer) ServerConfig.PLAYER_HISTORY_BACKLOG.getCurrentState()) {
						getHosts().remove(0);
					}

					spawnData.lastLogin = System.currentTimeMillis();
				}
			}
			//			if(healthChanged){
			//				handleServerHealthAndCheckAlive(getHealth(), null);
			//				healthChanged = false;
			//			}
//			getLogoutSpawnSector().set(currentSector);
//			if(firstControlledTransformable != null){
//
//				getLogoutSpawnPoint().set(firstControlledTransformable.getWorldTransform().origin);
//				for(ControllerStateUnit su : getControllerState().getUnits()){
//					if(su.playerControllable == firstControlledTransformable && su.playerControllable instanceof SegmentController && su.getParameter() != null){
//						Vector3f p = new Vector3f(su.getParameter().x-8,su.getParameter().y-8,su.getParameter().z-8);
//						firstControlledTransformable.getWorldTransform().transform(p);
//						getLogoutSpawnPoint().set(p);
//					}
//				}
//			}

			try {
				handleBluePrintQueuesServer();
			} catch(EntityNotFountException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch(EntityAlreadyExistsException e) {
				e.printStackTrace();
				sendServerMessage(new ServerMessage(Lng.astr("Cannot spawn!\n(already exists)"), ServerMessage.MESSAGE_TYPE_ERROR, id));
			} catch(IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			checkIfDiedOnServer();
		}

		catalog.update();

		if(!basicTutorialStarted && onServer && Sector.isTutorialSector(currentSector) && playerCharacter != null) {
			callTutorialServer("TutorialBasics00Welcome");
			basicTutorialStarted = true;
		}
		if(sectorChanged) {
			getFogOfWar().onSectorSwitch(lastSector, currentSector);
			if(onServer) {
				try {
					updateProximitySectors();

				} catch(IOException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}

			} else {

				if(isClientOwnPlayer()) {
					lastVisitedSectors.add(lastSector);
					for(int z = -1; z < 2; z++) {
						for(int y = -1; y < 2; y++) {
							for(int x = -1; x < 2; x++) {
								Vector3i p = new Vector3i(currentSector);
								p.add(x, y, z);
								lastVisitedSectors.add(p);
							}
						}
					}

					Iterator<Vector3i> iterator = lastVisitedSectors.iterator();
					int vsize = lastVisitedSectors.size();
					while(iterator.hasNext() && vsize > 300) {
						iterator.next();
						iterator.remove();
						vsize--;
					}
				}
			}
			Vector3i beforeSystem = new Vector3i(currentSystem);
			StellarSystem.getPosFromSector(currentSector, currentSystem);

			if(onServer && !invisibilityMode && !beforeSystem.equals(currentSystem)) {
				try {
					StellarSystem sSys = ((GameServerState) state).getUniverse().getStellarSystemFromStellarPos(currentSystem);
					if(sSys.getOwnerFaction() != 0 && sSys.getOwnerFaction() != getFactionId()) {
						Faction faction = ((GameServerState) state).getFactionManager().getFaction(sSys.getOwnerFaction());
						if(faction != null) {
							//how is the owner's relation to this player
							FactionRelation.RType relation = ((GameServerState) state).getFactionManager().getRelation(faction.getIdFaction(), getFactionId());

							switch(relation) {
								case ENEMY:
									String msgs = Lng.str("Our scanners picked up a HOSTILE\nsignature in our territory.\n\nOrigin System: %s %s.\nSend Scanner there to get exact\npositions!", sSys.getName(), sSys.getPos());
									Object[] msg = Lng.astr("Our scanners picked up a HOSTILE\nsignature in our territory.\n\nOrigin System: %s %s.\nSend Scanner there to get exact\npositions!", sSys.getName(), sSys.getPos());
									faction.broadcastMessage(msg, ServerMessage.MESSAGE_TYPE_WARNING, ((GameServerState) state));

									FactionNewsPost o = new FactionNewsPost();
									o.set(faction.getIdFaction(), Lng.str("Faction Auto Scanner"), System.currentTimeMillis(), Lng.str("Hostile signature in territory"), msgs, 0);
									((GameServerState) state).getFactionManager().addNewsPostServer(o);
									break;
								case FRIEND:
									faction.broadcastMessage(Lng.astr("Our scanners picked up a FRIENDLY\nsignature in our territory.\n\nOrigin System: %s %s.\nSend Scanner there to get exact\npositions!", sSys.getName(), sSys.getPos()), ServerMessage.MESSAGE_TYPE_INFO, ((GameServerState) state));
									break;
								case NEUTRAL:
									faction.broadcastMessage(Lng.astr("Our scanners picked up a NEUTRAL\nsignature in our territory.\n\nOrigin System: %s %s.\nSend Scanner there to get exact\npositions!", sSys.getName(), sSys.getPos()), ServerMessage.MESSAGE_TYPE_INFO, ((GameServerState) state));
									break;
								default:
									break;

							}

						}
					}
				} catch(IOException e) {
					e.printStackTrace();
				}
			}

			sectorBlackHoleEffectStart = -1;
			sectorChanged = false;
		}

		if(onServer) {
			try {

				currentSectorType = ((GameServerState) state).getUniverse().getStellarSystemFromStellarPos(currentSystem).getSectorType(currentSector);

				assert (((GameServerState) state).getUniverse().getStellarSystemFromStellarPos(currentSystem) == ((GameServerState) state).getUniverse().getStellarSystemFromSecPos(currentSector));

//				System.err.println("SERVER CENTER TYPE: "+((GameServerState)getState()).getUniverse().getStellarSystemFromStellarPos(currentSystem).getCenterSectorType());
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		handleReceivedInputEvents();

		try {

			skinUploadController.updateLocal();

			shipUploadController.updateLocal();

			controllerState.update(timer);

		} catch(ZipException e) {
			e.printStackTrace();
			if(onServer) {
				((GameServerState) state).getController().broadcastMessage(Lng.astr("[UPLOAD][ERROR] Critical error while extracting upload file!"), ServerMessage.MESSAGE_TYPE_ERROR);
			}
		} catch(Exception e) {
			e.printStackTrace();
			if(!onServer) {
				throw new RuntimeException(e);
			}
		}

		//		if (!getShopsInDistance().isEmpty() && !isOnServer()) {
		//			AbstractScene.infoList.add("PLAYER " + this
		//					+ " IS IN SHOP DISTANCE "
		//					+ ((GameClientState) getState()).getCharacter() + "; "
		//					+ ((GameClientState) getState()).getShip() + "; "
		//					+ ((GameClientState) getState()).getCurrentPlayerObject());
		//		}

//		System.err.println("CURRENT: "+currentSectorType);
		if(currentSectorType == SectorInformation.SectorType.BLACK_HOLE) {
			try {
				getFirstControlledTransformable();
				handlePlayerInBlackHoleSystem();
			} catch(PlayerControlledTransformableNotFound e) {
			}
		}

		if(onServer) {

			while(!serverToSendMessages.isEmpty()) {
				ServerMessage m = serverToSendMessages.remove(0);

				networkPlayerObject.messages.add(new RemoteServerMessage(m, networkPlayerObject));
			}
			if(!creditModifications.isEmpty()) {
				boolean changed;
				long p = 0;
				synchronized(creditModifications) {
					changed = !creditModifications.isEmpty();
					for(int i = 0; i < creditModifications.size(); i++) {
						long m = creditModifications.getLong(i);
						p += m;
					}
					creditModifications.clear();
				}
				BigInteger newP = BigInteger.valueOf(credits);
				newP = newP.add(BigInteger.valueOf(p));
				if(newP.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) { //Detect overflow // if (newP > Integer.MAX_VALUE) {
					sendServerMessage(new ServerMessage(Lng.astr("WARNING:\nCannot hold more credits!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
					setCredits(Long.MAX_VALUE);
				} else {
					setCredits(newP.longValue());
				}

				if(changed) {
					Starter.modManager.onPlayerCreditsChanged(this);
				}
			}
		}

		Starter.modManager.onPlayerUpdate(this, timer);

		if(oldSectorId != currentSectorId) {
			Starter.modManager.onPlayerSectorChanged(this);
			//INSERTED CODE
			PlayerChangeSectorEvent event = new PlayerChangeSectorEvent(this, oldSectorId, currentSectorId);
			StarLoader.fireEvent(event, onServer);
			///
			oldSectorId = currentSectorId;
		}

		updateInventory();

		synchronized(networkPlayerObject) {
			updateToNetworkObject();
		}

		for(int i = 0; i < wasMouseDown.length; i++) {
			wasMouseDown[i] = isMouseButtonDown(i);
		}

		if(timer.currentTime - lastLagReceived > 7000) {
			currentLag = 0;
		}
	}

	private void handleDragDrop(DragDrop g) {
		try {
			int slot = g.slot;
			int countToDrop = g.count;
			short typeToDrop = g.type;

			int invHolderId = g.invId;
			long parameter = g.parameter;

			Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolderId);

			if(sendable == null || !(sendable instanceof InventoryHolder)) {
				System.err.println("NO SENDABLE OR INVENTORY HOLDER " + sendable);
				return;
			}

			Inventory inventory = ((InventoryHolder) sendable).getInventory(parameter);

			if(inventory.isLockedInventory()) {
				if(onServer) {
					sendServerMessagePlayerError(Lng.astr("Cannot drop from locked inventory!"));
				}
				return;
			}

			short type = inventory.getType(slot);
			if(type != Element.TYPE_NONE) {
				Sector s = ((GameServerState) state).getUniverse().getSector(currentSectorId);
				if(s != null) {
					assert (inventory.checkVolume());
					if(type == InventorySlot.MULTI_SLOT && typeToDrop == InventorySlot.MULTI_SLOT) {
						System.err.println("[PLAYER] " + this + " DROPPING MULTISLOT");
						//drop whole multislot
						List<InventorySlot> subSlots = inventory.getSubSlots(slot);

						for(InventorySlot sub : subSlots) {
							int count = sub.count();
							int meta = sub.metaId;

							SimpleTransformableSendableObject f = getFirstControlledTransformable();

							System.err.println("[SERVER][PLAYER] " + this + " dropping item from " + f);

							Transform worldTransform = f.getWorldTransform();

							Vector3f pos = new Vector3f(worldTransform.origin);
							Vector3f forwardVector = getForward(new Vector3f());
							forwardVector.scale(2);
							pos.add(forwardVector);

							s.getRemoteSector().addItem(pos, sub.getType(), meta, count);
						}
						assert (inventory.checkVolume());
						//Removing slot
						inventory.put(slot, Element.TYPE_NONE, 0, -1);
						inventory.sendInventoryModification(slot);

						assert (inventory.checkVolume());

					} else if(type == InventorySlot.MULTI_SLOT) {
						assert (inventory.checkVolume());
						System.err.println("[PLAYER] " + this + " DROPPING FROM MULTISLOT TYPE: " + typeToDrop + "; count: " + countToDrop);
						List<InventorySlot> subSlots = inventory.getSubSlots(slot);

						for(InventorySlot sub : subSlots) {

							if(sub.getType() == typeToDrop) {
								int count = sub.count();
								int meta = sub.metaId;
								countToDrop = Math.min(countToDrop, count);
								break;
							}
						}

						inventory.getSlot(slot).setMulti(typeToDrop, Math.max(0, inventory.getCount(slot, typeToDrop) - countToDrop));

						SimpleTransformableSendableObject f = getFirstControlledTransformable();

						System.err.println("[SERVER][PLAYER] " + this + " dropping item from " + f);

						Transform worldTransform = f.getWorldTransform();

						Vector3f pos = new Vector3f(worldTransform.origin);
						Vector3f forwardVector = getForward(new Vector3f());
						forwardVector.scale(2);
						pos.add(forwardVector);

						s.getRemoteSector().addItem(pos, typeToDrop, -1, countToDrop);

						inventory.sendInventoryModification(slot);
						assert (inventory.checkVolume());
					} else {
						assert (inventory.checkVolume());
						int count = countToDrop > 0 ? Math.min(inventory.getCount(slot, (short) 0), countToDrop) : inventory.getCount(slot, (short) 0);
						int meta = inventory.getMeta(slot);

						if(((MetaObjectState) state).getMetaObjectManager().getObject(meta) != null && ((MetaObjectState) state).getMetaObjectManager().getObject(meta).isInventoryLocked(inventory)) {
							sendServerMessagePlayerError(Lng.astr("Can't drop active item!"));
							return;
						}
						System.err.println("[SERVER][PLAYER] DROPPING NORMAL SLOT " + ElementKeyMap.toString(type) + ": " + count);

						if(meta >= 0 || count >= inventory.getCount(slot, typeToDrop)) {
							//Removing slot
							inventory.put(slot, type, 0, -1);
						} else {
							inventory.inc(slot, typeToDrop, -count);
						}

						SimpleTransformableSendableObject<?> f = getFirstControlledTransformable();

						System.err.println("[SERVER][PLAYER] " + this + " dropping item from " + f);

						Transform worldTransform = f.getWorldTransform();

						Vector3f pos = new Vector3f(worldTransform.origin);
						Vector3f forwardVector = getForward(new Vector3f());
						forwardVector.scale(2);
						pos.add(forwardVector);

						s.getRemoteSector().addItem(pos, type, meta, count);
						inventory.sendInventoryModification(slot);
						assert (inventory.checkVolume());
					}

				}

			}

		} catch(PlayerControlledTransformableNotFound e) {
			System.err.println("CANNOT DROP ITEM");
			e.printStackTrace();
		}
	}

	private void handleCreateDockRequests() {
		assert (onServer);

		if(!createDockRequests.isEmpty()) {
			synchronized(createDockRequests) {
				while(!createDockRequests.isEmpty()) {
					CreateDockRequest d = createDockRequests.dequeue();

					if(getInventory().getOverallQuantity(ElementKeyMap.RAIL_BLOCK_DOCKER) < 1) {
						sendServerMessagePlayerError(Lng.astr("Cannot create dock!\nYou need a Rail Docker!"));
						continue;
					}
					if(getInventory().getOverallQuantity(ElementKeyMap.CORE_ID) < 1) {
						sendServerMessagePlayerError(Lng.astr("Cannot create dock!\nYou need a Ship Core!"));
						continue;
					}
					if(((GameServerState) state).existsEntity(SimpleTransformableSendableObject.EntityType.SHIP, d.name)) {
						sendServerMessagePlayerError(Lng.astr("Cannot create dock! Name already exists! Choose a different name."));
						continue;

					}
					if(isInTestSector()) {
						sendServerMessagePlayerError(Lng.astr("Cannot create dock!\nAction not allowed in test sector."));
						continue;
					}

					Sendable snd = state.getLocalAndRemoteObjectContainer().getUidObjectMap().get(d.core.uniqueIdentifierSegmentController);

					if(snd != null && snd instanceof SegmentController) {
						SegmentController dockOn = (SegmentController) snd;

						d.rail.setSegmentController(dockOn);

						for(RailRelation r : dockOn.railController.next) {
							if(r.rail.getAbsolutePos(new Vector3i()).equals(d.rail.getAbsolutePos(new Vector3i()))) {
								sendServerMessagePlayerError(Lng.astr("Cannot create dock!\nDock is already in use!"));
								return;
							}
						}

						IntOpenHashSet m = new IntOpenHashSet(2);
						getInventory().decreaseBatch(ElementKeyMap.RAIL_BLOCK_DOCKER, 1, m);
						getInventory().decreaseBatch(ElementKeyMap.CORE_ID, 1, m);
						getInventory().sendInventoryModification(m);

						Transform tr = new Transform();
						tr.setIdentity();
						float[] mat = new float[16];
						tr.getOpenGLMatrix(mat);
						Ship newShip = EntityRequest.getNewShip(state, SimpleTransformableSendableObject.EntityType.SHIP.dbPrefix + d.name, dockOn.getSectorId(), d.name, mat, -2, -2, -2, 2, 2, 2, getUniqueIdentifier(), false);

						d.core.uniqueIdentifierSegmentController = newShip.getUniqueIdentifier();
						d.docker.uniqueIdentifierSegmentController = newShip.getUniqueIdentifier();
						d.core.setSegmentController(newShip);
						d.docker.setSegmentController(newShip);

						//where the docker is in contect of the mother
						Vector3i railContact = new Vector3i();
						d.docker.getAbsolutePos(railContact);

						Vector3i diff = new Vector3i();

						diff.sub(d.docker.voidPos, d.core.voidPos);

						Oriencube rAlgo = (Oriencube) d.rail.getAlgorithm(ElementKeyMap.RAIL_BLOCK_BASIC);
						Oriencube dAlgo = (Oriencube) d.docker.getAlgorithm(ElementKeyMap.RAIL_BLOCK_BASIC);
						Oriencube cAlgo = (Oriencube) d.core.getAlgorithm(ElementKeyMap.RAIL_BLOCK_BASIC);

						Matrix3f rMat = rAlgo.getOrientationMatrixSwitched(new Matrix3f());
						Matrix3f dMat = dAlgo.getOrientationMatrixSwitched(new Matrix3f());
						Matrix3f cMat = cAlgo.getOrientationMatrixSwitched(new Matrix3f());

						Matrix3f rMatInv = rAlgo.getOrientationMatrixSwitched(new Matrix3f());
						rMatInv.invert();
						Matrix3f dMatInv = dAlgo.getOrientationMatrixSwitched(new Matrix3f());
						dMatInv.invert();
						Matrix3f cMatInv = cAlgo.getOrientationMatrixSwitched(new Matrix3f());
						cMatInv.invert();
						System.err.println("CORE MAT: " + cAlgo.getClass().getSimpleName() + "\n" + cMat);
						System.err.println("DOCKER MAT: " + dAlgo.getClass().getSimpleName() + "\n" + dMat);
						System.err.println("RAIL MAT: " + rAlgo.getClass().getSimpleName() + "\n" + rMat);

						//calculate the difference of where the core wants to look where the docker looks at

						Matrix3f diffCoreDocker = new Matrix3f(cMatInv);
//						diffCoreDocker.mul(dMatInv);

						//modify the position of the core relative to the docker according to that

						Vector3f diffF = new Vector3f(diff.x, diff.y, diff.z);

						diffCoreDocker.transform(diffF);

						Vector3f prim = new Vector3f(Element.DIRECTIONSf[Element.switchLeftRight(dAlgo.getOrientCubePrimaryOrientation())]);
						Vector3f sec = new Vector3f(Element.DIRECTIONSf[Element.switchLeftRight(dAlgo.getOrientCubeSecondaryOrientation())]);

						cMatInv.transform(prim);
						cMatInv.transform(sec);

						Vector3i pPrim = new Vector3i(Math.round(prim.x), Math.round(prim.y), Math.round(prim.z));
						Vector3i pSec = new Vector3i(Math.round(sec.x), Math.round(sec.y), Math.round(sec.z));

						int newPrim = -1;
						int newSec = -1;

						for(byte i = 0; i < 6; i++) {
							if(Element.DIRECTIONSi[Element.switchLeftRight(i)].equals(pPrim)) {
								newPrim = i;
							}
							if(Element.DIRECTIONSi[Element.switchLeftRight(i)].equals(pSec)) {
								newSec = i;
							}
						}
						assert (newPrim >= 0) : pPrim;
						assert (newSec >= 0) : pSec;

						System.err.println("NEW DIR: " + Element.getSideString(newPrim) + "; " + Element.getSideString(newSec));

						Oriencube nOrientCube = BlockShapeAlgorithm.getOrientcube(newPrim, newSec);

						boolean found = false;
						for(int i = 0; i < SegmentData.FULL_ORIENT; i++) {
							if(BlockShapeAlgorithm.algorithms[6 - 1][i].getClass().equals(nOrientCube.getClass())) {

								d.docker.setOrientation((byte) (i));
//								d.docker.setOrientation((byte) (i % SegmentData.MAX_ORIENT));
//								d.docker.setActive(i < SegmentData.MAX_ORIENT);

								assert (d.docker.getAlgorithm().getClass().equals(nOrientCube.getClass())) : "Is " + d.docker.getAlgorithm().getClass().getSimpleName() + "; " + nOrientCube.getClass().getSimpleName();

								found = true;
								break;
							}
						}

						assert (found);

						Vector3i relToPos = new Vector3i(Math.round(diffF.x) + SegmentData.SEG_HALF, Math.round(diffF.y) + SegmentData.SEG_HALF, Math.round(diffF.z) + SegmentData.SEG_HALF);

						d.docker.voidPos.set(relToPos);
						d.core.voidPos.set(SegmentData.SEG_HALF, SegmentData.SEG_HALF, SegmentData.SEG_HALF);

						Vector3i addSecPos = new Vector3i();
						Segment.getSegmentIndexFromSegmentElement(relToPos.x, relToPos.y, relToPos.z, addSecPos);
						addSecPos.scale(SegmentData.SEG);
						Vector3b local = new Vector3b();
						Segment.getElementIndexFrom(relToPos.x, relToPos.y, relToPos.z, local);

						RemoteSegment s = new RemoteSegment(newShip);
						s.setSegmentData(new SegmentDataIntArray(newShip.getState() instanceof ClientStateInterface));
						s.getSegmentData().setSegment(s);
						try {
							s.getSegmentData().setInfoElementUnsynched((byte) Ship.core.x, (byte) Ship.core.y, (byte) Ship.core.z, ElementKeyMap.CORE_ID, true, s.getAbsoluteIndex((byte) 8, (byte) 8, (byte) 8), newShip.getState().getUpdateTime());

							s.setLastChanged(System.currentTimeMillis());

							if(!s.pos.equals(addSecPos)) {
								System.err.println("[SERVER][CREATEDOCKING] adding additional segment for docker(" + relToPos + "): " + addSecPos);
								RemoteSegment sA = new RemoteSegment(newShip);
								sA.setSegmentData(new SegmentDataIntArray(newShip.getState() instanceof ClientStateInterface));
								sA.getSegmentData().setSegment(sA);
								sA.getSegmentData().setInfoElementUnsynched(local.x, local.y, local.z, ElementKeyMap.RAIL_BLOCK_DOCKER, d.docker.getOrientation(), d.docker.isActive() ? (byte) 1 : (byte) 0, true, sA.getAbsoluteIndex(local.x, local.y, local.z), newShip.getState().getUpdateTime());

								sA.setPos(addSecPos);
								sA.setLastChanged(System.currentTimeMillis());
								newShip.getSegmentBuffer().addImmediate(sA);
								newShip.getSegmentBuffer().updateBB(sA);
							} else {
								s.getSegmentData().setInfoElementUnsynched(local.x, local.y, local.z, ElementKeyMap.RAIL_BLOCK_DOCKER, d.docker.getOrientation(), d.docker.isActive() ? (byte) 1 : (byte) 0, true, s.getAbsoluteIndex(local.x, local.y, local.z), newShip.getState().getUpdateTime());
							}
						} catch(SegmentDataWriteException e) {
							throw new RuntimeException("this should be already normal chunk", e);
						}
						newShip.getSegmentBuffer().addImmediate(s);
						newShip.getSegmentBuffer().updateBB(s);

						assert (d.docker.getType() == ElementKeyMap.RAIL_BLOCK_DOCKER && newShip.getSegmentBuffer().getPointUnsave(d.docker.getAbsoluteIndex()).getType() == d.docker.getType());

						assert (d.rail.uniqueIdentifierSegmentController.equals(dockOn.getUniqueIdentifier()));
						assert (d.rail.getType() != 0 && dockOn.getSegmentBuffer().getPointUnsave(d.rail.getAbsoluteIndex()).getType() == d.rail.getType());

						RailRequest railRequest = newShip.railController.getRailRequest(d.docker, d.rail, railContact, null, RailRelation.DockingPermission.PUBLIC);
						railRequest.fromtag = true;
						railRequest.sentFromServer = false;
						newShip.railController.railRequestCurrent = railRequest;
						newShip.setFactionId(dockOn.getFactionId());
						newShip.setFactionRights(dockOn.getFactionRights());
						newShip.initialize();

						if(dockOn.isVirtualBlueprint()) {
							newShip.setVirtualBlueprintRecursive(true);
						}

						((GameServerState) state).getController().getSynchController().addNewSynchronizedObjectQueued(newShip);
					} else {
						sendServerMessagePlayerError(Lng.astr("Ship to dock on not found!"));
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "PlS[" + name + " " + (starmadeName != null ? "[" + starmadeName + "]" + (upgradedAccount ? "*" : "") : "") + "; id(" + id + ")(" + clientId + ")" + "f(" + getFactionId() + ")]";
	}

	@Override
	public AbstractCharacter<? extends AbstractOwnerState> getAbstractCharacterObject() {
		return playerCharacter;
	}

	@Override
	public NetworkInventoryInterface getInventoryNetworkObject() {
		return networkPlayerObject;
	}

	@Override
	public void onFiredWeapon(Weapon object) {
		if(isSpawnProtected()) {
			sendServerMessage(new ServerMessage(Lng.astr("Spawn protection ended!\n(weapon fired)"), ServerMessage.MESSAGE_TYPE_ERROR, id));
			lastSpawnedThisSession = 0;
		}
	}

	@Override
	public void updateToFullNetworkObject() {
		assert (id >= 0);
		assert (clientId >= 0);
		assert (state.getId() >= 0);

		networkPlayerObject.id.set(id);
		networkPlayerObject.clientId.set(clientId);
		networkPlayerObject.playerName.set(name);
		networkPlayerObject.credits.set(credits);
		networkPlayerObject.health.set(health);
		networkPlayerObject.kills.set(kills);
		networkPlayerObject.deaths.set(deaths);
		networkPlayerObject.lastDeathNotSuicide.set(lastDeathNotSuicide);
		networkPlayerObject.ping.set(ping);
		if(canAccessProtectedFields()) networkPlayerObject.currentSectorType.set(currentSectorType.ordinal());
		if(canAccessProtectedFields()) networkPlayerObject.sectorId.set(currentSectorId);
		if(canAccessProtectedFields()) networkPlayerObject.sectorPos.set(currentSector);
		networkPlayerObject.upgradedAccount.set(upgradedAccount);
		networkPlayerObject.tint.set(getTint());
		networkPlayerObject.dbId.set(dbId);
		networkPlayerObject.infiniteInventoryVolume.set(infiniteInventoryVolume);
		if(canAccessProtectedFields()) networkPlayerObject.personalSector.set(personalSector);
		if(canAccessProtectedFields()) networkPlayerObject.testSector.set(testSector);
		networkPlayerObject.lastSpawnedThisSession.set(lastSpawnedThisSession);
		boolean changedSit = networkPlayerObject.sittingState.getLongArray()[0] != sittingOnId;
		networkPlayerObject.sittingState.set(0, sittingOnId);
		networkPlayerObject.sittingState.set(1, ElementCollection.getIndex(sittingPos));
		networkPlayerObject.sittingState.set(2, ElementCollection.getIndex(sittingPosTo));
		networkPlayerObject.sittingState.set(3, ElementCollection.getIndex(sittingPosLegs));

		if(cargoInventoryBlock != null) {
			networkPlayerObject.cargoInventoryChange.add(new RemoteSegmentControllerBlock(cargoInventoryBlock, onServer));
		}

		assert (!changedSit || (networkPlayerObject.sittingState.hasChanged() && networkPlayerObject.isChanged()));

		networkPlayerObject.invisibility.set(invisibilityMode);

		factionController.updateToFullNetworkObject();

		playerAiManager.updateToFullNetworkObject();

		skinManager.updateToNetworkObject();

		spawnData.updateToFullNetworkObject();

		buildModePosition.updateToFullNetworkObject();

		super.updateToFullNetworkObject();

		controllerState.sendAll();

		networkPlayerObject.setChanged(true);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		assert (id != NetworkEntity.NEUTRAL_PLAYER_ID);
		this.id = id;
	}

	@Override
	public String getConversationScript() {
		return null;
	}

	/**
	 * @param invisibilityMode the invisibilityMode to set
	 */
	public void setInvisibilityMode(boolean invisibilityMode) {
		this.invisibilityMode = invisibilityMode;
	}

	private void dieOnServer(Damager from) {
		if(!alive) {
			return;
		}
		dieList.add(from);

		Starter.modManager.onPlayerKilled(this, from);
		if(isInTutorial() || isInTestSector()) {
			loadInventoryBackupServer();
			Sector defaultSec;
			try {
				defaultSec = ((GameServerState) state).getUniverse().getSector(new Vector3i((Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_X.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Y.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Z.getCurrentState()));

				SectorSwitch s = new SectorSwitch(getFirstControlledTransformable(), defaultSec.pos, SectorSwitch.TRANS_JUMP);

				((GameServerState) state).getSectorSwitches().add(s);
			} catch(IOException e) {
				e.printStackTrace();
			} catch(PlayerControlledTransformableNotFound e) {
				e.printStackTrace();
			}
		}

		Faction faction = ((GameServerState) state).getFactionManager().getFaction(factionController.getFactionId());
		if(faction != null) {
			if(!factionPointProtected) {

				try {
					Sector sector = ((GameServerState) state).getUniverse().getSector(currentSector);
					if(sector.isNoFPLoss()) {
						sendServerMessage(new ServerMessage(Lng.astr("No Faction points lost!\nSector set not to cost\nfaction points on death."), ServerMessage.MESSAGE_TYPE_INFO, id));
					} else {
						faction.onPlayerDied(this, from);
					}

				} catch(IOException e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("[SERVER] Player " + this + " didn't lose faction points. He is protected against faction point loss");
				((GameServerState) state).getController().broadcastMessage(Lng.astr("Player %s died\nbut didn't lose FP.\n(Admin FP loss protected)", name), ServerMessage.MESSAGE_TYPE_ERROR);
			}
		}

		if(from != this) {
			if(System.currentTimeMillis() - lastDeathNotSuicide > FactionPointsGeneralConfig.FACTION_POINT_DEATH_PROTECTION_MIN * 60 * 1000) {
				System.err.println("[PLAYER][DEATH] last faction point death not suicide " + lastDeathNotSuicide + " set to " + System.currentTimeMillis());
				lastDeathNotSuicide = System.currentTimeMillis();
			}
		}

		alive = false;

		if(onServer && !alive) {
			System.err.println("Revived PlayerState " + this);
			health = MAX_HEALTH;
			alive = true;
		}
	}

	//	private void handleReceivedSectorProximityRequests(NetworkPlayer p){
	//		for (int i = 0; i < p.sectorsInProximityBuffer.getReceiveBuffer().size(); i++) {
	//			RemoteIntegerArray a = p.sectorsInProximityBuffer.getReceiveBuffer().get(i);
	//			if(isOnServer()){
	//				this.requestedSectorProximityOnServer = new Vector4i(a.get(0).get(),a.get(1).get(),a.get(2).get(),a.get(3).get());
	//			}else{
	//				this.proxmityReceived.add(new int[]{a.get(0).get(),a.get(1).get(),a.get(2).get(),a.get(3).get(), a.get(4).get()});
	//			}
	//		}
	//	}

//	/**
//	 * @return the spawnPoint
//	 */
//	public Vector3f getSpawnPoint() {
//		return spawnData.deathSpawn.abspolutePos;
//	}
//
//	/**
//	 * @return the spawnSector
//	 */
//	public Vector3i getSpawnSector() {
//		return spawnData.deathSpawn.absoluteSector;
//	}

	public void dropCreditsIntoSpace(int amount) {
		assert (onServer);
		if(amount > 0) {
			try {
				SimpleTransformableSendableObject f = getFirstControlledTransformable();
				Transform worldTransform = f.getWorldTransform();
				Sector s = ((GameServerState) state).getUniverse().getSector(currentSectorId);
				Vector3f pos = new Vector3f(worldTransform.origin);
				Vector3f forwardVector = getForward(new Vector3f());
				forwardVector.scale(2);
				pos.add(forwardVector);
				long a = Math.min(credits, amount);
				modCreditsServer(-a);
				s.getRemoteSector().addItem(pos, FreeItem.CREDITS_TYPE, -1, (int) a);

			} catch(PlayerControlledTransformableNotFound e) {
				System.err.println("[SERVER][PLAYERSTATE] CANNOT DROP CREDITS: no transformable for player found");
				e.printStackTrace();
			}
		}
	}

	public boolean spawnCrew() {
		if(playerAiManager.getCrew().size() < 5) {
			CreatureSpawn s;
			try {
				s = new CreatureSpawn(new Vector3i(currentSector), new Transform(getFirstControlledTransformable().getWorldTransform()), "NoName", CreatureType.CHARACTER) {
					@Override
					public void initAI(AIGameCreatureConfiguration<?, ?> aiConfiguration) {
						try {
							assert (aiConfiguration != null);
							aiConfiguration.get(Types.ORIGIN_X).switchSetting(String.valueOf(Integer.MIN_VALUE), false);
							aiConfiguration.get(Types.ORIGIN_Y).switchSetting(String.valueOf(Integer.MIN_VALUE), false);
							aiConfiguration.get(Types.ORIGIN_Z).switchSetting(String.valueOf(Integer.MIN_VALUE), false);

							aiConfiguration.get(Types.ROAM_X).switchSetting("16", false);
							aiConfiguration.get(Types.ROAM_Y).switchSetting("3", false);
							aiConfiguration.get(Types.ROAM_Z).switchSetting("16", false);

							aiConfiguration.get(Types.ORDER).switchSetting(AIGameCreatureConfiguration.BEHAVIOR_IDLING, false);

							aiConfiguration.get(Types.OWNER).switchSetting(getUniqueIdentifier(), false);
						} catch(StateParameterNotFoundException e) {
							e.printStackTrace();
						}
						System.err.println("[AISPAWN] adding to crew");
						getPlayerAiManager().addAI(new UnloadedAiContainer(aiConfiguration.getAiEntityState().getEntity()));

						aiConfiguration.getAiEntityState().getEntity().setFactionId(getFactionId());
					}
				};

				((GameServerState) state).getController().queueCreatureSpawn(s);
				System.err.println("[SimpleCommand] [SUCCESS] Spawning creature");
				return true;
			} catch(PlayerControlledTransformableNotFound e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;

	}

	//	public void requestAllControlRelease(boolean hide) {
	//
	//		// System.err.println(this+", Issuing Control Release Request ");
	//		// try{
	//		// throw new NullPointerException("RELEASEALL");
	//		// }catch(NullPointerException e){
	//		// e.printStackTrace();
	//		// }
	//		synchronized (getNetworkObject()) {
	//			RemoteIntegerArray a = new RemoteIntegerArray(4, getNetworkObject());
	//			a.set(3, hide ? 1 : 0);
	//			getNetworkObject().controlRequestBuffer.add(new RemoteInteger(
	//					NetworkEntity.NEUTRAL_PLAYER_ID, getNetworkObject()));
	//			getNetworkObject().controlRequestParameterBuffer
	//					.add(new RemoteVector3i(new Vector3i(), getNetworkObject()));
	//		}
	//
	//	}

	public void loadInventoryBackupServer() {
		assert (onServer);

		if(mainInventoryBackup != null) {
			//make sure that also the slots are sent that are no longer in the old inventory
			IntOpenHashSet c = inventory.getAllSlots();
			inventory.clear();
			inventory.fromTagStructure(mainInventoryBackup);
			inventory.sendAllWithExtraSlots(c);
			System.err.println("[SERVER][PLAYERSTATE] " + this + " INVENTORY RESTORED!");
			mainInventoryBackup = null;
		}

		if(capsuleRefinerInventoryBackup != null) {
			personalFactoryInventoryCapsule.clear();
			personalFactoryInventoryCapsule.fromTagStructure(capsuleRefinerInventoryBackup);
			personalFactoryInventoryCapsule.sendAll();
			capsuleRefinerInventoryBackup = null;
		}

		if(microInventoryBackup != null) {
			personalFactoryInventoryMicro.clear();
			personalFactoryInventoryMicro.fromTagStructure(microInventoryBackup);
			personalFactoryInventoryMicro.sendAll();
			microInventoryBackup = null;
		}

		if(factoryInventoryBackup != null) {
			personalFactoryInventoryMacroBlock.clear();
			personalFactoryInventoryMacroBlock.fromTagStructure(factoryInventoryBackup);
			personalFactoryInventoryMacroBlock.sendAll();
			factoryInventoryBackup = null;
		}
	}

	public void invBackUpfromTag(Tag tag) {
		if(tag.getType() == Tag.Type.STRUCT) {
			Tag[] t = (Tag[]) tag.getValue();
			inventory.clear();
			inventory.fromTagStructure(t[0]);

			personalFactoryInventoryCapsule.clear();
			personalFactoryInventoryCapsule.fromTagStructure(t[1]);

			personalFactoryInventoryMicro.clear();
			personalFactoryInventoryMicro.fromTagStructure(t[2]);

			personalFactoryInventoryMacroBlock.clear();
			personalFactoryInventoryMacroBlock.fromTagStructure(t[3]);

			System.err.println("[SERVER] Player: " + this + " had backup inventory (used for instances) still. restored the backup!");
		}
	}

	public Tag invBackUpToTag() {
		if(mainInventoryBackup != null) {
			return new Tag(Tag.Type.STRUCT, null, new Tag[]{mainInventoryBackup, capsuleRefinerInventoryBackup, microInventoryBackup, factoryInventoryBackup, FinishTag.INST});
		} else {
			return new Tag(Tag.Type.BYTE, null, (byte) 0);
		}
	}

	public void instantiateInventoryServer(boolean clear) {
		assert (onServer);
		mainInventoryBackup = inventory.toTagStructure();
		capsuleRefinerInventoryBackup = personalFactoryInventoryCapsule.toTagStructure();
		microInventoryBackup = personalFactoryInventoryMicro.toTagStructure();
		factoryInventoryBackup = personalFactoryInventoryMacroBlock.toTagStructure();

		if(clear) {
			{
				IntOpenHashSet changed = new IntOpenHashSet();
				inventory.clear(changed);
				inventory.sendInventoryModification(changed);
			}
			{
				IntOpenHashSet changed = new IntOpenHashSet();
				personalFactoryInventoryCapsule.clear(changed);
				personalFactoryInventoryCapsule.sendInventoryModification(changed);
			}
			{
				IntOpenHashSet changed = new IntOpenHashSet();
				personalFactoryInventoryMicro.clear(changed);
				personalFactoryInventoryMicro.sendInventoryModification(changed);
			}
			{
				IntOpenHashSet changed = new IntOpenHashSet();
				personalFactoryInventoryMacroBlock.clear(changed);
				personalFactoryInventoryMacroBlock.sendInventoryModification(changed);
			}
		}
	}

	public PlayerStateSpawnData getSpawn() {
		return spawnData;
	}

	private void executeSimpleCommand(SimpleCommand<?> simpleCommand) {
		int command = simpleCommand.getCommand();
		if(command >= SimplePlayerCommands.values().length || command < 0) {
			sendServerMessage(new ServerMessage(Lng.astr("Unknown command: %s", command), ServerMessage.MESSAGE_TYPE_ERROR, id));
		} else {
			SimplePlayerCommands simplePlayerCommands = SimplePlayerCommands.values()[command];
			System.err.println("[SERVER] executing simple command: " + simplePlayerCommands.name());
			switch(simplePlayerCommands) {
				case END_TUTORIAL:
					if(isInTutorial()) {
						loadInventoryBackupServer();
						Sector originalSec;
						try {
							if(spawnData.preSpecialSector == null) {
								originalSec = ((GameServerState) state).getUniverse().getSector(new Vector3i((Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_X.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Y.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Z.getCurrentState()));
							} else {
								originalSec = ((GameServerState) state).getUniverse().getSector(spawnData.preSpecialSector);
							}

							System.err.println("[SERVER] Ending tutorial. Respawning in sector: " + originalSec + ". IsDefault: " + (spawnData.preSpecialSector == null));

							SectorSwitch s = new SectorSwitch(getFirstControlledTransformable(), originalSec.pos, SectorSwitch.TRANS_JUMP);
							if(spawnData.preSpecialSectorTransform != null) {
								s.sectorSpaceTransform = spawnData.preSpecialSectorTransform;
							}

							((GameServerState) state).getSectorSwitches().add(s);

						} catch(IOException e) {
							e.printStackTrace();
						} catch(PlayerControlledTransformableNotFound e) {
							e.printStackTrace();
						}
					}
					break;
				case END_SHIPYARD_TEST:
					if(isInTestSector()) {
						loadInventoryBackupServer();

						Sector originalSec;
						try {
							if(spawnData.preSpecialSector == null) {
								originalSec = ((GameServerState) state).getUniverse().getSector(new Vector3i((Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_X.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Y.getCurrentState(), (Integer) ServerConfig.DEFAULT_SPAWN_SECTOR_Z.getCurrentState()));
							} else {
								originalSec = ((GameServerState) state).getUniverse().getSector(spawnData.preSpecialSector);
							}

							System.err.println("[SERVER] Ending shipyard Test. Respawning in sector: " + originalSec + ". IsDefault: " + (spawnData.preSpecialSector == null));

							SectorSwitch s = new SectorSwitch(getFirstControlledTransformable(), originalSec.pos, SectorSwitch.TRANS_JUMP);
							if(spawnData.preSpecialSectorTransform != null) {
								s.sectorSpaceTransform = spawnData.preSpecialSectorTransform;
							}

							((GameServerState) state).getSectorSwitches().add(s);

						} catch(IOException e) {
							e.printStackTrace();
						} catch(PlayerControlledTransformableNotFound e) {
							e.printStackTrace();
						}
					}
					break;
				case BACKUP_INVENTORY:
					Boolean clear = (Boolean) simpleCommand.getArgs()[0];
					instantiateInventoryServer(clear);
					break;
				case RESTORE_INVENTORY:
					loadInventoryBackupServer();
					break;
				case DESTROY_TUTORIAL_ENTITY:
					String uid = (String) simpleCommand.getArgs()[0];

					if(isInTutorial()) {
						for(Sendable s : state.getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
							if(s instanceof SimpleTransformableSendableObject && ((SimpleTransformableSendableObject) s).getUniqueIdentifier() != null && ((SimpleTransformableSendableObject) s).getUniqueIdentifier().startsWith(uid) && ((SimpleTransformableSendableObject) s).getSectorId() == currentSectorId) {
								s.markForPermanentDelete(true);
								s.setMarkedForDeleteVolatile(true);
							}

						}
					} else {
						try {
							throw new IllegalArgumentException("Player " + this + " tried to destroy an entity while not in tutorial");
						} catch(Exception e) {
							e.printStackTrace();
						}
						((GameServerState) state).getController().broadcastMessage(Lng.astr("Possible hacking attempt\nby %s!", this), ServerMessage.MESSAGE_TYPE_ERROR);
					}

					break;
				case WARP_TO_TUTORIAL_SECTOR:
					Vector3i sec = new Vector3i();
					spawnData.preSpecialSector = new Vector3i(currentSector);
					try {
						SimpleTransformableSendableObject f = getFirstControlledTransformable();
						spawnData.preSpecialSectorTransform = new Transform(f.getWorldTransform());
					} catch(PlayerControlledTransformableNotFound e) {
						e.printStackTrace();
					}

					for(int z = 1; z < VoidSystem.SYSTEM_SIZE; z += 2) {
						for(int y = 1; y < VoidSystem.SYSTEM_SIZE; y += 2) {
							for(int x = 1; x < VoidSystem.SYSTEM_SIZE; x += 2) {
								int xSec = 130000000 * VoidSystem.SYSTEM_SIZE + x;
								int ySec = 130000000 * VoidSystem.SYSTEM_SIZE + y;
								int zSec = 130000000 * VoidSystem.SYSTEM_SIZE + z;
								sec.set(xSec, ySec, zSec);
								Sector sector = ((GameServerState) state).getUniverse().getSectorWithoutLoading(sec);
								if(sector == null) {
									//this sector is not loaded. that means it is ok to do tutorial there
									try {
										SectorUtil.importSectorFullDir("./data/prefabSectors", "tutorial-v2.0.smsec", sec, (GameServerState) state);

										SectorSwitch sw = new SectorSwitch(playerCharacter, sec, SectorSwitch.TRANS_JUMP);
										((GameServerState) state).getSectorSwitches().add(sw);
										return;
									} catch(IOException e) {
										e.printStackTrace();
									} catch(SQLException e) {
										e.printStackTrace();
									}

									sendServerMessage(new ServerMessage(Lng.astr("Something went wrong!\nPlease tell an admin or\ntry in a new universe.\nReport the error!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
									return;
								}
							}
						}
					}
					break;
				case SEARCH_LAST_ENTERED_SHIP:
					searchForLastEnteredEntity();
					break;
				case HIRE_CREW:
					spawnCrew();
					break;

				case SIT_DOWN: {
					sittingOnId = (Integer) simpleCommand.getArgs()[0];

					if(sittingOnId < 0) {
						System.err.println("[SERVER] " + this + "standing up");
						sittingStarted = 0;
						sittingUIDServer = "none";
					} else {
						long block = (Long) simpleCommand.getArgs()[1];
						long toBlock = (Long) simpleCommand.getArgs()[2];
						long legBlock = (Long) simpleCommand.getArgs()[3];

						sittingStarted = System.currentTimeMillis();

						ElementCollection.getPosFromIndex(block, sittingPos);
						ElementCollection.getPosFromIndex(toBlock, sittingPosTo);
						ElementCollection.getPosFromIndex(legBlock, sittingPosLegs);

						System.err.println("[SERVER] " + this + " sitting down " + sittingPos + " " + sittingPosTo);
					}
					break;
				}
				case PUT_ON_HELMET:

					System.err.println("HELMETCOMMAND: BuildSlot: " + getSelectedBuildSlot() + "; cur: " + helmetSlot);
					if(System.currentTimeMillis() - lastHelmetCommand > 1000) {
						if(getSelectedBuildSlot() == helmetSlot && helmetSlot >= 0 && getInventory().getType(helmetSlot) == MetaObjectManager.MetaObjectType.HELMET.type) {
							System.err.println("[SERVER] " + this + " TAKE OFF HELMET");
							helmetSlot = -1;
						} else {
							System.err.println("[SERVER] " + this + " PUT ON HELMET");
							helmetSlot = getSelectedBuildSlot();
						}
					}
					lastHelmetCommand = System.currentTimeMillis();
					break;
				case REPAIR_STATION: {
					int id = (Integer) simpleCommand.getArgs()[0];
					String name = (String) simpleCommand.getArgs()[1];

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalUpdatableObjects().get(id);

					if(s != null && s instanceof SpaceStation) {
						SpaceStation sp = ((SpaceStation) s);
						long price = sp.getElementClassCountMap().getPrice();
						if(credits >= price) {
							modCreditsServer(-price);
							sp.setScrap(false);
							sp.setRealName(name);
						} else {
							sendServerMessage(new ServerMessage(Lng.astr("Not enough credits!"), ServerMessage.MESSAGE_TYPE_ERROR, this.id));
						}

					}

					break;
				}
				case FAILED_TO_JOIN_CHAT_INVALLID_PASSWD: {
					playerChannelManager.handleFailedJoinInvalidPassword((String) simpleCommand.getArgs()[0]);
					break;
				}
				case CLIENT_TO_SERVER_LOG: {
					LogUtil.log().fine((String) simpleCommand.getArgs()[0]);
					break;
				}
				case REBOOT_STRUCTURE: {
					int id = (Integer) simpleCommand.getArgs()[0];
					boolean fast = (Boolean) simpleCommand.getArgs()[1];

					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(id);
					if(sendable != null && sendable instanceof SegmentController) {

						if(fast) {
							long shopRebootCost = ((SegmentController) sendable).getHpController().getShopRebootCost();
							if(shopRebootCost <= credits) {
								setCredits((int) (credits - shopRebootCost));
							} else {
								sendServerMessagePlayerError(Lng.astr("Cannot afford repairs!"));
								break;
							}
						}

						((SegmentController) sendable).getHpController().reboot(fast);
					}

					break;
				}
//				case REPAIR_ARMOR: {
//					int id = (Integer) simpleCommand.getArgs()[0];
//					boolean fast = (Boolean) simpleCommand.getArgs()[1];
//					Sendable sendable = getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(id);
//					if (sendable != null && sendable instanceof SegmentController) {
//						if (fast) {
//							long shopRebootCost = ((SegmentController) sendable).getHpController().getShopArmorRepairCost();
//							if (shopRebootCost <= getCredits()) {
//								setCredits((int) (getCredits() - shopRebootCost));
//							} else {
//								sendServerMessagePlayerError(Lng.astr("Cannot afford repairs!"));
//								break;
//							}
//						}
//						((SegmentController) sendable).getHpController().repairArmor(fast);
//					}
//
//					break;
//				}
				case REBOOT_STRUCTURE_REQUEST_TIME: {
					int id = (Integer) simpleCommand.getArgs()[0];

					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(id);
					if(sendable != null && sendable instanceof SegmentController) {
						((SegmentController) sendable).getHpController().setRebootTimeServerForced((((SegmentController) sendable).getHpController().getRebootTimeMS()));
					}

					break;
				}
				case SPAWN_SHOPKEEP: {
					int id = (Integer) simpleCommand.getArgs()[0];

					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(id);
					if(sendable != null && sendable instanceof ShopSpaceStation) {
						ShopSpaceStation c = (ShopSpaceStation) sendable;
						for(AICreature<? extends AIPlayer> a : c.getAttachedAffinity()) {
							if(a.getFactionId() == FactionManager.TRAIDING_GUILD_ID) {
								sendServerMessagePlayerError(Lng.astr("Shop already has a\nShopkeep!"));
								return;
							}
						}
						Vector3f p = new Vector3f(0, -7, 0);
						c.getWorldTransform().transform(p);

						Transform t = new Transform();
						t.setIdentity();
						t.origin.set(p);
						CreatureSpawn s = new CreatureSpawn(new Vector3i(currentSector), t, "ShopKeep", CreatureType.CHARACTER) {
							@Override
							public void initAI(AIGameCreatureConfiguration<?, ?> aiConfiguration) {
								try {
									assert (aiConfiguration != null);
									aiConfiguration.get(Types.ORIGIN_X).switchSetting("0", false);
									aiConfiguration.get(Types.ORIGIN_Y).switchSetting("-7", false);
									aiConfiguration.get(Types.ORIGIN_Z).switchSetting("0", false);

									aiConfiguration.get(Types.ROAM_X).switchSetting("3", false);
									aiConfiguration.get(Types.ROAM_Y).switchSetting("1", false);
									aiConfiguration.get(Types.ROAM_Z).switchSetting("3", false);

									aiConfiguration.get(Types.ORDER).switchSetting(AIGameCreatureConfiguration.BEHAVIOR_ROAMING, false);

									aiConfiguration.getAiEntityState().getEntity().setFactionId(FactionManager.TRAIDING_GUILD_ID);
								} catch(StateParameterNotFoundException e) {
									e.printStackTrace();
								}

							}
						};

						((GameServerState) state).getController().queueCreatureSpawn(s);
					}

					break;
				}
				case SET_FACTION_RANK_ON_OBJ: {
					int id = (Integer) simpleCommand.getArgs()[0];
					byte rank = (Byte) simpleCommand.getArgs()[1];
					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalUpdatableObjects().get(id);
					if(s != null && s instanceof SegmentController) {

						SegmentController sc = (SegmentController) s;
						sc.setRankRecursive(rank, this, true);

					}
					break;
				}
				case SCAN: {
					int id = (Integer) simpleCommand.getArgs()[0];

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalUpdatableObjects().get(id);

					if(s != null && s instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) s).getManagerContainer() instanceof ScannerManagerInterface) {

						ScannerManagerInterface scanInterface = (ScannerManagerInterface) ((ManagedSegmentController<?>) s).getManagerContainer();

						scanInterface.getScanner().getElementManager().executeScanOnServer(this);

					} else {
						assert (false);
					}

					break;
				}
				case ADD_BLUEPRINT_META_ALL: {
					int metaId = (Integer) simpleCommand.getArgs()[0];
					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					if(getInventory().isLockedInventory()) {
						sendServerMessagePlayerError(Lng.astr("Can't fill blueprint from creative mode!"));
					} else if(metaObject != null && metaObject instanceof BlueprintMetaItem) {
						BlueprintMetaItem bb = (BlueprintMetaItem) metaObject;

						BlueprintEntry bbl;
						try {
							bbl = BluePrintController.active.getBlueprint(bb.blueprintName);

							if(bbl.getType() == BlueprintType.SPACE_STATION && !ServerConfig.BLUEPRINT_SPAWNABLE_STATIONS.isOn()) {
								sendServerMessagePlayerError(Lng.astr("Server doesn't allow blueprint station spawning!"));
							} else if(bbl.getType() == BlueprintType.SHIP && !ServerConfig.BLUEPRINT_SPAWNABLE_SHIPS.isOn()) {
								sendServerMessagePlayerError(Lng.astr("Server doesn't allow blueprint ship spawning!\nPlease use a shipyard!"));
							} else {

								IntOpenHashSet mod = new IntOpenHashSet();

								for(int i = 0; i < ElementKeyMap.highestType + 1; i++) {

									short type = (short) i;
									short invType = type;
									if(ElementKeyMap.isValidType(type) && ElementKeyMap.getInfoFast(type).getSourceReference() != 0) {
										invType = (short) ElementKeyMap.getInfoFast(type).getSourceReference();
									}
									int count = getInventory().getOverallQuantity(invType);

									count = Math.min(count, bb.goal.get(type) - bb.progress.get(type));

									if(count > 0) {

										getInventory().decreaseBatch(invType, count, mod);

										bb.progress.inc(type, count);
									}
								}
								getInventory().sendInventoryModification(mod);
								((GameServerState) state).getGameState().announceMetaObject(metaObject);
							}
						} catch(EntityNotFountException e) {
							sendServerMessagePlayerError(Lng.astr("The blueprint of this item doesn't exist in the catalog!"));
						}
					}
					break;
				}
				case SEND_ALL_DESTINATIONS_OF_ENTITY: {
					int id = (Integer) simpleCommand.getArgs()[0];

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalUpdatableObjects().get(id);

					if(s != null && s instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) s).getManagerContainer() instanceof ActivationManagerInterface) {
						((SegmentController) s).railController.getRoot().railController.sendAdditionalBlueprintInfoToClient();
					}
					break;
				}
				case SET_SPAWN: {
					int id = (Integer) simpleCommand.getArgs()[0];
					long pos = (Long) simpleCommand.getArgs()[1];

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalUpdatableObjects().get(id);

					if(s != null && s instanceof SegmentController) {
						spawnData.setDeathSpawnTo((SegmentController) s, ElementCollection.getPosFromIndex(pos, new Vector3i()));
					}

					break;
				}
				case ADD_BLUEPRINT_META_SINGLE: {
					int metaId = (Integer) simpleCommand.getArgs()[0];
					short type = (Short) simpleCommand.getArgs()[1];
					int count = (Integer) simpleCommand.getArgs()[2];

					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					if(getInventory().isLockedInventory()) {
						sendServerMessagePlayerError(Lng.astr("Can't fill blueprint from creative mode!"));
					} else if(metaObject != null && metaObject instanceof BlueprintMetaItem) {

						BlueprintMetaItem bb = (BlueprintMetaItem) metaObject;

						BlueprintEntry bbl;
						try {
							bbl = BluePrintController.active.getBlueprint(bb.blueprintName);

							if(bbl.getType() == BlueprintType.SPACE_STATION && !ServerConfig.BLUEPRINT_SPAWNABLE_STATIONS.isOn()) {
								sendServerMessagePlayerError(Lng.astr("Server doesn't allow blueprint station spawning!"));
							} else if(bbl.getType() == BlueprintType.SHIP && !ServerConfig.BLUEPRINT_SPAWNABLE_SHIPS.isOn()) {
								sendServerMessagePlayerError(Lng.astr("Server doesn't allow blueprint ship spawning!\nPlease use a shipyard!"));
							} else {
								short invType = type;
								if(ElementKeyMap.isValidType(type) && ElementKeyMap.getInfoFast(type).getSourceReference() != 0) {
									invType = (short) ElementKeyMap.getInfoFast(type).getSourceReference();
								}
								count = Math.min(count, bb.goal.get(type) - bb.progress.get(type));

								count = Math.min(count, getInventory().getOverallQuantity(invType));
								if(count > 0) {
									IntOpenHashSet mod = new IntOpenHashSet();
									getInventory().decreaseBatch(invType, count, mod);
									getInventory().sendInventoryModification(mod);

									bb.progress.inc(type, count);

									((GameServerState) state).getGameState().announceMetaObject(metaObject);
								}
							}
						} catch(EntityNotFountException e) {
							sendServerMessagePlayerError(Lng.astr("The blueprint of this item doesn't exist in the catalog!"));
						}
					}

					break;
				}
				case VERIFY_FACTION_ID: {

					int cFid = (Integer) simpleCommand.getArgs()[0];
//					System.err.println("Faction Verification for "+this+": "+cFid+"; "+getFactionId());
					if(cFid != getFactionId()) {

						networkPlayerObject.factionId.set(getFactionId(), true);
						networkPlayerObject.factionId.setChanged(true);
						networkPlayerObject.setChanged(true);
					}
					break;
				}
				case SET_FREE_WARP_TARGET: {

					int entityId = (Integer) simpleCommand.getArgs()[0];
					long fromIndex = (Long) simpleCommand.getArgs()[1];
					int toX = (Integer) simpleCommand.getArgs()[2];
					int toY = (Integer) simpleCommand.getArgs()[3];
					int toZ = (Integer) simpleCommand.getArgs()[4];

					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
					if(sendable != null && sendable instanceof ManagedSegmentController<?> && ((ManagedSegmentController<?>) sendable).getManagerContainer() instanceof StationaryManagerContainer<?>) {
						StationaryManagerContainer<?> m = (StationaryManagerContainer<?>) ((ManagedSegmentController<?>) sendable).getManagerContainer();
						WarpgateCollectionManager c = m.getWarpgate().getCollectionManagersMap().get(fromIndex);
						if(c != null) {
							String directUID = FTLTable.DIRECT_PREFIX + toX + "_" + toY + "_" + toZ + "_" + DatabaseEntry.removePrefixWOException(m.getSegmentController().getUniqueIdentifier());
							c.setDestination(directUID, new Vector3i(toX, toY, toZ));
							sendServerMessagePlayerInfo(Lng.astr("Successfully set warp gate target to %s!", toX + ", " + toY + ", " + toZ));
						} else {
							sendServerMessagePlayerError(Lng.astr("Warp Gate not found!"));
						}
					} else {
						sendServerMessagePlayerError(Lng.astr("Entity not found!"));
					}

					break;
				}

				case SPAWN_BLUEPRINT_META: {
					int metaId = (Integer) simpleCommand.getArgs()[0];
					String name = (String) simpleCommand.getArgs()[1];
					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);
					int invHolder = (Integer) simpleCommand.getArgs()[2];
					Vector3i invParam = new Vector3i((Integer) simpleCommand.getArgs()[3], (Integer) simpleCommand.getArgs()[4], (Integer) simpleCommand.getArgs()[5]);
					boolean setOwnFaction = (Boolean) simpleCommand.getArgs()[6];
					int spawnOnId = (Integer) simpleCommand.getArgs()[7];
					long spawnOnBlock = (Long) simpleCommand.getArgs()[8];

					SegmentPiece spawnOnRail = null;
					if(spawnOnId > 0) {

						//rail block to spawn on selected
						GameServerState s = (GameServerState) state;
						Sendable sendable = s.getLocalAndRemoteObjectContainer().getLocalObjects().get(spawnOnId);

						if(!(sendable instanceof SegmentController)) {
							sendServerMessagePlayerError(Lng.astr("Cannot spawn on rail: selected object invalid"));
							break;
						}
						SegmentController c = (SegmentController) sendable;

						SegmentPiece pointUnsave = c.getSegmentBuffer().getPointUnsave(spawnOnBlock);

						if(pointUnsave == null || !pointUnsave.isValid() || !pointUnsave.getInfo().isRailDockable()) {
							sendServerMessagePlayerError(Lng.astr("Cannot spawn on rail: selected block is not a rail block to dock on"));
							break;
						}
						spawnOnRail = pointUnsave;

					}
					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolder);

					if(metaObject != null && metaObject instanceof BlueprintMetaItem && sendable != null && sendable instanceof InventoryHolder && ((InventoryHolder) sendable).getInventory(ElementCollection.getIndex(invParam)) != null) {

						Inventory inv = ((InventoryHolder) sendable).getInventory(ElementCollection.getIndex(invParam));

						if(!inv.conatainsMetaItem(metaObject)) {
							sendServerMessage(new ServerMessage(Lng.astr("ERROR: Inventory doesn't\ncontain blueprint!\n(Possible Hack)"), ServerMessage.MESSAGE_TYPE_ERROR, id));
						} else {
							BlueprintMetaItem bb = (BlueprintMetaItem) metaObject;
							if(bb.metGoal()) {
								int fid = setOwnFaction ? getFactionId() : 0;
								BluePrintSpawnQueueElement bluePrintSpawnQueueElement = new BluePrintSpawnQueueElement(bb.blueprintName, name, fid, true, false, false, spawnOnRail);
								bluePrintSpawnQueueElement.metaItem = metaId;
								bluePrintSpawnQueueElement.inv = inv;
								bluePrintSpawnQueue.add(bluePrintSpawnQueueElement);
							} else {
								sendServerMessage(new ServerMessage(Lng.astr("Cannot spawn blueprint!\nThe blueprint doesn't\nhave all the materials\nto spawn it!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
							}
						}
					}
					break;
				}
				case ADD_BLOCK_STORAGE_META_SINGLE: {
					int metaId = (Integer) simpleCommand.getArgs()[0];
					short type = (Short) simpleCommand.getArgs()[1];
					int count = (Integer) simpleCommand.getArgs()[2];

					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					int invHolderId = (Integer) simpleCommand.getArgs()[3];
					int invParamX = (Integer) simpleCommand.getArgs()[4];
					int invParamY = (Integer) simpleCommand.getArgs()[5];
					int invParamZ = (Integer) simpleCommand.getArgs()[6];

					Vector3i param = new Vector3i(invParamX, invParamY, invParamZ);

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolderId);
					if(s == null || !(s instanceof InventoryHolder)) {
						System.err.println("[PLAYER] ERROR: count find inv holder " + invHolderId);
						break;
					}

					InventoryHolder ih = (InventoryHolder) s;

					if(ih.getInventory(ElementCollection.getIndex(param)) == null) {
						System.err.println("[PLAYER] ERROR: count find inv on " + ih + ": " + param);
						break;
					}

					Inventory inv = ih.getInventory(ElementCollection.getIndex(param));

					if(metaObject != null && metaObject instanceof BlockStorageMetaItem) {

						BlockStorageMetaItem bb = (BlockStorageMetaItem) metaObject;

						short invType = type;
						if(ElementKeyMap.isValidType(type) && ElementKeyMap.getInfoFast(type).getSlab() > 0) {
							invType = (short) ElementKeyMap.getInfoFast(type).getSourceReference();
						}
						count = Math.min(count, inv.getOverallQuantity(invType));

						if(count > 0) {
							IntOpenHashSet mod = new IntOpenHashSet();
							inv.decreaseBatch(invType, count, mod);
							inv.sendInventoryModification(mod);

							bb.storage.inc(invType, count);

							((GameServerState) state).getGameState().announceMetaObject(metaObject);
						}
					}

					break;
				}
				case ADD_BLOCK_STORAGE_META_ALL: {
					if(!isAdmin()) {
						break;
					}
					int metaId = (Integer) simpleCommand.getArgs()[0];
					int invHolderId = (Integer) simpleCommand.getArgs()[1];
					int invParamX = (Integer) simpleCommand.getArgs()[2];
					int invParamY = (Integer) simpleCommand.getArgs()[3];
					int invParamZ = (Integer) simpleCommand.getArgs()[4];

					Vector3i param = new Vector3i(invParamX, invParamY, invParamZ);

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolderId);
					if(s == null || !(s instanceof InventoryHolder)) {
						System.err.println("[PLAYER] ERROR: count find inv holder " + invHolderId);
						break;
					}

					InventoryHolder ih = (InventoryHolder) s;

					if(ih.getInventory(ElementCollection.getIndex(param)) == null) {
						System.err.println("[PLAYER] ERROR: count find inv on " + ih + ": " + param);
						break;
					}

					Inventory inv = ih.getInventory(ElementCollection.getIndex(param));
					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);
					addAllToStorageMetaItem(metaObject, inv);

					break;
				}
				case GET_BLOCK_STORAGE_META_SINGLE: {
					int metaId = (Integer) simpleCommand.getArgs()[0];
					short type = (Short) simpleCommand.getArgs()[1];
					int count = (Integer) simpleCommand.getArgs()[2];

					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					int invHolderId = (Integer) simpleCommand.getArgs()[3];
					int invParamX = (Integer) simpleCommand.getArgs()[4];
					int invParamY = (Integer) simpleCommand.getArgs()[5];
					int invParamZ = (Integer) simpleCommand.getArgs()[6];

					Vector3i param = new Vector3i(invParamX, invParamY, invParamZ);

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolderId);
					if(s == null || !(s instanceof InventoryHolder)) {
						System.err.println("[PLAYER] ERROR: count find inv holder " + invHolderId);
						break;
					}

					InventoryHolder ih = (InventoryHolder) s;

					if(ih.getInventory(ElementCollection.getIndex(param)) == null) {
						System.err.println("[PLAYER] ERROR: count find inv on " + ih + ": " + param);
						break;
					}

					Inventory inv = ih.getInventory(ElementCollection.getIndex(param));

					if(metaObject != null && metaObject instanceof BlockStorageMetaItem) {

						BlockStorageMetaItem bb = (BlockStorageMetaItem) metaObject;

						short invType = type;
						count = Math.min(count, bb.storage.get(invType));
						count = Math.min(count, inv.canPutInHowMuch(invType, count, -1));

						if(count > 0 && inv.canPutIn(invType, count)) {
							IntOpenHashSet mod = new IntOpenHashSet();
							mod.add(inv.incExistingOrNextFreeSlotWithoutException(invType, count));

							bb.storage.inc(type, -count);
							if(bb.storage.getTotalAmount() <= 0) {
								int slot = inv.getSlotFromMetaId(bb.getId());
								if(slot >= 0) {
									inv.removeMetaItem(bb);
									mod.add(slot);
								}
							} else {
								((GameServerState) state).getGameState().announceMetaObject(metaObject);
							}
							inv.sendInventoryModification(mod);
						} else {
							sendServerMessage(new ServerMessage(Lng.astr("Requested items don't fit in inventory!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
						}
					}

					break;
				}
				case GET_BLOCK_STORAGE_META_ALL: {
					int metaId = (Integer) simpleCommand.getArgs()[0];

					MetaObject metaObject = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					int invHolderId = (Integer) simpleCommand.getArgs()[1];
					int invParamX = (Integer) simpleCommand.getArgs()[2];
					int invParamY = (Integer) simpleCommand.getArgs()[3];
					int invParamZ = (Integer) simpleCommand.getArgs()[4];

					Vector3i param = new Vector3i(invParamX, invParamY, invParamZ);

					Sendable s = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(invHolderId);
					if(s == null || !(s instanceof InventoryHolder)) {
						System.err.println("[PLAYER] ERROR: count find inv holder " + invHolderId);
						break;
					}

					InventoryHolder ih = (InventoryHolder) s;

					if(ih.getInventory(ElementCollection.getIndex(param)) == null) {
						System.err.println("[PLAYER] ERROR: count find inv on " + ih + ": " + param);
						break;
					}

					Inventory inv = ih.getInventory(ElementCollection.getIndex(param));

					if(metaObject != null && metaObject instanceof BlockStorageMetaItem) {

						BlockStorageMetaItem bb = (BlockStorageMetaItem) metaObject;

						for(short type : ElementKeyMap.keySet) {
							int count;
							if((count = bb.storage.get(type)) > 0) {
								short invType = type;
								count = Math.min(count, bb.storage.get(invType));
								count = Math.min(count, inv.canPutInHowMuch(invType, count, -1));

								if(count > 0 && inv.canPutIn(invType, count)) {
									IntOpenHashSet mod = new IntOpenHashSet();
									mod.add(inv.incExistingOrNextFreeSlotWithoutException(invType, count));

									bb.storage.inc(type, -count);
									if(bb.storage.getTotalAmount() <= 0) {
										int slot = inventory.getSlotFromMetaId(bb.getId());
										if(slot >= 0) {
											inventory.removeMetaItem(bb);
											mod.add(slot);
										}
									} else {
										((GameServerState) state).getGameState().announceMetaObject(metaObject);
									}
									inv.sendInventoryModification(mod);
								} else {
									sendServerMessage(new ServerMessage(Lng.astr("Requested items don't fully fit in inventory!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
									break;
								}
							}
						}
					}

					break;
				}
				case REQUEST_BLUEPRINT_ITEM_LIST: {

					String name = (String) simpleCommand.getArgs()[0];

					try {
						BlueprintEntry blueprint = BluePrintController.active.getBlueprint(name);

						networkPlayerObject.blockCountMapBuffer.add(new RemoteBlockCountMap(blueprint.getElementCountMapWithChilds(), onServer));

					} catch(EntityNotFountException e) {
						e.printStackTrace();
					}

					break;
				}
				default:
					break;
			}

		}
	}

	public void addAllToStorageMetaItem(MetaObject metaObject, Inventory inv) {

		if(metaObject != null && metaObject instanceof BlockStorageMetaItem) {
			BlockStorageMetaItem bb = (BlockStorageMetaItem) metaObject;

			BlueprintEntry bbl;

			IntOpenHashSet mod = new IntOpenHashSet();

			for(short i : ElementKeyMap.keySet) {

				short type = i;
				short invType = type;
				if(ElementKeyMap.isValidType(type) && ElementKeyMap.getInfoFast(type).getSourceReference() != 0) {
					invType = (short) ElementKeyMap.getInfoFast(type).getSourceReference();
				}
				int count = inv.getOverallQuantity(invType);

				if(count > 0) {

					inv.decreaseBatch(invType, count, mod);
					bb.storage.inc(invType, count);
				}
			}
			inv.sendInventoryModification(mod);
			((GameServerState) state).getGameState().announceMetaObject(metaObject);
		}
	}

	public boolean isAdmin() {
		if(onServer) {
			return ((GameServerState) state).isAdmin(name);
		}
		return networkPlayerObject.isAdminClient.getBoolean();
	}

	public String getFactionOwnRankName() {
		return factionController.getFactionOwnRankName();
	}

	public String getFactionRankName(byte rank) {
		return factionController.getFactionRankName(rank);
	}

	public boolean isInTutorial() {
		return Sector.isTutorialSector(currentSector);
	}

	public boolean isInTestSector() {
		return currentSector.equals(testSector);
	}

	public boolean isInPersonalSector() {
		return currentSector.equals(personalSector);
	}

	@Override
	public void fromTagStructure(Tag tag) {
		newPlayerOnServer = false;
		Tag[] struct = (Tag[]) tag.getValue();

		Object creditsObj = struct[0].getValue();
		if(creditsObj instanceof Long) {
			setCredits((Long) creditsObj);
		} else if(creditsObj instanceof Integer) {
			setCredits((Integer) creditsObj);
		}

		//		System.err.println("["+(isOnServer()? "SERVER": "CLIENT")+"][TAG] SPAWNING POINT OF "+this+" READ: "+this.getSpawnPoint());
		if(struct[1].getType() == Tag.Type.VECTOR3f) {
			//old
			spawnData.deathSpawn.localPos.set((Vector3f) struct[1].getValue());
		} else if(struct[1].getType() == Tag.Type.STRUCT) {
			spawnData.fromTagStructure(struct[1]);
		}

		if(struct[3].getType() == Tag.Type.VECTOR3i && "sector".equals(struct[3].getName())) {
			spawnData.deathSpawn.absoluteSector.set((Vector3i) struct[3].getValue());
		}
		getPersonalInventory().fromTagStructure(struct[2]);

		if(struct.length >= 6 && struct[4].getType() == Tag.Type.VECTOR3f && "lspawn".equals(struct[4].getName())) {
			spawnData.logoutSpawn.localPos.set((Vector3f) struct[4].getValue());
			//			System.err.println("[SERVER][PLAYER][TAG] read logout spawn point: "+this.getLogoutSpawnPoint());
		} else {
//			this.getLogoutSpawnPoint().set(getSpawnPoint());
		}

		if(struct.length >= 6 && struct[5].getType() == Tag.Type.VECTOR3i && "lsector".equals(struct[5].getName())) {
			currentSector.set((Vector3i) struct[5].getValue());
			spawnData.logoutSpawn.absoluteSector.set((Vector3i) struct[5].getValue());
			//			System.err.println("[SERVER][PLAYER][TAG] read logout spawn sector: "+this.getLogoutSpawnSector());
		} else {
//			this.getLogoutSpawnSector().set(getSpawnSector());
//			this.getCurrentSector().set(getSpawnSector());
		}

		if(struct.length >= 7) {
			factionController.fromTagStructure(struct[6]);
		}

		if(struct.length >= 11) {

			spawnData.lastLogin = (Long) struct[7].getValue();
			spawnData.lastLogout = (Long) struct[8].getValue();

			if("ips".equals(struct[9].getName())) {
				ArrayList<String> r = new ArrayList<String>();
				Tag.listFromTagStruct(r, (Tag[]) struct[9].getValue());
				for(String s : r) {
					getHosts().add(new PlayerInfoHistory(0, s, ""));
				}
			} else {
				try {
					Tag.listFromTagStruct(PlayerInfoHistory.class.getConstructor(), getHosts(), (Tag[]) struct[9].getValue());
				} catch(SecurityException e) {
					e.printStackTrace();
				} catch(NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
		}
		Collections.sort(getHosts());
		if(struct.length > 10 && struct[10].getType() == Tag.Type.BYTE) {
			hasCreativeMode = ((Byte) struct[10].getValue()) != 0;
//			cubatomInventory.fromTagStructure(struct[10]);
		}
		if(struct.length >= 13) {
			lastEnteredEntity = (String) struct[11].getValue();
		}
		if(struct.length >= 14 && struct[12].getType() == Tag.Type.VECTOR3i) {
			testSector = (Vector3i) struct[12].getValue();
		}
		if(struct.length >= 15 && struct[13].getType() == Tag.Type.INT) {
			helmetSlot = (Integer) struct[13].getValue();
		}
		if(struct.length >= 16 && struct[14].getType() != Tag.Type.FINISH) {
			playerAiManager.fromTagStructure(struct[14]);
		}

		if(struct.length >= 17 && struct[15].getType() != Tag.Type.FINISH && struct[15].getType() != Tag.Type.BYTE) {
			personalSector = (Vector3i) struct[15].getValue();
		}

		if(struct.length >= 18 && struct[16].getType() != Tag.Type.FINISH) {
			getPersonalFactoryInventoryCapsule().fromTagStructure(struct[16]);
		}
		if(struct.length >= 19 && struct[17].getType() != Tag.Type.FINISH) {
			getPersonalFactoryInventoryMicro().fromTagStructure(struct[17]);
		}
		if(struct.length >= 20 && struct[18].getType() != Tag.Type.FINISH) {
			getPersonalFactoryInventoryMacroBlock().fromTagStructure(struct[18]);
		}
		if(struct.length >= 21 && struct[19].getType() != Tag.Type.FINISH) {
			lastDeathNotSuicide = (Long) struct[19].getValue();
		}
		if(struct.length >= 22 && struct[20].getType() != Tag.Type.FINISH) {

			Tag.listFromTagStructSP(scanHistory, struct[20], new ListSpawnObjectCallback<ScanData>() {

				@Override
				public ScanData get(Object fromValue) {
					ScanData d = new ScanData();
					d.fromTagStructure((Tag) fromValue);
					return d;
				}
			});
//			assert(false):scanHistory;
		}
		if(struct.length >= 23 && struct[21].getType() != Tag.Type.FINISH) {
			factionPointProtected = ((Byte) struct[21].getValue()).byteValue() == (byte) 1;
		}

		if(struct.length >= 24 && struct[22].getType() != Tag.Type.FINISH) {
			invBackUpfromTag(struct[22]);
		}

		if(struct.length >= 25 && struct[23].getType() != Tag.Type.FINISH) {

			Tag.listFromTagStructSPElimDouble(savedCoordinates, struct[23], new ListSpawnObjectCallback<SavedCoordinate>() {

				@Override
				public SavedCoordinate get(Object fromValue) {
					SavedCoordinate d = new SavedCoordinate();
					d.fromTagStructure((Tag) fromValue);
					return d;
				}
			});
//			assert(false):scanHistory;
		}

//		if (struct.length >= 26 && struct[24].getType() == Type.VECTOR3f) {
//			if (preTutorialTransform == null) {
//				preTutorialTransform = new Transform();
//			}
//			preTutorialTransform.origin.set((Vector3f) struct[24].getValue());
//		}

//		if (struct.length >= 27 && struct[25].getType() == Type.VECTOR3i) {
//			if (preTutorialSector == null) {
//				preTutorialSector = new Vector3i();
//			}
//			preTutorialSector.set((Vector3i) struct[25].getValue());
//		}
		if(struct.length >= 28 && struct[26].getType() == Tag.Type.STRUCT) {
			Tag.listFromTagStruct(ignored, (Tag[]) struct[26].getValue());
		}
		if(struct.length >= 29 && struct[27].getType() == Tag.Type.FLOAT) {
			health = (Float) struct[27].getValue();
			lastHP = health; // Prevent hurt 'animation' from being triggered
		}

		if(struct.length >= 30 && struct[28].getType() == Tag.Type.STRUCT) {
			cargoInventoryBlock = SegmentPiece.getFromUniqueTag(struct[28], 0);

		}
		if(struct.length > 29 && struct[29].getType() == Tag.Type.STRUCT) {
			infiniteInventoryVolume = struct[29].getBoolean();
		}
		if(struct.length > 30 && struct[30].getType() == Tag.Type.INT) {
			mineAutoArmSecs = struct[30].getInt();
		}
	}

	public boolean isControllingCore(ManagedUsableSegmentController<?> c) {
		if(c instanceof Ship) {
			return controllerState.isControlling(this, c, Ship.core);
		} else {
			return false;
		}
	}

	@Override
	public Tag toTagStructure() {
		try {
			((GameServerState) state).getDatabaseIndex().getTableManager().getPlayerTable().updateOrInsertPlayer(this);
		} catch(SQLException e) {
			e.printStackTrace();
		}

		Tag[] d = new Tag[32];
		d[0] = (new Tag(Tag.Type.LONG, "credits", credits));
//		System.err.println("["+(isOnServer()? "SERVER": "CLIENT")+"][TAG] SPAWNING POINT OF "+this+" WRITTEN: "+this.getSpawnPoint());
		d[1] = spawnData.toTagStructure();
		d[2] = getPersonalInventory().toTagStructure();
		d[3] = (new Tag(Tag.Type.BYTE, null, (byte) 0)); //removed (spawnpoint meta). can be used for somethign else

//		if(getLogoutSpawnPoint().lengthSquared() == 0){
//			((GameServerState)getState()).getController().setLogoutSpawnToPlayerPos(this);
//		}
//		System.err.println("["+(isOnServer()? "SERVER": "CLIENT")+"][TAG] LOGOUT SPAWNING POINT OF "+this+" WRITTEN: "+this.getLogoutSpawnPoint()+" SECTOR "+getLogoutSpawnSector());
		d[4] = (new Tag(Tag.Type.BYTE, null, (byte) 0));//removed (spawnpoint meta). can be used for somethign else

		d[5] = (new Tag(Tag.Type.BYTE, null, (byte) 0));//removed (spawnpoint meta). can be used for somethign else

		d[6] = factionController.toTagStructure();

		d[7] = new Tag(Tag.Type.LONG, null, spawnData.lastLogin);
		d[8] = new Tag(Tag.Type.LONG, null, System.currentTimeMillis()); //last logout

		d[9] = Tag.listToTagStruct(getHosts(), "hist");

		d[10] = (new Tag(Tag.Type.BYTE, null, hasCreativeMode ? (byte) 1 : (byte) 0));//cubatomInventory.toTagStructure();
		d[11] = new Tag(Tag.Type.STRING, null, lastEnteredEntity != null ? lastEnteredEntity : "none");

		//free to use since playerKeyConfig became deprecated and is no longer saved
		//make sure, tag isnt struct though
		d[12] = testSector == null ? new Tag(Tag.Type.BYTE, null, (byte) 0) : new Tag(Tag.Type.VECTOR3i, null, testSector); //playerKeyConfigurationManager.toTagStructure();

		d[13] = new Tag(Tag.Type.INT, null, helmetSlot);
		d[14] = playerAiManager.toTagStructure();

		d[15] = personalSector == null ? new Tag(Tag.Type.BYTE, null, (byte) 0) : new Tag(Tag.Type.VECTOR3i, null, personalSector);

		d[16] = getPersonalFactoryInventoryCapsule().toTagStructure();
		d[17] = getPersonalFactoryInventoryMicro().toTagStructure();
		d[18] = getPersonalFactoryInventoryMacroBlock().toTagStructure();

		d[19] = new Tag(Tag.Type.LONG, null, lastDeathNotSuicide);

		d[20] = Tag.listToTagStruct(scanHistory, null);

		d[21] = factionPointProtected ? new Tag(Tag.Type.BYTE, null, (byte) 1) : new Tag(Tag.Type.BYTE, null, (byte) 0);

		d[22] = invBackUpToTag();

		d[23] = Tag.listToTagStruct(savedCoordinates, null);

		d[24] = new Tag(Tag.Type.BYTE, null, (byte) 0); //preTutorialTransform == null ? new Tag(Type.BYTE, null, (byte) 0) : new Tag(Type.VECTOR3f, "pretutpoint", preTutorialTransform.origin);
		d[25] = new Tag(Tag.Type.BYTE, null, (byte) 0); //preTutorialSector == null ? new Tag(Type.BYTE, null, (byte) 0) : new Tag(Type.VECTOR3i, "pretutsector", preTutorialSector);

		d[26] = Tag.listToTagStruct(getIgnored(), Tag.Type.STRING, null);

		d[27] = new Tag(Tag.Type.FLOAT, null, health);

		d[28] = cargoInventoryBlock != null ? cargoInventoryBlock.getUniqueTag() : new Tag(Tag.Type.BYTE, null, (byte) 0);

		d[29] = new Tag(null, infiniteInventoryVolume);

		d[30] = new Tag(Tag.Type.INT, null, mineAutoArmSecs);

		d[31] = FinishTag.INST;
		Tag root = new Tag(Tag.Type.STRUCT, "PlayerState", d);
		return root;
	}

	public SimpleTransformableSendableObject getAquiredTarget() {
		return aquiredTarget;
	}

	public void setAquiredTarget(SimpleTransformableSendableObject target) {
		//INSERTED CODE
		//Filter out null sets because it spams it every few ms
		if(target != null) {
			PlayerAcquireTargetEvent event = new PlayerAcquireTargetEvent(this, aquiredTarget);
			StarLoader.fireEvent(event, onServer);
		}
		///
		aquiredTarget = target;
	}

	public PlayerCharacter getAssingedPlayerCharacter() {
		return playerCharacter;
	}

	public PlayerCatalogManager getCatalog() {
		return catalog;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public void callTutorialServer(String tutorialName) {
		assert (onServer);
		networkPlayerObject.tutorialCalls.add(new RemoteString(tutorialName, true));
	}

	public void callTutorialClient(String tutorialName) {

		assert (!onServer);
		if(((GameClientState) state).getController().getTutorialMode() != null) {
			((GameClientState) state).getController().getTutorialMode().setCurrentMachine(tutorialName);
			assert (((GameClientState) state).getController().getTutorialMode().getMachine() != null) : tutorialName + " :::: " + ((GameClientState) state).getController().getTutorialMode().getMachineNames();
			((GameClientState) state).getController().getTutorialMode().getMachine().reset();
			tutorialCallClient = null;
		}
	}

	private ShopInterface getClosestShopsInDistance() {
		float l = -1;
		ShopInterface currentClosestShop = null;
		for(ShopInterface shop : shopsInDistance) {
			Vector3f pos = new Vector3f(shop.getWorldTransform().origin);
			pos.sub(((SimpleTransformableSendableObject) controllerState.getUnits().iterator().next().playerControllable).getWorldTransform().origin);
			if(l < 0 || pos.lengthSquared() < l) {
				currentClosestShop = shop;
				l = pos.lengthSquared();
			}
		}
		return currentClosestShop;
	}

	/**
	 * @return the controllerState
	 */
	public ControllerState getControllerState() {
		return controllerState;
	}

	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * @return the credits
	 */
	public long getCredits() {
		return credits;
	}

	/**
	 * @return Credits as an integer, MAX_INT if above
	 */
	public int getCreditsInt() {
		return (int) Math.min(Integer.MAX_VALUE, credits);
	}

	/**
	 * @param credits the credits to set
	 */
	public void setCredits(long credits) {
		// System.err.println("SETTING CREDITS TO "+credits+" on "+getState());
		this.credits = Math.max(0, credits);

		if(onServer) {
			ruleEntityManager.triggerPlayerCreditsChanged();
		}
	}

//	public void handleSpawnSetFromNT(NetworkPlayer p) {
//		if(isOnServer()){
//			for (RemoteVector3f s : p.spawnPointSetBuffer.getReceiveBuffer()) {
//				((GameServerState)getState()).getController().setSpawnToPos(this, s.getVector(), false);
//			}
//		}
//	}

	//	public void requestControl(PlayerControllable controllable, Vector3i param) {
	//
	//		//		 System.err.println(this+", Issuing Control Request: "+controllable+", "+param);
	//		// try{
	//		// throw new NullPointerException("GAIN");
	//		// }catch(NullPointerException e){
	//		// e.printStackTrace();
	//		// }
	//		synchronized (getNetworkObject()) {
	//			assert (param != null);
	//
	//
	//
	//			RemoteIntegerArray a = new RemoteIntegerArray(6, getNetworkObject());
	//			if(controllable != null){
	//				a.set(0, controllable.getId());
	//			}else{
	//				a.set(0, -1);
	//			}
	//			a.set(1, 1);
	//			a.set(2, param.x);
	//			a.set(3, param.y);
	//			a.set(4, param.z);
	//			a.set(5, 0);
	//			getNetworkObject().controlRequestParameterBuffer.add(a);
	//		}
	//	}

	/**
	 * @return the currentSector
	 */
	public Vector3i getCurrentSector() {
		return currentSector;
	}

	/**
	 * @param currentSector the currentSector to set
	 */
	public void setCurrentSector(Vector3i currentSector) {
		if(!currentSector.equals(this.currentSector)) {
			lastSector.set(this.currentSector);
			sectorChanged = true;
		}
		this.currentSector.set(currentSector);

	}

	/**
	 * @return the currentSectorId
	 */
	public int getCurrentSectorId() {
		return currentSectorId;
	}

	/**
	 * @param currentSectorId the currentSectorId to set
	 */
	public void setCurrentSectorId(int currentSectorId) {
		this.currentSectorId = currentSectorId;
	}

	public int getCurrentShipControllerSlot() {
		if(isClientOwnPlayer()) {
			return shipControllerSlotClient;
		}
		return networkPlayerObject.shipControllerSlot.get();
	}

	public void setCurrentShipControllerSlot(byte i, float delay) {
		assert (!onServer);
		shipControllerSlotClient = i;
		if(delay == 0) {
			networkPlayerObject.shipControllerSlot.set(i, true);
		}
		slotChangeUpdateDelay = delay;
	}

	/**
	 * @return the deaths
	 */
	public int getDeaths() {
		return deaths;
	}

	/**
	 * @param deaths the deaths to set
	 */
	public void setDeaths(int deaths) {
		this.deaths = deaths;
	}

	/**
	 * @return the playerConversationManager
	 */
	public PlayerConversationManager getPlayerConversationManager() {
		return playerConversationManager;
	}

	public PlayerFactionController getFactionController() {
		return factionController;
	}

	@Override
	public int getFactionId() {
		return factionController.getFactionId();
	}

	@Override
	public String getUniqueIdentifier() {
		return "ENTITY_PLAYERSTATE_" + name;
	}

	public SimpleTransformableSendableObject getFirstControlledTransformable() throws PlayerControlledTransformableNotFound {
		for(ControllerStateUnit c : controllerState.getUnits()) {
			if(c.playerState.equals(this) && c.playerControllable instanceof SimpleTransformableSendableObject) {
				SimpleTransformableSendableObject obj = (SimpleTransformableSendableObject) c.playerControllable;
				return obj;
			}
		}
		throw new PlayerControlledTransformableNotFound(this);
	}

	public SimpleTransformableSendableObject getFirstControlledTransformableWOExc() {
		for(ControllerStateUnit c : controllerState.getUnits()) {
			if(c.playerState.equals(this) && c.playerControllable instanceof SimpleTransformableSendableObject) {
				SimpleTransformableSendableObject obj = (SimpleTransformableSendableObject) c.playerControllable;
				return obj;
			}
		}
		return null;
	}

	/**
	 * @return the hosts
	 */
	public List<PlayerInfoHistory> getHosts() {
		return spawnData.hosts;
	}

	/**
	 * @return the inventoryController
	 */
	public InventoryController getInventoryController() {
		return inventoryController;
	}

	/**
	 * @param controller the inventoryController to set
	 */
	public void setInventoryController(InventoryController controller) {
		inventoryController = controller;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return the kills
	 */
	public int getKills() {
		return kills;
	}

	/**
	 * @param kills the kills to set
	 */
	public void setKills(int kills) {
		this.kills = kills;
	}

	/**
	 * @return the lastEnteredEntity
	 */
	public String getLastEnteredEntity() {
		return lastEnteredEntity;
	}

	/**
	 * @param lastEnteredEntity the lastEnteredEntity to set
	 */
	public void setLastEnteredEntity(String lastEnteredEntity) {
		this.lastEnteredEntity = lastEnteredEntity;
	}

	/**
	 * @return the lastLogin
	 */
	public long getLastLogin() {
		return spawnData.lastLogin;
	}

	/**
	 * @return the lastLogout
	 */
	public long getLastLogout() {
		return spawnData.lastLogout;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the state
	 */
	@Override
	public StateInterface getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(StateInterface state) {
		this.state = state;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the ping
	 */
	public int getPing() {
		return ping;
	}

	/**
	 * @param ping the ping to set
	 */
	public void setPing(int ping) {
		this.ping = ping;
	}

	public ClientProximitySector getProximitySector() {
		return proximitySector;
	}

	public ClientProximitySystem getProximitySystem() {
		return proximitySystem;
	}

	public FactionRelation.RType getRelation(int factionId) {
		if(factionId != 0 && factionId == getFactionId()) {
			return FactionRelation.RType.FRIEND;
		}

		return factionController.getRelation(factionId);
	}

	public FactionRelation.RType getRelation(PlayerState otherState) {

		if(otherState.getFactionId() != 0 && otherState.getFactionId() == getFactionId()) {
			return FactionRelation.RType.FRIEND;
		}

		FactionRelation.RType type0 = factionController.getRelation(otherState);
		FactionRelation.RType type1 = otherState.factionController.getRelation(this);

		if(type0 == FactionRelation.RType.ENEMY || type1 == FactionRelation.RType.ENEMY) {
			return FactionRelation.RType.ENEMY;
		}

		return type0;
	}

	/**
	 * @return the uploadController
	 */
	public UploadController getShipUploadController() {
		return shipUploadController;
	}

	/**
	 * @return the shopsInDistance
	 */
	public Set<ShopInterface> getShopsInDistance() {
		return shopsInDistance;
	}

	/**
	 * @return the skinManager
	 */
	public SkinManager getSkinManager() {
		return skinManager;
	}

	/**
	 * @return the skinUploadController
	 */
	public SkinUploadController getSkinUploadController() {
		return skinUploadController;
	}

	public void getWordTransform(Transform out) {
		GlUtil.setRightVector(getRight(transTmp.origin), out);
		GlUtil.setUpVector(getUp(transTmp.origin), out);
		GlUtil.setForwardVector(getForward(transTmp.origin), out);
	}

	public void handleBeingKilled() {
		if(!killerIds.isEmpty()) {
			synchronized(killerIds) {
				while(!killerIds.isEmpty()) {

					int killerId = killerIds.dequeueInt();

					Sendable sendableKiller = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(killerId);

					if(!onServer) {
						String message = "";
						controllerState.removeAllUnitsFromPlayer(this, true);

						if(isClientOwnPlayer()) {
							((GameClientState) state).setCurrentPlayerObject(null);
//							if(getAbstractCharacterObject() != null){
//								getAbstractCharacterObject().neverSpawnedBefore = true;
//							}
						}
						if(sendableKiller != null) {
							if(id == sendableKiller.getId()) {
								message = Lng.str("Cause of last Death:\nYou committed suicide.");
								((GameClientState) state).getController().popupGameTextMessage(Lng.str("%s\ncommitted suicide.", name), 0);

							} else {
								if(sendableKiller instanceof Damager) {
									message = "Cause of last Death:\n" + ((Damager) sendableKiller).getName() + " killed you";
									((GameClientState) state).getController().popupGameTextMessage(Lng.str("%s\nkilled\n%s!", ((Damager) sendableKiller).getName(), name), 0);
								} else {
									message = "Cause of last Death:\n" + sendableKiller + " killed you";
									((GameClientState) state).getController().popupGameTextMessage(Lng.str("%s\nkilled\n%s!", sendableKiller, name), 0);
								}

							}
						} else {
							message = "Cause of last Death:\n" + "unkownObject(" + killerId + ") killed you";
							((GameClientState) state).getController().popupGameTextMessage(Lng.str("Unkown Object(%s) killed\n%s!", killerId, name), 0);
						}
						lastDiedMessage = message;
					} else {
						System.err.println("[SERVER][PLAYER] " + this + " received suicide request: killer: " + sendableKiller);
						LogUtil.log().fine("[DEATH] " + name + " committed suicide");
						handleServerHealthAndCheckAliveOnServer(0, (Damager) sendableKiller);
					}

				}
			}
		}
	}

	public void enqueueClientBlueprintToWrite(SegmentController ship, String name, BlueprintClassification classification) {
		BluePrintWriteQueueElement e = new BluePrintWriteQueueElement(ship, name, classification, true);
		bluePrintWriteQueue.add(e);
	}

	public void handleBluePrintQueuesClient() throws EntityNotFountException, IOException, EntityAlreadyExistsException {
		for(int i = 0; i < bluePrintWriteQueue.size(); i++) {
			BluePrintWriteQueueElement e = bluePrintWriteQueue.get(i);

			if(!e.requestedAdditionalBlueprintData) {
				e.segmentController.getNetworkObject().additionalBlueprintData.set(false, true);
				e.requestedAdditionalBlueprintData = true;
				sendSimpleCommand(SimplePlayerCommands.SEND_ALL_DESTINATIONS_OF_ENTITY, e.segmentController.getId());
				System.err.println("[CLIENT][PLAYER][BLUEPRINT] " + this + " REQUESTED EXTRA INFO FOR LOCAL BLUEPRINT " + e.segmentController);
			} else if(e.segmentController.railController.getRoot().railController.isAllAdditionalBlueprintInfoReceived()) {

				try {
					((CatalogState) state).getCatalogManager().writeEntryClient(e, name);
					System.err.println("[CLIENT][PLAYER][BLUEPRINT] " + this + " SAVED LOCAL BLUEPRINT " + e.segmentController);
				} catch(IOException ex) {
					ex.printStackTrace();
				}
				bluePrintWriteQueue.remove(i);
				i--;
			}
		}
	}

	public void handleBluePrintQueuesServer() throws EntityNotFountException, IOException, EntityAlreadyExistsException {
		while(!bluePrintWriteQueue.isEmpty()) {
			BluePrintWriteQueueElement e = bluePrintWriteQueue.remove(0);
			try {
				((CatalogState) state).getCatalogManager().writeEntryServer(e, name);
				System.err.println("[SERVER][PLAYER][BLUEPRINT] " + this + " SAVED BLUEPRINT " + e.segmentController);
				LogUtil.log().fine("[BLUEPRINT][SAVE] " + name + " saved: \"" + e.name + "\"");
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		while(!bluePrintSpawnQueue.isEmpty()) {
			BluePrintSpawnQueueElement e = bluePrintSpawnQueue.remove(0);
			Transform t = new Transform();
			t.setIdentity();
			try {
				if(System.currentTimeMillis() - lastBlueprintSpawn > blueprintDelay) {
					SimpleTransformableSendableObject firstControlledTransformable = getFirstControlledTransformable();
					t.set(firstControlledTransformable.getWorldTransform());
					long ti = System.currentTimeMillis();

					if(e.metaItem >= 0) {

						MetaObject object = ((GameServerState) state).getMetaObjectManager().getObject(e.metaItem);

						if(object == null) {
							System.err.println("[SERVER][BLUEPRINT][BUY] Exception: Meta Object Blueptinz not found: " + e.metaItem);
							continue;
						}
						if(!(object instanceof BlueprintMetaItem)) {
							System.err.println("[SERVER][BLUEPRINT][BUY] Exception: Metaitem not a blueprint: " + object);
							continue;
						}
						BlueprintMetaItem bb = ((BlueprintMetaItem) object);
						if(!bb.metGoal()) {
							System.err.println("[SERVER][BLUEPRINT][BUY] Exception: Metaitem not a blueprint: " + object);
							continue;
						}
						SegmentControllerOutline<?> loadBluePrint = null;
						try {
							loadBluePrint = BluePrintController.active.loadBluePrint((GameServerState) state, e.catalogName, e.shipName, t, -1, e.factionId, currentSector, getUniqueIdentifier(), buffer, e.toDockOn, e.activeAI, new ChildStats(false));
							loadBluePrint.checkOkName();

							if(!((GameServerState) state).getGameConfig().isBBOk(loadBluePrint.en)) {
								sendServerMessagePlayerError(Lng.astr("Cannot spawn blueprint! Server doesn't allow ship dimension!\nAllowed: %s", ((GameServerState) state).getGameConfig().toStringAllowedSize(loadBluePrint.en)));
								continue;
							}

						} catch(EntityNotFountException ex) {
							ex.printStackTrace();
							//blueprint was deleted
						}

						int stationsAllowed = (Integer) ServerConfig.ALLOWED_STATIONS_PER_SECTOR.getCurrentState();
						boolean sectorOk = true;
						boolean spawningAllowed = true;

						int stations = 0;
						if(loadBluePrint != null && loadBluePrint.en.getType() == org.schema.game.server.data.blueprintnw.BlueprintType.SPACE_STATION) {
							if(!ServerConfig.BLUEPRINT_SPAWNABLE_STATIONS.isOn()) {
								spawningAllowed = false;
							}
							for(Sendable s : state.getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
								if(s instanceof SpaceStation && ((SpaceStation) s).getSectorId() == currentSectorId) {
									stations++;
								}
							}
							sectorOk = stations < stationsAllowed;
						}

						if(loadBluePrint != null && loadBluePrint.en.getType() == org.schema.game.server.data.blueprintnw.BlueprintType.SHIP) {
							if(!ServerConfig.BLUEPRINT_SPAWNABLE_SHIPS.isOn()) {
								spawningAllowed = false;
							}
						}
						if(!spawningAllowed) {
							sendServerMessagePlayerError(Lng.astr("Server doesn't allow blueprint spawning!"));
						} else if(sectorOk) {
							if(loadBluePrint != null && loadBluePrint.en.getElementCountMapWithChilds().equals(bb.goal)) {

								long timeLoad = System.currentTimeMillis() - ti;
								ti = System.currentTimeMillis();
								loadBluePrint.spawnSectorId.set(currentSector);
								loadBluePrint.checkProspectedBlockCount = true;
								synchronized(((GameServerState) state).getBluePrintsToSpawn()) {
									((GameServerState) state).getBluePrintsToSpawn().add(loadBluePrint);
								}
//								loadBluePrint.spawn(this.getCurrentSector(), true, new ChildStats(false));

								long timeSpawn = System.currentTimeMillis() - ti;

								lastBlueprintSpawn = System.currentTimeMillis();
								String msg = "[BLUEPRINT][BUY] " + name + " bought blueprint from metaItem: \"" + loadBluePrint.en.getName() + "\" as \"" + loadBluePrint.realName + "\"; Price: " + loadBluePrint.en.getPrice() + "; to sector: " + currentSector + " (loadTime: " + timeLoad + "ms, spawnTime: " + timeSpawn + "ms)";

								System.err.println(msg);
								LogUtil.log().fine(msg);
							} else {

								sendServerMessagePlayerError(Lng.astr("Can't spawn this blueprint!\nIt was changed or removed.\nSpawning items in space."));
								if(getFirstControlledTransformableWOExc() != null) {
									bb.progress.spawnInSpace(getFirstControlledTransformableWOExc());
								}
								String bbname = (loadBluePrint != null ? loadBluePrint.en.getName() : "DELETED");
								String bbRname = (loadBluePrint != null ? loadBluePrint.realName : "DELETED");
								String msg = "[BLUEPRINT][BUY] " + name + " failed to buy blueprint from metaItem: \"" + e.catalogName + "\" as \"" + bbRname + "\"; to sector: " + currentSector + "; MetaItem goal for that blueprint differed";
								System.err.println(msg);
								LogUtil.log().fine(msg);
							}

							e.inv.removeMetaItem(object);
						} else {
							sendServerMessagePlayerError(Lng.astr("Cannot spawn here!\nOnly %s station(s) per sector\nallowed!", stationsAllowed));
						}

					} else {
						SegmentControllerOutline loadBluePrint = BluePrintController.active.loadBluePrint((GameServerState) state, e.catalogName, e.shipName, t, e.infiniteShop ? -1 : credits, e.factionId, currentSector, getUniqueIdentifier(), buffer, e.toDockOn, e.activeAI, new ChildStats(false));
						//check this blueprint against block count actually spawned (cheat protection)
						loadBluePrint.checkOkName();
						if(loadBluePrint.en.getType() == org.schema.game.server.data.blueprintnw.BlueprintType.SHIP) {
							long timeLoad = System.currentTimeMillis() - ti;
							ti = System.currentTimeMillis();
							if(!e.infiniteShop) {
								modCreditsServer(-loadBluePrint.en.getPrice());
							} else {
								sendServerMessage(new ServerMessage(Lng.astr("Blueprint is free!\n(infinite shop)"), ServerMessage.MESSAGE_TYPE_INFO, id));
							}
							loadBluePrint.spawnSectorId.set(currentSector);
							loadBluePrint.checkProspectedBlockCount = true;
							synchronized(((GameServerState) state).getBluePrintsToSpawn()) {
								((GameServerState) state).getBluePrintsToSpawn().add(loadBluePrint);
							}
//							loadBluePrint.spawn(loadBluePrint.spawnSectorId, loadBluePrint.checkProspectedBlockCount, new ChildStats(false));

							long timeSpawn = System.currentTimeMillis() - ti;

							lastBlueprintSpawn = System.currentTimeMillis();
							String msg = "[BLUEPRINT][BUY] " + name + " bought: \"" + loadBluePrint.en.getName() + "\" as \"" + loadBluePrint.realName + "\"; Price: " + loadBluePrint.en.getPrice() + "; to sector: " + currentSector + " (loadTime: " + timeLoad + "ms, spawnTime: " + timeSpawn + "ms)";

							System.err.println(msg);
							LogUtil.log().fine(msg);
						} else {
							sendServerMessagePlayerError(Lng.astr("Can only spawn ships\nfrom shop directly. Please\nuse the blueprint buy system!"));
						}
					}
				} else {

					sendServerMessage(new ServerMessage(Lng.astr("Cannot buy blueprint.\nSpam protection!\nWait %s seconds", ((blueprintDelay / 1000) - (System.currentTimeMillis() - lastBlueprintSpawn) / 1000)), ServerMessage.MESSAGE_TYPE_ERROR, id));
				}
			} catch(NotEnoughCreditsException ex) {

				sendServerMessage(new ServerMessage(Lng.astr("Cannot buy blueprint.\nNot enough credits!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
				ex.printStackTrace();
			} catch(PlayerControlledTransformableNotFound ex) {
				ex.printStackTrace();
			} catch(EntityNotFountException ex) {
				ex.printStackTrace();
			} catch(IOException ex) {
				ex.printStackTrace();
			} catch(EntityAlreadyExistsException ex) {
				if(onServer) {
					sendServerMessage(new ServerMessage(Lng.astr("Name already exists!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
				}
				ex.printStackTrace();
			}
		}
	}

	public void handleInventoryStateFromNT(NetworkPlayer p) {

		handleNormalShoppingFromNT(p);

		if(onServer) {

			if(!p.recipeSellRequests.getReceiveBuffer().isEmpty()) {
				IntOpenHashSet changed = new IntOpenHashSet();

				for(int slot : p.recipeSellRequests.getReceiveBuffer()) {

					int metaId = getInventory(null).getMeta(slot);

					MetaObject object = ((GameServerState) state).getMetaObjectManager().getObject(metaId);

					if(object != null) {
						boolean ok = false;
						if(((Recipe) object).fixedPrice >= 0) {
							int price = (int) (((Recipe) object).fixedPrice * ((Float) ServerConfig.RECIPE_REFUND_MULT.getCurrentState()).floatValue());
							modCreditsServer(price);
							ok = true;
						} else {
							int refund = (Integer) ServerConfig.RECIPE_BLOCK_COST.getCurrentState();

							refund = (int) (refund * ((Float) ServerConfig.RECIPE_REFUND_MULT.getCurrentState()).floatValue());

							short type = ((Recipe) object).recipeProduct[0].outputResource[0].type;

							int slotC;

							slotC = getInventory().incExistingOrNextFreeSlot(type, refund);
							changed.add(slotC);
							ok = true;
						}
						if(ok) {
							getInventory(null).put(slot, (short) 0, 0, -1);
							changed.add(slot);
						}

					}
				}
				sendInventoryModification(changed, Long.MIN_VALUE);
			}
			for(int index : p.fixedRecipeBuyRequests.getReceiveBuffer()) {
				if(index >= 0 && index < ElementKeyMap.fixedRecipes.recipes.size()) {
					FixedRecipe fixedRecipe = ElementKeyMap.fixedRecipes.recipes.get(index);
					if(fixedRecipe.canAfford(this)) {
						IntOpenHashSet mod = new IntOpenHashSet();
						boolean ok = false;
						if(fixedRecipe.costType == -1) {
							modCreditsServer(-fixedRecipe.costAmount);
							ok = true;
						} else {
							if(ElementKeyMap.exists(fixedRecipe.costType)) {

								ElementInformation info = ElementKeyMap.getInfo(fixedRecipe.costType);
								Inventory inv = getInventory(null);
								int overallQuantity = inv.getOverallQuantity(fixedRecipe.costType);

								if(overallQuantity >= fixedRecipe.costAmount) {
									inv.decreaseBatch(fixedRecipe.costType, fixedRecipe.costAmount, mod);
								}

								ok = true;
							} else {
								sendServerMessage(new ServerMessage(Lng.astr("SERVER: recipe cost type unknown %s\n%s", fixedRecipe.costType, fixedRecipe.name), ServerMessage.MESSAGE_TYPE_ERROR, id));
							}

						}
						if(ok) {
							Recipe recipe = fixedRecipe.getMetaItem();
							int slot;
							try {
								slot = getInventory(null).getFreeSlot();
								getInventory(null).put(slot, recipe);
								mod.add(slot);
							} catch(NoSlotFreeException e) {
								e.printStackTrace();
								sendServerMessage(new ServerMessage(Lng.astr("SERVER: no slot free for recipe!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
							}
						}
						if(mod.size() > 0) {
							getInventory(null).sendInventoryModification(mod);
						}
					} else {
						sendServerMessage(new ServerMessage(Lng.astr("SERVER: Cannot afford recipe \n%s\n%s", index, fixedRecipe.name), ServerMessage.MESSAGE_TYPE_ERROR, id));
					}
				} else {
					sendServerMessage(new ServerMessage(Lng.astr("SERVER: Invalid recipe request \n%s", index), ServerMessage.MESSAGE_TYPE_ERROR, id));
				}
			}
			for(int intType : p.recipeRequests.getReceiveBuffer()) {
				sendServerMessage(new ServerMessage(Lng.astr("Can no longer buy recipes...\nPlease use the crafting system!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
			}
			for(RemoteDragDrop rd : p.dropOrPickupSlots.getReceiveBuffer()) {

				DragDrop g = rd.get();
				dropsIntoSpace.enqueue(g);
			}

			for(RemoteBlueprintPlayerRequest s : p.catalogPlayerHandleBuffer.getReceiveBuffer()) {
				BlueprintPlayerHandleRequest bbR = s.get();
				String catalogName = bbR.catalogName;
				String entitySpawnName = bbR.entitySpawnName;
				int toSaveShip = bbR.toSaveShip;
				System.err.println("[SERVER][PLAYER] RECEIVED SAVE BUY: " + catalogName + " " + entitySpawnName);
				if(bbR.save) {

					//SAVE

					Sendable sendable = state.getLocalAndRemoteObjectContainer().getLocalObjects().get(toSaveShip);
					if(sendable != null && sendable instanceof Ship || sendable instanceof SpaceStation) {
						SegmentController segmentController = (SegmentController) sendable;
						if((Integer) ServerConfig.CATALOG_SLOTS_PER_PLAYER.getCurrentState() < 0 || catalog.getPersonalCatalog().size() < (Integer) ServerConfig.CATALOG_SLOTS_PER_PLAYER.getCurrentState()) {

							BluePrintWriteQueueElement e = new BluePrintWriteQueueElement(segmentController, entitySpawnName, bbR.classification, false);
							bluePrintWriteQueue.add(e);
						} else {
							sendServerMessage(new ServerMessage(Lng.astr("Cannot save blueprint:\nout of slots: %s/%s!", catalog.getPersonalCatalog().size(), ServerConfig.CATALOG_SLOTS_PER_PLAYER.getCurrentState()), ServerMessage.MESSAGE_TYPE_ERROR, id));
						}

					} else {
						System.err.println("[SERVER][SAVE][ERROR] COULD NOT FIND SHIP WITH ID " + toSaveShip);
					}
				} else {
					if(bbR.directBuy) {
						SimpleTransformableSendableObject o = getFirstControlledTransformableWOExc();
						if(o instanceof ShopperInterface) {

							ShopperInterface sh = (ShopperInterface) o;
							boolean ok = false;
							boolean inInfiniteShop = false;
							if(!sh.getShopsInDistance().isEmpty()) {
								ok = true;

								inInfiniteShop = sh.getShopsInDistance().iterator().next().isInfiniteSupply();

							} else if(networkPlayerObject.isAdminClient.get()) {
								ok = true;

								sendServerMessage(new ServerMessage(Lng.astr("Not near shop,\nOVERWRITTEN BY ADMINRIGHTS!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
							} else {
								sendServerMessage(new ServerMessage(Lng.astr("You are not\nnear a shop!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
							}
							if(ok) {
								Faction f = ((FactionState) state).getFactionManager().getFaction(getFactionId());

								if(((GameStateInterface) state).getGameState().allowedToSpawnBBShips(this, f)) {

									SegmentPiece spawnOnRail = bbR.getToSpawnOnRail(this, (GameServerState) state);

									int fid = bbR.setOwnFaction ? getFactionId() : 0;
									//LOAD
									BluePrintSpawnQueueElement e = new BluePrintSpawnQueueElement(catalogName, entitySpawnName, fid, inInfiniteShop, false, true, spawnOnRail);
									bluePrintSpawnQueue.add(e);
								}
							}
						}
					} else {
						giveMetaBlueprint = catalogName;

					}

				}
			}
		}

	}

	public void handleKilledFromNT(NetworkPlayer p) {
		for(int killerId : p.killedBuffer.getReceiveBuffer()) {

			synchronized(killerIds) {
				killerIds.enqueue(killerId);
			}

		}
	}

	public void suicideOnServer() {
		synchronized(killerIds) {
			killerIds.enqueue(id);
		}
	}

	public boolean canClientPressKey() {
		assert (!onServer);
		if(lastCanKeyPressCheck != state.getNumberOfUpdate()) {
			lastCanKeyPressCheckResult = !((GameClientState) state).getGlobalGameControlManager().getIngameControlManager().getChatControlManager().isActive() && ((GameClientState) state).getGlobalGameControlManager().getIngameControlManager().isActive() && !((GameClientState) state).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().isAnyMenuActive() && ((GameClientState) state).getPlayerInputs().isEmpty();
			lastCanKeyPressCheck = state.getNumberOfUpdate();
		}
		return lastCanKeyPressCheckResult;
	}

	public void handleLocalKeyboardInput() {
		if(isClientOwnPlayer() && ((GameClientController) state.getController()).isWindowActive()) {
			if(canClientPressKey()) {
				for(int i = 0; i < KeyboardMappings.remoteMappings.length; i++) {
					KeyboardMappings.remoteMappings[i].setNTKeyDown(networkPlayerObject.keyboardOfController, (ClientStateInterface) state);
				}
			}
		}
	}

	public void handleLocalMouseInput() {
		if(isClientOwnPlayer() && ClientController.hasGraphics(state) && Display.isActive()) {
			GameClientState s = (GameClientState) state;
			if(s.getGlobalGameControlManager() != null) {
				if(((GameClientController) state.getController()).isWindowActiveOutOfMenu()) {
					networkPlayerObject.setMouseDown(s.getController());
				}
			}

			if(Controller.getCamera() != null && Controller.getCamera().isStable()) {
				Transform t = new Transform();
				t.setIdentity();

				if(Controller.getCamera() instanceof InShipCamera && state.getUpdateTime() - ((InShipCamera) Controller.getCamera()).getHelperCamera().lastInitialRecoil < Math.max(140, ((GameClientState) state).getPing())) {
					//recoil aim prevention (should be fixed better in the future) FIXME
					//this basically delayes recoil to be send to the server
					Matrix3f camT = ((InShipCamera) Controller.getCamera()).getHelperCamera().lastRecoilState;
//					System.err.println("USING RECOIL STATE");
					t.set(camT);
//					GlUtil.setRightVector(getRight(transTmp.origin), t);
//					GlUtil.setForwardVector(getForward(transTmp.origin), t);
//					GlUtil.setUpVector(getUp(transTmp.origin), t);
				} else {
					GlUtil.setRightVector(getRight(transTmp.origin), t);
					GlUtil.setForwardVector(getForward(transTmp.origin), t);
					GlUtil.setUpVector(getUp(transTmp.origin), t);
				}

				if(!networkPlayerObject.camOrientation.equalsMatrix(t.basis)) {
//					System.err.println("SENDING FOR::: "+GlUtil.getForwardVector(new Vector3f(), t.basis));
					networkPlayerObject.camOrientation.set(t.basis, true);
				}
			}
		}
	}

	public void handleNormalShoppingFromNT(NetworkPlayer p) {
		if(p.deleteBuffer.getReceiveBuffer().size() > 0) {
			for(RemoteIntegerArray s : p.deleteBuffer.getReceiveBuffer()) {
				int quantity = s.get()[0].get();
				int elementId = s.get()[1].get();
				int slot = s.get()[2].get();
				short type = (short) elementId;
				ElementInformation info = ElementKeyMap.getInfo(type);
				getInventory().inc(slot, type, -quantity);
				getInventory().sendInventoryModification(slot);
			}
		}
		if(p.buyBuffer.getReceiveBuffer().size() > 0 || p.sellBuffer.getReceiveBuffer().size() > 0) {
			IntOpenHashSet slots = new IntOpenHashSet();
			Object2ObjectOpenHashMap<ShopInterface, IntOpenHashSet> slotsShop = new Object2ObjectOpenHashMap<ShopInterface, IntOpenHashSet>();
			ShopInterface closestShopsInDistance = getClosestShopsInDistance();

			if(closestShopsInDistance == null) {
				ServerMessage m = new ServerMessage(Lng.astr("Cannot buy!\nNo shop in range!"), ServerMessage.MESSAGE_TYPE_ERROR, id);
				System.err.println("[SERVER] " + this + " No Shops in distance: " + shopsInDistance);
				sendServerMessage(m);
				return;
			}

			if(!closestShopsInDistance.getShoppingAddOn().hasPermission(this)) {
				ServerMessage m = new ServerMessage(Lng.astr("Server:\nNo permission to use shop!"), ServerMessage.MESSAGE_TYPE_ERROR, id);
				sendServerMessage(m);
				return;
			}

			for(RemoteIntegerArray s : p.buyBuffer.getReceiveBuffer()) {
				int quantity = s.get()[0].get();
				int elementId = s.get()[1].get();
				short type = (short) elementId;
				ElementInformation info = ElementKeyMap.getInfo(type);

				if(quantity <= 0) {
					System.err.println("[SERVER] ERROR: invalid quantity Shopping Buy: " + quantity + " of " + info.getName() + " for " + this);
				}

				System.err.println("[SERVER] Executing Shopping Buy: " + quantity + " of " + info.getName() + " for " + this);

				int totalPrice = 0;

				if(closestShopsInDistance != null) {
					//				totalPrice = closestShopsInDistance.getPriceBasedOfQuantity(info).canAfford(this, quantity);
					IntOpenHashSet shopHash = slotsShop.get(closestShopsInDistance);
					if(shopHash == null) {
						shopHash = new IntOpenHashSet();
						slotsShop.put(closestShopsInDistance, shopHash);
					}
					try {
						closestShopsInDistance.getShoppingAddOn().buy(this, type, quantity, closestShopsInDistance, slots, shopHash);
					} catch(NoSlotFreeException e1) {
						e1.printStackTrace();
						ServerMessage m = new ServerMessage(Lng.astr("No space in inventory!"), ServerMessage.MESSAGE_TYPE_ERROR, id);
						sendServerMessage(m);
					}
				} else {
					System.err.println("Exception no shop in distance found " + this);
					continue;
				}

			}

			// System.err.println("UPDATE SELL PLAYER FROMNT "+p.sellBuffer.getReceiveBuffer().size());
			for(RemoteIntegerArray s : p.sellBuffer.getReceiveBuffer()) {
				int quantity = s.get()[0].get();
				int elementId = s.get()[1].get();

				ElementInformation info = ElementKeyMap.getInfo((short) elementId);
				if(!info.isShoppable()) {
					((GameServerState) state).getController().broadcastMessage(Lng.astr("WARNING\nPossible hacking:\nPlayer %s\ntried to sell forbidden item.", name), ServerMessage.MESSAGE_TYPE_ERROR);
				}
				if(quantity <= 0) {
					System.err.println("[SERVER] ERROR: invalid quantity Shopping Sell: " + quantity + " of " + info.getName() + " for " + this);
				}
				System.err.println("[SERVER] Executing Shopping Sell: " + quantity + " of " + info.getName() + " for " + this);

				int totalPrice = 0;
				if(closestShopsInDistance != null) {
					IntOpenHashSet shopHash = slotsShop.get(closestShopsInDistance);
					if(shopHash == null) {
						shopHash = new IntOpenHashSet();
						slotsShop.put(closestShopsInDistance, shopHash);
					}

					try {
						closestShopsInDistance.getShoppingAddOn().sell(this, (short) elementId, quantity, closestShopsInDistance, slots, shopHash);
					} catch(NoSlotFreeException e1) {
						e1.printStackTrace();
						ServerMessage m = new ServerMessage(Lng.astr("No space in inventory!"), ServerMessage.MESSAGE_TYPE_ERROR, id);
						sendServerMessage(m);
					}
				} else {
					System.err.println("Exception no shop in distance found " + this);
					continue;
				}

			}

			if(!slots.isEmpty()) {
				getInventory().sendInventoryModification(slots);
			}
			for(Map.Entry<ShopInterface, IntOpenHashSet> e : slotsShop.entrySet()) {
				//				System.err.println("SENDING UPDATE FOR "+e.getKey()+"; "+e.getValue());
				e.getKey().getShopInventory().sendInventoryModification(e.getValue());
			}
		}
	}

	private void handlePlayerInBlackHoleSystem() {

		if(sectorBlackHoleEffectStart < 0) {
			sectorBlackHoleEffectStart = System.currentTimeMillis();
			if(isClientOwnPlayer()) {
				((GameClientState) state).getController().popupAlertTextMessage(Lng.str("WARNING\nSpace time disruption\ndetected!"), 0);
			}
		} else {

		}
		long time = System.currentTimeMillis();
		if(isClientOwnPlayer() && time - sectorBlackHoleEffectStart > 1000) {
			((GameClientState) state).getController().popupAlertTextMessage(Lng.str("WARNING\nTime and space is warping!"), 0);
		}
//		if(isOnServer()){
//			if(time - sectorBlackHoleEffectStart > 20000){
//
//				sectorBlackHoleEffectStart = 0;
//				//commencing jump
//
//				try {
//					((GameServerState)getState()).getController().queueSectorSwitch(getFirstControlledTransformable(), new Vector3i(getCurrentSector().x+60, getCurrentSector().y+60, getCurrentSector().z+60), SectorSwitch.TRANS_JUMP, false);
//
//				} catch (PlayerControlledTransformableNotFound e) {
//					e.printStackTrace();
//				}
//			}
//		}

	}

	private void handleReceivedInputEvents() {
		Arrays.fill(wasKeyDown, false);

		for(int i = 0; i < keyboardEvents.size(); i++) {
			byte b = keyboardEvents.getByte(i);

			boolean pressed = b > 0;
			int ordinal = Math.abs(b) - 1;
			KeyboardMappings m = KeyboardMappings.values()[ordinal];

			for(int k = 0; k < KeyboardMappings.remoteMappings.length; k++) {

				if(KeyboardMappings.remoteMappings[k] == m) {
					wasKeyDown[k] = pressed;
					break;
				}
			}

			controllerState.handleKeyEvent(m);

		}

		keyboardEvents.clear();
		for(byte button : mouseEvents) {
			MouseEvent e = new MouseEvent();
			e.button = button;
			e.state = true;
			controllerState.handleMouseEvent(e);
		}
		mouseEvents.clear();
	}

	public void handleRoundEndFromNT(NetworkPlayer pn) {
		if(!onServer) {
			GameClientState state = (GameClientState) this.state;
			if(state.getPlayer() == this) {
				for(RemoteIntegerArray s : pn.roundEndBuffer.getReceiveBuffer()) {

					int winner = s.get(0).get().intValue();
					int loser = s.get(1).get().intValue();

					int lastKillId = s.get(2).get();
					if(lastKillId < 0) {
						System.out.println("[CLIENT][ROUNDEND] NOBODY HAD THE LAST KILL");
					} else {
						System.out.println("[CLIENT][ROUNDEND] ENTITY " + lastKillId + " HAD THE LAST KILL");
					}

					boolean contains = false;
					synchronized(state.getPlayerInputs()) {
						for(DialogInterface p : state.getPlayerInputs()) {
							if(p instanceof RoundEndMenu) {
								contains = true;
								break;
							}
						}
					}
					if(!contains) {
						RoundEndMenu gameMenu = new RoundEndMenu(state, winner, loser);
						state.getPlayerInputs().add(gameMenu);
					}
				}
			}
		}
	}

	/**
	 * @param health the health to set
	 * @param from
	 */
	public void handleServerHealthAndCheckAliveOnServer(float health, Damager from) {
		if(!onServer) {
			return;
		}

		if(isSpawnProtected()) {
			((GameStateInterface) state).getGameState().getSpawnProtectionSec();
			System.err.println("[SERVER] player " + this + " was hit but is invulnerable from previous death " + getSpawnProtectionTimeLeft() / 1000 + " / " + getSpawnProtectionTimeSecsMax() + " sec");
			if(System.currentTimeMillis() - lastSetProtectionMsg > 3000) {
				sendServerMessage(new ServerMessage(Lng.astr("You are not taking damage!\n(spawn protection %s / %s sec)", (getSpawnProtectionTimeLeft() / 1000), getSpawnProtectionTimeSecsMax()), ServerMessage.MESSAGE_TYPE_INFO, id));
			}
			return;
		}

		float oldHealth = this.health;
		this.health = Math.min(MAX_HEALTH, health);
		boolean suicide = from == this;
//		System.err.println("[SERVER] " + this + ": NEW HEALTH IS " + health + "; from: " + from);
		if(health <= 0 && oldHealth > 0 && alive) {
			String responsible = Lng.str("Responsible: <unknown>");
			if(from != null) {
				String uid = "n/a";
				if(from instanceof UniqueInterface) {
					uid = ((UniqueInterface) from).getUniqueIdentifier();
				}
				String fromName = from.getName() + "[Faction=" + from.getFactionId() + ", Owner=" + from.getOwnerState() + ", UID={" + uid + "}]";

				PlayerState r = null;
				if(from instanceof PlayerControllable) {
					if(!((PlayerControllable) from).getAttachedPlayers().isEmpty()) {
						r = ((PlayerControllable) from).getAttachedPlayers().get(0);
					} else {
						responsible = Lng.str("Responsible: %s", fromName);
					}

				} else if(from instanceof PlayerState) {
					r = (PlayerState) from;

				} else {
					responsible = Lng.str("Responsible: %s", fromName);
				}

				if(r != null) {
					responsible = Lng.str("Killer: %s (%s/%s HP left)", fromName, r.health, r.MAX_HEALTH);
				}

			}
			LogUtil.log().fine("[DEATH] " + name + " has been killed by '" + responsible + "'; controllable: " + from);
			if(!suicide) {
				sendServerMessage(new ServerMessage(Lng.astr("%s has died!\n\n%s", name, responsible), ServerMessage.MESSAGE_TYPE_WARNING));
			}
			dieOnServer(from);
		} else {

		}
	}

	public void handleSpawnRequestFromNT(NetworkPlayer p) {
		if(onServer) {

			if(!p.spawnRequest.getReceiveBuffer().isEmpty()) {
				GameServerState s = (GameServerState) state;
				synchronized(s.getSpawnRequests()) {
					s.getSpawnRequests().add(this);

				}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if(o != null && o instanceof PlayerState) {
			return id == ((PlayerState) o).id;
		}
		return false;
	}

	private void hurtClientAnimation(Sendable vessel) {
		if(isClientOwnPlayer()) {
			((GameClientState) state).getController().onHurt(this, vessel);
		}
	}

	public boolean isClientOwnPlayer() {
		if(onServer) {
			return false;
		}
		return (((GameClientState) state).getPlayer() != null && ((GameClientState) state).getPlayer().id == id);
	}

//	/**
//	 * @param spawnPoint
//	 *            the spawnPoint to set
//	 */
//	public void setSpawnPoint(Vector3f spawnPoint) {
//		this.spawnData.deathSpawn.abspolutePos.set(spawnPoint);
//		//		System.err.println("["+(isOnServer()? "SERVER": "CLIENT")+"] SET PAWNING POINT OF "+this+" TO: "+this.getSpawnPoint());
//	}

	// #RM1946 add new method isDamageable(Damager)
	public boolean isDamageable(Damager from) {
		Sector sector = ((GameServerState) state).getUniverse().getSector(currentSectorId);
		if(sector != null && sector.isProtected()) {
			List<PlayerState> attachedPlayers = ((PlayerControllable) from).getAttachedPlayers();
			for(int i = 0; i < attachedPlayers.size(); i++) {
				PlayerState ps = attachedPlayers.get(i);
				if(System.currentTimeMillis() - ps.lastSectorProtectedMsgSent > 5000) {
					ps.lastSectorProtectedMsgSent = System.currentTimeMillis();
					ps.sendServerMessage(new ServerMessage(Lng.astr("This Sector is Protected!"), ServerMessage.MESSAGE_TYPE_WARNING, ps.id));
				}
			}
			return false;
		}
		return true;
	}

	public boolean isEnemy(PlayerState otherState) {
		return factionController.isEnemy(otherState);
	}

	public boolean isFriend(PlayerState otherState) {
		return factionController.isFriend(otherState);
	}

	/**
	 * @return the godMode
	 */
	public boolean isGodMode() {
		return godMode;
	}

	/**
	 * @param godMode the godMode to set
	 */
	public void setGodMode(boolean godMode) {
		this.godMode = godMode;
	}

	public boolean isKeyDownOrSticky(KeyboardMappings key) {
		if(onServer || !isClientOwnPlayer()) {

			boolean controllerKeyDown = key.isNTKeyDown(networkPlayerObject.keyboardOfController.get());

			boolean wKeyDown = wasKeyDown(key);

			boolean pressed = controllerKeyDown || wKeyDown;

			return pressed;
		} else {
			if(((GameClientController) state.getController()).isWindowActive()) {
				if(canClientPressKey()) {
					return key.isDownOrSticky(state);
				} else {
					return false;
				}

			}
		}
		return false;
	}

	public void handleJoystickDir(Vector3f dir, Vector3f forward, Vector3f right, Vector3f up) {
		if(!onServer) {
			if(JoystickMappingFile.ok()) {
				{
					float joystickAxis = (float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.FORWARD_BACK);
					Vector3f jDir = new Vector3f(forward);
					jDir.scale(joystickAxis);
					dir.add(jDir);
				}
				{
					float joystickAxis = (float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.RIGHT_LEFT);
					Vector3f jDir = new Vector3f(right);
					jDir.scale(joystickAxis);
					dir.add(jDir);
				}
				{
					float joystickAxis = (float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.UP_DOWN);
					Vector3f jDir = new Vector3f(up);
					jDir.scale(joystickAxis);
					dir.add(jDir);
				}
			}
		} else {
			{
				float joystickAxis = networkPlayerObject.frontBackAxis.getFloat();
				Vector3f jForw = new Vector3f(forward);
				jForw.scale(joystickAxis);
				dir.add(jForw);
			}
			{
				float joystickAxis = networkPlayerObject.rightLeftAxis.getFloat();
				Vector3f jForw = new Vector3f(right);
				jForw.scale(joystickAxis);
				dir.add(jForw);
			}
			{
				float joystickAxis = networkPlayerObject.upDownAxis.getFloat();
				Vector3f jForw = new Vector3f(up);
				jForw.scale(joystickAxis);
				dir.add(jForw);
			}
		}

	}

	public void updateNTJoystick() {
		if(isClientOwnPlayer()) {
			networkPlayerObject.frontBackAxis.forceClientUpdates();
			networkPlayerObject.frontBackAxis.set((float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.FORWARD_BACK));

			networkPlayerObject.upDownAxis.forceClientUpdates();
			networkPlayerObject.upDownAxis.set((float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.UP_DOWN));

			networkPlayerObject.rightLeftAxis.forceClientUpdates();
			networkPlayerObject.rightLeftAxis.set((float) ((GameClientState) state).getController().getJoystickAxis(JoystickAxisMapping.RIGHT_LEFT));
		}
	}

	public boolean wasMouseButtonDown(int i) {
		return (i >= 0 && i < wasMouseDown.length && wasMouseDown[i]);
	}

	public boolean wasMouseEventServer(int i) {
		return (i >= 0 && i < wasMouseEvent.length && wasMouseEvent[i]);
	}

	public boolean isMouseButtonDown(int i) {
		if(onServer || !isClientOwnPlayer()) {
			boolean mouse = networkPlayerObject.isMouseDown(i);
			return mouse || wasMouseEventServer(i);
		} else {
			if(((GameClientController) state.getController()).isWindowActiveOutOfMenu()) {
				return ((GameClientController) state.getController()).isMouseButtonDown(i);
			}
		}
		return false;
	}

	public boolean isMouseSwitched() {
		if(onServer || !isClientOwnPlayer()) {
			return networkPlayerObject.mouseSwitched.get();
		} else {
			if(((GameClientController) state.getController()).isWindowActiveOutOfMenu()) {
				//				System.err.println("OUT OF MENU "+outOfMenu());
				return EngineSettings.C_MOUSE_BUTTON_SWITCH.isOn();

			}
		}
		return false;
	}

	public boolean isNeutral(PlayerState otherState) {
		return factionController.isNeutral(otherState);
	}

	@Override
	public boolean isVolatile() {
		return false;
	}

	public void modCreditsServer(long amount) {
		synchronized(creditModifications) {
			creditModifications.add(amount);
		}
	}

	public void notifyRoundEnded(int winner, int loser, Damager lastKill) {
		System.err.println("SERVER NOTIFYING OF ROUND END");
		RemoteIntegerArray a = new RemoteIntegerArray(3, networkPlayerObject);
		a.set(0, winner);
		a.set(1, loser);
		if(lastKill != null) {
			a.set(2, -1);
		} else {
			a.set(2, -1);
		}
		networkPlayerObject.roundEndBuffer.add(a);

	}

	public void onDestroyedElement(SegmentPiece segmentPiece) {
		controllerState.onDestroyedElement(segmentPiece);

	}

	public void onVesselHit(Sendable vessel) {
		hurtClientAnimation(vessel);
	}

	public void printControllerState() {
		int i = 0;
		for(ControllerStateUnit cu : controllerState.getUnits()) {
			AbstractScene.infoList.add("+ " + i + " CONTR: " + name + "): " + cu);
			i++;
		}
	}

	public void queueInventoryModification(IntOpenHashSet slots, long parameter) {
		InventoryMultMod m = new InventoryMultMod(slots, getInventory(), parameter);
		synchronized(queuedModifactions) {
			queuedModifactions.enqueue(m);
		}
	}

	public void searchForLastEnteredEntity() {
		assert (onServer);
		if(lastEnteredEntity == null || lastEnteredEntity.isEmpty() || !lastEnteredEntity.startsWith("ENTITY_")) {
			sendServerMessage(new ServerMessage(Lng.astr("Invalid last entered:\n", lastEnteredEntity == null ? "Not set" : lastEnteredEntity), ServerMessage.MESSAGE_TYPE_ERROR, id));
			return;
		}

		GameServerState state = (GameServerState) this.state;

		try {
			int type = DatabaseEntry.getType(lastEnteredEntity);

			String entity = DatabaseEntry.removePrefix(lastEnteredEntity);
			if(lastEnteredEntity != null && !"none".equals(lastEnteredEntity)) {
				try {
					List<DatabaseEntry> byUID = state.getDatabaseIndex().getTableManager().getEntityTable().getByUIDExact(DatabaseIndex.escape(entity), 20);

					if(byUID.isEmpty()) {
						sendServerMessage(new ServerMessage(Lng.astr("Ship has not been found! (original name: %s)", entity), ServerMessage.MESSAGE_TYPE_ERROR, id));
					} else {
						for(DatabaseEntry a : byUID) {
							sendServerMessage(new ServerMessage(Lng.astr("Found ship in sector:\n(orig: %s)\n-> %s", entity, a.sectorPos), ServerMessage.MESSAGE_TYPE_INFO, id));
						}
					}
				} catch(SQLException e) {
					e.printStackTrace();
				}
			} else {
				sendServerMessage(new ServerMessage(Lng.astr("Last entered ship\ncan't be retraced!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
			}
		} catch(DatabaseEntry.EntityTypeNotFoundException e1) {
			e1.printStackTrace();
			sendServerMessage(new ServerMessage(Lng.astr("Last entered ship\ncan't be retraced!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
		}
	}

	private void sendKill(int killerId) {
		networkPlayerObject.killedBuffer.add(killerId);
	}

	public void sendServerMessage(ServerMessage m) {
		serverToSendMessages.add(m);
	}

	public void sendServerMessagePlayerError(Object[] msg) {
		serverToSendMessages.add(new ServerMessage(msg, ServerMessage.MESSAGE_TYPE_ERROR, id));
	}

	public void sendServerMessagePlayerInfo(Object[] msg) {
		serverToSendMessages.add(new ServerMessage(msg, ServerMessage.MESSAGE_TYPE_INFO, id));
	}

	public void sendServerMessagePlayerSimple(Object[] msg) {
		serverToSendMessages.add(new ServerMessage(msg, ServerMessage.MESSAGE_TYPE_SIMPLE, id));
	}

	public void sendServerMessagePlayerWarning(Object[] msg) {
		serverToSendMessages.add(new ServerMessage(msg, ServerMessage.MESSAGE_TYPE_WARNING, id));
	}

	public void sendSimpleCommand(SimplePlayerCommands command, Object... args) {
		networkPlayerObject.simpleCommandQueue.add(new RemoteSimpelCommand(new SimplePlayerCommand(command, args), networkPlayerObject));
	}

	public void setAssignedPlayerCharacter(PlayerCharacter c) {
		playerCharacter = c;
	}

	public void setLastEnteredShip(UniqueInterface controllable) {
		lastEnteredEntity = controllable.getUniqueIdentifier();
	}

	/**
	 * @param networkPlayerObject the networkPlayerObject to set
	 */
	public void setNetworkPlayerObject(NetworkPlayer networkPlayerObject) {
		this.networkPlayerObject = networkPlayerObject;
	}

	//	/**
	//	 * @param teamId
	//	 *            the teamId to set
	//	 */
	//	public void setTeamId(int teamId, boolean send) {
	//		boolean changed = teamId != this.teamId;
	//		this.teamId = teamId;
	//		if (changed) {
	//			// System.err.println("setting team");
	//			if(send){
	//				System.err.println("SENDING NEW TEAM "+((Teamable)getState()).getTeamOf(this).getClass().getSimpleName()+
	//						" OF "+this+" TO CLIENTS");
	//				getNetworkObject().requestTeamChange.add(new RemoteInteger(teamId,
	//						getNetworkObject()));
	//			}
	//
	//			((Teamable) getState()).notifyOfChangedTeams(this);
	//		}
	//	}
	public void suicideOnClient() {
		((GameClientState) state).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getInShipControlManager().exitShip(true);
		((GameClientState) state).getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getInShipControlManager().setActive(false);

		sendKill(id);
	}

	public void setLastOrientation() {
		getForward(lastForward);
		getUp(lastUp);
		getRight(lastRight);
	}

	public void setLastOrientation(Transform t) {

		GlUtil.getForwardVector(lastForward, t);
		GlUtil.getUpVector(lastUp, t);
		GlUtil.getRightVector(lastRight, t);

	}

	private void handleMetaBlueprintGive() {
		if(giveMetaBlueprint != null) {
			SimpleTransformableSendableObject o = getFirstControlledTransformableWOExc();
			if(o != null && o instanceof ShopperInterface) {

				ShopperInterface sh = (ShopperInterface) o;
				boolean ok = false;
				boolean inInfiniteShop = false;
				if(!sh.getShopsInDistance().isEmpty()) {
					ok = true;

					inInfiniteShop = sh.getShopsInDistance().iterator().next().isInfiniteSupply();

				} else if(networkPlayerObject.isAdminClient.get()) {
					ok = true;

					sendServerMessage(new ServerMessage(Lng.astr("Not near shop,\nOVERWRITTEN BY ADMINRIGHTS!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
				} else {
					sendServerMessage(new ServerMessage(Lng.astr("You are not\nnear a shop!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
				}

				//INSERTED CODE
				PlayerRequestMetaBlueprintEvent event = new PlayerRequestMetaBlueprintEvent(this, ok, inInfiniteShop, giveMetaBlueprint);
				StarLoader.fireEvent(event, true);
				ok = event.isAllowed();
				inInfiniteShop = event.isShopInfiniteSupply();
				giveMetaBlueprint = event.getBlueprintName();
				///

				if(ok) {
					Faction f = ((FactionState) state).getFactionManager().getFaction(getFactionId());

					if(((GameStateInterface) state).getGameState().allowedToSpawnBBShips(this, f)) {
						try {
							int slot = getInventory().getFreeSlot();
							BlueprintEntry bb = BluePrintController.active.getBlueprint(giveMetaBlueprint);
							if(bb.getType() != BlueprintType.SPACE_STATION || credits >= ((GameStateInterface) state).getGameState().getStationCost() || inInfiniteShop) {

								if(bb.getType() == BlueprintType.SPACE_STATION && !inInfiniteShop) {
									modCreditsServer(-((GameStateInterface) state).getGameState().getStationCost());
								}

								BlueprintMetaItem m = (BlueprintMetaItem) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.BLUEPRINT, (short) -1, true);
								m.blueprintName = giveMetaBlueprint;
								m.goal = new ElementCountMap(bb.getElementCountMapWithChilds());
								m.progress = new ElementCountMap();
//								if(inInfiniteShop){
//									m.progress = new ElementCountMap(bb.getElementCountMapWithChilds());
//								}else{
//									m.progress = new ElementCountMap();
//								}
								getInventory().put(slot, m);
								sendInventoryModification(slot, Long.MIN_VALUE);
							} else {
								sendServerMessagePlayerError(Lng.astr("You don't have enough\ncredits to buy a\nstation blueprint!"));
							}

						} catch(EntityNotFountException e) {
							e.printStackTrace();
							sendServerMessage(new ServerMessage(Lng.astr("Blueprint not found!\n%s", giveMetaBlueprint), ServerMessage.MESSAGE_TYPE_ERROR, id));
						} catch(NoSlotFreeException e) {
							e.printStackTrace();
							sendServerMessage(new ServerMessage(Lng.astr("Inventory is full!"), ServerMessage.MESSAGE_TYPE_ERROR, id));
						}
					} else {
						sendServerMessagePlayerError(Lng.astr("You are currently\nnot allowed to buy\nblueprints!"));
					}
				}
			}

			giveMetaBlueprint = null;
		}
	}

	private void onClientChannelCreatedOnServer() {
		for(ScanData d : scanHistory) {
			clientChannel.getNetworkObject().scanDataUpdates.add(new RemoteScanData(d, onServer));
		}
		for(SavedCoordinate d : savedCoordinates) {
			clientChannel.getNetworkObject().savedCoordinates.add(new RemoteSavedCoordinate(d, onServer));
		}

		((GameServerState) state).getUniverse().getGalaxyManager().sendAllFtlDataTo(clientChannel.getNetworkObject());

		((GameServerState) state).getUniverse().getGalaxyManager().sendAllTradeStubsTo(clientChannel.getNetworkObject());

		((GameServerState) state).getChannelRouter().onLogin(clientChannel);

	}

	private void handleReceivedSavedCoordinates() {
		if(!savedCoordinatesToAdd.isEmpty()) {
			synchronized(savedCoordinatesToAdd) {
				while(!savedCoordinatesToAdd.isEmpty()) {
					SavedCoordinate dequeue = savedCoordinatesToAdd.dequeue();
					if(onServer) {
						int max = (Integer) ServerConfig.MAX_COORDINATE_BOOKMARKS.getCurrentState();
						if(max == 0) {
							sendServerMessagePlayerError(Lng.astr("Server doesn't allow bookmarks!"));
						} else {
							// #RM1788 changed <= to < and allow deletion regardless of the total number of saved waypoints
							if(savedCoordinates.size() < max || dequeue.isRemoveFlag()) {
								if(dequeue.isRemoveFlag()) {
									savedCoordinates.remove(dequeue);
									clientChannel.getNetworkObject().savedCoordinates.add(new RemoteSavedCoordinate(dequeue, onServer));
								} else {
									if(!savedCoordinates.contains(dequeue)) {
										savedCoordinates.add(dequeue);
										clientChannel.getNetworkObject().savedCoordinates.add(new RemoteSavedCoordinate(dequeue, onServer));
									}
								}
							} else {
								sendServerMessagePlayerError(Lng.astr("Cannot save more than\n%s coordinates.\nPlease remove some first.", max));
							}
						}
					} else {

						if(dequeue.isRemoveFlag()) {
							savedCoordinates.remove(dequeue);
						} else {
							savedCoordinates.add(dequeue);
						}
						if(savedCoordinatesList != null) {
							savedCoordinatesList.flagDirty();
						}
					}
				}
			}
		}
	}

	public void updateProximitySectors() throws IOException {
		if(onServer) {
			proximitySector.updateServer();
			proximitySystem.updateServer();

		}
	}

	private boolean wasKeyDown(KeyboardMappings key) {

		for(int i = 0; i < wasKeyDown.length; i++) {
			if(key == KeyboardMappings.remoteMappings[i] && wasKeyDown[i]) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the starmadeName
	 */
	public String getStarmadeName() {
		return starmadeName;
	}

	/**
	 * @param starmadeName the starmadeName to set
	 */
	public void setStarmadeName(String starmadeName) {
		this.starmadeName = starmadeName;
	}

	/**
	 * @return the upgradedAccount
	 */
	public boolean isUpgradedAccount() {
		return upgradedAccount;
	}

	/**
	 * @param upgradedAccount the upgradedAccount to set
	 */
	public void setUpgradedAccount(boolean upgradedAccount) {
		this.upgradedAccount = upgradedAccount;
	}

	/**
	 * @return the helmetSlot
	 */
	public int getHelmetSlot() {
		return helmetSlot;
	}

	/**
	 * @param helmetSlot the helmetSlot to set
	 */
	public void setHelmetSlot(int helmetSlot) {
		this.helmetSlot = helmetSlot;
	}

	public PlayerAiManager getPlayerAiManager() {
		return playerAiManager;
	}

	/**
	 * @return the clientChannel
	 */
	public ClientChannel getClientChannel() {
		return clientChannel;
	}

	/**
	 * @param clientChannel the clientChannel to set
	 */
	public void setClientChannel(ClientChannel clientChannel) {
		this.clientChannel = clientChannel;
		if(clientChannel != null) {
			createdClientChannelOnServer = true;
		}
	}

	public Vector3i getUniqueSpawningSector(int uniquePlayerId) {
		return new Vector3i(Integer.MAX_VALUE - 32, uniquePlayerId * VoidSystem.SYSTEM_SIZE, Integer.MAX_VALUE - 32);
	}

	public Vector3i getUniqueTestingSector(int uniquePlayerId) {
		return new Vector3i(Integer.MAX_VALUE - 32, uniquePlayerId * VoidSystem.SYSTEM_SIZE + VoidSystem.SYSTEM_SIZE_HALF, Integer.MAX_VALUE - 32);
	}

	public boolean isSpectator() {
		Faction f = ((FactionState) state).getFactionManager().getFaction(getFactionId());
		return f != null && f.isFactionMode(Faction.MODE_SPECTATORS);
	}

	public boolean isNewPlayerServer() {
		assert (onServer);
		return newPlayerOnServer;
	}

	public boolean canRotate() {
		if(isClientOwnPlayer() && Controller.getCamera() != null) {
			return Controller.getCamera().isStable();
		}
		return networkPlayerObject.canRotate.get();
	}

	/**
	 * @return the clientHitNotifaction
	 */
	public byte getClientHitNotifaction() {
		return clientHitNotifaction;
	}

	/**
	 * @param clientHitNotifaction the clientHitNotifaction to set
	 */
	public void setClientHitNotifaction(byte clientHitNotifaction) {
		this.clientHitNotifaction = clientHitNotifaction;
	}

	/**
	 * @return the thisLogin
	 */
	public long getThisLogin() {
		return spawnData.thisLogin;
	}

	public long getLastDeathNotSuicideFactionProt() {
		return lastDeathNotSuicide;
	}

	/**
	 * @return the currentSystem
	 */
	public Vector3i getCurrentSystem() {
		return currentSystem;
	}

	/**
	 * @return the scanHistory
	 */
	public List<ScanData> getScanHistory() {
		return scanHistory;
	}

	public void addScanHistory(ScanData data) {
		scanHistory.add(data);
		while(scanHistory.size() > 5) {
			scanHistory.remove(0);
		}
	}

	/**
	 * @return the factionPointProtected
	 */
	public boolean isFactionPointProtected() {
		return factionPointProtected;
	}

	/**
	 * @param factionPointProtected the factionPointProtected to set
	 */
	public void setFactionPointProtected(boolean factionPointProtected) {
		this.factionPointProtected = factionPointProtected;
	}

	public void warpToTutorialSectorClient() {
		sendSimpleCommand(SimplePlayerCommands.WARP_TO_TUTORIAL_SECTOR);
	}

	@Override
	public byte getFactionRights() {
		return factionController.getFactionRank();
	}

	/**
	 * @return the savedCoordinates
	 */
	public ObjectArrayList<SavedCoordinate> getSavedCoordinates() {
		return savedCoordinates;
	}

	/**
	 * @return the savedCoordinatesToAdd
	 */
	public ObjectArrayFIFOQueue<SavedCoordinate> getSavedCoordinatesToAdd() {
		return savedCoordinatesToAdd;
	}

	public String getLastDiedMessage() {
		return lastDiedMessage;
	}

	public String getFactionName() {
		return factionController.getFactionName();
	}

	/**
	 * @return the playerChannelManager
	 */
	public PlayerChannelManager getPlayerChannelManager() {
		return playerChannelManager;
	}

	public boolean hasIgnored() {
		return ignored.size() > 0;
	}

	public String[] getIgnored() {
		return ignored.toArray(new String[ignored.size()]);
	}

	public boolean isIgnored(String playerName) {
		return ignored.contains(playerName.toLowerCase(Locale.ENGLISH));
	}

	public boolean isIgnored(PlayerState playerState) {
		return isIgnored(playerState.name);
	}

	public void addIgnore(String toIgnore) {
		if(!isIgnored(toIgnore.toLowerCase(Locale.ENGLISH))) {
			ignored.add(toIgnore.toLowerCase(Locale.ENGLISH));
		}
	}

	public void removeIgnore(String toRemove) {
		if(isIgnored(toRemove.toLowerCase(Locale.ENGLISH))) {
			ignored.remove(toRemove.toLowerCase(Locale.ENGLISH));
		}
	}

	public boolean isHasCreativeMode() {
		return hasCreativeMode;
	}

	public void setHasCreativeMode(boolean hasCreativeMode) {
		this.hasCreativeMode = hasCreativeMode;
	}

	public boolean isUseCreativeMode() {
		return useCreativeMode;
	}

	public void setUseCreativeMode(boolean useCreativeMode) {
		this.useCreativeMode = useCreativeMode;
	}

	public void setPersonalSectors() throws IOException {
		if(personalSector == null || testSector == null) {
			int uniquePlayerId = 1;
			File ddd = new FileExt(GameServerState.DATABASE_PATH);
			ddd.mkdirs();
			File uniquePlayerIds = new FileExt(GameServerState.DATABASE_PATH + "uniquePlayerCount");
			if(!uniquePlayerIds.exists()) {
				File oldUniquePlayerIds = new FileExt("./.ipid");
				if(oldUniquePlayerIds.exists()) {
					FileUtil.copyFile(oldUniquePlayerIds, uniquePlayerIds);
					oldUniquePlayerIds.deleteOnExit();
				} else {
					uniquePlayerIds.createNewFile();
				}
			} else {
				DataInputStream in = new DataInputStream(new FileInputStream(uniquePlayerIds));
				uniquePlayerId = in.readInt();
				uniquePlayerId++;
				in.close();
			}

			DataOutputStream d = new DataOutputStream(new FileOutputStream(uniquePlayerIds));
			d.writeInt(uniquePlayerId);
			d.close();
			assert (uniquePlayerId > 0);
			personalSector = getUniqueSpawningSector(uniquePlayerId);
			testSector = getUniqueSpawningSector(uniquePlayerId);
		}
	}

	public ObjectArrayFIFOQueue<CreateDockRequest> getCreateDockRequests() {
		return createDockRequests;
	}

	@Override
	public long getCurrentLag() {
		return currentLag;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public boolean isUseCargoInventory() {
		return useCargoInventory;
	}

	public void setUseCargoInventory(boolean b) {
		useCargoInventory = b;
	}

	public void requestCargoInventoryChange(SegmentPiece p) {
		if(p != null) {
			networkPlayerObject.cargoInventoryChange.add(new RemoteSegmentControllerBlock(new VoidUniqueSegmentPiece(p), networkPlayerObject));
		} else {
			VoidUniqueSegmentPiece v = new VoidUniqueSegmentPiece();
			v.uniqueIdentifierSegmentController = "NONE";
			networkPlayerObject.cargoInventoryChange.add(new RemoteSegmentControllerBlock(v, networkPlayerObject));
		}
	}

	public void requestUseCargoInventory(boolean b) {
		networkPlayerObject.requestCargoMode.add(b ? 1 : 0);
	}

	private boolean hasCargoBlockMigratedChunk16;
	private long dbId = -1;
	private final Vector3i tmpSystem = new Vector3i();
	private final FogOfWarController fow = new FogOfWarController(this);
	private final LinkedHashSet<Vector3i> lastVisitedSectors = new LinkedHashSet<Vector3i>();
	public final long[] offlinePermssion = new long[2];
	public int tempSeed;
	private boolean infiniteInventoryVolume;
	private int mineAutoArmSecs = -1;
	private long lastSpawnedThisSession;
	private Sector forcedEnterSector;
	private String forcedEnterUID;
	private final Cockpit cockpit = new Cockpit(this);
	private final PlayerRuleEntityManager ruleEntityManager;

	private Inventory getCargoInventoryIfActive() {
		if(useCargoInventory) {

			if(isInTutorial()) {
				if(onServer) {
					sendServerMessagePlayerError(Lng.astr("Cargo disabled in Tutorial!"));
					useCargoInventory = false;
				}
			} else if(cargoInventoryBlock != null) {
				Sendable sendable = state.getLocalAndRemoteObjectContainer().getUidObjectMap().get(cargoInventoryBlock.uniqueIdentifierSegmentController);

				if(sendable != null && sendable instanceof SegmentController) {
					cargoInventoryBlock.setSegmentController((SegmentController) sendable);
					if(!hasCargoBlockMigratedChunk16 && cargoInventoryBlock.getSegmentController().isLoadedFromChunk16()) {
						cargoInventoryBlock.voidPos.add(Chunk16SegmentData.SHIFT);
					}
					hasCargoBlockMigratedChunk16 = true;
					if(cargoInventoryBlock.getSegmentController().getSectorId() == currentSectorId) {
						Inventory inventory;
						if(cargoInventoryBlock.getSegmentController() instanceof ManagedSegmentController<?> && (inventory = ((ManagedSegmentController<?>) cargoInventoryBlock.getSegmentController()).getManagerContainer().getInventory(cargoInventoryBlock.getAbsoluteIndex())) != null) {

							return inventory;
						} else {
							if(onServer) {
								sendServerMessagePlayerError(Lng.astr("Selected personal cargo doesn't point to any storage block!"));
								useCargoInventory = false;
							}
						}
					} else {
						if(onServer) {
							sendServerMessagePlayerError(Lng.astr("Selected personal cargo out of range!\nIt must be in the same sector as you."));
							useCargoInventory = false;
						}
					}
				} else {
					if(onServer) {
						sendServerMessagePlayerError(Lng.astr("Selected personal cargo out of range!"));
						useCargoInventory = false;
					}
				}
			} else {
				if(onServer) {
					sendServerMessagePlayerError(Lng.astr("No cargo selected!\nOpen any storage block and\nselect it as personal cargo."));
					useCargoInventory = false;
				}
			}
		}
		return null;
	}

	public boolean isInventoryPersonalCargo(StashInventory inventory) {
		if(cargoInventoryBlock != null && inventory.getInventoryHolder() instanceof ManagerContainer<?>) {
			ManagerContainer<?> c = (ManagerContainer<?>) inventory.getInventoryHolder();
			return c.getSegmentController().getUniqueIdentifier().equals(cargoInventoryBlock.uniqueIdentifierSegmentController) && c.getInventory(cargoInventoryBlock.getAbsoluteIndex()) == inventory;
		}
		return false;
	}

	public void handleReceivedBlockMsg(ManagerContainer<?> c, ServerMessage s) {
		assert (!onServer);
		GameClientState state = (GameClientState) this.state;
		InventoryControllerManager inventoryControlManager = state.getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getInventoryControlManager();
		Inventory secondInventory = inventoryControlManager.getSecondInventory();

		Transform t = new Transform();
		t.setIdentity();
		Vector3i p = ElementCollection.getPosFromIndex(s.block, new Vector3i());
		t.origin.set(p.x - SegmentData.SEG_HALF, p.y - SegmentData.SEG_HALF, p.z - SegmentData.SEG_HALF);
		c.getSegmentController().getWorldTransform().transform(t.origin);
		RaisingIndication raisingIndication = new RaisingIndication(t, StringTools.getFormatedMessage(s.getMessage()), 1.0f, 0.3f, 0.3f, 1.0f);
		raisingIndication.speed = 0.1f;
		raisingIndication.lifetime = 4.6f;
		HudIndicatorOverlay.toDrawTexts.add(raisingIndication);

		if(secondInventory != null && inventoryControlManager.isTreeActive() && secondInventory.getInventoryHolder() == c && c.getInventory(s.block) == secondInventory) {
			s.type = ServerMessage.MESSAGE_TYPE_ERROR;
			((GameClientState) this.state).getServerMessages().add(s);
		}
	}

	public String toDetailedString() {
		return name + ", credits: " + credits + "; Sector: " + currentSector + "; Invetory slots filled: " + inventory.getCountFilledSlots() + "; last login: " + getLastLogin() + "; last logout: " + getLastLogout() + "; Spawn data: " + spawnData;
	}

	public boolean isVisibleSectorServer(Vector3i sectorPos) {
		return getFogOfWar().isVisibleSectorServer(sectorPos);
	}

	@Override
	public long getFogOfWarId() {
		return dbId;
	}

	public void readDatabase() throws SQLException {
		assert (onServer);

		dbId = ((GameServerState) state).getDatabaseIndex().getTableManager().getPlayerTable().getPlayerId(this);
	}

	public void updateDatabase() throws SQLException {
		assert (onServer);
		((GameServerState) state).getDatabaseIndex().getTableManager().getPlayerTable().getPlayerFactionAndPermission(this, offlinePermssion);
		((GameServerState) state).getDatabaseIndex().getTableManager().getPlayerTable().updateOrInsertPlayer(this);
	}

	public long getDbId() {
		return dbId;
	}

	public void setDbId(long dbId) {
		this.dbId = dbId;
	}

	@Override
	public FogOfWarController getFogOfWar() {
		FogOfWarController ffog = factionController.getFactionFow();
		if(ffog != null) {
			return ffog;
		}
		return fow;
	}

	public void onChangedFactionServer(FactionChange factionChange) {
		Faction from = ((GameServerState) state).getFactionManager().getFaction(factionChange.from);
		Faction to = ((GameServerState) state).getFactionManager().getFaction(factionChange.to);

		if(from != null) {
			if((factionChange.previousPermission & FactionPermission.PermType.FOG_OF_WAR_SHARE.value) == FactionPermission.PermType.FOG_OF_WAR_SHARE.value) {
				//put all info of old faction into player if that player had fow share
				fow.merge(from);
			}
		}
		if(to != null) {
			//put all info of player into new faction
			to.getFogOfWar().merge(this);
		}

		if((from == null && factionChange.from != 0) || (from != null && from.getMembersUID().isEmpty())) {

			try {
				((GameServerState) state).getDatabaseIndex().getTableManager().getVisibilityTable().clearVisibility(factionChange.from);
			} catch(SQLException e) {
				e.printStackTrace();
			}
//			assert(false):factionChange.from;
		}
	}

	public long getFactionPermission() {
		return factionController.getFactionPermission();
	}

	public Set<Vector3i> getLastVisitedSectors() {
		return lastVisitedSectors;
	}

	@Override
	public void sendFowResetToClient(Vector3i sysTo) {
		assert (onServer);
		networkPlayerObject.resetFowBuffer.add(new RemoteVector3i(sysTo, true));
	}

	/**
	 * this is an incrementing seed based on mouse inputs.
	 * This is consistent on both client and server
	 *
	 * @return seed
	 */
	public int getInputBasedSeed() {
		return inputBasedSeed;
	}

	@Override
	public boolean isInfiniteInventoryVolume() {
		return infiniteInventoryVolume;
	}

	public void setInfiniteInventoryVolume(boolean b) {
		infiniteInventoryVolume = b;
	}

	@Override
	public int getSectorId() {
		return currentSectorId;
	}

	public int getMineAutoArmSeconds() {
		return mineAutoArmSecs;
	}

	public void requestMineArmTimerChange(int secs) {
		networkPlayerObject.mineArmTimerRequests.add(secs);
	}

	public void lastSpawnedThisSession(long time) {
		lastSpawnedThisSession = time;
	}

	private int getSpawnProtectionTimeSecsMax() {
		return ((GameStateInterface) state).getGameState().getSpawnProtectionSec();
	}

	private long getSpawnProtectionTimeLeft() {
		return (lastSpawnedThisSession + getSpawnProtectionTimeSecsMax() * 1000L) - state.getUpdateTime();
	}

	public boolean isSpawnProtected() {
		return getSpawnProtectionTimeLeft() > 0;
	}

	@Override
	public void sendServerMessage(Object[] astr, int msgType) {
		sendServerMessage(new ServerMessage(astr, msgType));
	}

	public void forcePlayerIntoEntity(Sector to, String uID) {
		forcedEnterSector = to;
		forcedEnterUID = uID;
	}

	public BuildModePosition getBuildModePosition() {
		return buildModePosition;
	}

	public Cockpit getCockpit() {
		return cockpit;
	}

	@Override
	public PlayerRuleEntityManager getRuleEntityManager() {
		return ruleEntityManager;
	}
}