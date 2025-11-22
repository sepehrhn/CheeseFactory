package dev.sepehrhn.cheesefactory.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handles smart config migration that preserves user customizations
 * while updating to new config structure.
 */
public class ConfigMigrator {

    private final Plugin plugin;
    private final List<String> migrationReport = new ArrayList<>();

    public ConfigMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrate a config file, preserving user values while updating structure.
     * 
     * @param userFile The user's current config file
     * @param defaultResource Input stream of the default config from resources
     * @param versionKey The key that stores the config version (e.g., "config_version")
     * @return true if migration occurred, false if already up to date
     */
    public boolean migrateConfig(File userFile, InputStream defaultResource, String versionKey) {
        migrationReport.clear();

        try {
            // Load default config
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultResource)
            );
            int defaultVersion = defaultConfig.getInt(versionKey, 1);

            // If user file doesn't exist, create it with defaults
            if (!userFile.exists()) {
                userFile.getParentFile().mkdirs();
                Files.copy(defaultResource, userFile.toPath());
                plugin.getLogger().info("Created " + userFile.getName() + " v" + defaultVersion);
                return false;
            }

            // Load user config
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
            int userVersion = userConfig.getInt(versionKey, 0);

            // Check if migration needed
            if (userVersion >= defaultVersion) {
                plugin.getLogger().fine(userFile.getName() + " is up to date (v" + userVersion + ")");
                return false;
            }

            // Perform migration
            plugin.getLogger().info("Migrating " + userFile.getName() + " v" + userVersion + " → v" + defaultVersion);

            // Create backup
            createBackup(userFile, userVersion);

            // Merge configs
            YamlConfiguration merged = mergeConfigs(userConfig, defaultConfig);

            // Save merged config
            merged.save(userFile);

            // Log migration report
            for (String message : migrationReport) {
                plugin.getLogger().info(message);
            }

            plugin.getLogger().info("Migration complete! Backup saved as " + getBackupFileName(userFile, userVersion));
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to migrate " + userFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deep merge user config values into default config structure.
     */
    private YamlConfiguration mergeConfigs(YamlConfiguration user, YamlConfiguration defaults) {
        YamlConfiguration merged = new YamlConfiguration();

        int preserved = 0;
        int added = 0;
        int obsolete = 0;

        // Copy all paths from default (defines structure)
        for (String path : defaults.getKeys(true)) {
            // Skip configuration sections (only process leaf values)
            if (defaults.isConfigurationSection(path)) {
                continue;
            }

            // If user has this path, preserve their value
            if (user.contains(path)) {
                Object userValue = user.get(path);
                merged.set(path, userValue);
                preserved++;
            } else {
                // New setting in defaults, add it
                Object defaultValue = defaults.get(path);
                merged.set(path, defaultValue);
                added++;
                migrationReport.add("  + Added: " + path + " = " + formatValue(defaultValue));
            }
        }

        // Detect obsolete settings (in user but not in defaults)
        for (String path : user.getKeys(true)) {
            if (!defaults.contains(path) && !user.isConfigurationSection(path)) {
                obsolete++;
                migrationReport.add("  - Obsolete: " + path + " = " + formatValue(user.get(path)) + " (removed)");
            }
        }

        migrationReport.add(0, "  ✓ Preserved " + preserved + " user setting(s)");
        if (added > 0) {
            migrationReport.add(1, "  + Added " + added + " new setting(s)");
        }
        if (obsolete > 0) {
            migrationReport.add("  - Removed " + obsolete + " obsolete setting(s)");
        }

        return merged;
    }

    /**
     * Create a timestamped backup of the config file.
     */
    private void createBackup(File original, int version) throws IOException {
        File backup = new File(original.getParentFile(), getBackupFileName(original, version));
        Files.copy(original.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // Clean old backups (keep last 5)
        cleanOldBackups(original);
    }

    /**
     * Generate backup filename with version and timestamp.
     */
    private String getBackupFileName(File original, int version) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmm").format(new Date());
        return original.getName() + ".backup-v" + version + "-" + timestamp;
    }

    /**
     * Remove old backups, keeping only the most recent 5.
     */
    private void cleanOldBackups(File original) {
        File[] backups = original.getParentFile().listFiles((dir, name) -> 
            name.startsWith(original.getName() + ".backup-")
        );

        if (backups != null && backups.length > 5) {
            // Sort by last modified (oldest first)
            java.util.Arrays.sort(backups, (a, b) -> 
                Long.compare(a.lastModified(), b.lastModified())
            );

            // Delete oldest backups
            for (int i = 0; i < backups.length - 5; i++) {
                backups[i].delete();
            }
        }
    }

    /**
     * Format a value for logging (truncate long lists/strings).
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        String str = value.toString();
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        return str;
    }

    /**
     * Get the migration report from the last migration.
     */
    public List<String> getMigrationReport() {
        return new ArrayList<>(migrationReport);
    }
}
