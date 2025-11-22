package dev.sepehrhn.cheesefactory.barrel;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CheeseBarrelState implements InventoryHolder {
    private final Location location;
    private final int[] progressTicks;
    private Inventory inventory;

    public CheeseBarrelState(Location location) {
        this.location = location;
        this.progressTicks = new int[27]; // Support up to 27 slots (vanilla barrel size), though we use 9
    }

    public Location getLocation() {
        return location;
    }

    public int getProgress(int slot) {
        if (slot < 0 || slot >= progressTicks.length) return 0;
        return progressTicks[slot];
    }

    public void resetProgress(int slot) {
        if (slot >= 0 && slot < progressTicks.length) {
            progressTicks[slot] = 0;
        }
    }

    public void addProgress(int slot, int amount) {
        if (slot >= 0 && slot < progressTicks.length) {
            progressTicks[slot] += amount;
        }
    }
    
    // Legacy support or for storage
    public int[] getAllProgress() {
        return progressTicks;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
    private org.bukkit.inventory.ItemStack[] savedItems;

    public void setSavedItems(org.bukkit.inventory.ItemStack[] items) {
        this.savedItems = items;
    }

    public org.bukkit.inventory.ItemStack[] getSavedItems() {
        return savedItems;
    }
}
