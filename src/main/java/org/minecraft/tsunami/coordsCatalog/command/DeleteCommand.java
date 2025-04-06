package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;

public class DeleteCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleDeleteCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordscatalog.delete")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(configManager.getMessage("usage-delete"));
            return true;
        }

        String coordId = args[1];
        Coordinate coordToDelete = coordsManager.getCoordinateById(coordId);

        if (coordToDelete == null) {
            sender.sendMessage(configManager.getMessage("coord-not-found"));
            return true;
        }

        Coordinate deletedCoord = coordsManager.deleteCoordinate(coordId, player);

        if (deletedCoord != null) {
            sender.sendMessage(configManager.getFormattedMessage("prefix") +
                    configManager.getFormattedMessage("coord-deleted",
                            "name", deletedCoord.getName(), "id", deletedCoord.getId()
                    ));
        } else {
            sender.sendMessage(configManager.getFormattedMessage("prefix") + configManager.getMessage("coord-delete-failed"));
        }
        return true;
    }
}
