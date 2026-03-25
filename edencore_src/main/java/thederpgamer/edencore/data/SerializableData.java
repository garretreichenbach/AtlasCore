package thederpgamer.edencore.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.json.JSONObject;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;
import thederpgamer.edencore.data.exchangedata.ExchangeData;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;
import thederpgamer.edencore.data.playerdata.PlayerData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;

import java.io.IOException;
import java.util.UUID;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class SerializableData {

	public enum DataType {
		PLAYER_DATA(PlayerData.class, PlayerDataManager.class),
		PLAYER_BANKING_TRANSACTION_DATA(PlayerData.PlayerBankTransactionData.class, null),
		BUILD_SECTOR_DATA(BuildSectorData.class, BuildSectorDataManager.class),
		BUILD_SECTOR_ENTITY_DATA(BuildSectorData.BuildSectorEntityData.class, null),
		EXCHANGE_DATA(ExchangeData.class, ExchangeDataManager.class),;

		private final Class<? extends SerializableData> dataClass;
		private final Class<? extends DataManager<?>> dataManagerClass;

		DataType(Class<? extends SerializableData> dataClass, Class<? extends DataManager<?>> dataManagerClass) {
			this.dataClass = dataClass;
			this.dataManagerClass = dataManagerClass;
		}

		public Class<? extends SerializableData> getDataClass() {
			return dataClass;
		}
		
		public Class<? extends DataManager<?>> getDataManagerClass() {
			return dataManagerClass;
		}
	}

	protected String dataUUID;
	protected DataType dataType;

	protected SerializableData(DataType dataType) {
		this.dataType = dataType;
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

	public DataType getDataType() {
		return dataType;
	}

	public abstract JSONObject serialize();

	public abstract void deserialize(JSONObject data);

	public abstract void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException;

	public abstract void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException;
}
