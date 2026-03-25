package atlas.core.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.AtlasCore;
import atlas.core.data.SerializableData;
import atlas.core.data.DataTypeRegistry;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

public class SendDataPacket extends Packet {

	private String dataTypeName;
	private SerializableData data;
	private int type;

	public SendDataPacket() {}

	public SendDataPacket(SerializableData data, int type) {
		this.data = data;
		this.type = type;
		dataTypeName = data.getDataTypeName();
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) {
		try {
			type = packetReadBuffer.readInt();
			dataTypeName = packetReadBuffer.readString();
			DataTypeRegistry.Entry entry = DataTypeRegistry.get(dataTypeName);
			if(entry != null) {
				data = entry.deserializeNetwork(packetReadBuffer);
			} else {
				AtlasCore.getInstance().logWarning("No DataTypeRegistry entry found for type: " + dataTypeName);
			}
		} catch(Exception exception) {
			AtlasCore.getInstance().logException("An error occurred while reading data packet", exception);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(type);
		packetWriteBuffer.writeString(dataTypeName);
		data.serializeNetwork(packetWriteBuffer);
	}

	@Override
	public void processPacketOnClient() {
		DataTypeRegistry.Entry entry = DataTypeRegistry.get(dataTypeName);
		if(entry != null) {
			entry.getManager(false).handlePacket(data, type, false);
		} else {
			AtlasCore.getInstance().logWarning("No DataTypeRegistry entry found for type: " + dataTypeName);
		}
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		DataTypeRegistry.Entry entry = DataTypeRegistry.get(dataTypeName);
		if(entry != null) {
			entry.getManager(playerState.isOnServer()).handlePacket(data, type, playerState.isOnServer());
		} else {
			AtlasCore.getInstance().logWarning("No DataTypeRegistry entry found for type: " + dataTypeName);
		}
	}
}
