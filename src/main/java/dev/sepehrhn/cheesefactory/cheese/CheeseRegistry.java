package dev.sepehrhn.cheesefactory.cheese;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.integration.CraftEngineItemProvider;
import dev.sepehrhn.cheesefactory.integration.ExternalItemProvider;
import dev.sepehrhn.cheesefactory.integration.NexoItemProvider;
import dev.sepehrhn.cheesefactory.item.CheeseItemManager;
import dev.sepehrhn.cheesefactory.item.CheeseKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class CheeseRegistry {
    private static final Material DEFAULT_CHEESE_MATERIAL = Material.PUMPKIN_PIE;

    private final CheeseFactoryPlugin plugin;
    private final CheeseKeys keys;
    private final CheeseItemManager itemManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, CheeseDefinition> cheeses = new LinkedHashMap<>();
    private final Random random = new Random();

    private boolean hasNexo;
    private boolean hasCraftEngine;
    private int totalWeight;
    private ExternalItemProvider nexoProvider;
    private ExternalItemProvider craftEngineProvider;

    public CheeseRegistry(CheeseFactoryPlugin plugin, CheeseKeys keys, CheeseItemManager itemManager) {
        this.plugin = plugin;
        this.keys = keys;
        this.itemManager = itemManager;
    }

    public void reload() {
        File dataFile = new File(plugin.getDataFolder(), "cheese.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("cheese.yml", false);
        }

        FileConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load cheese.yml: " + e.getMessage());
            return;
        }

        cheeses.clear();
        totalWeight = 0;
        Logger log = plugin.getLogger();
        hasNexo = plugin.getServer().getPluginManager().getPlugin("Nexo") != null;
        hasCraftEngine = plugin.getServer().getPluginManager().getPlugin("CraftEngine") != null;
        nexoProvider = hasNexo ? new NexoItemProvider(plugin) : null;
        craftEngineProvider = hasCraftEngine ? new CraftEngineItemProvider(plugin) : null;

        for (String id : cfg.getKeys(false)) {
            String path = id + ".";
            CustomIdDefinition customId = parseCustomId(cfg, path + "custom_id", id, log);
            String type = normalizeType(customId);
            boolean needsFactoryMeta = "minecraft".equals(type) || customId == null;

            String rawName = resolveName(cfg, path + "minecraft_item.name", path + "name");
            if ((rawName == null || rawName.isBlank()) && needsFactoryMeta) {
                log.warning("Skipping cheese '" + id + "' because 'name' is missing for a minecraft custom_id.");
                continue;
            }
            if (rawName == null || rawName.isBlank()) {
                rawName = "<white>" + id + "</white>";
            }

            int weight = cfg.getInt(path + "weight", -1);
            if (weight < 1) {
                log.warning("Skipping cheese '" + id + "' because 'weight' is missing or < 1.");
                continue;
            }

            List<String> rawLore = resolveLore(cfg, path + "minecraft_item.lore", path + "lore");
            List<String> rawEffects = cfg.getStringList(path + "effects");
            List<CheeseEffectDefinition> effects = parseEffects(rawEffects, id, log);
            Integer customModelData = resolveCmd(cfg, path + "minecraft_item.custom_model_data", path + "custom_model_data");

            var displayName = mm.deserialize(rawName);
            var lore = new ArrayList<net.kyori.adventure.text.Component>();
            for (String line : rawLore) {
                lore.add(mm.deserialize(line));
            }

            CheeseDefinition definition = new CheeseDefinition(
                    id,
                    rawName,
                    displayName,
                    weight,
                    effects,
                    rawLore,
                    lore,
                    customId,
                    customModelData
            );
            cheeses.put(id, definition);
            totalWeight += weight;
        }

        if (cheeses.isEmpty()) {
            log.warning("No cheeses loaded from cheese.yml. Fermentation will not produce cheese until this is fixed.");
        } else {
            log.info("Loaded " + cheeses.size() + " cheeses from cheese.yml.");
        }
    }

    public Optional<CheeseDefinition> randomCheese() {
        if (cheeses.isEmpty() || totalWeight <= 0) {
            return Optional.empty();
        }
        int roll = random.nextInt(totalWeight) + 1;
        int cumulative = 0;
        for (CheeseDefinition def : cheeses.values()) {
            cumulative += def.weight();
            if (roll <= cumulative) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public Optional<CheeseDefinition> getById(String id) {
        return Optional.ofNullable(cheeses.get(id));
    }

    public ItemStack createItem(CheeseDefinition def) {
        ResolvedItem resolved = resolveBaseItem(def);
        ItemStack stack = resolved.stack();
        ItemMeta meta = stack.getItemMeta();

        if (shouldApplyFactoryMeta(resolved)) {
            meta.displayName(def.displayName());
            meta.lore(def.lore());
            if (def.customModelData() != null && def.customModelData() > 0) {
                meta.setCustomModelData(def.customModelData());
            }
        }

        var pdc = meta.getPersistentDataContainer();
        pdc.set(keys.cheeseId(), PersistentDataType.STRING, def.id());
        CustomIdDefinition custom = def.customId();
        if (custom != null) {
            String type = resolved.type();
            if ("nexo".equals(type) || "craftengine".equals(type)) {
                if (custom.item() != null) {
                    pdc.set(keys.customItem(), PersistentDataType.STRING, custom.item());
                }
                pdc.set(keys.customType(), PersistentDataType.STRING, type);
            }
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private ResolvedItem resolveBaseItem(CheeseDefinition def) {
        CustomIdDefinition custom = def.customId();
        String type = normalizeType(custom);
        String itemId = custom == null ? null : custom.item();
        Material mat = DEFAULT_CHEESE_MATERIAL;
        ItemStack externalStack = null;

        switch (type) {
            case "minecraft" -> {
                if (itemId != null) {
                    Material parsed = Material.matchMaterial(itemId, true);
                    if (parsed != null) {
                        mat = parsed;
                    } else {
                        plugin.getLogger().warning("Invalid minecraft material '" + itemId + "' for cheese '" + def.id() + "'. Using default.");
                    }
                }
            }
            case "nexo" -> {
                if (!hasNexo) {
                    plugin.getLogger().warning("Nexo not found but cheese '" + def.id() + "' requests it; using default material with CheeseFactory name/lore.");
                } else if (nexoProvider != null && itemId != null) {
                    externalStack = nexoProvider.createItem(itemId);
                }
            }
            case "craftengine" -> {
                if (!hasCraftEngine) {
                    plugin.getLogger().warning("CraftEngine not found but cheese '" + def.id() + "' requests it; using default material with CheeseFactory name/lore.");
                } else if (craftEngineProvider != null && itemId != null) {
                    externalStack = craftEngineProvider.createItem(itemId);
                }
            }
            default -> plugin.getLogger().warning("Unknown custom_id.type '" + type + "' for cheese '" + def.id() + "'. Using default material with CheeseFactory name/lore.");
        }

        boolean usedExternal = externalStack != null;
        ItemStack base = usedExternal ? externalStack : new ItemStack(mat);
        return new ResolvedItem(base, usedExternal, type);
    }

    private String normalizeType(CustomIdDefinition custom) {
        if (custom == null || custom.type() == null) {
            return "minecraft";
        }
        return custom.type().toLowerCase(Locale.ROOT);
    }

    private boolean shouldApplyFactoryMeta(ResolvedItem resolved) {
        return "minecraft".equals(resolved.type()) || !resolved.externalProvided();
    }

    private List<CheeseEffectDefinition> parseEffects(List<String> rawEffects, String cheeseId, Logger log) {
        List<CheeseEffectDefinition> effects = new ArrayList<>();
        for (String raw : rawEffects) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] parts = raw.split("/");
            if (parts.length < 2) {
                log.warning("Invalid effect format '" + raw + "' for cheese '" + cheeseId + "'. Expected EFFECT/DURATION[/AMPLIFIER].");
                continue;
            }
            PotionEffectType type = PotionEffectType.getByName(parts[0].trim());
            if (type == null) {
                log.warning("Unknown potion effect '" + parts[0] + "' for cheese '" + cheeseId + "'.");
                continue;
            }
            int durationSeconds;
            try {
                durationSeconds = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ex) {
                log.warning("Invalid duration in '" + raw + "' for cheese '" + cheeseId + "'.");
                continue;
            }
            int amplifier = 0;
            if (parts.length >= 3) {
                try {
                    amplifier = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ex) {
                    log.warning("Invalid amplifier in '" + raw + "' for cheese '" + cheeseId + "'. Defaulting to 0.");
                    amplifier = 0;
                }
            }
            effects.add(new CheeseEffectDefinition(type, durationSeconds * 20, amplifier));
        }
        return effects;
    }

    private CustomIdDefinition parseCustomId(FileConfiguration cfg, String path, String cheeseId, Logger log) {
        if (!cfg.isConfigurationSection(path)) {
            return null;
        }
        String type = cfg.getString(path + ".type", "minecraft");
        String item = cfg.getString(path + ".item");
        if (type == null) {
            log.warning("Missing custom_id.type for cheese '" + cheeseId + "', defaulting to minecraft.");
            type = "minecraft";
        }
        return new CustomIdDefinition(type, item);
    }

    private record ResolvedItem(ItemStack stack, boolean externalProvided, String type) {
    }

    private String resolveName(FileConfiguration cfg, String newPath, String legacyPath) {
        String value = cfg.getString(newPath);
        if (value == null || value.isBlank()) {
            value = cfg.getString(legacyPath);
        }
        return value;
    }

    private List<String> resolveLore(FileConfiguration cfg, String newPath, String legacyPath) {
        List<String> lore = cfg.getStringList(newPath);
        if (lore == null || lore.isEmpty()) {
            lore = cfg.getStringList(legacyPath);
        }
        if (lore == null) {
            return Collections.emptyList();
        }
        return lore;
    }

    private Integer resolveCmd(FileConfiguration cfg, String newPath, String legacyPath) {
        Integer cmd = null;
        if (cfg.contains(newPath)) {
            int parsed = cfg.getInt(newPath, -1);
            cmd = parsed > 0 ? parsed : 0;
            return cmd;
        }
        if (legacyPath != null && cfg.contains(legacyPath)) {
            int parsed = cfg.getInt(legacyPath, -1);
            cmd = parsed > 0 ? parsed : 0;
        }
        return cmd;
    }
}
