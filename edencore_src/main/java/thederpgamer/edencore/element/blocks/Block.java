package thederpgamer.edencore.element.blocks;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import thederpgamer.edencore.EdenCore;

/**
 * Base abstract block class.
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public abstract class Block {
	protected ElementInformation blockInfo;

	public Block(String name, ElementCategory category) {
		blockInfo = BlockConfig.newElement(EdenCore.getInstance(), name, new short[6]);
		BlockConfig.setElementCategory(blockInfo, category);
	}

	public final ElementInformation getBlockInfo() {
		return blockInfo;
	}

	public final short getId() {
		return blockInfo.getId();
	}

	public abstract void initialize();
}
