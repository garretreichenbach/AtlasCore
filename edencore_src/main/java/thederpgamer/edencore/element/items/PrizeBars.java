package thederpgamer.edencore.element.items;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.graphicsengine.core.GraphicsContext;
import thederpgamer.edencore.element.ElementManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/17/2021]
 */
public class PrizeBars implements ItemGroup {
	@Override
	public Item[] getItems() {
		return new Item[] {new BronzeBar(), new SilverBar(), new GoldBar()};
	}

	@Override
	public ElementCategory getCategory() {
		return ElementManager.getCategory("Items");
	}

	@Override
	public String getMultiSlot() {
		return "prizebars";
	}

	public class BronzeBar extends Item {
		public BronzeBar() {
			super("Bronze Bar", getCategory());
		}

		@Override
		public void initialize() {
			if(GraphicsContext.initialized) {
				itemInfo.setTextureId(ElementKeyMap.getInfo(341).getTextureIds());
				itemInfo.setBuildIconNum(ElementKeyMap.getInfo(341).getBuildIconNum());
			}
			itemInfo.setDescription("A rare bronze bar which can be redeemed for unique prizes at the server shop.");
			itemInfo.setInRecipe(false);
			itemInfo.setShoppable(false);
			itemInfo.setPlacable(false);
			itemInfo.setPhysical(false);
			itemInfo.volume = 0.05f;
			itemInfo.mass = 0.05f;
			BlockConfig.add(itemInfo);
		}
	}

	public class SilverBar extends Item {
		public SilverBar() {
			super("Silver Bar", getCategory());
		}

		@Override
		public void initialize() {
			if(GraphicsContext.initialized) {
				itemInfo.setTextureId(ElementKeyMap.getInfo(342).getTextureIds());
				itemInfo.setBuildIconNum(ElementKeyMap.getInfo(342).getBuildIconNum());
			}
			itemInfo.setDescription("An esteemed silver bar which can be redeemed for unique prizes at the server shop.");
			itemInfo.setInRecipe(false);
			itemInfo.setShoppable(false);
			itemInfo.setPlacable(false);
			itemInfo.setPhysical(false);
			itemInfo.volume = 0.05f;
			itemInfo.mass = 0.05f;
			BlockConfig.add(itemInfo);
		}
	}

	public class GoldBar extends Item {
		public GoldBar() {
			super("Gold Bar", getCategory());
		}

		@Override
		public void initialize() {
			if(GraphicsContext.initialized) {
				itemInfo.setTextureId(ElementKeyMap.getInfo(343).getTextureIds());
				itemInfo.setBuildIconNum(ElementKeyMap.getInfo(343).getBuildIconNum());
			}
			itemInfo.setDescription("An exquisite gold bar which can be redeemed for unique prizes at the server shop.");
			itemInfo.setInRecipe(false);
			itemInfo.setShoppable(false);
			itemInfo.setPlacable(false);
			itemInfo.setPhysical(false);
			itemInfo.volume = 0.05f;
			itemInfo.mass = 0.05f;
			BlockConfig.add(itemInfo);
		}
	}
}