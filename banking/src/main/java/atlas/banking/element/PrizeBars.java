package atlas.banking.element;

import api.config.BlockConfig;
import atlas.banking.AtlasBanking;
import atlas.core.element.item.Item;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.graphicsengine.core.GraphicsContext;

/**
 * Prize bars (Bronze, Silver, Gold) used as the currency for exchange purchases.
 * Call {@link #register()} from {@code AtlasBanking.onBlockConfigLoad()}.
 *
 * @author TheDerpGamer
 */
public class PrizeBars {

    public static void register() {
        new BronzeBar().register();
        new SilverBar().register();
        new GoldBar().register();
    }

    // ── Inner items ───────────────────────────────────────────────────────────

    public static class BronzeBar extends Item {

        public BronzeBar() {
            super("Bronze Bar", AtlasBanking.getInstance());
        }

        /** Call once during block config load to fully initialize and add the item. */
        public void register() {
            initData();
            postInitData();
            initResources();
        }

        @Override
        public void postInitData() {
            itemInfo.setDescription("A rare bronze bar which can be redeemed for unique prizes at the server shop.");
            itemInfo.setInRecipe(false);
            itemInfo.setShoppable(false);
            itemInfo.setPlacable(false);
            itemInfo.setPhysical(false);
            itemInfo.volume = 0.05f;
            itemInfo.mass   = 0.05f;
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

    public static class SilverBar extends Item {

        public SilverBar() {
            super("Silver Bar", AtlasBanking.getInstance());
        }

        public void register() {
            initData();
            postInitData();
            initResources();
        }

        @Override
        public void postInitData() {
            itemInfo.setDescription("An esteemed silver bar which can be redeemed for unique prizes at the server shop.");
            itemInfo.setInRecipe(false);
            itemInfo.setShoppable(false);
            itemInfo.setPlacable(false);
            itemInfo.setPhysical(false);
            itemInfo.volume = 0.05f;
            itemInfo.mass   = 0.05f;
            BlockConfig.add(itemInfo);
        }

        @Override
        public void initResources() {
            if(GraphicsContext.initialized) {
                itemInfo.setTextureId(ElementKeyMap.getInfo(342).getTextureIds());
                itemInfo.setBuildIconNum(ElementKeyMap.getInfo(342).getBuildIconNum());
            }
        }
    }

    public static class GoldBar extends Item {

        public GoldBar() {
            super("Gold Bar", AtlasBanking.getInstance());
        }

        public void register() {
            initData();
            postInitData();
            initResources();
        }

        @Override
        public void postInitData() {
            itemInfo.setDescription("An exquisite gold bar which can be redeemed for unique prizes at the server shop.");
            itemInfo.setInRecipe(false);
            itemInfo.setShoppable(false);
            itemInfo.setPlacable(false);
            itemInfo.setPhysical(false);
            itemInfo.volume = 0.05f;
            itemInfo.mass   = 0.05f;
            BlockConfig.add(itemInfo);
        }

        @Override
        public void initResources() {
            if(GraphicsContext.initialized) {
                itemInfo.setTextureId(ElementKeyMap.getInfo(343).getTextureIds());
                itemInfo.setBuildIconNum(ElementKeyMap.getInfo(343).getBuildIconNum());
            }
        }
    }
}
