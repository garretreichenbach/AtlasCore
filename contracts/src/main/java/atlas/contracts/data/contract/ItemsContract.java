package atlas.contracts.data.contract;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.inventory.InventoryUtils;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;

import java.io.IOException;

public class ItemsContract extends ContractData {

	private static final byte VERSION = 0;
	private short targetID;
	private int targetAmount;

	public ItemsContract(PacketReadBuffer packetReadBuffer) throws IOException {
		super(packetReadBuffer);
	}

	public ItemsContract(int contractorID, String name, long reward, short targetID, int targetAmount, Difficulty difficulty) {
		super(ContractType.ITEMS, contractorID, name, reward, difficulty);
		this.targetID = targetID;
		this.targetAmount = targetAmount;
	}

	public ItemsContract(JSONObject json) {
		super(json);
	}

	public short getTargetID() {
		return targetID;
	}

	public int getTargetAmount() {
		return targetAmount;
	}

	@Override
	public JSONObject serialize() {
		JSONObject data = super.serialize();
		data.put("target_id", targetID);
		data.put("target_amount", targetAmount);
		return data;
	}

	@Override
	public void deserialize(JSONObject data) {
		super.deserialize(data);
		targetID = (short) data.getInt("target_id");
		targetAmount = data.getInt("target_amount");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		super.serializeNetwork(writeBuffer);
		writeBuffer.writeShort(targetID);
		writeBuffer.writeInt(targetAmount);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		super.deserializeNetwork(readBuffer);
		targetID = readBuffer.readShort();
		targetAmount = readBuffer.readInt();
	}

	@Override
	public boolean canComplete(PlayerState player) {
		if(player == null || !claimants.containsKey(player.getName())) return false;
		if(player.isAdmin() && player.isUseCreativeMode()) return true;
		Inventory playerInventory = player.getInventory();
		if(playerInventory.isLockedInventory()) return false;
		int count = InventoryUtils.getItemAmount(playerInventory, targetID);
		return count >= targetAmount;
	}

	@Override
	public ContractType getContractType() {
		return ContractType.ITEMS;
	}

	@Override
	public void onCompletion(PlayerState player) {
		assert player.isOnServer();
		Inventory playerInventory = player.getInventory();
		// Admins in creative mode are not charged the items (canComplete short-circuits true for them).
		if(!(player.isAdmin() && player.isUseCreativeMode())) {
			InventoryUtils.consumeItems(playerInventory, targetID, targetAmount);
		}
		payoutReward(player);
	}
}
