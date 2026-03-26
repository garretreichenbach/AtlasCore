package atlas.core.element;

import api.config.BlockConfig;
import atlas.core.AtlasCore;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Central registry that defines and registers all AtlasCore mod elements (items, blocks, etc.).
 * Add new core elements as enum constants here.
 */
public enum ElementRegistry {
	/* No core-level elements yet. Add constants here as needed. */;

	public final ElementInterface elementInterface;

	ElementRegistry(ElementInterface elementInterface) {
		this.elementInterface = elementInterface;
	}

	public static void registerElements() {
		for(ElementRegistry registry : values()) {
			registry.elementInterface.initData();
		}
		for(ElementRegistry registry : values()) {
			registry.elementInterface.postInitData();
		}
		for(ElementRegistry registry : values()) {
			registry.elementInterface.initResources();
		}
		for(ElementRegistry registry : values()) {
			BlockConfig.add(registry.getInfo());
		}
		if(values().length > 0) {
			AtlasCore.getInstance().logInfo("Registered " + values().length + " core elements.");
		}
	}

	public ElementInformation getInfo() {
		return elementInterface.getInfo();
	}

	public short getId() {
		return getInfo().id;
	}
}
