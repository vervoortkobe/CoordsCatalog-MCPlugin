package org.minecraft.tsunami.coordsCatalog.dao;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.minecraft.tsunami.coordsCatalog.util.CoordsUtil.generateCoordId;

public class CoordsDAO {
    private static final String COORDS_FILE = "coords.csv";
    private final Main plugin;
    private static final int COORDS_PER_PAGE = 10;

    public CoordsDAO(Main plugin) {
        this.plugin = plugin;
        initCoordsFile();
    }

    private void initCoordsFile() {
        File coordsFile = new File(plugin.getDataFolder(), COORDS_FILE);
        if (!coordsFile.exists()) {
            try {
                coordsFile.createNewFile();
                try (PrintWriter writer = new PrintWriter(new FileWriter(coordsFile, true))) {
                    writer.println("id,name,x,y,z,world,owner");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create coordinates file: " + e.getMessage());
            }
        }
    }

    public String saveCoordinate(String name, double x, double y, double z, World world, Player player) {
        String coordId = generateCoordId();
        String worldName = world.getName();
        String playerUUID = player.getUniqueId().toString();

        try (PrintWriter writer = new PrintWriter(new FileWriter(new File(plugin.getDataFolder(), COORDS_FILE), true))) {
            writer.println(String.format("%s,%s,%.2f,%.2f,%.2f,%s,%s",
                    coordId, name, x, y, z, worldName, playerUUID));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save coordinate: " + e.getMessage());
            return null;
        }

        return coordId;
    }

    public boolean deleteCoordinate(String coordId, Player player) {
        List<String> lines = readCoordLines();
        boolean deleted = false;

        try (PrintWriter writer = new PrintWriter(new FileWriter(new File(plugin.getDataFolder(), COORDS_FILE)))) {
            writer.println("id,name,x,y,z,world,owner");

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    if (parts[0].equals(coordId) &&
                            (player.isOp() || parts[6].equals(player.getUniqueId().toString()))) {
                        deleted = true;
                        continue;
                    }
                }
                writer.println(line);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not delete coordinate: " + e.getMessage());
            return false;
        }

        return deleted;
    }

    public List<String> listCoordinates(int page) {
        List<String> lines = readCoordLines().stream()
                .skip((long) (page - 1) * COORDS_PER_PAGE)
                .limit(COORDS_PER_PAGE)
                .map(this::formatCoordLine)
                .collect(Collectors.toList());

        return lines;
    }

    public List<String> findCoordinatesByName(String searchName, int page) {
        List<String> matchingCoords = readCoordLines().stream()
                .filter(line -> line.toLowerCase().contains(searchName.toLowerCase()))
                .skip((long) (page - 1) * COORDS_PER_PAGE)
                .limit(COORDS_PER_PAGE)
                .map(this::formatCoordLine)
                .collect(Collectors.toList());

        return matchingCoords;
    }

    public String getCoordByName(String searchName) {
        List<String> matchingCoords = readCoordLines().stream()
                .filter(line -> line.toLowerCase().contains(searchName.toLowerCase()))
                .map(this::formatCoordLine)
                .toList();
        return matchingCoords.get(0);
    }

    public List<String> listPlayerCoordinates(Player player, int page) {
        List<String> playerCoords = readCoordLines().stream()
                .filter(line -> line.endsWith(player.getUniqueId().toString()))
                .skip((long) (page - 1) * COORDS_PER_PAGE)
                .limit(COORDS_PER_PAGE)
                .map(this::formatCoordLine)
                .collect(Collectors.toList());

        return playerCoords;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) readCoordLines().size() / COORDS_PER_PAGE);
    }

    public int getTotalPagesForSearch(String searchName) {
        long matchingCoords = readCoordLines().stream()
                .filter(line -> line.toLowerCase().contains(searchName.toLowerCase()))
                .count();
        return (int) Math.ceil((double) matchingCoords / COORDS_PER_PAGE);
    }

    public int getTotalPagesForPlayer(Player player) {
        long playerCoords = readCoordLines().stream()
                .filter(line -> line.endsWith(player.getUniqueId().toString()))
                .count();
        return (int) Math.ceil((double) playerCoords / COORDS_PER_PAGE);
    }

    private List<String> readCoordLines() {
        try {
            return java.nio.file.Files.readAllLines(
                            new File(plugin.getDataFolder(), COORDS_FILE).toPath())
                    .stream()
                    .skip(1)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read coordinates file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String formatCoordLine(String line) {
        String[] parts = line.split(",");
        if (parts.length >= 7) {
            return String.format(ChatColor.WHITE + "- %s " + ChatColor.BOLD + "%s" + ChatColor.RESET + ChatColor.WHITE + ": %.2f %.2f %.2f in %s",
                    parts[0], parts[1],
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    parts[5]);
        }
        return "§cInvalid coordinate entry";
    }
}