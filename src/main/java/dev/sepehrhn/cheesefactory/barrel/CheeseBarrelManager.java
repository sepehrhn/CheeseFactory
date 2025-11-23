package dev.sepehrhn.cheesefactory.barrel;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.cheese.CheeseRegistry;
import dev.sepehrhn.cheesefactory.config.ConfigUtil;
import dev.sepehrhn.cheesefactory.item.CheeseItemManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheeseBarrelManager {

    private final CheeseFactoryPlugin plugin;
    private final CheeseItemManager itemManager;
    private final CheeseRegistry cheeseRegistry;
    private final BarrelItemService barrelItemService;
    private final Map<Location, CheeseBarrelState> barrels = new ConcurrentHashMap<>();
    private final Map<Location, ScheduledTask> barrelTasks = new ConcurrentHashMap<>();
    private final BarrelStorage storage;

    private int fermentationTicks;
    private int tickInterval;
    
    // Effects Config
    private boolean particlesEnabled;
    private Particle particleType;
    private java.util.List<Color> particleColors;
    private int particleCount;
    private double particleOffsetY;
    
    private boolean soundsEnabled;
    private Sound soundType;
    private float soundVolume;
    private float soundPitch;
    private double soundChance;

    // Barrel GUI Sounds
    private Sound barrelOpenSound;
    private Sound barrelCloseSound;
    private float barrelSoundVolume;
    private float barrelSoundPitch;

    public CheeseBarrelManager(CheeseFactoryPlugin plugin, CheeseItemManager itemManager, CheeseRegistry cheeseRegistry, BarrelItemService barrelItemService) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cheeseRegistry = cheeseRegistry;
        this.barrelItemService = barrelItemService;
        this.storage = new BarrelStorage(plugin.getDataFolder(), plugin.getLogger());
        reload();
    }

    public void reload() {
        var config = plugin.getConfig();
        fermentationTicks = ConfigUtil.getInt(config, plugin.getLogger(), "fermentation.time_ticks", "fermentation.time-ticks", 9600);
        tickInterval = Math.max(1, ConfigUtil.getInt(config, plugin.getLogger(), "fermentation.tick_interval", "fermentation.tick-interval", 20));
        
        // Load Effects
        particlesEnabled = config.getBoolean("fermentation.effects.particles.enabled", true);
        String pTypeStr = config.getString("fermentation.effects.particles.type", "DUST");
        try {
            particleType = Particle.valueOf(pTypeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type '" + pTypeStr + "'; defaulting to DUST.");
            particleType = Particle.DUST;
        }
        
        particleColors = new java.util.ArrayList<>();
        for (String hex : config.getStringList("fermentation.effects.particles.colors")) {
            try {
                if (hex.startsWith("#")) hex = hex.substring(1);
                int rgb = Integer.parseInt(hex, 16);
                particleColors.add(Color.fromRGB(rgb));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid color hex '" + hex + "'; ignoring.");
            }
        }
        if (particleColors.isEmpty()) {
            particleColors.add(Color.YELLOW);
            particleColors.add(Color.fromRGB(255, 215, 0)); // Gold
        }
        
        particleCount = config.getInt("fermentation.effects.particles.count", 3);
        particleOffsetY = config.getDouble("fermentation.effects.particles.offset_y", 1.1);
        
        soundsEnabled = config.getBoolean("fermentation.effects.sounds.enabled", true);
        String sTypeStr = config.getString("fermentation.effects.sounds.sound", "BLOCK_BREWING_STAND_BREW");
        try {
            soundType = Sound.valueOf(sTypeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type '" + sTypeStr + "'; defaulting to BLOCK_BREWING_STAND_BREW.");
            soundType = Sound.BLOCK_BREWING_STAND_BREW;
        }
        soundVolume = (float) config.getDouble("fermentation.effects.sounds.volume", 0.3);
        soundPitch = (float) config.getDouble("fermentation.effects.sounds.pitch", 0.5);
        soundChance = config.getDouble("fermentation.effects.sounds.chance", 0.2);

        // Load Barrel GUI Sounds
        String openSoundStr = config.getString("cheese_barrel.sounds.open", "BLOCK_BARREL_OPEN");
        try {
            barrelOpenSound = Sound.valueOf(openSoundStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound '" + openSoundStr + "'; defaulting to BLOCK_BARREL_OPEN.");
            barrelOpenSound = Sound.BLOCK_BARREL_OPEN;
        }

        String closeSoundStr = config.getString("cheese_barrel.sounds.close", "BLOCK_BARREL_CLOSE");
        try {
            barrelCloseSound = Sound.valueOf(closeSoundStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound '" + closeSoundStr + "'; defaulting to BLOCK_BARREL_CLOSE.");
            barrelCloseSound = Sound.BLOCK_BARREL_CLOSE;
        }

        barrelSoundVolume = (float) config.getDouble("cheese_barrel.sounds.volume", 1.0);
        barrelSoundPitch = (float) config.getDouble("cheese_barrel.sounds.pitch", 1.0);
        
        // Load saved barrels
        var loaded = storage.load();
        for (var state : loaded) {
            barrels.put(state.getLocation(), state);
        }
        
        rescheduleAll();
    }

    public void shutdown() {
        storage.save(barrels);
        barrelTasks.values().forEach(ScheduledTask::cancel);
        barrelTasks.clear();
        barrels.clear();
    }

    public boolean isRegistered(Block block) {
        return barrels.containsKey(block.getLocation().toBlockLocation());
    }

    public CheeseBarrelState getState(Block block) {
        return barrels.get(block.getLocation().toBlockLocation());
    }

    public boolean registerBarrel(Block block) {
        if (block.getType().isAir()) {
            return false;
        }
        Location loc = block.getLocation().toBlockLocation();
        plugin.getLogger().info("[CheeseDebug] registerBarrel called for " + block.getType() + " at ("
                + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ")");
        boolean isNew = !barrels.containsKey(loc.toBlockLocation());
        CheeseBarrelState state = barrels.computeIfAbsent(loc, this::createState);
        scheduleBarrelTask(loc, state);
        if (block.getState() instanceof Barrel barrel) {
            barrel.setCustomName(getBarrelTitle());
            barrel.update();
        }
        return isNew;
    }

    public void removeBarrel(Block block, boolean dropBarrelItem) {
        Location loc = block.getLocation().toBlockLocation();
        cancelTask(loc);
        var state = barrels.remove(loc);
        if (state != null) {
            dropInventoryContents(state);
        }
        if (dropBarrelItem && block.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            dropBarrelItem(loc.clone().add(0.5, 0.5, 0.5));
        }
    }

    public void openBarrel(Player player, Block block) {
        CheeseBarrelState state = getState(block);
        if (state != null && state.getInventory() != null) {
            playSound(block.getLocation(), barrelOpenSound);
            setBarrelOpen(block, true);
            player.openInventory(state.getInventory());
        }
    }

    public void closeBarrel(Player player, Block block) {
        if (block != null && !block.getType().isAir()) {
            playSound(block.getLocation(), barrelCloseSound);
            setBarrelOpen(block, false);
        }
    }

    private void setBarrelOpen(Block block, boolean open) {
        if (barrelItemService.isNexoBarrel()) {
            barrelItemService.placeBarrel(block.getLocation(), open);
        } else if (block.getBlockData() instanceof org.bukkit.block.data.type.Barrel barrelData) {
            barrelData.setOpen(open);
            block.setBlockData(barrelData);
        }
    }

    private void playSound(Location location, Sound sound) {
        if (sound != null && location.getWorld() != null) {
            location.getWorld().playSound(location, sound, barrelSoundVolume, barrelSoundPitch);
        }
    }

    private void tickBarrel(CheeseBarrelState state) {
        Location loc = state.getLocation();
        var world = loc.getWorld();
        if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        Block block = world.getBlockAt(loc);
        if (block.getType().isAir()) {
            dropBarrelItem(loc.clone().add(0.5, 0.5, 0.5));
            dropInventoryContents(state);
            removeState(loc);
            return;
        }

        Inventory inv = state.getInventory();
        if (inv == null) {
            inv = createInventory(loc, state);
        }

        boolean anyFermenting = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (itemManager.isCurd(slot)) {
                state.addProgress(i, tickInterval);
                anyFermenting = true;
                if (state.getProgress(i) >= fermentationTicks) {
                    state.resetProgress(i);
                    produceCheese(inv, i, block.getLocation());
                }
            } else {
                state.resetProgress(i);
            }
        }
        
        if (anyFermenting) {
            if (particlesEnabled) {
                spawnIndicator(block);
            }
            if (soundsEnabled && Math.random() < soundChance) {
                world.playSound(loc.clone().add(0.5, 0.5, 0.5), soundType, soundVolume, soundPitch);
            }
        }
    }

    private CheeseBarrelState createState(Location loc) {
        CheeseBarrelState state = new CheeseBarrelState(loc);
        createInventory(loc, state);
        return state;
    }

    private Inventory createInventory(Location loc, CheeseBarrelState state) {
        Inventory inv = Bukkit.createInventory(state, 9, getBarrelTitle());
        if (state.getSavedItems() != null) {
            inv.setContents(state.getSavedItems());
            state.setSavedItems(null); // Clear after restoring
        }
        state.setInventory(inv);
        return inv;
    }

    private void scheduleBarrelTask(Location loc, CheeseBarrelState state) {
        cancelTask(loc);
        ScheduledTask task = Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc, scheduledTask -> tickBarrel(state), tickInterval, tickInterval);
        barrelTasks.put(loc, task);
    }

    private void rescheduleAll() {
        for (var entry : barrels.entrySet()) {
            scheduleBarrelTask(entry.getKey(), entry.getValue());
        }
    }

    private void cancelTask(Location loc) {
        ScheduledTask task = barrelTasks.remove(loc);
        if (task != null) {
            task.cancel();
        }
    }

    private void removeState(Location loc) {
        cancelTask(loc);
        barrels.remove(loc);
    }

    private void spawnIndicator(Block block) {
        var world = block.getWorld();
        Location loc = block.getLocation().add(0.5, particleOffsetY, 0.5);
        
        if (particleType == Particle.DUST) {
            for (int i = 0; i < particleCount; i++) {
                Color color = particleColors.get((int) (Math.random() * particleColors.size()));
                Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
                // Spread slightly
                double offsetX = (Math.random() - 0.5) * 0.5;
                double offsetZ = (Math.random() - 0.5) * 0.5;
                world.spawnParticle(Particle.DUST, loc.clone().add(offsetX, 0, offsetZ), 1, dust);
            }
        } else {
            world.spawnParticle(particleType, loc, particleCount, 0.3, 0.2, 0.3, 0.1);
        }
    }

    private void produceCheese(Inventory inv, int slot, Location location) {
        ItemStack current = inv.getItem(slot);
        if (current != null) {
            current.setAmount(current.getAmount() - 1);
            inv.setItem(slot, current); // Update decremented stack
        }

        var chosen = cheeseRegistry.randomCheese();
        if (chosen.isEmpty()) {
            plugin.getLogger().warning("Fermentation finished but no cheeses are loaded; skipping cheese output.");
        } else {
            ItemStack cheese = cheeseRegistry.createItem(chosen.get());
            var leftovers = inv.addItem(cheese);
            dropLeftovers(leftovers, location);
        }

        location.getWorld().spawnParticle(Particle.LANDING_HONEY, location.clone().add(0.5, 0.25, 0.5), 20, 0.5, 0.5, 0.5, 0.01);
    }

    private void dropInventoryContents(CheeseBarrelState state) {
        Inventory inv = state.getInventory();
        if (inv == null) {
            return;
        }
        Location loc = state.getLocation();
        var world = loc.getWorld();
        if (world == null || !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }
        for (ItemStack content : inv.getContents()) {
            if (content == null || content.getType() == Material.AIR) {
                continue;
            }
            world.dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), content);
        }
        inv.clear();
    }

    private void dropLeftovers(Map<Integer, ItemStack> leftovers, Location location) {
        var world = location.getWorld();
        if (world == null) {
            return;
        }
        leftovers.values().forEach(item -> world.dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), item));
    }

    private String getBarrelTitle() {
        return plugin.getLocaleManager().getDefaultMessage("barrel.title");
    }

    private void dropBarrelItem(Location dropLoc) {
        ItemStack drop = barrelItemService.createBarrelItem();
        dropLoc.getWorld().dropItemNaturally(dropLoc, drop);
    }
}
