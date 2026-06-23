package atlas.contracts.data.contract.active;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.ContractDataManager;
import atlas.core.data.SerializableData;
import org.json.JSONObject;

import java.io.IOException;

public class ActiveContractData extends SerializableData {

	public static final String DATA_TYPE = "ACTIVE_CONTRACT_DATA";

	private final byte VERSION = 0;
	private String targetContract;
	private ContractData.ContractType contractType;
	private String claimer;
	private boolean canComplete;

	public ActiveContractData(ContractData contract, String claimer) {
		super(DATA_TYPE);
		targetContract = contract.getUUID();
		contractType = contract.getContractType();
		this.claimer = claimer;
	}

	public ActiveContractData(JSONObject json) {
		deserialize(json);
		dataTypeName = DATA_TYPE;
	}

	public ActiveContractData(PacketReadBuffer packetReadBuffer) throws IOException {
		deserializeNetwork(packetReadBuffer);
		dataTypeName = DATA_TYPE;
	}

	public String getClaimer() {
		return claimer;
	}

	public String getTargetContractID() {
		return targetContract;
	}

	public ContractData.ContractType getContractType() {
		return contractType;
	}

	public ContractData getTargetContract(boolean server) {
		return ContractDataManager.getInstance(server).getFromUUID(targetContract, server);
	}

	public boolean canComplete() {
		return canComplete;
	}

	public void setCanComplete(boolean canComplete, boolean server) {
		if(this.canComplete == canComplete) return; // no-op guard: avoids repeated broadcasts
		this.canComplete = canComplete;
		// updateData already broadcasts to all players on the server side; no separate sendPacket needed.
		ActiveContractDataManager.getInstance(server).updateData(this, server);
	}

	@Override
	public JSONObject serialize() {
		JSONObject json = new JSONObject();
		json.put("version", VERSION);
		json.put("uuid", dataUUID);
		json.put("target_contract", targetContract);
		json.put("contract_type", contractType.name());
		json.put("claimer", claimer);
		json.put("can_complete", canComplete);
		return json;
	}

	@Override
	public void deserialize(JSONObject data) {
		byte version = (byte) data.getInt("version");
		dataUUID = data.getString("uuid");
		targetContract = data.getString("target_contract");
		contractType = ContractData.ContractType.valueOf(data.getString("contract_type").toUpperCase());
		claimer = data.getString("claimer");
		canComplete = data.getBoolean("can_complete");
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(targetContract);
		writeBuffer.writeString(contractType.name());
		writeBuffer.writeString(claimer);
		writeBuffer.writeBoolean(canComplete);
	}

	@Override
	public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		targetContract = readBuffer.readString();
		contractType = ContractData.ContractType.valueOf(readBuffer.readString().toUpperCase());
		claimer = readBuffer.readString();
		canComplete = readBuffer.readBoolean();
	}
}
