package atlas.core.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;
import atlas.core.data.SerializableData;
import atlas.core.data.DataTypeRegistry;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class SyncRequestPacket extends Packet {

	private String dataTypeName;

	public SyncRequestPacket() {
	}

	public SyncRequestPacket(String dataTypeName) {
		this.dataTypeName = dataTypeName;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		dataTypeName = packetReadBuffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeString(dataTypeName);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		DataTypeRegistry.Entry entry = DataTypeRegistry.get(dataTypeName);
		if(entry != null) {
			DataManager<?> dataManager = entry.getManager(true);
			if(dataManager != null) {
				for(SerializableData data : dataManager.getCache(true)) dataManager.sendDataToPlayer(playerState, data, DataManager.ADD_DATA);
			} else {
				AtlasCore.getInstance().logWarning(
						"Failed to find DataManager for type: " + dataTypeName +
						". Ensure the DataManager is properly registered and initialized."
				);
			}
		} else {
			AtlasCore.getInstance().logWarning(
					"No DataTypeRegistry entry found for type: " + dataTypeName +
					". Ensure the type is properly registered."
			);
		}
	}
}
