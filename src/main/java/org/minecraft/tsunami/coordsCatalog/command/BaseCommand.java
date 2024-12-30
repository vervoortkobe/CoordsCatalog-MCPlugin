package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.dao.CoordsDAO;

import java.util.List;

import static org.minecraft.tsunami.coordsCatalog.util.CoordsUtil.parseCoordinate;
import static org.minecraft.tsunami.coordsCatalog.util.CoordsUtil.parseWorld;

public class BaseCommand implements CommandExecutor {
    private final CoordsDAO coordsDAO;

    public BaseCommand(Main plugin) {
        this.coordsDAO = new CoordsDAO(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "save":
                if (!sender.hasPermission("coordscatalog.save")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to save coordinates.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                return handleSaveCommand((Player) sender, args);

            case "delete":
            case "del":
                if (!sender.hasPermission("coordscatalog.delete")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to delete coordinates.");
                    return true;
                }
                return handleDeleteCommand(sender, args);

            case "list":
                if (!sender.hasPermission("coordscatalog.list")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to list coordinates.");
                    return true;
                }
                return handleListCommand(sender, args);

            case "find":
                if (!sender.hasPermission("coordscatalog.find")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to find coordinates.");
                    return true;
                }
                return handleFindCommand(sender, args);

            case "me":
                if (!sender.hasPermission("coordscatalog.me")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to view your coordinates.");
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                return handleMeCommand((Player) sender, args);

            case "check":
                if (!sender.hasPermission("coordscatalog.check")) {
                    sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to check other players' coordinates.");
                    return true;
                }
                return handleCheckCommand(sender, args);

            default:
                sender.sendMessage(ChatColor.RED + "Invalid command. Use /coordscatalog help for a list of commands.");
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "CoordsCatalog Commands:");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog save <name> [X] [Y] [Z] [world] " + ChatColor.GRAY + "- Save coordinates");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog delete <coordID> " + ChatColor.GRAY + "- Delete a coordinate");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog list [page] " + ChatColor.GRAY + "- List saved coordinates");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog find <name> " + ChatColor.GRAY + "- Find coordinates by name");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog me " + ChatColor.GRAY + "- Show your saved coordinates");
        sender.sendMessage(ChatColor.WHITE + "/coordscatalog check <player> " + ChatColor.GRAY + "- Check a player's coordinates");
    }

    private boolean handleSaveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /coordscatalog save <name> [X] [Y] [Z] [world]");
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
            player.sendMessage(ChatColor.RED + e.getMessage());
            return true;
        }

        assert world != null;
        String coordId = coordsDAO.saveCoordinate(name, x, y, z, world, player);
        String formattedMessage = String.format("%s (%s), %s %s %s in %s", name, coordId, x, y, z, world);
    player.sendMessage(ChatColor.GREEN + "📍 Coordinate saved: " + ChatColor.DARK_AQUA + formattedMessage);
        return true;
    }

    private boolean handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /coordscatalog delete <coordID>");
            return true;
        }

        String coordId = args[1];
        String name = coordsDAO.getCoordByName(coordId);
        if (name == null) {
            sender.sendMessage(ChatColor.RED + "Coordinate not found.");
            return true;
        }
        boolean deleted = coordsDAO.deleteCoordinate(coordId, sender instanceof Player ? (Player) sender : null);

        if (deleted) {
            sender.sendMessage(ChatColor.GREEN + "📍 Coordinate " + ChatColor.DARK_AQUA + name + " (" + coordId + ")" + ChatColor.GREEN + " deleted successfully.");
        } else {
            sender.sendMessage(ChatColor.RED + "Could not delete coordinate. It may not exist or you may not have permission.");
        }
        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        List<String> coords = coordsDAO.listCoordinates(page);
        int totalPages = coordsDAO.getTotalPages();

        sender.sendMessage(ChatColor.DARK_AQUA + "Coordinates (Page " + page + "/" + totalPages + "):");
        coords.forEach(sender::sendMessage);

        if (page > 1) {
            sender.sendMessage(ChatColor.GRAY + "... Previous pages available");
        }
        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "... More pages available");
        }

        return true;
    }

    private boolean handleFindCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /coordscatalog find <name>");
            return true;
        }

        String searchName = args[1];
        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        List<String> foundCoords = coordsDAO.findCoordinatesByName(searchName, page);
        int totalPages = coordsDAO.getTotalPagesForSearch(searchName);

        sender.sendMessage(ChatColor.DARK_AQUA + "📍 Coordinates matching '" + searchName + "' (Page " + page + "/" + totalPages + "):");
        foundCoords.forEach(sender::sendMessage);

        if (page > 1) {
            sender.sendMessage(ChatColor.GRAY + "... Previous pages available");
        }
        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "... More pages available");
        }

        return true;
    }

    private boolean handleMeCommand(Player player, String[] args) {
        int page = 1;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        List<String> myCoords = coordsDAO.listPlayerCoordinates(player, page);
        int totalPages = coordsDAO.getTotalPagesForPlayer(player);

        player.sendMessage(ChatColor.DARK_AQUA + "📍 Your Coordinates (Page " + page + "/" + totalPages + "):");
        myCoords.forEach(player::sendMessage);

        if (page > 1) {
            player.sendMessage(ChatColor.GRAY + "... Previous pages available");
        }
        if (page < totalPages) {
            player.sendMessage(ChatColor.GRAY + "... More pages available");
        }

        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /coordscatalog check <player>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online to check their coordinates.");
            return true;
        }

        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid page number.");
                return true;
            }
        }

        List<String> targetCoords = coordsDAO.listPlayerCoordinates(targetPlayer, page);
        int totalPages = coordsDAO.getTotalPagesForPlayer(targetPlayer);

        sender.sendMessage(ChatColor.DARK_AQUA + "📍 " + targetPlayer.getName() + "'s Coordinates (Page " + page + "/" + totalPages + "):");
        targetCoords.forEach(sender::sendMessage);

        if (page > 1) {
            sender.sendMessage(ChatColor.GRAY + "... Previous pages available");
        }
        if (page < totalPages) {
            sender.sendMessage(ChatColor.GRAY + "... More pages available");
        }

        return true;
    }
}