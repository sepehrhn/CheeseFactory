package dev.sepehrhn.cheesefactory.barrel;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.cheese.CustomIdDefinition;
import dev.sepehrhn.cheesefactory.integration.CraftEngineItemProvider;
import dev.sepehrhn.cheesefactory.integration.ExternalItemProvider;
import dev.sepehrhn.cheesefactory.integration.NexoItemProvider;
import dev.sepehrhn.cheesefactory.item.CheeseKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class BarrelItemService {

    private static final String ROOT = "cheese_barrel.";
    private static final CustomIdDefinition DEFAULT_CUSTOM_ID = new CustomIdDefinition("minecraft", "BARREL");

    private final CheeseFactoryPlugin plugin;
    private final CheeseKeys keys;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private boolean hasNexo;
    private boolean hasCraftEngine;
    private ExternalItemProvider nexoProvider;
    private ExternalItemProvider craftEngineProvider;

    private String rawName = "<gold>Cheese Barrel</gold>";
    private List<String> rawLore = List.of();
    private CustomIdDefinition customId = DEFAULT_CUSTOM_ID;
    private CustomIdDefinition customIdOpen = DEFAULT_CUSTOM_ID;

    public BarrelItemService(CheeseFactoryPlugin plugin, CheeseKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        Logger log = plugin.getLogger();
        hasNexo = plugin.getServer().getPluginManager().getPlugin("Nexo") != null;
        hasCraftEngine = plugin.getServer().getPluginManager().getPlugin("CraftEngine") != null;
        nexoProvider = hasNexo ? new NexoItemProvider(plugin) : null;
        craftEngineProvider = hasCraftEngine ? new CraftEngineItemProvider(plugin) : null;

        rawName = resolveName(cfg, ROOT + "minecraft_item.name", ROOT + "name", "<gold>Cheese Barrel</gold>");
        rawLore = resolveLore(cfg, ROOT + "minecraft_item.lore", ROOT + "lore");
        customId = parseCustomId(cfg, ROOT + "custom_id", log);
        customIdOpen = parseCustomId(cfg, ROOT + "custom_id_open", log);

        var legacy = plugin.getDataFolder().toPath().resolve("cheese_barrel.yml").toFile();
        if (legacy.exists()) {
            log.warning("Legacy cheese_barrel.yml detected. Cheese barrel configuration now lives under 'cheese_barrel' in config.yml.");
        }
    }

    public ItemStack createBarrelItem() {
        ResolvedItem resolved = resolveBaseItem(customId);
        ItemStack base = resolved.stack();
        ItemMeta meta = base.getItemMeta();

        if (shouldApplyFactoryMeta(resolved)) {
            meta.displayName(mm.deserialize(rawName));
            var loreComponents = new ArrayList<net.kyori.adventure.text.Component>();
            for (String line : rawLore) {
                loreComponents.add(mm.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        meta.getPersistentDataContainer().set(keys.barrelItem(), PersistentDataType.BYTE, (byte) 1);
        base.setItemMeta(meta);
        return base;
    }

    public boolean isBarrelItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(keys.barrelItem(), PersistentDataType.BYTE);
    }

    private CustomIdDefinition parseCustomId(FileConfiguration cfg, String path, Logger log) {
        if (!cfg.isConfigurationSection(path)) {
            return DEFAULT_CUSTOM_ID;
        }
        String type = cfg.getString(path + ".type", "minecraft");
        String item = cfg.getString(path + ".item", "BARREL");
        if (type == null) {
            type = "minecraft";
        }
        return new CustomIdDefinition(type, item);
    }

    private ResolvedItem resolveBaseItem(CustomIdDefinition custom) {
        String type = normalizeType(custom);
        String itemId = custom.item();
        Material fallback = Material.BARREL;
        ItemStack external = null;

        switch (type) {
            case "minecraft" -> {
                Material parsed = itemId == null ? null : Material.matchMaterial(itemId, true);
                if (parsed != null) {
                    fallback = parsed;
                } else {
                    plugin.getLogger().warning("Invalid minecraft material '" + itemId + "' for cheese_barrel; using BARREL.");
                }
            }
            case "nexo" -> {
                if (!hasNexo) {
                    plugin.getLogger().warning("Nexo not found but cheese_barrel requests it; using BARREL with CheeseFactory name/lore.");
                } else if (nexoProvider != null && itemId != null) {
                    external = nexoProvider.createItem(itemId);
                }
            }
            case "craftengine" -> {
                if (!hasCraftEngine) {
                    plugin.getLogger().warning("CraftEngine not found but cheese_barrel requests it; using BARREL with CheeseFactory name/lore.");
                } else if (craftEngineProvider != null && itemId != null) {
                    external = craftEngineProvider.createItem(itemId);
                }
            }
            default -> plugin.getLogger().warning("Unknown custom_id.type '" + type + "' for cheese_barrel; using BARREL with CheeseFactory name/lore.");
        }

        boolean usedExternal = external != null;
        ItemStack base = usedExternal ? external : new ItemStack(fallback);
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

    private record ResolvedItem(ItemStack stack, boolean externalProvided, String type) {
    }

    private String resolveName(FileConfiguration cfg, String newPath, String legacyPath, String def) {
        String value = cfg.getString(newPath);
        if (value == null || value.isBlank()) {
            value = cfg.getString(legacyPath, def);
        }
        if (value == null || value.isBlank()) {
            return def;
        }
        return value;
    }

    private List<String> resolveLore(FileConfiguration cfg, String newPath, String legacyPath) {
        List<String> lore = cfg.getStringList(newPath);
        if (lore == null || lore.isEmpty()) {
            lore = cfg.getStringList(legacyPath);
        }
        if (lore == null || lore.isEmpty()) {
            return Collections.emptyList();
        }
        return lore;
    }

    public void placeBarrel(org.bukkit.Location location, boolean open) {
        CustomIdDefinition id = open ? customIdOpen : customId;
        String type = normalizeType(id);
        String itemId = id.item();

        if ("nexo".equals(type) && hasNexo && nexoProvider != null && itemId != null) {
            nexoProvider.placeBlock(itemId, location);
        }
        // For vanilla barrels, block state is handled in CheeseBarrelManager
    }

    public boolean isNexoBarrel() {
        return "nexo".equals(normalizeType(customId));
    }
}
