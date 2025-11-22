package dev.sepehrhn.cheesefactory.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ConfigUtil {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ConfigUtil() {
    }

    public static boolean getBoolean(FileConfiguration cfg, Logger log, String newPath, String oldPath, boolean def) {
        if (cfg.contains(newPath)) {
            return cfg.getBoolean(newPath, def);
        }
        if (oldPath != null && cfg.contains(oldPath)) {
            warn(log, oldPath, newPath);
            return cfg.getBoolean(oldPath, def);
        }
        return def;
    }

    public static int getInt(FileConfiguration cfg, Logger log, String newPath, String oldPath, int def) {
        if (cfg.contains(newPath)) {
            return cfg.getInt(newPath, def);
        }
        if (oldPath != null && cfg.contains(oldPath)) {
            warn(log, oldPath, newPath);
            return cfg.getInt(oldPath, def);
        }
        return def;
    }

    public static double getDouble(FileConfiguration cfg, Logger log, String newPath, String oldPath, double def) {
        if (cfg.contains(newPath)) {
            return cfg.getDouble(newPath, def);
        }
        if (oldPath != null && cfg.contains(oldPath)) {
            warn(log, oldPath, newPath);
            return cfg.getDouble(oldPath, def);
        }
        return def;
    }

    private static void warn(Logger log, String oldPath, String newPath) {
        if (log == null) {
            return;
        }
        if (WARNED.add(oldPath)) {
            log.warning("Config: '" + oldPath + "' is deprecated, please rename it to '" + newPath + "'.");
        }
    }
}
