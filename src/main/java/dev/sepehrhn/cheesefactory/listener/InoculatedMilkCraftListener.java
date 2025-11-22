package dev.sepehrhn.cheesefactory.listener;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.item.CheeseItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.Recipe;

public class InoculatedMilkCraftListener implements Listener {

    private final CheeseFactoryPlugin plugin;
    private final CheeseItemManager itemManager;
    private final NamespacedKey recipeKey;

    public InoculatedMilkCraftListener(CheeseFactoryPlugin plugin, CheeseItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipeKey = new NamespacedKey(plugin, "inoculated_milk_recipe");
    }

    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapelessRecipe shapeless) || !shapeless.getKey().equals(recipeKey)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        
        for (ItemStack item : matrix) {
            if (itemManager.isInoculatedMilk(item)) {
                inventory.setResult(new ItemStack(Material.AIR));
                return;
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapelessRecipe shapeless) || !shapeless.getKey().equals(recipeKey)) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item != null && item.getType() == Material.MILK_BUCKET) {
                // We cannot remove the item here because it breaks the recipe match and prevents
                // other ingredients (like bacteria) from being consumed.
                // Instead, we wait for the craft to complete (which turns Milk Bucket into Bucket)
                // and then remove the empty bucket.
                final int slotIndex = i;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ItemStack[] currentMatrix = inventory.getMatrix();
                    // Check if the slot now contains a BUCKET (the empty bucket left behind)
                    if (slotIndex < currentMatrix.length) {
                        ItemStack currentItem = currentMatrix[slotIndex];
                        if (currentItem != null && currentItem.getType() == Material.BUCKET) {
                            currentMatrix[slotIndex] = null;
                            inventory.setMatrix(currentMatrix);
                        }
                    }
                });
                break; // Only one milk bucket expected
            }
        }
    }
}
