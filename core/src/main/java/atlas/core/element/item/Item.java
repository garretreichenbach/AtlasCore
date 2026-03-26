package atlas.core.element.item;

import api.config.BlockConfig;
import api.mod.StarMod;
import atlas.core.AtlasCore;
import atlas.core.element.ElementInterface;
import org.schema.game.common.data.element.ElementInformation;

public abstract class Item implements ElementInterface {

	protected String name;
	protected ElementInformation itemInfo;
	private final StarMod ownerMod;

	/** Constructor that uses AtlasCore as the owning mod (for core-registered items). */
	protected Item(String name) {
		this(name, AtlasCore.getInstance());
	}

	/** Constructor for sub-mod items — pass the sub-mod's instance so BlockConfig uses the right mod ID. */
	protected Item(String name, StarMod ownerMod) {
		this.name = name;
		this.ownerMod = ownerMod;
	}

	@Override
	public void initData() {
		itemInfo = BlockConfig.newElement(ownerMod, name, new short[6]);
		itemInfo.placable = false; // Items are not placable in the world like blocks, so set this to false.
	}

	@Override
	public short getId() {
		return itemInfo.id;
	}

	@Override
	public ElementInformation getInfo() {
		return itemInfo;
	}
}