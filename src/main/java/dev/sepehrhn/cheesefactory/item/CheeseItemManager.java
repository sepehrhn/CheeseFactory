package dev.sepehrhn.cheesefactory.item;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.cheese.CustomIdDefinition;
import dev.sepehrhn.cheesefactory.config.ConfigUtil;
import dev.sepehrhn.cheesefactory.integration.CraftEngineItemProvider;
import dev.sepehrhn.cheesefactory.integration.ExternalItemProvider;
import dev.sepehrhn.cheesefactory.integration.NexoItemProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

public class CheeseItemManager {

    private static final CustomIdDefinition DEFAULT_BACTERIA_ID = new CustomIdDefinition("minecraft", "SLIME_BALL");
    private static final String DEFAULT_BACTERIA_NAME = "<gold><b>Mesophilic Bacteria</b></gold>";
    private static final java.util.List<String> DEFAULT_BACTERIA_LORE = java.util.List.of("<yellow>Produces ATP from lactose.</yellow>");

    private static final CustomIdDefinition DEFAULT_CURD_ID = new CustomIdDefinition("minecraft", "BREAD");
    private static final String DEFAULT_CURD_NAME = "<gold><b>Curd</b></gold>";
    private static final java.util.List<String> DEFAULT_CURD_LORE = java.util.List.of("<yellow>Fresh cheese curd.</yellow>");

    private static final CustomIdDefinition DEFAULT_INOCULATED_MILK_ID = new CustomIdDefinition("minecraft", "MILK_BUCKET");
    private static final String DEFAULT_INOCULATED_MILK_NAME = "<white>Inoculated Milk</white>";
    private static final java.util.List<String> DEFAULT_INOCULATED_MILK_LORE = java.util.List.of();

    private final CheeseFactoryPlugin plugin;
    private final CheeseKeys keys;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private int cmdBacteria;
    private int cmdCurd;
    private int cmdInoculatedMilk;
    private boolean hasNexo;
    private boolean hasCraftEngine;
    private ExternalItemProvider nexoProvider;
    private ExternalItemProvider craftEngineProvider;
    private String bacteriaRawName = DEFAULT_BACTERIA_NAME;
    private java.util.List<String> bacteriaRawLore = java.util.List.of();
    private CustomIdDefinition bacteriaCustomId = DEFAULT_BACTERIA_ID;
    private String curdRawName = DEFAULT_CURD_NAME;
    private java.util.List<String> curdRawLore = java.util.List.of();
    private CustomIdDefinition curdCustomId = DEFAULT_CURD_ID;
    private String inoculatedMilkRawName = DEFAULT_INOCULATED_MILK_NAME;
    private java.util.List<String> inoculatedMilkRawLore = java.util.List.of();
    private CustomIdDefinition inoculatedMilkCustomId = DEFAULT_INOCULATED_MILK_ID;

    public CheeseItemManager(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
        this.keys = new CheeseKeys(plugin);
        reload();
    }

    public void reload() {
        var config = plugin.getConfig();
        var log = plugin.getLogger();
        cmdBacteria = resolveCmd(config, log, "bacteria.minecraft_item.custom_model_data", "items.custom_model_data.bacteria", "items.custom-model-data.bacteria", 0);
        cmdCurd = resolveCmd(config, log, "curd.minecraft_item.custom_model_data", "items.custom_model_data.curd", "items.custom-model-data.curd", 0);
        cmdInoculatedMilk = resolveCmd(config, log, "inoculated_milk.minecraft_item.custom_model_data", "inoculated_milk.custom_model_data", null, 0);

        hasNexo = plugin.getServer().getPluginManager().getPlugin("Nexo") != null;
        hasCraftEngine = plugin.getServer().getPluginManager().getPlugin("CraftEngine") != null;
        nexoProvider = hasNexo ? new NexoItemProvider(plugin) : null;
        craftEngineProvider = hasCraftEngine ? new CraftEngineItemProvider(plugin) : null;

        bacteriaRawName = resolveName(config, "bacteria.minecraft_item.name", "bacteria.item.name", DEFAULT_BACTERIA_NAME);
        bacteriaRawLore = resolveLore(config, "bacteria.minecraft_item.lore", "bacteria.item.lore", DEFAULT_BACTERIA_LORE);
        bacteriaCustomId = parseCustomId(config, "bacteria.custom_id", "bacteria.item.custom_id", DEFAULT_BACTERIA_ID);

        curdRawName = resolveName(config, "curd.minecraft_item.name", "curd.item.name", DEFAULT_CURD_NAME);
        curdRawLore = resolveLore(config, "curd.minecraft_item.lore", "curd.item.lore", DEFAULT_CURD_LORE);
        curdCustomId = parseCustomId(config, "curd.custom_id", "curd.item.custom_id", DEFAULT_CURD_ID);
        
        inoculatedMilkRawName = resolveName(config, "inoculated_milk.minecraft_item.name", "inoculated_milk.item.name", DEFAULT_INOCULATED_MILK_NAME);
        inoculatedMilkRawLore = resolveLore(config, "inoculated_milk.minecraft_item.lore", "inoculated_milk.item.lore", DEFAULT_INOCULATED_MILK_LORE);
        inoculatedMilkCustomId = parseCustomId(config, "inoculated_milk.custom_id", "inoculated_milk.item.custom_id", DEFAULT_INOCULATED_MILK_ID);
        
        registerRecipes();
    }
    
    private void registerRecipes() {
        // Remove old recipe if exists
        var key = new org.bukkit.NamespacedKey(plugin, "inoculated_milk_recipe");
        plugin.getServer().removeRecipe(key);
        
        var recipe = new org.bukkit.inventory.ShapelessRecipe(key, createInoculatedMilk());
        
        // We need to ensure the milk bucket is NOT already inoculated milk.
        // Since Bukkit's RecipeChoice.ExactChoice checks for exact meta match,
        // and vanilla milk bucket has no meta, we can try using a MaterialChoice
        // but that won't filter out our custom item if it's also a MILK_BUCKET.
        // However, our custom item HAS persistent data.
        // The best way to prevent recursive crafting is to check in the PrepareItemCraftEvent or CraftItemEvent.
        // But to be safe here, we just register MILK_BUCKET.
        // The recursive fix will be handled in the listener.
        recipe.addIngredient(Material.MILK_BUCKET);
        recipe.addIngredient(new org.bukkit.inventory.RecipeChoice.ExactChoice(createBacteriaItem()));
        plugin.getServer().addRecipe(recipe);
    }

    public CheeseKeys keys() {
        return keys;
    }

    public ItemStack createBacteriaItem() {
        ResolvedItem resolved = resolveBacteriaBase();
        ItemStack stack = resolved.stack();
        ItemMeta meta = stack.getItemMeta();

        if (shouldApplyFactoryMeta(resolved)) {
            meta.displayName(mm.deserialize(bacteriaRawName));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : bacteriaRawLore) {
                lore.add(mm.deserialize(line));
            }
            meta.lore(lore);
            if (cmdBacteria > 0) {
                meta.setCustomModelData(cmdBacteria);
            }
        }

        meta.getPersistentDataContainer().set(keys.bacteria(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createCurdItem() {
        ResolvedItem resolved = resolveCurdBase();
        ItemStack stack = resolved.stack();
        ItemMeta meta = stack.getItemMeta();

        if (shouldApplyFactoryMeta(resolved)) {
            meta.displayName(mm.deserialize(curdRawName));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : curdRawLore) {
                lore.add(mm.deserialize(line));
            }
            meta.lore(lore);
            if (cmdCurd > 0) {
                meta.setCustomModelData(cmdCurd);
            }
        }

        meta.getPersistentDataContainer().set(keys.curd(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(keys.curdUnique(), PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack createInoculatedMilk() {
        ResolvedItem resolved = resolveInoculatedMilkBase();
        ItemStack stack = resolved.stack();
        ItemMeta meta = stack.getItemMeta();

        if (shouldApplyFactoryMeta(resolved)) {
            meta.displayName(mm.deserialize(inoculatedMilkRawName));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : inoculatedMilkRawLore) {
                lore.add(mm.deserialize(line));
            }
            meta.lore(lore);
            if (cmdInoculatedMilk > 0) {
                meta.setCustomModelData(cmdInoculatedMilk);
            }
        }

        meta.getPersistentDataContainer().set(keys.inoculatedMilk(), PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isBacteria(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(keys.bacteria(), PersistentDataType.BYTE);
    }

    public boolean isCurd(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(keys.curd(), PersistentDataType.BYTE);
    }

    public boolean isInoculatedMilk(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(keys.inoculatedMilk(), PersistentDataType.BYTE);
    }

    private CustomIdDefinition parseCustomId(org.bukkit.configuration.file.FileConfiguration cfg, String newPath, String legacyPath, CustomIdDefinition def) {
        if (cfg.isConfigurationSection(newPath)) {
            String type = cfg.getString(newPath + ".type", def.type());
            String item = cfg.getString(newPath + ".item", def.item());
            if (type == null) {
                type = def.type();
            }
            if (item == null) {
                item = def.item();
            }
            return new CustomIdDefinition(type, item);
        }
        if (legacyPath != null && cfg.isConfigurationSection(legacyPath)) {
            String type = cfg.getString(legacyPath + ".type", def.type());
            String item = cfg.getString(legacyPath + ".item", def.item());
            if (type == null) {
                type = def.type();
            }
            if (item == null) {
                item = def.item();
            }
            return new CustomIdDefinition(type, item);
        }
        return def;
    }

    private ResolvedItem resolveBacteriaBase() {
        String type = normalizeType(bacteriaCustomId);
        String itemId = bacteriaCustomId.item();
        Material fallback = Material.SLIME_BALL;
        ItemStack external = null;

        switch (type) {
            case "minecraft" -> {
                Material parsed = itemId == null ? null : Material.matchMaterial(itemId, true);
                if (parsed != null) {
                    fallback = parsed;
                } else {
                    plugin.getLogger().warning("Invalid minecraft material '" + itemId + "' for bacteria; using SLIME_BALL.");
                }
            }
            case "nexo" -> {
                if (!hasNexo) {
                    plugin.getLogger().warning("Nexo not found but bacteria custom_id requests it; using SLIME_BALL with CheeseFactory name/lore.");
                } else if (nexoProvider != null && itemId != null) {
                    external = nexoProvider.createItem(itemId);
                }
            }
            case "craftengine" -> {
                if (!hasCraftEngine) {
                    plugin.getLogger().warning("CraftEngine not found but bacteria custom_id requests it; using SLIME_BALL with CheeseFactory name/lore.");
                } else if (craftEngineProvider != null && itemId != null) {
                    external = craftEngineProvider.createItem(itemId);
                }
            }
            default -> plugin.getLogger().warning("Unknown custom_id.type '" + type + "' for bacteria; using SLIME_BALL with CheeseFactory name/lore.");
        }

        boolean usedExternal = external != null;
        ItemStack base = usedExternal ? external : new ItemStack(fallback);
        return new ResolvedItem(base, usedExternal, type);
    }

    private ResolvedItem resolveCurdBase() {
        String type = normalizeType(curdCustomId);
        String itemId = curdCustomId.item();
        Material fallback = Material.BREAD;
        ItemStack external = null;

        switch (type) {
            case "minecraft" -> {
                Material parsed = itemId == null ? null : Material.matchMaterial(itemId, true);
                if (parsed != null) {
                    fallback = parsed;
                } else {
                    plugin.getLogger().warning("Invalid minecraft material '" + itemId + "' for curd; using BREAD.");
                }
            }
            case "nexo" -> {
                if (!hasNexo) {
                    plugin.getLogger().warning("Nexo not found but curd custom_id requests it; using BREAD with CheeseFactory name/lore.");
                } else if (nexoProvider != null && itemId != null) {
                    external = nexoProvider.createItem(itemId);
                }
            }
            case "craftengine" -> {
                if (!hasCraftEngine) {
                    plugin.getLogger().warning("CraftEngine not found but curd custom_id requests it; using BREAD with CheeseFactory name/lore.");
                } else if (craftEngineProvider != null && itemId != null) {
                    external = craftEngineProvider.createItem(itemId);
                }
            }
            default -> plugin.getLogger().warning("Unknown custom_id.type '" + type + "' for curd; using BREAD with CheeseFactory name/lore.");
        }

        boolean usedExternal = external != null;
        ItemStack base = usedExternal ? external : new ItemStack(fallback);
        return new ResolvedItem(base, usedExternal, type);
    }

    private ResolvedItem resolveInoculatedMilkBase() {
        String type = normalizeType(inoculatedMilkCustomId);
        String itemId = inoculatedMilkCustomId.item();
        Material fallback = Material.MILK_BUCKET;
        ItemStack external = null;

        switch (type) {
            case "minecraft" -> {
                Material parsed = itemId == null ? null : Material.matchMaterial(itemId, true);
                if (parsed != null) {
                    fallback = parsed;
                } else {
                    plugin.getLogger().warning("Invalid minecraft material '" + itemId + "' for inoculated_milk; using MILK_BUCKET.");
                }
            }
            case "nexo" -> {
                if (!hasNexo) {
                    plugin.getLogger().warning("Nexo not found but inoculated_milk custom_id requests it; using MILK_BUCKET with CheeseFactory name/lore.");
                } else if (nexoProvider != null && itemId != null) {
                    external = nexoProvider.createItem(itemId);
                }
            }
            case "craftengine" -> {
                if (!hasCraftEngine) {
                    plugin.getLogger().warning("CraftEngine not found but inoculated_milk custom_id requests it; using MILK_BUCKET with CheeseFactory name/lore.");
                } else if (craftEngineProvider != null && itemId != null) {
                    external = craftEngineProvider.createItem(itemId);
                }
            }
            default -> plugin.getLogger().warning("Unknown custom_id.type '" + type + "' for inoculated_milk; using MILK_BUCKET with CheeseFactory name/lore.");
        }

        boolean usedExternal = external != null;
        ItemStack base = usedExternal ? external : new ItemStack(fallback);
        return new ResolvedItem(base, usedExternal, type);
    }

    private String normalizeType(CustomIdDefinition customId) {
        if (customId == null || customId.type() == null) {
            return "minecraft";
        }
        return customId.type().toLowerCase(Locale.ROOT);
    }

    private boolean shouldApplyFactoryMeta(ResolvedItem resolved) {
        return "minecraft".equals(resolved.type()) || !resolved.externalProvided();
    }

    private record ResolvedItem(ItemStack stack, boolean externalProvided, String type) {
    }

    private int resolveCmd(org.bukkit.configuration.file.FileConfiguration cfg, Logger log, String newPath, String legacyPath, String legacyDashPath, int def) {
        if (cfg.contains(newPath)) {
            return cfg.getInt(newPath, def);
        }
        if (legacyPath != null || legacyDashPath != null) {
            return ConfigUtil.getInt(cfg, log, legacyPath, legacyDashPath, def);
        }
        return def;
    }

    private String resolveName(org.bukkit.configuration.file.FileConfiguration cfg, String newPath, String legacyPath, String def) {
        String value = cfg.getString(newPath);
        if (value == null || value.isBlank()) {
            value = cfg.getString(legacyPath, def);
        }
        if (value == null || value.isBlank()) {
            return def;
        }
        return value;
    }

    private List<String> resolveLore(org.bukkit.configuration.file.FileConfiguration cfg, String newPath, String legacyPath, List<String> def) {
        List<String> lore = cfg.getStringList(newPath);
        if (lore == null || lore.isEmpty()) {
            lore = cfg.getStringList(legacyPath);
        }
        if (lore == null || lore.isEmpty()) {
            return def;
        }
        return lore;
    }
}
