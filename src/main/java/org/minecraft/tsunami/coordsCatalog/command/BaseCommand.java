package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;
import org.minecraft.tsunami.coordsCatalog.data.CoordsManager;
import org.minecraft.tsunami.coordsCatalog.util.CoordsUtil;

import java.util.*;


@SuppressWarnings("SameReturnValue")
public class BaseCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final CoordsManager coordsManager;
    private final ConfigManager configManager;

    public BaseCommand(Main plugin) {
        this.plugin = plugin;
        this.coordsManager = plugin.getCoordsManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "save" -> handleSaveCommand(sender, args);
            case "delete", "del" -> handleDeleteCommand(sender, args);
            case "list" -> handleListCommand(sender, args, label);
            case "find", "search" -> handleFindCommand(sender, args, label);
            case "me" -> handleMeCommand(sender, args, label);
            case "check" -> handleCheckCommand(sender, args, label);
            case "reload" -> handleReloadCommand(sender);
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("prefix", "") + configManager.getMessage("invalid-command"));
                yield true;
            }
        };
    }

    private boolean handleSaveCommand(CommandSender sender, String[] args) {
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

    private boolean handleDeleteCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coordscatalog.delete")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getMessage("player-only")); // Keeping player only for now
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

    private boolean handleListCommand(CommandSender sender, String[] args, String label) {
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

        displayPaginatedList(sender, label, sourceList, page,
                "list-header", new String[]{},
                "list-empty",
                "list",
                ""
        );
        return true;
    }

    private boolean handleFindCommand(CommandSender sender, String[] args, String label) {
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

        displayPaginatedList(sender, label, sourceList, page,
                "find-header", new String[]{"search", searchQuery},
                "find-empty",
                "find",
                searchQuery
        );
        return true;
    }


    private boolean handleMeCommand(CommandSender sender, String[] args, String label) {
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

        displayPaginatedList(sender, label, sourceList, page,
                "me-header", new String[]{},
                "me-empty",
                "me",
                ""
        );
        return true;
    }

    private boolean handleCheckCommand(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("coordscatalog.check")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("usage-check"));
            return true;
        }

        String targetPlayerName = args[1];
        @SuppressWarnings("deprecation") org.bukkit.OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);

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

        displayPaginatedList(sender, label, sourceList, page,
                "check-header", new String[] {"player", displayName},
                "check-empty",
                "check",
                displayName
        );
        return true;
    }


    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("coordscatalog.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        plugin.getConfigManager().loadConfig();
        plugin.getCoordsManager().loadCoords();
        sender.sendMessage(configManager.getMessage("prefix") + "&aConfiguration and coordinates reloaded.");
        return true;
    }

    private Optional<Integer> parsePageArgument(CommandSender sender, String[] args, int pageArgIndex, ConfigManager configManager) {
        int page = 1;
        if (args.length > pageArgIndex) {
            try {
                page = Integer.parseInt(args[pageArgIndex]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getMessage("invalid-page"));
                return Optional.empty();
            }
        }
        return Optional.of(page);
    }

    private void displayPaginatedList(CommandSender sender, String label,
                                      List<Coordinate> sourceList, int page,
                                      String headerMsgKey, String[] headerReplacements,
                                      String emptyListMsgKey,
                                      String pageInfoSubCommand, String pageInfoArgs) {

        int coordsPerPage = configManager.getCoordsPerPage();
        int totalPages = coordsManager.getTotalPages(sourceList.size(), coordsPerPage);
        List<Coordinate> paginatedCoords = coordsManager.getPaginatedCoordinates(sourceList, page, coordsPerPage);

        if (page > totalPages && totalPages > 0) {
            page = totalPages;
            paginatedCoords = coordsManager.getPaginatedCoordinates(sourceList, page, coordsPerPage);
        } else if (page < 1) {
            page = 1;
            paginatedCoords = coordsManager.getPaginatedCoordinates(sourceList, page, coordsPerPage);
        }

        List<String> finalHeaderReplacements = new ArrayList<>(Arrays.asList(headerReplacements));
        finalHeaderReplacements.add("page");
        finalHeaderReplacements.add(String.valueOf(page));
        finalHeaderReplacements.add("totalPages");
        finalHeaderReplacements.add(String.valueOf(totalPages));

        sender.sendMessage(configManager.getFormattedMessage(headerMsgKey, finalHeaderReplacements.toArray(String[]::new)));

        if (paginatedCoords.isEmpty()) {
            String emptyMessage = configManager.getFormattedMessage(emptyListMsgKey, headerReplacements);
            sender.sendMessage(configManager.getMessage("prefix") + emptyMessage);
        } else {
            paginatedCoords.forEach(coord -> sender.sendMessage(CoordsUtil.formatCoordForDisplay(coord, configManager)));
        }

        if (totalPages > 1 && page < totalPages) {
            sender.sendMessage(configManager.getFormattedMessage("page-info",
                    "command", label,
                    "subcommand", pageInfoSubCommand,
                    "args", pageInfoArgs != null ? pageInfoArgs : "",
                    "nextPage", String.valueOf(page + 1)));
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(configManager.getMessage("prefix") + ChatColor.BOLD + "CoordsCatalog Commands:");
        sender.sendMessage(ChatColor.WHITE + "/cc save <name...> [X Y Z] [world] " + ChatColor.GRAY + "- Save coordinates (uses current pos/world if unspecified)");
        sender.sendMessage(ChatColor.WHITE + "/cc delete <coordID> " + ChatColor.GRAY + "- Delete a coordinate by its ID");
        sender.sendMessage(ChatColor.WHITE + "/cc list [page] " + ChatColor.GRAY + "- List all saved coordinates");
        sender.sendMessage(ChatColor.WHITE + "/cc find <name|id> [page] " + ChatColor.GRAY + "- Find coordinates by name or ID");
        sender.sendMessage(ChatColor.WHITE + "/cc me [page] " + ChatColor.GRAY + "- Show your saved coordinates");
        if (sender.hasPermission("coordscatalog.check")) {
            sender.sendMessage(ChatColor.WHITE + "/cc check <player> [page] " + ChatColor.GRAY + "- Check another player's coordinates");
        }
        if (sender.hasPermission("coordscatalog.admin")) {
            sender.sendMessage(ChatColor.WHITE + "/cc reload " + ChatColor.GRAY + "- Reload configuration and data");
        }
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> possibilities = new ArrayList<>();

        if (args.length == 1) {
            possibilities.addAll(Arrays.asList("help", "save", "delete", "list", "find", "me"));
            if (sender.hasPermission("coordscatalog.check")) possibilities.add("check");
            if (sender.hasPermission("coordscatalog.admin")) possibilities.add("reload");

        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "delete":
                case "del":
                    if (args.length == 2 && sender instanceof Player p1) {
                        boolean isAdmin = sender.hasPermission("coordscatalog.admin") || sender.isOp();
                        UUID playerUUID = p1.getUniqueId();
                        coordsManager.getAllCoordinates().stream()
                                .filter(c -> isAdmin || playerUUID.equals(c.getOwnerUUID()))
                                .forEach(c -> possibilities.add(c.getId()));
                    }
                    break;
                case "list":
                case "me":
                    break;
                case "find":
                case "search":
                    if (args.length == 2) {
                        coordsManager.getAllCoordinates().forEach(c -> {
                            possibilities.add(c.getId());
                            if (!possibilities.contains(c.getName())) {
                                possibilities.add(c.getName());
                            }
                        });
                    }
                    break;
                case "check":
                    if (args.length == 2 && sender.hasPermission("coordscatalog.check")) {
                        Bukkit.getOnlinePlayers().forEach(p -> possibilities.add(p.getName()));
                        // Bukkit.getOfflinePlayers().forEach(p -> { if (p.getName() != null) possibilities.add(p.getName()); });
                    }
                    break;
                case "save":
                    if (sender instanceof Player ignored) {
                        int argIndex = args.length - 1;
                        boolean likelyCoords;
                        int coordArgCount = 0;
                        String lastNonCoordArg = "";
                        for(int i = 1; i < argIndex; i++) {
                            if(args[i].matches("~|~?-?\\d+(\\.\\d+)?") || args[i].matches("-?\\d+(\\.\\d+)?")) {
                                coordArgCount++;
                            } else {
                                coordArgCount = 0;
                                lastNonCoordArg = args[i];
                            }
                        }
                        likelyCoords = (coordArgCount < 3 && argIndex > 1 && !lastNonCoordArg.isEmpty());


                        if (likelyCoords) {
                            possibilities.add("~");
                        }

                        if (coordArgCount == 3 || (args.length > 3 && !likelyCoords)) {
                            Bukkit.getWorlds().forEach(w -> possibilities.add(w.getName()));
                            possibilities.addAll(Arrays.asList("overworld", "nether", "end"));
                        }
                    }
                    break;
            }
        }

        String currentArg = args[args.length - 1].toLowerCase();
        for (String p : possibilities) {
            if (p != null && p.toLowerCase().startsWith(currentArg)) {
                completions.add(p);
            }
        }

        Collections.sort(completions);

        return completions;
    }
}