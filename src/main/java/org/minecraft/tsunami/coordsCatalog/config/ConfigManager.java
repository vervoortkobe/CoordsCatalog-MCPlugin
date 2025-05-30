package org.minecraft.tsunami.coordsCatalog.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.minecraft.tsunami.coordsCatalog.Main;

import java.util.List;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("Configuration loaded.");
    }

    public boolean isUpdateCheckEnabled() {
        return config.getBoolean("update-checker.enabled", true);
    }

    public String getUpdateCheckUrl() {
        return config.getString("update-checker.url", "");
    }

    public boolean isWebhooksEnabled() {
        return !config.getBoolean("webhooks.enabled", false);
    }

    public List<String> getWebhookUrls() {
        return config.getStringList("webhooks.urls");
    }

    public int getCoordsPerPage() {
        return config.getInt("coords-per-page", 10);
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "&cMissing message: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getRawMessage(String key) {
        return config.getString("messages." + key, "Missing message: " + key);
    }

    public String getFormattedMessage(String key, String... replacements) {
        String message = getMessage(key);
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Invalid number of replacements for message key: " + key);
            return message;
        }
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return message;
    }
}