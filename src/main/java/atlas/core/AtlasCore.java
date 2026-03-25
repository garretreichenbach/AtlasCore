package atlas.core;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarMod;
import manager.ConfigManager;
import manager.EventManager;

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
		registerCommands();
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {

	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
	}

	public void logDebug(String message) {
		if(ConfigManager.isDebugMode()) {
			logMessage("[DEBUG]: [AtlasCore] " + message);
		}
	}
	
	private void registerCommands() {
	}
}
