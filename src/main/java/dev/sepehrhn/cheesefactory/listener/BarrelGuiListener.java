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
        if (block == null || block.getType() != Material.BARREL) {
            return;
        }
        if (!barrelManager.isRegistered(block)) {
            return;
        }

        event.setCancelled(true);
        barrelManager.openBarrel(event.getPlayer(), block);
    }
}
