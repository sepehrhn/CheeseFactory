package dev.sepehrhn.cheesefactory.integration;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import org.bukkit.inventory.ItemStack;

public class CraftEngineItemProvider implements ExternalItemProvider {

    private final CheeseFactoryPlugin plugin;

    public CraftEngineItemProvider(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem(String itemId) {
        // TODO Integrate with CraftEngine API to fetch an item by id.
        plugin.getLogger().fine("CraftEngine integration placeholder for item '" + itemId + "'.");
        return null;
    }
}
