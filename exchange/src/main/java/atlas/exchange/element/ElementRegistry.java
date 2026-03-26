package atlas.exchange.element;

import api.config.BlockConfig;
import atlas.core.element.ElementInterface;
import atlas.exchange.AtlasExchange;
import atlas.exchange.element.item.BronzeBar;
import atlas.exchange.element.item.GoldBar;
import atlas.exchange.element.item.SilverBar;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Central registry that defines and registers all Exchange mod elements (items, blocks, etc.).
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
		AtlasExchange.getInstance().logInfo("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.postInitData();
		}
		AtlasExchange.getInstance().logInfo("Post-initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.initResources();
		}
		AtlasExchange.getInstance().logInfo("Initialized element resources for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			BlockConfig.add(registry.getInfo());
		}
		AtlasExchange.getInstance().logInfo("Registered " + values().length + " elements with BlockConfig");
	}

	public ElementInformation getInfo() {
		return elementInterface.getInfo();
	}

	public short getId() {
		return getInfo().id;
	}
}
