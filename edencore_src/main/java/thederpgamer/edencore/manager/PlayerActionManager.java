package thederpgamer.edencore.manager;

import api.common.GameCommon;
import api.common.GameServer;
import api.utils.game.PlayerUtils;
import api.utils.game.inventory.InventoryUtils;
import api.utils.gui.ModGUIHandler;
import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.element.meta.MetaObjectManager;
import org.schema.game.common.data.element.meta.weapon.Weapon;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.controller.SectorSwitch;
import org.schema.schine.network.objects.Sendable;
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;
import thederpgamer.edencore.data.playerdata.PlayerData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;
import thederpgamer.edencore.element.ElementManager;
import thederpgamer.edencore.utils.EntityUtils;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerActionManager {

	public static final int OPEN_GUIDE = 0;
	public static final int WARP_INTO_ENTITY = 1;
	public static final int SET_CREDITS = 2;
	public static final int ENTER_BUILD_SECTOR = 3;
	public static final int LEAVE_BUILD_SECTOR = 4;
	public static final int ADD_BARS = 5;
	public static final int GIVE_ITEM = 6;

	public static void processAction(int type, String[] args) {
		try {
			PlayerState playerState;
			PlayerData playerData;
			switch(type) {
				case OPEN_GUIDE:
					ModGUIHandler.getGUIControlManager("glossarPanel").setActive(true);
					break;
				case WARP_INTO_ENTITY:
					int entityId = Integer.parseInt(args[0]);
					Sendable sendable = GameCommon.getGameObject(entityId);
					if(sendable instanceof SegmentController)
						EntityUtils.warpPlayerIntoEntity((SegmentController) sendable);
					break;
				case SET_CREDITS:
					playerState = GameCommon.getPlayerFromName(args[0]);
					long credits = Long.parseLong(args[1]);
					if(playerState != null) playerState.setCredits(credits);
					break;
				case ENTER_BUILD_SECTOR:
					playerState = GameCommon.getPlayerFromName(args[0]);
					if(playerState.getFirstControlledTransformableWOExc() instanceof SegmentController) {
						PlayerUtils.sendMessage(playerState, "You cannot enter a build sector while piloting a ship. Please leave the ship first.");
						return;
					}
					playerState.getControllerState().forcePlayerOutOfSegmentControllers();
					playerState.getControllerState().forcePlayerOutOfShips();
					playerData = PlayerDataManager.getInstance(playerState.isOnServer()).getFromName(playerState.getName(), playerState.isOnServer());
					BuildSectorData data = BuildSectorDataManager.getInstance(playerState.isOnServer()).getFromUUID(args[1], playerState.isOnServer());
					playerData.setLastRealSector(playerState.getCurrentSector());
					Transform lastRealTransform = new Transform();
					lastRealTransform.setIdentity();
					if(playerState.getFirstControlledTransformableWOExc() instanceof SegmentController)
						lastRealTransform.origin.set(playerState.getFirstControlledTransformableWOExc().getWorldTransform().origin);
					else
						lastRealTransform.origin.set(playerState.getAssingedPlayerCharacter().getWorldTransform().origin);
					playerData.setLastRealTransform(lastRealTransform);
					Vector3i sector = data.getSector();
					GameServer.getServerState().getController().queueSectorSwitch(playerState.getFirstControlledTransformableWOExc(), sector, SectorSwitch.TRANS_JUMP, false, true, true);
					playerState.setHasCreativeMode(false);
					playerState.setUseCreativeMode(false);
					playerState.updateInventory();
					break;
				case LEAVE_BUILD_SECTOR:
					playerState = GameCommon.getPlayerFromName(args[0]);
					if(playerState.getFirstControlledTransformableWOExc() instanceof SegmentController) {
						PlayerUtils.sendMessage(playerState, "You cannot exit a build sector while piloting a ship. Please leave the ship first.");
						return;
					}
					playerState.getControllerState().forcePlayerOutOfSegmentControllers();
					playerState.getControllerState().forcePlayerOutOfShips();
					playerData = PlayerDataManager.getInstance(playerState.isOnServer()).getFromName(playerState.getName(), playerState.isOnServer());
					Vector3i lastRealSector = playerData.getLastRealSector();
					if(lastRealSector.equals(playerState.getCurrentSector()))
						lastRealSector.set(playerState.spawnData.getSpawnSector(playerState.spawnedOnce).pos);
					Transform lastRealTransform1 = playerData.getLastRealTransform();
					playerState.setHasCreativeMode(false);
					playerState.setUseCreativeMode(false);
					GameServer.getServerState().getController().queueSectorSwitch(playerState.getFirstControlledTransformableWOExc(), lastRealSector, SectorSwitch.TRANS_JUMP, false, true, true);
					playerState.getAssingedPlayerCharacter().warpTransformable(lastRealTransform1, false, true, null);
					playerState.updateInventory();
					break;
				case ADD_BARS:
					playerState = GameCommon.getPlayerFromName(args[0]);
					PlayerState toState = GameCommon.getPlayerFromName(args[1]);
					if(toState == null) {
						toState = thederpgamer.edencore.utils.PlayerUtils.getPlayerFromDB(args[1]);
					}
					if(toState == null) {
						EdenCore.getInstance().logWarning("Player not found: " + args[1]);
						return;
					}
					int bars = Integer.parseInt(args[2]);
					if(toState.getInventory().hasFreeSlot()) {
						InventoryUtils.addItem(toState.getInventory(), ElementManager.getItem("Gold Bar").getId(), bars);
						thederpgamer.edencore.utils.PlayerUtils.sendMail(playerState.getName(), toState.getName(), "Blueprint Sold", "You have received " + bars + " Gold Bars from " + playerState.getName() + ".", playerState.isOnServer());
					}
					break;
				case GIVE_ITEM:
					playerState = GameCommon.getPlayerFromName(args[0]);
					short itemId = Short.parseShort(args[1]);
					int amount = Integer.parseInt(args[2]);
					boolean isMetaItem = Boolean.parseBoolean(args[3]);
					if(playerState.getInventory().hasFreeSlot()) {
						if(isMetaItem) {
							Weapon weapon = (Weapon) MetaObjectManager.instantiate(MetaObjectManager.MetaObjectType.WEAPON, itemId, true);
							int slot = playerState.getInventory().getFreeSlot();
							playerState.getInventory().put(slot, weapon);
							playerState.sendInventoryModification(slot, Long.MIN_VALUE);
						} else {
							InventoryUtils.addItem(playerState.getInventory(), itemId, amount);
						}
					}
					break;
			}
		} catch(Exception exception) {
			EdenCore.getInstance().logException("An error occurred while processing player action", exception);
		}
	}
}
