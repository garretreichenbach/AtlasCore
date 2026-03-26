package atlas.core.element.items;

import atlas.core.element.item.Item;
import org.schema.game.common.data.element.ElementCategory;

/**
 * Interface for grouping related items under a shared {@link ElementCategory}.
 * Sub-mods implement this to describe a set of custom items and the category they belong to.
 *
 * @author TheDerpGamer
 */
public interface ItemGroup {

    /** Returns all items belonging to this group. */
    Item[] getItems();

    /** Returns the {@link ElementCategory} this group's items should be listed under. */
    ElementCategory getCategory();

    /** Returns a multi-slot identifier used to group items in the inventory. */
    String getMultiSlot();
}
