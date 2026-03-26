package atlas.banking.element;

import api.config.BlockConfig;
import atlas.banking.AtlasBanking;
import atlas.banking.element.item.BronzeBar;
import atlas.banking.element.item.GoldBar;
import atlas.banking.element.item.SilverBar;
import atlas.core.element.ElementInterface;
import org.schema.game.common.data.element.ElementInformation;


/**
 * Central registry that defines and registers all mod blocks/chambers/weapons/etc. by implementing the ElementInterface and adding them to the enum.
 */
public enum ElementRegistry {
	BRONZE_BAR(new BronzeBar()),
	SILVER_BAR(new SilverBar()),
	GOLD_BAR(new GoldBar());

	public final ElementInterface elementInterface;

	ElementRegistry(ElementInterface elementInterface) {
		this.elementInterface = elementInterface;
	}

	public static void registerElements() {
		for(ElementRegistry registry : values()) {
			registry.elementInterface.initData();
		}
		AtlasBanking.getInstance().logInfo("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.postInitData();
		}
		AtlasBanking.getInstance().logInfo("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.initResources();
		}
		AtlasBanking.getInstance().logInfo("Initialized element resources for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			BlockConfig.add(registry.getInfo());
		}
		AtlasBanking.getInstance().logInfo("Initialized element resources for " + values().length + " elements");
	}

	private static ElementInformation getInfoByName(String name) {
		for(ElementInformation info : BlockConfig.getElements()) {
			if(info.getName().equals(name)) {
				return info;
			}
		}
		throw new IllegalStateException("Element with name '" + name + "' not found");
	}

	public ElementInformation getInfo() {
		return elementInterface.getInfo();
	}

	public short getId() {
		return getInfo().id;
	}
}