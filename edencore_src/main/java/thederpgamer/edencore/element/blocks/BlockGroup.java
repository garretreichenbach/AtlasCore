package thederpgamer.edencore.element.blocks;

import org.schema.game.common.data.element.ElementCategory;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public interface BlockGroup {
	Block[] getBlocks();

	ElementCategory getCategory();

	String getMultiSlot();
}
