package atlas.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.schema.game.client.view.gui.catalog.newcatalog.CatalogPanelNew;

/**
 * Mixin for {@link CatalogPanelNew}.
 *
 * <p>This replaces the old {@code overwriteClass("CatalogPanelNew")} bytecode substitution
 * in EdenCore. The old overwrite added a custom catalog scrollable list tab
 * ({@code ECCatalogScrollableListNew}) to the catalog panel.
 *
 * <p>Add {@code @Inject} and {@code @Shadow} members here to inject the custom tab into
 * the {@code recreateTabs()} method rather than replacing the entire class.
 *
 * <p>Registered in {@code atlascore.mixins.json} under {@code "client"} (client-only).
 */
@Mixin(value = CatalogPanelNew.class, remap = false)
public abstract class MixinCatalogPanelNew {

    // TODO: Inject into recreateTabs() to add the ECCatalogScrollableListNew tab.
    // Reference the original overwrite at:
    // edencore_src/main/java/org/schema/game/client/view/gui/catalog/newcatalog/CatalogPanelNew.java

}
