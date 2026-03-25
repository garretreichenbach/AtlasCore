package thederpgamer.edencore.element;

import api.config.BlockConfig;
import org.apache.commons.lang3.StringUtils;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import thederpgamer.edencore.element.blocks.Block;
import thederpgamer.edencore.element.blocks.BlockGroup;
import thederpgamer.edencore.element.items.Item;
import thederpgamer.edencore.element.items.ItemGroup;

import java.util.ArrayList;

/**
 * Manages mod blocks and items.
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public class ElementManager {
	private static final ArrayList<Block> blockList = new ArrayList<>();
	private static final ArrayList<Item> itemList = new ArrayList<>();

	public static void initialize() {
		for(Block block : blockList) block.initialize();
		for(Item item : itemList) item.initialize();
	}

	public static Block getBlock(short id) {
		for(Block blockElement : getAllBlocks()) if(blockElement.getBlockInfo().getId() == id) return blockElement;
		return null;
	}

	public static ArrayList<Block> getAllBlocks() {
		return blockList;
	}

	public static Block getBlock(SegmentPiece segmentPiece) {
		for(Block block : getAllBlocks()) if(block.getId() == segmentPiece.getType()) return block;
		return null;
	}

	public static Item getItem(short itemId) {
		for(Item item : getAllItems()) {
			if(item.getItemInfo().getId() == itemId) return item;
		}
		return null;
	}

	public static ArrayList<Item> getAllItems() {
		return itemList;
	}

	public static void addBlockGroup(BlockGroup blockGroup) {
		Block[] blocks = blockGroup.getBlocks();
		boolean multiSlot = blockGroup.getMultiSlot() != null && !blockGroup.getMultiSlot().isEmpty();
		for(Block block : blocks) {
			if(multiSlot) block.getBlockInfo().setInventoryGroup(blockGroup.getMultiSlot());
			addBlock(block);
		}
	}

	public static void addBlock(Block block) {
		blockList.add(block);
	}

	public static void addItemGroup(ItemGroup itemGroup) {
		Item[] items = itemGroup.getItems();
		boolean multiSlot = itemGroup.getMultiSlot() != null && !itemGroup.getMultiSlot().isEmpty();
		for(Item item : items) {
			if(multiSlot) item.getItemInfo().setInventoryGroup(itemGroup.getMultiSlot());
			addItem(item);
		}
	}

	public static void addItem(Item item) {
		itemList.add(item);
	}

	public static ElementCategory getCategory(String path) {
		String[] split = path.split("\\.");
		ElementCategory category = ElementKeyMap.getCategoryHirarchy();
		for(String s : split) {
			boolean createNew = false;
			if(category.hasChildren()) {
				for(ElementCategory child : category.getChildren()) {
					if(child.getCategory().equalsIgnoreCase(s)) {
						category = child;
						break;
					}
					createNew = true;
				}
			} else createNew = true;
			if(createNew) category = BlockConfig.newElementCategory(category, StringUtils.capitalize(s));
		}
		return category;
	}

	public static ElementInformation getInfo(String name) {
		Block block = getBlock(name);
		if(block != null) return block.getBlockInfo();
		else {
			Item item = getItem(name);
			if(item != null) return item.getItemInfo();
			else {
				for(ElementInformation elementInfo : ElementKeyMap.getInfoArray()) {
					if(elementInfo.getName().equalsIgnoreCase(name)) return elementInfo;
				}
			}
			return null;
		}
	}

	public static Block getBlock(String blockName) {
		for(Block block : getAllBlocks()) {
			if(block.getBlockInfo().getName().equalsIgnoreCase(blockName)) return block;
		}
		return null;
	}

	public static Item getItem(String itemName) {
		for(Item item : getAllItems()) {
			if(item.getItemInfo().getName().equalsIgnoreCase(itemName)) return item;
		}
		return null;
	}

	public enum FactoryType {NONE, CAPSULE_REFINERY, MICRO_ASSEMBLER, BASIC_FACTORY, STANDARD_FACTORY, ADVANCED_FACTORY}
}