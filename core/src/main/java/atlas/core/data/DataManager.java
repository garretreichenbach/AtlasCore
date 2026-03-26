package atlas.core.data;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import atlas.core.AtlasCore;
import atlas.core.network.SendDataPacket;
import atlas.core.network.SyncRequestPacket;
import org.schema.game.common.data.player.PlayerState;

import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class DataManager<E extends SerializableData> {

	public static final int ADD_DATA = 0;
	public static final int REMOVE_DATA = 1;
	public static final int UPDATE_DATA = 2;

	public void sendDataToAllPlayers(SerializableData data, int type) {
		for(PlayerState player : GameServer.getServerState().getPlayerStatesByName().values()) sendDataToPlayer(player, data, type);
	}

	public void sendDataToPlayer(PlayerState player, SerializableData data, int type) {
		AtlasCore.getInstance().logInfo("[SERVER] Sending " + data.getDataTypeName() + " " + data.getUUID() + " to player " + player.getName() + " with type " + getTypeString(type) + ".");
		PacketUtil.sendPacket(player, new SendDataPacket(data, type)); // Send the packet to the specific player
	}

	public void sendAllDataToPlayer(PlayerState player) {
		Set<E> cache = getCache(true);
		for(E data : cache) sendDataToPlayer(player, data, ADD_DATA);
	}

	public void requestFromServer() {
		AtlasCore.getInstance().logInfo("[CLIENT] Requesting all data from server for " + getDataTypeName() + ".");
		PacketUtil.sendPacketToServer(new SyncRequestPacket(getDataTypeName()));
	}

	public void sendPacket(SerializableData data, int type, boolean toServer) {
		AtlasCore.getInstance().logInfo((toServer ? "[CLIENT]" : "[SERVER]") + " Sending " + data.getDataTypeName() + " " + data.getUUID() + " with type " + getTypeString(type) + ".");
		if(toServer) PacketUtil.sendPacketToServer(new SendDataPacket(data, type));
		else sendDataToAllPlayers(data, type);
	}

	public Set<E> getCache(boolean isServer) {
		return isServer ? getServerCache() : getClientCache();
	}

	public void addData(E data, boolean server) {
		AtlasCore.getInstance().logInfo("Adding " + data.getDataTypeName() + " " + data.getUUID() + " to " + (server ? "server" : "client") + " cache.");
		if(server) addToServerCache(data);
		else addToClientCache(data);
	}

	public void removeData(E data, boolean server) {
		AtlasCore.getInstance().logInfo("Removing " + data.getDataTypeName() + " " + data.getUUID() + " from " + (server ? "server" : "client") + " cache.");
		if(server) removeFromServerCache(data);
		else removeFromClientCache(data);
	}

	public void updateData(E data, boolean server) {
		AtlasCore.getInstance().logInfo("Updating " + data.getDataTypeName() + " " + data.getUUID() + " in " + (server ? "server" : "client") + " cache.");
		if(server) updateServerCache(data);
		else updateClientCache(data);
	}

	public void handlePacket(SerializableData data, int type, boolean server) {
		AtlasCore.getInstance().logInfo(server ? "[SERVER]" : "[CLIENT]" + " Received " + data.getDataTypeName() + " " + data.getUUID() + " with type " + getTypeString(type) + ".");
		switch(type) {
			case ADD_DATA:
				addData((E) data, server);
				break;
			case REMOVE_DATA:
				removeData((E) data, server);
				break;
			case UPDATE_DATA:
				updateData((E) data, server);
				break;
		}
	}

	public E getFromUUID(String uuid, boolean server) {
		Set<E> cache = getCache(server);
		for(E data : cache) if(data.getUUID().equals(uuid)) return data;
		return null;
	}

	public abstract Set<E> getServerCache();

	public void addToServerCache(E data) {
		PersistentObjectUtil.addObject(AtlasCore.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(AtlasCore.getInstance().getSkeleton());
		sendDataToAllPlayers(data, ADD_DATA);
	}

	public void removeFromServerCache(E data) {
		PersistentObjectUtil.removeObject(AtlasCore.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(AtlasCore.getInstance().getSkeleton());
		sendDataToAllPlayers(data, REMOVE_DATA);
	}

	public void updateServerCache(E data) {
		PersistentObjectUtil.removeObject(AtlasCore.getInstance().getSkeleton(), data);
		PersistentObjectUtil.addObject(AtlasCore.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(AtlasCore.getInstance().getSkeleton());
		sendDataToAllPlayers(data, UPDATE_DATA);
	}

	protected String getTypeString(int type) {
		switch(type) {
			case ADD_DATA:
				return "ADD_DATA";
			case REMOVE_DATA:
				return "REMOVE_DATA";
			case UPDATE_DATA:
				return "UPDATE_DATA";
			default:
				return "UNKNOWN(" + type + ")";
		}
	}

	public abstract String getDataTypeName();

	public abstract Set<E> getClientCache();

	public abstract void addToClientCache(E data);

	public abstract void removeFromClientCache(E data);

	public abstract void updateClientCache(E data);

	public abstract void createMissingData(Object... args);
}
