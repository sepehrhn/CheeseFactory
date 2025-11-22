package dev.sepehrhn.cheesefactory.integration;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import org.bukkit.inventory.ItemStack;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.NexoBlocks;

public class NexoItemProvider implements ExternalItemProvider {

    private final CheeseFactoryPlugin plugin;

    public NexoItemProvider(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack createItem(String itemId) {
        var builder = NexoItems.itemFromId(itemId);
        if (builder == null) {
            plugin.getLogger().warning("Nexo item '" + itemId + "' not found or not loaded yet.");
            return null;
        }
        return builder.build();
    }

    @Override
    public void placeBlock(String itemId, org.bukkit.Location location) {
        try {
            NexoBlocks.place(itemId, location);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to place Nexo block '" + itemId + "' at location: " + e.getMessage());
        }
    }
}
