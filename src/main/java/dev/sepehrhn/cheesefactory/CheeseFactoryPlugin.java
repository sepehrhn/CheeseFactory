package dev.sepehrhn.cheesefactory;

import dev.sepehrhn.cheesefactory.barrel.CheeseBarrelManager;
import dev.sepehrhn.cheesefactory.cheese.CheeseRegistry;
import dev.sepehrhn.cheesefactory.command.CheeseFactoryCommand;
import dev.sepehrhn.cheesefactory.item.CheeseItemManager;
import dev.sepehrhn.cheesefactory.listener.BarrelGuiListener;
import dev.sepehrhn.cheesefactory.listener.BarrelSetupListener;
import dev.sepehrhn.cheesefactory.listener.CheeseConsumeListener;
import dev.sepehrhn.cheesefactory.listener.CheeseConsumeListener;
import dev.sepehrhn.cheesefactory.listener.CampfireCurdListener;
import dev.sepehrhn.cheesefactory.listener.InoculatedMilkCraftListener;
import dev.sepehrhn.cheesefactory.locale.LocaleManager;
import dev.sepehrhn.cheesefactory.barrel.BarrelItemService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class CheeseFactoryPlugin extends JavaPlugin {

    private CheeseItemManager itemManager;
    private CheeseRegistry cheeseRegistry;
    private CheeseBarrelManager barrelManager;
    private BarrelItemService barrelItemService;
    private LocaleManager localeManager;
    private boolean cheeseBarrelDebugEnabled = false;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            updateYamlResource("config.yml", new File(getDataFolder(), "config.yml"), "config_version");
            ensureDefaultLocale();
            updateYamlResource("locale/en_US.yml", new File(getDataFolder(), "locale/en_US.yml"), "locale_version");
            updateYamlResource("cheese.yml", new File(getDataFolder(), "cheese.yml"), "cheese_version");
            ensureRoseLootExamples();
            this.localeManager = new LocaleManager(this);
            this.localeManager.reloadLocales();

            this.itemManager = new CheeseItemManager(this);
            this.cheeseRegistry = new CheeseRegistry(this, itemManager.keys(), itemManager);
            this.cheeseRegistry.reload();
            this.barrelItemService = new BarrelItemService(this, itemManager.keys());
            this.barrelItemService.reload();
            this.barrelManager = new CheeseBarrelManager(this, itemManager, cheeseRegistry, barrelItemService);

            registerListeners();
            registerCommands();
            announceIntegrations();

            getLogger().info("CheeseFactory enabled.");
        } catch (Throwable t) {
            getLogger().severe("Failed to enable CheeseFactory: " + t.getMessage());
            t.printStackTrace();
            throw t; // Re-throw to ensure it still disables, but we get the log first
        }
    }

    @Override
    public void onDisable() {
        if (barrelManager != null) {
            barrelManager.shutdown();
        }
        getLogger().info("CheeseFactory disabled.");
    }

    public CheeseItemManager getItemManager() {
        return itemManager;
    }

    public CheeseBarrelManager getBarrelManager() {
        return barrelManager;
    }

    public BarrelItemService getBarrelItemService() {
        return barrelItemService;
    }

    public LocaleManager getLocaleManager() {
        return localeManager;
    }

    public boolean isCheeseBarrelDebugEnabled() {
        return cheeseBarrelDebugEnabled;
    }

    public boolean toggleCheeseBarrelDebug() {
        cheeseBarrelDebugEnabled = !cheeseBarrelDebugEnabled;
        return cheeseBarrelDebugEnabled;
    }

    public void reloadCheeseConfig() {
        saveDefaultConfig();
        updateYamlResource("config.yml", new File(getDataFolder(), "config.yml"), "config_version");
        ensureDefaultLocale();
        updateYamlResource("locale/en_US.yml", new File(getDataFolder(), "locale/en_US.yml"), "locale_version");
        updateYamlResource("cheese.yml", new File(getDataFolder(), "cheese.yml"), "cheese_version");
        reloadConfig();
        itemManager.reload();
        cheeseRegistry.reload();
        barrelItemService.reload();
        barrelManager.reload();
        localeManager.reloadLocales();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CampfireCurdListener(this, itemManager), this);
        Bukkit.getPluginManager().registerEvents(new BarrelSetupListener(this, barrelManager, barrelItemService), this);
        Bukkit.getPluginManager().registerEvents(new BarrelGuiListener(barrelManager), this);
        Bukkit.getPluginManager().registerEvents(new CheeseConsumeListener(itemManager.keys(), cheeseRegistry), this);
        Bukkit.getPluginManager().registerEvents(new InoculatedMilkCraftListener(this, itemManager), this);
        getLogger().info("Registered cheese barrel listeners for placement and breaking.");
    }

    private void announceIntegrations() {
        var roseloot = getServer().getPluginManager().getPlugin("RoseLoot");
        if (roseloot != null) {
            getLogger().info("[CheeseFactory] RoseLoot detected. Configure bacteria drops via RoseLoot BLOCK loot tables.");
        } else {
            getLogger().warning("[CheeseFactory] RoseLoot is not installed. Bacteria will not drop from blocks; use /cf give bacteria instead.");
        }
        var tbp = getServer().getPluginManager().getPlugin("TheBrewingProject");
        if (tbp != null) {
            getLogger().info("TheBrewingProject detected. CheeseFactory will isolate its cauldron interactions for curd creation.");
        }
    }

    private void registerCommands() {
        var executor = new CheeseFactoryCommand(this);
        var primary = getCommand("cheesefactory");
        var cf = getCommand("cf");
        var cheese = getCommand("cheese");

        if (primary != null) {
            primary.setExecutor(executor);
            primary.setTabCompleter(executor);
        }
        if (cf != null) {
            cf.setExecutor(executor);
            cf.setTabCompleter(executor);
        }
        if (cheese != null) {
            cheese.setExecutor(executor);
            cheese.setTabCompleter(executor);
        }
        
        registerCampfireRecipe();
    }

    private void registerCampfireRecipe() {
        var key = new org.bukkit.NamespacedKey(this, "curd_campfire");
        getServer().removeRecipe(key);
        
        int cookingTime = dev.sepehrhn.cheesefactory.config.ConfigUtil.getInt(getConfig(), getLogger(), "inoculated_milk.cooking_time_ticks", null, 600);
        var recipe = new org.bukkit.inventory.CampfireRecipe(key, itemManager.createCurdItem(), org.bukkit.Material.MILK_BUCKET, 0.35f, cookingTime);
        getServer().addRecipe(recipe);
    }

    private void ensureDefaultLocale() {
        File localeDir = new File(getDataFolder(), "locale");
        if (!localeDir.exists() && !localeDir.mkdirs()) {
            getLogger().warning("Unable to create locale folder at " + localeDir.getAbsolutePath());
        }
        File defaultLocale = new File(localeDir, "en_US.yml");
        if (!defaultLocale.exists()) {
            saveResource("locale/en_US.yml", false);
        }
    }

    private void ensureRoseLootExamples() {
        File examplesDir = new File(getDataFolder(), "roseloot_examples");
        if (!examplesDir.exists() && !examplesDir.mkdirs()) {
            getLogger().warning("Unable to create roseloot_examples folder at " + examplesDir.getAbsolutePath());
        }
        File newExample = new File(examplesDir, "bacteria_roseloot.yml");
        File oldExample = new File(examplesDir, "bacteria_podzol.yml");
        if (oldExample.exists() && !newExample.exists()) {
            try {
                Files.move(oldExample.toPath(), newExample.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("Renamed legacy roseloot example to bacteria_roseloot.yml.");
            } catch (IOException ex) {
                getLogger().warning("Failed to rename old roseloot example: " + ex.getMessage());
            }
        }
        if (!newExample.exists()) {
            saveResource("roseloot_examples/bacteria_roseloot.yml", false);
        }
    }

    private void updateYamlResource(String resourcePath, File targetFile, String versionKey) {
        if (targetFile == null) {
            return;
        }
        if (!targetFile.exists()) {
            saveResource(resourcePath, false);
        }
        var resourceStream = getResource(resourcePath);
        if (resourceStream == null) {
            return;
        }
        var existing = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(targetFile);
        var defaults = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
        int existingVersion = existing.getInt(versionKey, 0);
        int defaultVersion = defaults.getInt(versionKey, 0);
        if (existingVersion < defaultVersion) {
            for (String key : defaults.getKeys(true)) {
                if (!existing.isSet(key)) {
                    existing.set(key, defaults.get(key));
                }
            }
            existing.set(versionKey, defaultVersion);
            try {
                existing.save(targetFile);
            } catch (IOException e) {
                getLogger().warning("Failed to update " + targetFile.getName() + ": " + e.getMessage());
            }
        }
    }
}
