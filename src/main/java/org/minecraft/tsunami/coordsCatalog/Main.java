package org.minecraft.tsunami.coordsCatalog;

import org.bukkit.plugin.java.JavaPlugin;
import org.minecraft.tsunami.coordsCatalog.command.BaseCommand;

public class Main extends JavaPlugin {
    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;

        getDataFolder().mkdirs();

        getCommand("coordscatalog").setExecutor(new BaseCommand(this));

        getLogger().info("📍 CoordsCatalog plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("📍 CoordsCatalog plugin has been disabled!");
    }

    public static Main getInstance() {
        return instance;
    }
}