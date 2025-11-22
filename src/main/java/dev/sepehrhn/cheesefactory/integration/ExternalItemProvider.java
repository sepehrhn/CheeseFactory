package dev.sepehrhn.cheesefactory.integration;

import org.bukkit.inventory.ItemStack;

public interface ExternalItemProvider {
    /**
     * Attempt to create an ItemStack from an external plugin by id.
     * @param itemId external plugin item identifier
     * @return created ItemStack, or null if not available
     */
    ItemStack createItem(String itemId);
}
