package atlas.core.element;

import api.config.BlockConfig;
import atlas.core.AtlasCore;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Central registry that defines and registers all mod blocks/chambers/weapons/etc. by implementing the ElementInterface and adding them to the enum.
 */
public enum ElementRegistry {
	;

	public final ElementInterface elementInterface;

	ElementRegistry(ElementInterface elementInterface) {
		this.elementInterface = elementInterface;
	}

	public static void registerElements() {
		for(ElementRegistry registry : values()) {
			registry.elementInterface.initData();
		}
		AtlasCore.getInstance().logDebug("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.postInitData();
		}
		AtlasCore.getInstance().logDebug("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.initResources();
		}
		AtlasCore.getInstance().logDebug("Initialized element resources for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			BlockConfig.add(registry.getInfo());
		}
		AtlasCore.getInstance().logDebug("Initialized element resources for " + values().length + " elements");
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