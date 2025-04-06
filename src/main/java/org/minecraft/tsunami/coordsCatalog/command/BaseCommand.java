package org.minecraft.tsunami.coordsCatalog.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import static org.minecraft.tsunami.coordsCatalog.command.CheckCommand.handleCheckCommand;
import static org.minecraft.tsunami.coordsCatalog.command.DeleteCommand.handleDeleteCommand;
import static org.minecraft.tsunami.coordsCatalog.command.FindCommand.handleFindCommand;
import static org.minecraft.tsunami.coordsCatalog.command.ListCommand.handleListCommand;
import static org.minecraft.tsunami.coordsCatalog.command.MeCommand.handleMeCommand;
import static org.minecraft.tsunami.coordsCatalog.command.ReloadCommand.handleReloadCommand;
import static org.minecraft.tsunami.coordsCatalog.command.SaveCommand.handleSaveCommand;

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
            case "save" -> handleSaveCommand(coordsManager, configManager, sender, args);
            case "delete", "del" -> handleDeleteCommand(coordsManager, configManager, sender, args);
            case "list" -> handleListCommand(coordsManager, configManager, sender, args, label);
            case "find", "search" -> handleFindCommand(coordsManager, configManager, sender, args, label);
            case "me" -> handleMeCommand(coordsManager, configManager, sender, args, label);
            case "check" -> handleCheckCommand(coordsManager, configManager, sender, args, label);
            case "reload" -> handleReloadCommand(plugin, configManager, sender);
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("prefix", "") + configManager.getMessage("invalid-command"));
                yield true;
            }
        };
    }

    public static Optional<Integer> parsePageArgument(CommandSender sender, String[] args, int pageArgIndex, ConfigManager configManager) {
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

    public static void displayPaginatedList(CoordsManager coordsManager, ConfigManager configManager,
                                            CommandSender sender, String label, List<Coordinate> sourceList, int page,
                                            String headerMsgKey,
                                            String[] headerReplacements,
                                            String emptyListMsgKey,
                                            String pageInfoSubCommand,
                                            String pageInfoArgs) {

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