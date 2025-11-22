package dev.sepehrhn.cheesefactory.locale;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class LocaleManager {

    private static final String DEFAULT_LOCALE = "en_US";

    private final CheeseFactoryPlugin plugin;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private YamlConfiguration defaultLocale;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public LocaleManager(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadLocales() {
        locales.clear();
        loadLocales();
    }

    public String getMessage(CommandSender sender, String key) {
        return getMessage(sender, key, Collections.emptyMap());
    }

    public String getMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        return legacySerializer.serialize(component(sender, key, placeholders));
    }

    public Component component(CommandSender sender, String key) {
        return component(sender, key, Collections.emptyMap());
    }

    public Component component(CommandSender sender, String key, Map<String, String> placeholders) {
        String localeKey = DEFAULT_LOCALE;
        if (sender instanceof Player player) {
            localeKey = player.getLocale();
        }
        return miniMessage.deserialize(resolveMessage(localeKey, key, placeholders));
    }

    public String getDefaultMessage(String key) {
        return legacySerializer.serialize(defaultComponent(key));
    }

    public Component defaultComponent(String key) {
        return defaultComponent(key, Collections.emptyMap());
    }

    public Component defaultComponent(String key, Map<String, String> placeholders) {
        return miniMessage.deserialize(resolveMessage(DEFAULT_LOCALE, key, placeholders));
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }

    private void loadLocales() {
        Logger log = plugin.getLogger();
        File localeDir = new File(plugin.getDataFolder(), "locale");
        if (!localeDir.exists() && !localeDir.mkdirs()) {
            log.warning("Unable to create locale directory at " + localeDir.getAbsolutePath());
        }

        File[] files = localeDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration cfg = new YamlConfiguration();
                    cfg.load(file);
                    locales.put(normalize(stripExtension(file.getName())), cfg);
                } catch (IOException | InvalidConfigurationException e) {
                    log.warning("Failed to load locale file " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        defaultLocale = locales.get(normalize(DEFAULT_LOCALE));
        if (defaultLocale == null) {
            File fallbackFile = new File(localeDir, DEFAULT_LOCALE + ".yml");
            if (fallbackFile.exists()) {
                try {
                    YamlConfiguration cfg = new YamlConfiguration();
                    cfg.load(fallbackFile);
                    defaultLocale = cfg;
                    locales.put(normalize(DEFAULT_LOCALE), cfg);
                } catch (IOException | InvalidConfigurationException e) {
                    log.severe("Failed to load default locale file " + fallbackFile.getName() + ": " + e.getMessage());
                }
            } else {
                log.severe("Default locale file en_US.yml is missing. Messages will fall back to hardcoded strings.");
            }
        }
    }

    private String resolveMessage(String rawLocale, String key, Map<String, String> placeholders) {
        String normLocale = normalize(rawLocale);
        String message = lookup(normLocale, key);
        if (message == null) {
            message = lookup(normalize(DEFAULT_LOCALE), key);
        }
        if (message == null) {
            return "Missing message: " + key;
        }
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return message;
    }

    private String lookup(String localeKey, String path) {
        YamlConfiguration cfg = locales.get(localeKey);
        if (cfg == null) {
            return null;
        }
        return cfg.getString(path);
    }

    private String normalize(String locale) {
        if (locale == null || locale.isBlank()) {
            return DEFAULT_LOCALE.toLowerCase(Locale.ROOT);
        }
        return locale.toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        if (idx == -1) {
            return name;
        }
        return name.substring(0, idx);
    }
}
