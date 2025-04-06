package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;

import java.util.List;
import java.util.Optional;

import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.displayPaginatedList;
import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.parsePageArgument;

public class ListCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleListCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("coordscatalog.list")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        Optional<Integer> pageOptional = parsePageArgument(sender, args, 1, configManager);
        if (pageOptional.isEmpty()) {
            return true;
        }
        int page = pageOptional.get();

        List<Coordinate> sourceList = coordsManager.getAllCoordinates();

        displayPaginatedList(coordsManager, configManager, sender, label, sourceList, page,
                "list-header",
                new String[]{},
                "list-empty",
                "list",
                ""
        );
        return true;
    }
}
