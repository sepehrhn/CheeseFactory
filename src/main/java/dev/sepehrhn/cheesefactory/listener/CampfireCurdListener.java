package dev.sepehrhn.cheesefactory.listener;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.item.CheeseItemManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CampfireCurdListener implements Listener {

    private final CheeseFactoryPlugin plugin;
    private final CheeseItemManager itemManager;

    public CampfireCurdListener(CheeseFactoryPlugin plugin, CheeseItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MILK_BUCKET) return;

        // If it is NOT inoculated milk, prevent it from being placed on the campfire
        // because we registered a generic MILK_BUCKET recipe which would otherwise accept it.
        if (!itemManager.isInoculatedMilk(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (event.getBlock().getType() != Material.CAMPFIRE && event.getBlock().getType() != Material.SOUL_CAMPFIRE) return;
        
        ItemStack source = event.getSource();
        ItemStack result = event.getResult();
        
        // Check if the result is our Curd
        if (itemManager.isCurd(result)) {
            // Drop an empty bucket
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.BUCKET));
        }
    }
}
