package thederpgamer.edencore.element.items;

import org.schema.game.common.data.element.ElementCategory;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public interface ItemGroup {
	Item[] getItems();

	ElementCategory getCategory();

	String getMultiSlot();
}