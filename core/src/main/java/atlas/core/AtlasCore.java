package atlas.core;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarMod;
import api.network.PacketReadBuffer;
import api.network.packets.PacketUtil;
import api.utils.game.PlayerUtils;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataManager;
import atlas.core.data.DataTypeRegistry;
import atlas.core.data.SerializableData;
import atlas.core.data.misc.ControlBindingData;
import atlas.core.data.player.PlayerData;
import atlas.core.data.player.PlayerDataManager;
import atlas.core.element.ElementRegistry;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.core.network.SendDataPacket;
import atlas.core.network.SyncRequestPacket;
import atlas.core.manager.ConfigManager;
import atlas.core.manager.EventManager;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

public class AtlasCore extends StarMod {

	private static AtlasCore instance;

	public AtlasCore() {
		instance = this;
	}

	public static AtlasCore getInstance() {
		return instance;
	}

	@Override
	public void onEnable() {
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		registerPackets();
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onServerCreated(ServerInitializeEvent event) {
		final long tipInterval = ConfigManager.getTipIntervalMs();
		(new Thread("AtlasCore_Tip_Thread") {
			@Override
			public void run() {
				while(true) {
					try {
						sleep(tipInterval);
						String tip = ConfigManager.getRandomTip();
						if(tip.isEmpty()) continue;
						for(PlayerState playerState : api.common.GameServer.getServerState().getPlayerStatesByName().values()) {
							PlayerUtils.sendMessage(playerState, tip);
						}
					} catch(InterruptedException exception) {
						instance.logException("AtlasCore tip thread interrupted", exception);
						return;
					}
				}
			}
		}).start();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		ControlBindingData.load(this);
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		registerPlayerDataType();
		ElementRegistry.registerElements();
		SubModRegistry.fireAtlasCoreReady();
		logInfo("AtlasCore initialized.");
	}

	public void logDebug(String message) {
		if(ConfigManager.isDebugMode()) {
			logMessage("[DEBUG]: [AtlasCore] " + message);
		}
	}

	@Override
	public void logInfo(String message) {
		super.logInfo(message);
		System.out.println("[INFO] [AtlasCore] " + message);
	}

	@Override
	public void logWarning(String message) {
		super.logWarning(message);
		System.err.println("[WARNING] [AtlasCore] " + message);
	}

	@Override
	public void logException(String message, Exception exception) {
		super.logException(message, exception);
		System.err.println("[EXCEPTION] [AtlasCore] " + message + ": " + exception.getMessage());
	}

	private void registerPackets() {
		PacketUtil.registerPacket(PlayerActionCommandPacket.class);
		PacketUtil.registerPacket(SendDataPacket.class);
		PacketUtil.registerPacket(SyncRequestPacket.class);
		logDebug("Registered packets.");
	}

	private void registerPlayerDataType() {
		DataTypeRegistry.register(new DataTypeRegistry.Entry() {
			@Override
			public String getName() { return "PLAYER_DATA"; }

			@Override
			public SerializableData deserializeNetwork(PacketReadBuffer buf) throws IOException {
				return new PlayerData(buf);
			}

			@Override
			public SerializableData deserializeJSON(JSONObject obj) {
				PlayerData data = new PlayerData();
				data.deserialize(obj);
				return data;
			}

			@Override
			public DataManager<?> getManager(boolean server) {
				return PlayerDataManager.getInstance(server);
			}
		});
	}
}
