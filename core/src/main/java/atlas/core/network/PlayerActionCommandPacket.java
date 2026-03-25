package atlas.core.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.core.manager.PlayerActionRegistry;
import org.schema.game.common.data.player.PlayerState;

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
		PlayerActionRegistry.process(type, args);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		PlayerActionRegistry.process(type, args);
	}
}
