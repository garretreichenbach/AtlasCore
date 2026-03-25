package thederpgamer.edencore.element.items;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import thederpgamer.edencore.EdenCore;

/**
 * Base abstract item class.
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public abstract class Item {
	protected ElementInformation itemInfo;

	public Item(String name, ElementCategory category) {
		itemInfo = BlockConfig.newElement(EdenCore.getInstance(), name, new short[6]);
		itemInfo.setPlacable(false);
		itemInfo.setPhysical(false);
		BlockConfig.setElementCategory(itemInfo, category);
	}

	public final ElementInformation getItemInfo() {
		return itemInfo;
	}

	public final short getId() {
		return itemInfo.getId();
	}

	public abstract void initialize();
}
