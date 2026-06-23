package atlas.contracts.utils;

/**
 * Generates flavorful spawn and group names for contract targets.
 */
public class FlavorUtils {

	public static String generateSpawnName(FlavorType flavorType) {
		String adjectiveName = flavorType.adjectiveNames[(int) (Math.random() * flavorType.adjectiveNames.length)];
		String name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
		return flavorType.prefix + "-" + adjectiveName + " " + name;
	}

	public static String generateGroupName(FlavorType flavorType) {
		String adjectiveName = flavorType.adjectiveNames[(int) (Math.random() * flavorType.adjectiveNames.length)];
		String name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
		return "The " + adjectiveName + " " + name + "s";
	}

	public enum FlavorType {
		TRADERS("TGS", new String[]{
				"Profitable", "Mercantile", "Trading", "Commercial", "Business", "Economic", "Financial", "Monetary", "Capital", "Market", "Retail", "Wholesale", "Distribution", "Profiteering"
		}, new String[]{
				"Trader", "Merchant", "Dealer", "Broker", "Peddler", "Hawker", "Salesman", "Supplier", "Wholesaler", "Retailer", "Distributor", "Profiteer", "Vendor", "Marketer"
		}),
		MERCENARIES("MCS", new String[]{
				"Hired", "Veteran", "Hardened", "Elite", "Seasoned"
		}, new String[]{
				"Mercenary", "Gun", "Soldier", "Hunter", "Blade"
		}),
		PIRATE("PRS", new String[]{
				"Crimson", "Hardy", "Marauding", "Raiding", "Ruthless", "Savage", "Scourge", "Vicious", "Black", "Bloody", "Cutthroat", "Dread", "Fearsome", "Fierce", "Grim", "Merciless", "Red", "Sinister", "Vile", "Wicked", "Sly"
		}, new String[]{
				"Pirate", "Buccaneer", "Corsair", "Freebooter", "Privateer", "Raider", "Reaver", "Reaper", "Dog", "Swashbuckler", "Hook", "Hand", "Cannon"
		});

		public final String prefix;
		public final String[] adjectiveNames;
		public final String[] names;

		FlavorType(String prefix, String[] adjectiveNames, String[] names) {
			this.prefix = prefix;
			this.adjectiveNames = adjectiveNames;
			this.names = names;
		}
	}
}
