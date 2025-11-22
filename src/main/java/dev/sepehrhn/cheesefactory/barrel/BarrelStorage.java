package dev.sepehrhn.cheesefactory.barrel;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BarrelStorage {

    private final File file;
    private final Logger logger;

    public BarrelStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "barrels.yml");
        this.logger = logger;
    }

    public void save(Map<Location, CheeseBarrelState> barrels) {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();

        for (CheeseBarrelState state : barrels.values()) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("world", state.getLocation().getWorld().getName());
            data.put("x", state.getLocation().getBlockX());
            data.put("y", state.getLocation().getBlockY());
            data.put("z", state.getLocation().getBlockZ());
            
            // Save array as list
            List<Integer> progList = new ArrayList<>();
            for (int p : state.getAllProgress()) {
                progList.add(p);
            }
            data.put("progress", progList);
            
            if (state.getInventory() != null) {
                data.put("inventory", state.getInventory().getContents());
            }
            list.add(data);
        }

        config.set("barrels", list);
        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("Failed to save barrels.yml: " + e.getMessage());
        }
    }

    public List<CheeseBarrelState> load() {
        List<CheeseBarrelState> loaded = new ArrayList<>();
        if (!file.exists()) {
            return loaded;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var list = config.getMapList("barrels");

        for (Map<?, ?> map : list) {
            try {
                String worldName = (String) map.get("world");
                int x = (int) map.get("x");
                int y = (int) map.get("y");
                int z = (int) map.get("z");
                
                var world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }
                Location loc = new Location(world, x, y, z);
                CheeseBarrelState state = new CheeseBarrelState(loc);
                
                Object progressObj = map.get("progress");
                if (progressObj instanceof Integer legacyProgress) {
                    // Migration: Apply legacy progress to slot 0
                    state.addProgress(0, legacyProgress);
                } else if (progressObj instanceof List<?> progList) {
                    int slot = 0;
                    for (Object obj : progList) {
                        if (obj instanceof Number num) {
                            state.addProgress(slot, num.intValue());
                        }
                        slot++;
                    }
                }

                if (map.containsKey("inventory")) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> items = (List<ItemStack>) map.get("inventory");
                    state.setSavedItems(items.toArray(new ItemStack[0]));
                }
                loaded.add(state);
            } catch (Exception e) {
                logger.warning("Failed to load a barrel entry: " + e.getMessage());
            }
        }
        return loaded;
    }
}
