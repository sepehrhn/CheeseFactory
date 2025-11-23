package dev.sepehrhn.cheesefactory.listener;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.barrel.BarrelItemService;
import dev.sepehrhn.cheesefactory.barrel.CheeseBarrelManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BarrelSetupListener implements Listener {

    private static final String DEBUG_PREFIX = "[CheeseDebug] ";

    private final CheeseFactoryPlugin plugin;
    private final CheeseBarrelManager barrelManager;
    private final BarrelItemService barrelItemService;

    public BarrelSetupListener(CheeseFactoryPlugin plugin, CheeseBarrelManager barrelManager, BarrelItemService barrelItemService) {
        this.plugin = plugin;
        this.barrelManager = barrelManager;
        this.barrelItemService = barrelItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBarrelPlace(BlockPlaceEvent event) {
        if (!barrelItemService.isBarrelItem(event.getItemInHand())) {
            return;
        }
        Block placed = event.getBlockPlaced();
        if (placed.getType().isAir()) {
            return;
        }
        boolean success = barrelManager.registerBarrel(placed);
        if (success) {
            logDebug("Registered placed cheese barrel at " + formatBlock(placed) + ".");
        } else {
            logDebug("Cheese barrel at " + formatBlock(placed) + " was already registered.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBarrelBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType().isAir()) {
            return;
        }

        if (barrelManager.isRegistered(block)) {
            event.setDropItems(false);
            barrelManager.removeBarrel(block, true);
            logDebug("Removed cheese barrel at " + formatBlock(block) + " and dropped configured barrel item.");
        }
    }

    private void logDebug(String message) {
        plugin.getLogger().info(DEBUG_PREFIX + message);
    }

    private String formatLoc(org.bukkit.Location loc) {
        return loc.getWorld().getName() + " (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")";
    }

    private String formatBlock(Block block) {
        var loc = block.getLocation();
        return block.getType() + " at (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")";
    }
}

