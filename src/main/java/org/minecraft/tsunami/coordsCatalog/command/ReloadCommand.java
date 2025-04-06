package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;

public class ReloadCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleReloadCommand(Main plugin, ConfigManager configManager, CommandSender sender) {
        if (!sender.hasPermission("coordscatalog.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        plugin.getConfigManager().loadConfig();
        plugin.getCoordsManager().loadCoords();
        sender.sendMessage(configManager.getMessage("prefix") + "&aConfiguration and coordinates reloaded.");
        return true;
    }
}
