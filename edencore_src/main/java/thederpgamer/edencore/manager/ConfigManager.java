package thederpgamer.edencore.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.edencore.EdenCore;

import java.util.Arrays;

/**
 * Manages mod config files and values.
 *
 * @author TheDerpGamer
 * @version 1.1 - [10/08/2021]
 */
public class ConfigManager {

	private static final String[] defaultMainConfig = {
			"debug_mode: false",
			"build_sector_distance_offset: 1000000",
			"player_login_reward_timer: 900000",
			"tip_interval: 600000",
			"discord_invite_code: kcb84yRwHU",
	};
	private static final String[] defaultTipsConfig = {
			"Tip: You can access the in-game Guide by pressing the GUIDE button underneath the PLAYER dropdown in the top right corner of the screen or by using the /guide command.",
			"Tip: You can join the server's Discord by pressing the DISCORD button underneath the PLAYER dropdown in the top right corner of the screen or by using the /discord command.",
			"Tip: Make sure to read the server rules in the Guide menu!",
			"Tip: Have questions? Feel free to ask in our Discord server! You can find a link via the DISCORD button underneath the PLAYER dropdown in the top right corner of the screen.",
//			"Tip: You can build freely in your own private Build Sector. To access it, press the BUILD SECTOR button underneath the PLAYER dropdown in the top right corner of the screen.",
//			"Tip: You can buy and sell blueprints via the Exchange Menu. To access it, press the EXCHANGE button underneath the PLAYER dropdown in the top right corner of the screen.",
//			"Tip: You will receive a daily login reward for playing on the server. Make sure to claim it every day!",
			"Tip: Be sure to explore the galaxy to find new resources and materials. It's always a good idea to look around first before choosing a sector to be your home.",
			"Tip: Heavy Armor slows you down, but provides more protection and Armor HP. Be sure to balance your ship's armor and speed to fit your play style.",
//			"Tip: You can repair damaged or destroyed blocks by using an Astrotech Beam. It can even restore Armor HP!",
			"Tip: Feeling outnumbered? Try bringing some AI support ships to escort you on your next adventure.",
			"Tip: Auras can provide a variety of benefits to nearby friendly ships, and can even be used by the AI. Be sure to check out the different types of Auras available.",
			"Tip: Enemy Auras got you down? Try countering with Aura Disruptor Beams to disable them. They can even be used by the AI!",
			"Tip: Try using Chamber Configs to quickly switch between different chamber setups for your ship. It's a great way to adapt to different situations on the fly.",
			"Tip: You can use Shield Hardening chambers to provide additional resistance against specific weapon types.",
			"Tip: It's always a good idea to diversify your weapon types across your ships in order to adapt to different situations.",
			"Tip: Study your opponents carefully to learn their weaknesses and exploit them. Do they use a lot of beams? Try adding Shield Hardening chambers to your ships to reduce beam damage.",
			"Tip: Don't be afraid to experiment with different designs and tactics. You never know what might work until you try it!",
			"Tip: Defeat is a far better teacher than victory. Learn from your mistakes and use them to improve your skills.",
	};

	private static FileConfiguration mainConfig;
	private static FileConfiguration tipsConfig;

	public static void initialize(EdenCore instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
		tipsConfig = instance.getConfig("tips");
		if(tipsConfig.getList("tips").isEmpty()) {
			tipsConfig.set("tips", Arrays.asList(defaultTipsConfig));
			tipsConfig.saveConfig();
		}
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
	
	public static String getRandomTip() {
		return tipsConfig.getList("tips").get((int) (Math.random() * tipsConfig.getList("tips").size()));
	}
}
