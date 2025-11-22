package dev.sepehrhn.cheesefactory.listener;

import dev.sepehrhn.cheesefactory.barrel.CheeseBarrelManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BarrelGuiListener implements Listener {

    private final CheeseBarrelManager barrelManager;

    public BarrelGuiListener(CheeseBarrelManager barrelManager) {
        this.barrelManager = barrelManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBarrelInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null || block.getType().isAir()) {
            return;
        }
        if (!barrelManager.isRegistered(block)) {
            return;
        }

        event.setCancelled(true);
        barrelManager.openBarrel(event.getPlayer(), block);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onBarrelClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        // Check if this is a cheese barrel inventory
        if (!(event.getInventory().getHolder() instanceof dev.sepehrhn.cheesefactory.barrel.CheeseBarrelState)) {
            return;
        }

        // Get the block location from the barrel state
        var state = (dev.sepehrhn.cheesefactory.barrel.CheeseBarrelState) event.getInventory().getHolder();
        var location = state.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        var block = location.getWorld().getBlockAt(location);
        if (event.getPlayer() instanceof org.bukkit.entity.Player player) {
            barrelManager.closeBarrel(player, block);
        }
    }
}
