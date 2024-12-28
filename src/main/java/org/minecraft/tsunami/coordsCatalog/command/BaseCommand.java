package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.dao.CoordsDAO;

import java.util.List;
import java.util.UUID;

import static org.minecraft.tsunami.coordsCatalog.util.CoordsUtil.parseCoordinate;
import static org.minecraft.tsunami.coordsCatalog.util.CoordsUtil.parseWorld;

public class BaseCommand implements CommandExecutor {
    private final Main plugin;
    private final CoordsDAO coordDAO;

    public BaseCommand(Main plugin) {
        this.plugin = plugin;
        this.coordDAO = new CoordsDAO(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendHelpMessage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            if (!player.hasPermission("coordscatalog.save")) {
                player.sendMessage("§c📍 You don't have permission to save coordinates.");
                return true;
            }
            return handleSaveCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("del")) {
            if (!player.hasPermission("coordscatalog.delete")) {
                player.sendMessage("§c📍 You don't have permission to delete coordinates.");
                return true;
            }
            return handleDeleteCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!player.hasPermission("coordscatalog.list")) {
                player.sendMessage("§c📍 You don't have permission to list coordinates.");
                return true;
            }
            return handleListCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("find")) {
            if (!player.hasPermission("coordscatalog.find")) {
                player.sendMessage("§c📍 You don't have permission to find coordinates.");
                return true;
            }
            return handleFindCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("me")) {
            if (!player.hasPermission("coordscatalog.me")) {
                player.sendMessage("§c📍 You don't have permission to view your coordinates.");
                return true;
            }
            return handleMeCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (!player.hasPermission("coordscatalog.check")) {
                player.sendMessage("§c📍 You don't have permission to check other players' coordinates.");
                return true;
            }
            return handleCheckCommand(player, args);
        }

        player.sendMessage("§c📍 Invalid command. Use /cc help for a list of commands.");
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("§e📍 CoordsCatalog Commands:");
        player.sendMessage("§7/cc save <name> [X] [Y] [Z] [world] - Save coordinates");
        player.sendMessage("§7/cc delete <coordID> - Delete a coordinate");
        player.sendMessage("§7/cc list [page] - List saved coordinates");
        player.sendMessage("§7/cc find <name> - Find coordinates by name");
        player.sendMessage("§7/cc me - Show your saved coordinates");
        player.sendMessage("§7/cc check <player> - Check a player's coordinates (OP only)");
    }

    private boolean handleSaveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c📍 Usage: /cc save <name> [X] [Y] [Z] [world]");
            return true;
        }

        String name = args[1];
        Location location = player.getLocation();
        double x, y, z;
        World world = location.getWorld();

        try {
            if (args.length >= 5) {
                x = parseCoordinate(args[2], location.getX());
                y = parseCoordinate(args[3], location.getY());
                z = parseCoordinate(args[4], location.getZ());
            } else {
                x = location.getX();
                y = location.getY();
                z = location.getZ();
            }

            if (args.length == 6) {
                world = parseWorld(args[5]);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c📍 " + e.getMessage());
            return true;
        }

        String coordId = coordDAO.saveCoordinate(name, x, y, z, world, player);
        player.sendMessage("§a📍 Coordinate saved with ID: " + coordId);
        return true;
    }

    private boolean handleDeleteCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("§c📍 Usage: /cc delete <coordID>");
            return true;
        }

        String coordId = args[1];
        boolean deleted = coordDAO.deleteCoordinate(coordId, player);

        if (deleted) {
            player.sendMessage("§a📍 Coordinate deleted successfully.");
        } else {
            player.sendMessage("§c📍 Could not delete coordinate. It may not exist or you may not have permission.");
        }
        return true;
    }

    private boolean handleListCommand(Player player, String[] args) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c📍 Invalid page number.");
                return true;
            }
        }

        List<String> coords = coordDAO.listCoordinates(page);
        int totalPages = coordDAO.getTotalPages();

        player.sendMessage("§e📍 Coordinates (Page " + page + "/" + totalPages + "):");
        coords.forEach(player::sendMessage);

        if (page > 1) {
            player.sendMessage("§7... Previous pages available");
        }
        if (page < totalPages) {
            player.sendMessage("§7... More pages available");
        }

        return true;
    }

    private boolean handleFindCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c📍 Usage: /cc find <name>");
            return true;
        }

        String searchName = args[1];
        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c📍 Invalid page number.");
                return true;
            }
        }

        List<String> foundCoords = coordDAO.findCoordinatesByName(searchName, page);
        int totalPages = coordDAO.getTotalPagesForSearch(searchName);

        player.sendMessage("§e📍 Coordinates matching '" + searchName + "' (Page " + page + "/" + totalPages + "):");
        foundCoords.forEach(player::sendMessage);

        if (page > 1) {
            player.sendMessage("§7... Previous pages available");
        }
        if (page < totalPages) {
            player.sendMessage("§7... More pages available");
        }

        return true;
    }

    private boolean handleMeCommand(Player player, String[] args) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c📍 Invalid page number.");
                return true;
            }
        }

        List<String> myCoords = coordDAO.listPlayerCoordinates(player, page);
        int totalPages = coordDAO.getTotalPagesForPlayer(player);

        player.sendMessage("§e📍 Your Coordinates (Page " + page + "/" + totalPages + "):");
        myCoords.forEach(player::sendMessage);

        if (page > 1) {
            player.sendMessage("§7... Previous pages available");
        }
        if (page < totalPages) {
            player.sendMessage("§7... More pages available");
        }

        return true;
    }

    private boolean handleCheckCommand(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage("§c📍 Only operators can use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c📍 Usage: /cc check <player>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage("§c📍 Player not found.");
            return true;
        }

        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c📍 Invalid page number.");
                return true;
            }
        }

        List<String> targetCoords = coordDAO.listPlayerCoordinates(targetPlayer, page);
        int totalPages = coordDAO.getTotalPagesForPlayer(targetPlayer);

        player.sendMessage("§e📍 " + targetPlayer.getName() + "'s Coordinates (Page " + page + "/" + totalPages + "):");
        targetCoords.forEach(player::sendMessage);

        if (page > 1) {
            player.sendMessage("§7... Previous pages available");
        }
        if (page < totalPages) {
            player.sendMessage("§7... More pages available");
        }

        return true;
    }
}