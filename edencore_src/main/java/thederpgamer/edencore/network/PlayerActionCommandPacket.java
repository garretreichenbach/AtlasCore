package thederpgamer.edencore.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.edencore.manager.PlayerActionManager;

import java.io.IOException;
import java.util.Arrays;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerActionCommandPacket extends Packet {
	
	private int type;
	private String[] args;
	
	public PlayerActionCommandPacket(int type, String... args) {
		this.type = type;
		this.args = args;
	}
	
	public PlayerActionCommandPacket() {}
	
	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		type = packetReadBuffer.readInt();
		args = packetReadBuffer.readStringList().toArray(new String[0]);
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(type);
		packetWriteBuffer.writeStringList(Arrays.asList(args));
	}

	@Override
	public void processPacketOnClient() {
		PlayerActionManager.processAction(type, args);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		PlayerActionManager.processAction(type, args);
	}
}
