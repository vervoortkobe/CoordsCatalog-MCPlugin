package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;

import java.util.List;
import java.util.Optional;

import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.displayPaginatedList;
import static org.minecraft.tsunami.coordsCatalog.command.BaseCommand.parsePageArgument;

public class FindCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleFindCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("coordscatalog.find")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage-find"));
            return true;
        }

        String searchQuery = args[1];

        Optional<Integer> pageOptional = parsePageArgument(sender, args, 2, configManager);
        if (pageOptional.isEmpty()) {
            return true;
        }
        int page = pageOptional.get();

        List<Coordinate> sourceList = coordsManager.findCoordinatesByIdOrName(searchQuery);

        displayPaginatedList(coordsManager, configManager, sender, label, sourceList, page,
                "find-header",
                new String[]{"search", searchQuery},
                "find-empty",
                "find",
                searchQuery
        );
        return true;
    }
}
