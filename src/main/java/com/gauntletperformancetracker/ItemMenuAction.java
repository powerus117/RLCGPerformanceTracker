package com.gauntletperformancetracker;

import lombok.AllArgsConstructor;
import net.runelite.api.Item;

/**
 * Data class that tracks all info related to a menu click action
 */
@AllArgsConstructor
public class ItemMenuAction
{
    public Item[] oldInventory;

    static class ItemAction extends ItemMenuAction
    {
        public final int itemID;
        public final int slot;

        ItemAction(final Item[] oldInventory, final int itemID, final int slot)
        {
            super(oldInventory);
            this.itemID = itemID;
            this.slot = slot;
        }
    }
}
