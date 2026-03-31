package atlas.core.manager;

import api.utils.simpleconfig.*;
import atlas.core.AtlasCore;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class ConfigManager {

	private static final Random RANDOM = new Random();

	private static SimpleConfigContainer config;

	private static SimpleConfigBool debugMode;
	private static SimpleConfigString discordInviteCode;
	private static SimpleConfigInt tipIntervalSeconds;
	private static SimpleConfigString tips;

	private ConfigManager() {
	}

	public static void initialize(AtlasCore instance) {
		config = new SimpleConfigContainer(instance, "config", false);

		debugMode = new SimpleConfigBool(config, "debug_mode", false, "If true, enables debug logging and features.");

		discordInviteCode = new SimpleConfigString(config, "discord_invite_code", "kcb84yRwHU", "Discord invite code used for the DISCORD top-bar button.");

		tipIntervalSeconds = new SimpleConfigInt(config, "tip_interval_seconds", 600, "How often (in seconds) a random tip is broadcast to all players.");

		tips = new SimpleConfigString(config, "tips", "Welcome to the server!;Check /guide for server info.;Join our Discord for updates!", "Semicolon-separated list of tips that are broadcast to players periodically.");

		config.readWriteFields();
	}

	public static void reload() {
		if(config != null) config.readFields();
	}

	public static boolean isDebugMode() {
		return boolOrDefault(debugMode, false);
	}

	public static String getDiscordInviteCode() {
		return stringOrDefault(discordInviteCode, "kcb84yRwHU");
	}

	/** Returns the tip broadcast interval in milliseconds. */
	public static long getTipIntervalMs() {
		return intOrDefault(tipIntervalSeconds, 600) * 1000L;
	}

	/** Returns a randomly chosen tip from the tips config. */
	public static String getRandomTip() {
		String raw = stringOrDefault(tips, "");
		if(raw.isEmpty()) return "";
		List<String> list = Arrays.asList(raw.split(";"));
		Collections.shuffle(list, RANDOM);
		return list.get(0).trim();
	}

	// ── private helpers ──────────────────────────────────────────────────────

	private static boolean boolOrDefault(SimpleConfigBool entry, boolean def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static int intOrDefault(SimpleConfigInt entry, int def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static double doubleOrDefault(SimpleConfigDouble entry, double def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static String stringOrDefault(SimpleConfigString entry, String def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}
}
