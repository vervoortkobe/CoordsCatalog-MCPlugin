package org.minecraft.tsunami.coordsCatalog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecraft.tsunami.coordsCatalog.command.BaseCommand;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;
import org.minecraft.tsunami.coordsCatalog.service.UpdateChecker;
import org.minecraft.tsunami.coordsCatalog.service.WebhookNotifier;

import java.util.Objects;

public class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private CoordsManager coordsManager;
    private WebhookNotifier webhookNotifier;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        coordsManager = new CoordsManager(this);
        coordsManager.loadCoords();

        webhookNotifier = new WebhookNotifier(this);

        try {
            Objects.requireNonNull(getCommand("coordscatalog"), "Command 'coordscatalog' not found in plugin.yml")
                    .setExecutor(new BaseCommand(this));
        } catch (NullPointerException e) {
            getLogger().severe("Failed to register command! Make sure 'coordscatalog' is defined in your plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (configManager.isUpdateCheckEnabled()) {
            new UpdateChecker(this).checkForUpdates();
        }

        getLogger().info(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("prefix")) + "CoordsCatalog plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (coordsManager != null) {
            coordsManager.saveCoords();
        }
        getLogger().info(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("prefix")) + "CoordsCatalog plugin has been disabled!");
        instance = null;
    }

    public static Main getInstance() {
        if (instance == null) {
            throw new IllegalStateException("CoordsCatalog plugin instance is not available. Is the plugin enabled?");
        }
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoordsManager getCoordsManager() {
        return coordsManager;
    }

    public WebhookNotifier getWebhookNotifier() {
        return webhookNotifier;
    }
}