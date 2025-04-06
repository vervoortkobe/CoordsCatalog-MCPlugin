package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;

import java.util.List;
import java.util.Optional;

import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.displayPaginatedList;
import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.parsePageArgument;

public class MeCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleMeCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("coordscatalog.me")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Optional<Integer> pageOptional = parsePageArgument(sender, args, 1, configManager);
        if (pageOptional.isEmpty()) {
            return true;
        }
        int page = pageOptional.get();

        List<Coordinate> sourceList = coordsManager.getCoordinatesByOwner(player.getUniqueId());

        displayPaginatedList(coordsManager, configManager, sender, label, sourceList, page,
                "me-header",
                new String[]{},
                "me-empty",
                "me",
                ""
        );
        return true;
    }
}
