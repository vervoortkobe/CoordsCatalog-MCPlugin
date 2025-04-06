package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.displayPaginatedList;
import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.parsePageArgument;

public class CheckCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleCheckCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("coordscatalog.check")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage-check"));
            return true;
        }

        String targetPlayerName = args[1];
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(configManager.getFormattedMessage("prefix") +
                    configManager.getFormattedMessage("player-not-found", "player", targetPlayerName));
            return true;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        String displayName = targetPlayer.getName() != null ? targetPlayer.getName() : targetPlayerName;

        Optional<Integer> pageOptional = parsePageArgument(sender, args, 2, configManager);
        if (pageOptional.isEmpty()) {
            return true;
        }
        int page = pageOptional.get();

        List<Coordinate> sourceList = coordsManager.getCoordinatesByOwner(targetUUID);

        displayPaginatedList(
                coordsManager,
                configManager,
                sender,
                label,
                sourceList,
                page,
                "check-header",
                new String[]{"player", displayName},
                "check-empty",
                "check",
                displayName
        );
        return true;
    }
}