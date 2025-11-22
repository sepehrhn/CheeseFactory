package dev.sepehrhn.cheesefactory.integration;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import org.bukkit.inventory.ItemStack;

public class NexoItemProvider implements ExternalItemProvider {

    private final CheeseFactoryPlugin plugin;

    public NexoItemProvider(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem(String itemId) {
        // TODO Integrate with Nexo API to fetch an item by id.
        plugin.getLogger().fine("Nexo integration placeholder for item '" + itemId + "'.");
        return null;
    }
}
