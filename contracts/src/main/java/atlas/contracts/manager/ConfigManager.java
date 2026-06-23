package atlas.contracts.manager;

import api.utils.simpleconfig.*;
import atlas.contracts.AtlasContracts;

/**
 * AtlasContracts configuration. Rewards/costs are denominated in Gold Bars (physical inventory items),
 * not credits, so the values are deliberately small. There is no reward-type option: Gold Bars are the
 * only currency.
 */
public final class ConfigManager {

	private static SimpleConfigContainer config;

	private static SimpleConfigBool debugMode;
	private static SimpleConfigBool autoGenerateContracts;
	private static SimpleConfigInt maxAutoGenerateContracts;
	private static SimpleConfigInt autoGenerateContractCheckTimer;
	private static SimpleConfigInt contractTimerMax;
	private static SimpleConfigInt contractTimeoutTimer;
	private static SimpleConfigInt clientMaxActiveContracts;
	private static SimpleConfigInt blueprintUpdateInterval;
	private static SimpleConfigInt maxBountyMobCount;
	private static SimpleConfigInt maxBountyMobCombinedMass;
	private static SimpleConfigDouble rewardBaseMultiplier;
	private static SimpleConfigBool autoBountyEnabled;
	private static SimpleConfigInt autoBountyKillThreshold;
	private static SimpleConfigInt autoBountyReward;
	private static SimpleConfigInt autoBountyDecayTimer;
	private static SimpleConfigString escortCargoBlueprintPool;
	private static SimpleConfigString escortDefenderBlueprintPool;
	private static SimpleConfigInt escortRouteMinLength;
	private static SimpleConfigInt escortRouteMaxLength;
	private static SimpleConfigInt escortCargoCount;
	private static SimpleConfigInt escortBaseReward;
	private static SimpleConfigInt escortPirateWaveInterval;
	private static SimpleConfigInt escortPirateMaxPerWave;
	private static SimpleConfigInt escortSmallShipBonusMass;
	private static SimpleConfigDouble escortSmallShipBonusMultiplier;
	private static SimpleConfigDouble escortCargoLossPenalty;
	private static SimpleConfigInt escortUpdateInterval;
	private static SimpleConfigInt bountyDefaultReward;
	private static SimpleConfigInt bountyMinReward;
	private static SimpleConfigBool escortEnabled;
	private static SimpleConfigInt itemsMinAmount;
	private static SimpleConfigInt itemsMaxAmount;
	private static SimpleConfigInt itemsRewardDivisor;
	private static SimpleConfigInt itemsRewardMin;

	private ConfigManager() {
	}

	public static void initialize(AtlasContracts instance) {
		config = new SimpleConfigContainer(instance, "config", false);

		debugMode = new SimpleConfigBool(config, "debug_mode", false, "If true, enables debug logging.");
		autoGenerateContracts = new SimpleConfigBool(config, "auto_generate_contracts", true, "If true, the server will periodically generate random contracts.");
		maxAutoGenerateContracts = new SimpleConfigInt(config, "max_auto_generate_contracts", 10, "Maximum number of auto-generated contracts allowed at once.");
		autoGenerateContractCheckTimer = new SimpleConfigInt(config, "auto_generate_contract_check_timer", 3000, "Interval in milliseconds between auto-generation checks.");
		contractTimerMax = new SimpleConfigInt(config, "contract_timer_max", 3600000, "Maximum contract duration in milliseconds.");
		contractTimeoutTimer = new SimpleConfigInt(config, "contract_timeout_timer", 900000, "Time in milliseconds before an unclaimed contract expires.");
		clientMaxActiveContracts = new SimpleConfigInt(config, "client_max_active_contracts", 5, "Maximum number of contracts a single player can hold at once.");
		blueprintUpdateInterval = new SimpleConfigInt(config, "blueprint_update_interval", 300000, "Interval in milliseconds between blueprint cache refreshes.");
		maxBountyMobCount = new SimpleConfigInt(config, "max_bounty_mob_count", 12, "Maximum number of mobs in a single bounty contract.");
		maxBountyMobCombinedMass = new SimpleConfigInt(config, "max_bounty_mob_combined_mass", 350000, "Maximum combined mass of mobs in a single bounty contract.");
		rewardBaseMultiplier = new SimpleConfigDouble(config, "reward_base_multiplier", 1.0, "Base multiplier applied to all Gold Bar rewards before difficulty scaling.");
		autoBountyEnabled = new SimpleConfigBool(config, "auto_bounty_enabled", true, "If true, NPC factions will automatically place bounties on aggressive players.");
		autoBountyKillThreshold = new SimpleConfigInt(config, "auto_bounty_kill_threshold", 3, "Number of NPC faction kills before that faction places an auto-bounty.");
		autoBountyReward = new SimpleConfigInt(config, "auto_bounty_reward", 5, "Base Gold Bar reward for auto-generated NPC bounties (escalates by the number of bounties placed).");
		autoBountyDecayTimer = new SimpleConfigInt(config, "auto_bounty_decay_timer", 10800000, "Time in milliseconds before a player's aggression count decays by one.");
		escortCargoBlueprintPool = new SimpleConfigString(config, "escort_cargo_blueprint_pool", "T180-18", "Comma-separated list of blueprint names to use as cargo ships in escort contracts.");
		escortDefenderBlueprintPool = new SimpleConfigString(config, "escort_defender_blueprint_pool", "C140-9,C120-12,B110-11", "Comma-separated list of blueprint names to use as friendly defender escorts. If empty, no defenders spawn.");
		escortRouteMinLength = new SimpleConfigInt(config, "escort_route_min_length", 3, "Minimum number of waypoint sectors in an escort route.");
		escortRouteMaxLength = new SimpleConfigInt(config, "escort_route_max_length", 8, "Maximum number of waypoint sectors in an escort route.");
		escortCargoCount = new SimpleConfigInt(config, "escort_cargo_count", 3, "Number of cargo ships to escort.");
		escortBaseReward = new SimpleConfigInt(config, "escort_base_reward", 7, "Base Gold Bar reward for escort contracts before difficulty scaling, cargo-loss penalty, and small-ship bonus.");
		escortPirateWaveInterval = new SimpleConfigInt(config, "escort_pirate_wave_interval", 60000, "Time in milliseconds between pirate wave spawns during escort.");
		escortPirateMaxPerWave = new SimpleConfigInt(config, "escort_pirate_max_per_wave", 4, "Maximum number of pirates per wave during escort.");
		escortSmallShipBonusMass = new SimpleConfigInt(config, "escort_small_ship_bonus_mass", 5000, "Ship mass threshold below which the small ship bonus is applied.");
		escortSmallShipBonusMultiplier = new SimpleConfigDouble(config, "escort_small_ship_bonus_multiplier", 1.5, "Reward multiplier when the player's ship mass is below the small ship bonus threshold.");
		escortCargoLossPenalty = new SimpleConfigDouble(config, "escort_cargo_loss_penalty", 0.25, "Fraction of reward lost per cargo ship destroyed (e.g. 0.25 = 25% per ship).");
		escortUpdateInterval = new SimpleConfigInt(config, "escort_update_interval", 5000, "Time in milliseconds between escort state update ticks.");
		bountyDefaultReward = new SimpleConfigInt(config, "bounty_default_reward", 5, "Default Gold Bar amount pre-filled in the player bounty creation form.");
		bountyMinReward = new SimpleConfigInt(config, "bounty_min_reward", 1, "Minimum Gold Bars a player must put up to post a bounty.");
		escortEnabled = new SimpleConfigBool(config, "escort_enabled", false, "If true, escort contracts can be auto-generated and created by players. Disabled by default until trader/cargo blueprints are available.");
		itemsMinAmount = new SimpleConfigInt(config, "items_min_amount", 100, "Minimum item quantity requested by an auto-generated items contract.");
		itemsMaxAmount = new SimpleConfigInt(config, "items_max_amount", 1000, "Maximum item quantity requested by an auto-generated items contract.");
		itemsRewardDivisor = new SimpleConfigInt(config, "items_reward_divisor", 250, "Auto-generated items reward = max(items_reward_min, quantity / items_reward_divisor) Gold Bars.");
		itemsRewardMin = new SimpleConfigInt(config, "items_reward_min", 1, "Minimum Gold Bar reward for an auto-generated items contract.");

		config.readWriteFields();

		if(isDebugMode()) {
			instance.logInfo("Config initialized (mode=" + (config.isServer() ? "server" : "client") + ")");
		}
	}

	public static void reload() {
		if(config != null) config.readFields();
	}

	public static boolean isDebugMode() {
		return boolOrDefault(debugMode, false);
	}

	public static boolean isAutoGenerateContracts() {
		return boolOrDefault(autoGenerateContracts, true);
	}

	public static int getMaxAutoGenerateContracts() {
		return clampInt(intOrDefault(maxAutoGenerateContracts, 10), 1, 100);
	}

	public static long getAutoGenerateContractCheckTimer() {
		return clampInt(intOrDefault(autoGenerateContractCheckTimer, 3000), 1000, 60000);
	}

	public static long getContractTimerMax() {
		return clampInt(intOrDefault(contractTimerMax, 3600000), 60000, 86400000);
	}

	public static long getContractTimeoutTimer() {
		return clampInt(intOrDefault(contractTimeoutTimer, 900000), 60000, 86400000);
	}

	public static int getClientMaxActiveContracts() {
		return clampInt(intOrDefault(clientMaxActiveContracts, 5), 1, 50);
	}

	public static long getBlueprintUpdateInterval() {
		return clampInt(intOrDefault(blueprintUpdateInterval, 300000), 10000, 3600000);
	}

	public static int getMaxBountyMobCount() {
		return clampInt(intOrDefault(maxBountyMobCount, 12), 1, 50);
	}

	public static int getMaxBountyMobCombinedMass() {
		return clampInt(intOrDefault(maxBountyMobCombinedMass, 350000), 1000, 10000000);
	}

	public static boolean isAutoBountyEnabled() {
		return boolOrDefault(autoBountyEnabled, true);
	}

	public static int getAutoBountyKillThreshold() {
		return clampInt(intOrDefault(autoBountyKillThreshold, 3), 1, 100);
	}

	public static long getAutoBountyReward() {
		return Math.max(1, intOrDefault(autoBountyReward, 5));
	}

	public static long getAutoBountyDecayTimer() {
		return clampInt(intOrDefault(autoBountyDecayTimer, 10800000), 10000, 86400000);
	}

	public static String[] getEscortCargoBlueprintPool() {
		String val = stringOrDefault(escortCargoBlueprintPool, "");
		if(val.isEmpty()) return new String[0];
		return val.split(",");
	}

	public static String[] getEscortDefenderBlueprintPool() {
		String val = stringOrDefault(escortDefenderBlueprintPool, "");
		if(val.isEmpty()) return new String[0];
		return val.split(",");
	}

	public static int getEscortRouteMinLength() {
		return clampInt(intOrDefault(escortRouteMinLength, 3), 2, 20);
	}

	public static int getEscortRouteMaxLength() {
		return clampInt(intOrDefault(escortRouteMaxLength, 8), getEscortRouteMinLength(), 30);
	}

	public static int getEscortCargoCount() {
		return clampInt(intOrDefault(escortCargoCount, 3), 1, 10);
	}

	public static long getEscortBaseReward() {
		return Math.max(1, intOrDefault(escortBaseReward, 7));
	}

	public static long getEscortPirateWaveInterval() {
		return clampInt(intOrDefault(escortPirateWaveInterval, 60000), 10000, 600000);
	}

	public static int getEscortPirateMaxPerWave() {
		return clampInt(intOrDefault(escortPirateMaxPerWave, 4), 1, 20);
	}

	public static int getEscortSmallShipBonusMass() {
		return clampInt(intOrDefault(escortSmallShipBonusMass, 5000), 100, 1000000);
	}

	public static double getEscortSmallShipBonusMultiplier() {
		return Math.max(1.0, doubleOrDefault(escortSmallShipBonusMultiplier, 1.5));
	}

	public static double getEscortCargoLossPenalty() {
		double val = doubleOrDefault(escortCargoLossPenalty, 0.25);
		return Math.max(0.0, Math.min(1.0, val));
	}

	public static long getEscortUpdateInterval() {
		return clampInt(intOrDefault(escortUpdateInterval, 5000), 1000, 60000);
	}

	public static double getRewardBaseMultiplier() {
		return Math.max(0.01, doubleOrDefault(rewardBaseMultiplier, 1.0));
	}

	public static int getBountyDefaultReward() {
		return Math.max(getBountyMinReward(), intOrDefault(bountyDefaultReward, 5));
	}

	public static int getBountyMinReward() {
		return Math.max(1, intOrDefault(bountyMinReward, 1));
	}

	public static boolean isEscortEnabled() {
		return boolOrDefault(escortEnabled, false);
	}

	public static int getItemsMinAmount() {
		return Math.max(1, intOrDefault(itemsMinAmount, 100));
	}

	public static int getItemsMaxAmount() {
		return Math.max(getItemsMinAmount(), intOrDefault(itemsMaxAmount, 1000));
	}

	public static int getItemsRewardDivisor() {
		return Math.max(1, intOrDefault(itemsRewardDivisor, 250));
	}

	public static int getItemsRewardMin() {
		return Math.max(1, intOrDefault(itemsRewardMin, 1));
	}

	// --- helpers ---

	private static boolean boolOrDefault(SimpleConfigBool entry, boolean def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static int intOrDefault(SimpleConfigInt entry, int def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static String stringOrDefault(SimpleConfigString entry, String def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static double doubleOrDefault(SimpleConfigDouble entry, double def) {
		return (entry == null || entry.getValue() == null) ? def : entry.getValue();
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
