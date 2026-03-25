package atlas.core.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class SerializableData {

	protected String dataUUID;
	protected String dataTypeName;

	protected SerializableData(String dataTypeName) {
		this.dataTypeName = dataTypeName;
		dataUUID = UUID.randomUUID().toString();
	}

	protected SerializableData() {

	}

	@Override
	public boolean equals(Object obj) {
		return obj.getClass() == getClass() && ((SerializableData) obj).dataUUID.equals(dataUUID);
	}

	@Override
	public int hashCode() {
		return dataUUID.hashCode();
	}

	public String getUUID() {
		return dataUUID;
	}

	public String getDataTypeName() {
		return dataTypeName;
	}

	public abstract JSONObject serialize();

	public abstract void deserialize(JSONObject data);

	public abstract void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException;

	public abstract void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException;
}
