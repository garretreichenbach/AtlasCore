package manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigDouble;
import api.utils.simpleconfig.SimpleConfigInt;
import atlas.core.AtlasCore;

public final class ConfigManager {

	private static SimpleConfigContainer config;

	private static SimpleConfigBool debugMode;

	private ConfigManager() {
	}

	public static void initialize(AtlasCore instance) {
		config = new SimpleConfigContainer(instance, "config", false);

		debugMode = new SimpleConfigBool(config, "debug_mode", false, "If true, enables debug logging and features.");

		config.readWriteFields();
	}

	public static void reload() {
		if(config != null) {
			config.readFields();
		}
	}

	public static boolean isDebugMode() {
		return boolOrDefault(debugMode, false);
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean boolOrDefault(SimpleConfigBool entry, boolean defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}

	private static int intOrDefault(SimpleConfigInt entry, int defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}

	private static double doubleOrDefault(SimpleConfigDouble entry, double defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}
}
