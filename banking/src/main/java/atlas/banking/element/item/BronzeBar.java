package atlas.banking.element.item;

import api.config.BlockConfig;
import atlas.banking.AtlasBanking;
import atlas.core.element.item.Item;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.graphicsengine.core.GraphicsContext;

public class BronzeBar extends Item {

	public BronzeBar() {
		super("Bronze Bar", AtlasBanking.getInstance());
	}

	@Override
	public void postInitData() {
		itemInfo.setDescription("A rare bronze bar which can be redeemed for unique prizes at the server shop.");
		itemInfo.setInRecipe(false);
		itemInfo.setShoppable(false);
		itemInfo.setPlacable(false);
		itemInfo.setPhysical(false);
		itemInfo.volume = 0.05f;
		itemInfo.mass = 0.05f;
		BlockConfig.add(itemInfo);
	}

	@Override
	public void initResources() {
		if(GraphicsContext.initialized) {
			itemInfo.setTextureId(ElementKeyMap.getInfo(341).getTextureIds());
			itemInfo.setBuildIconNum(ElementKeyMap.getInfo(341).getBuildIconNum());
		}
	}
}
