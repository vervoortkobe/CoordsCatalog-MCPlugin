package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;
import org.minecraft.tsunami.coordsCatalog.util.CoordsUtil;

import java.util.ArrayList;
import java.util.List;

public class SaveCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleSaveCommand(CoordsManager coordsManager, ConfigManager configManager, CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordscatalog.save")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        // /cc save <name...> [x y z] [world]
        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("usage-save"));
            return true;
        }

        List<String> nameParts = new ArrayList<>();
        List<String> potentialCoords = new ArrayList<>();
        String potentialWorld = null;
        boolean coordsStarted = false;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ((arg.matches("~|~?-?\\d+(\\.\\d+)?") || arg.matches("-?\\d+(\\.\\d+)?")) && (potentialCoords.size() < 3)) {
                if (!coordsStarted) coordsStarted = true;
                potentialCoords.add(arg);
            } else if (coordsStarted && potentialCoords.size() == 3 && potentialWorld == null) {
                potentialWorld = arg;
            } else if (!coordsStarted) {
                nameParts.add(arg);
            } else {
                player.sendMessage(configManager.getMessage("usage-save"));
                return true;
            }
        }

        if (nameParts.isEmpty()) {
            player.sendMessage(configManager.getMessage("usage-save") + " - Name cannot be empty.");
            return true;
        }
        String name = String.join(" ", nameParts);

        Location playerLoc = player.getLocation();
        double x = playerLoc.getX();
        double y = playerLoc.getY();
        double z = playerLoc.getZ();
        World world = playerLoc.getWorld();

        try {
            if (!potentialCoords.isEmpty()) x = CoordsUtil.parseCoordinate(potentialCoords.get(0), playerLoc.getX());
            if (potentialCoords.size() >= 2) y = CoordsUtil.parseCoordinate(potentialCoords.get(1), playerLoc.getY());
            if (potentialCoords.size() == 3) z = CoordsUtil.parseCoordinate(potentialCoords.get(2), playerLoc.getZ());
            else if (!potentialCoords.isEmpty()) {
                player.sendMessage(configManager.getMessage("usage-save") + " - Provide X, Y, and Z or none for current location.");
                return true;
            }

            if (potentialWorld != null) {
                World parsedWorld = CoordsUtil.parseWorld(potentialWorld, playerLoc);
                if (parsedWorld == null) {
                    player.sendMessage(configManager.getFormattedMessage("invalid-world", "world", potentialWorld));
                    return true;
                }
                world = parsedWorld;
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(configManager.getFormattedMessage("invalid-coordinate", "value", e.getMessage().split(": ")[1]));
            return true;
        }

        if (world == null) {
            player.sendMessage(configManager.getFormattedMessage("prefix") + "&cCould not determine the world.");
            return true;
        }

        Coordinate savedCoord = coordsManager.addCoordinate(name, x, y, z, world, player);

        player.sendMessage(configManager.getFormattedMessage("prefix") +
                configManager.getFormattedMessage("coord-saved",
                        "name", savedCoord.getName(), "id", savedCoord.getId(),
                        "x", String.format("%.2f", savedCoord.getX()),
                        "y", String.format("%.2f", savedCoord.getY()),
                        "z", String.format("%.2f", savedCoord.getZ()),
                        "world", savedCoord.getWorldName()
                ));
        return true;
    }
}
