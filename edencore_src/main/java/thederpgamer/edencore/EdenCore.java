package thederpgamer.edencore;

import api.common.GameCommon;
import api.common.GameServer;
import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.game.PlayerUtils;
import api.utils.textures.StarLoaderTexture;
import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import org.apache.commons.io.IOUtils;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.edencore.commands.GuideCommand;
import thederpgamer.edencore.data.misc.ControlBindingData;
import thederpgamer.edencore.element.ElementManager;
import thederpgamer.edencore.element.items.PrizeBars;
import thederpgamer.edencore.manager.ConfigManager;
import thederpgamer.edencore.manager.EventManager;
import thederpgamer.edencore.network.PlayerActionCommandPacket;
import thederpgamer.edencore.network.SendDataPacket;
import thederpgamer.edencore.network.SyncRequestPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EdenCore extends StarMod {

	private static EdenCore instance;

	public EdenCore() {
		instance = this;
	}

	public static void main(String[] args) {
	}

	public static EdenCore getInstance() {
		return instance;
	}

	private final String[] overwrites = {"CatalogPanelNew", "PlayerState"};

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		EventManager.initialize(this);
		registerPackets();
		registerCommands();
	}

	@Override
	public byte[] onClassTransform(String className, byte[] byteCode) {
		for(String name : overwrites) {
			if(className.endsWith(name)) return overwriteClass(className, byteCode);
		}
		return super.onClassTransform(className, byteCode);
	}

	@Override
	public void onServerCreated(ServerInitializeEvent serverInitializeEvent) {
		final long tipInterval = ConfigManager.getMainConfig().getLong("tip_interval");
		(new Thread("EdenCore_Login_Timer_Thread") {
			@Override
			public void run() {
				while(true) {
					try {
						sleep(tipInterval);
						String tip = ConfigManager.getRandomTip();
						for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
							PlayerUtils.sendMessage(playerState, tip);
						}
					} catch(InterruptedException exception) {
						instance.logException("An error occurred while checking player login timers", exception);
						return;
					}
				}
			}
		}).start();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
		registerBindings();
		initializeGlossary();
	}

	@Override
	public void onBlockConfigLoad(BlockConfig blockConfig) {
		ElementManager.addItemGroup(new PrizeBars());
		ElementManager.initialize();
		logInfo("Initialized Blocks");
	}

	@Override
	public void logInfo(String message) {
		super.logInfo(message);
		System.out.println("[INFO] " + message);
	}

	@Override
	public void logWarning(String message) {
		super.logWarning(message);
		System.err.println("[WARNING] " + message);
	}

	@Override
	public void logException(String message, Exception exception) {
		super.logException(message, exception);
		System.err.println("[EXCEPTION] " + message + "\n" + exception.getMessage() + "\n" + Arrays.toString(exception.getStackTrace()));
	}

	@Override
	public void logFatal(String message, Exception exception) {
		logException(message, exception);
		if(GameCommon.getGameState().isOnServer()) GameServer.getServerState().addCountdownMessage(10, "Server will perform an emergency shutdown due to a fatal error: " + message);
	}

	public void initializeGlossary() {
		StarLoaderTexture.runOnGraphicsThread(new Runnable() {
			@Override
			public void run() {
				GlossarInit.initGlossar(getInstance());
				GlossarCategory rules = new GlossarCategory("Server Rules");
				rules.addEntry(new GlossarEntry("Server Info", "Skies of Eden is a modded survival StarMade server run by the SOE staff team and hosted on PingPerfect hardware.\n" + "We work hard to bring new features and content to the server, and we hope you enjoy your time here.\n" + "Note that not all features are complete, and some may be buggy. If you find any bugs, please report them to a staff member.\n" + "Please read the rules section before playing on the server, and be sure to join our discord at https://discord.gg/qxzvBxT."));
				rules.addEntry(new GlossarEntry("Rules", "1) Be polite and respectful in chat.\n" + "2) Do not spam chat or advertise links to other servers.\n" + "3) Do not use any cheats, glitches, exploits, etc. that give you an unfair advantage over other players. If you find a bug, please report it to a staff member.\n" + "4) Keep politics at an absolute minimum. This is a starmade server, not a political forum.\n" + "5) Hate speech and hate symbols are not tolerated. This includes racism, sexism, homophobia, etc.\n" + "6) Do not intentionally create server lag. If your entity is lagging the server, it may be deleted by staff without compensation.\n" + "7) Do not create home-bases on planets.\n" + "8) Do not attempt to attack or capture public infrastructure such as warpgates.\n" + "9) Use common sense. If you are unsure about something, ask a staff member.\n" + "10) Repeated or serious violations of any of the server rules can result in bans of the offenders, deletion of ships/stations, and penalties to anyone involved or associated."));
				GlossarInit.addCategory(rules);
				GlossarCategory edenCore = new GlossarCategory("Eden Core");
//				edenCore.addEntry(new GlossarEntry("Build Sectors", "Build Sectors are special sectors unique to each player where you can build freely in creative mode. They are protected from other players and hostiles.\n" + "You can invite other players to your Build Sector, set permissions, spawn entities, and more using the Build Sector menu.\nTo access the Build Sector menu, look in the top right menu bar under PLAYER.\n"));
				GlossarInit.addCategory(edenCore);
				logInfo("Initialized Glossary");
			}
		});
	}

	private void registerPackets() {
		PacketUtil.registerPacket(PlayerActionCommandPacket.class);
		PacketUtil.registerPacket(SendDataPacket.class);
		PacketUtil.registerPacket(SyncRequestPacket.class);
		logInfo("Registered Packets");
	}

	private void registerCommands() {
		StarLoader.registerCommand(new GuideCommand());
		logInfo("Registered Commands");
	}

	public void registerBindings() {
		ControlBindingData.load(this);
		ControlBindingData.registerBinding(this, "Open Guide", "Opens the Guide Menu", 181);
		ControlBindingData.registerBinding(this, "Open Banking Menu", "Opens the Banking Menu", 78);
//		ControlBindingData.registerBinding(this, "Open Exchange Menu", "Opens the Exchange Menu", 74);
//		ControlBindingData.registerBinding(this, "Open Build Sector Menu", "Opens the Build Sector Menu", 55);
		logInfo("Registered Bindings");
	}

	private byte[] overwriteClass(String className, byte[] byteCode) {
		byte[] bytes = null;
		try {
			ZipInputStream file = new ZipInputStream(Files.newInputStream(getSkeleton().getJarFile().toPath()));
			while(true) {
				ZipEntry nextEntry = file.getNextEntry();
				if(nextEntry == null) break;
				if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
			}
			file.close();
		} catch(IOException exception) {
			logException("Failed to overwrite class: " + className, exception);
		}
		if(bytes != null) return bytes;
		else return byteCode;
	}
}
