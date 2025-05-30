package org.minecraft.tsunami.coordsCatalog.data;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.util.CoordsUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CoordsManager {

    private final Main plugin;
    private final File coordsFile;
    private FileConfiguration coordsConfig;
    private final Map<String, Coordinate> coordinatesCache = new HashMap<>(); // In-memory cache

    public CoordsManager(Main plugin) {
        this.plugin = plugin;
        this.coordsFile = new File(plugin.getDataFolder(), "coords.yml");
    }

    public void loadCoords() {
        if (!coordsFile.exists()) {
            plugin.getLogger().info("coords.yml not found, creating a new one...");
            try {
                coordsFile.getParentFile().mkdirs();
                coordsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create coords.yml!", e);
                return;
            }
        }

        coordsConfig = YamlConfiguration.loadConfiguration(coordsFile);
        coordinatesCache.clear();

        ConfigurationSection coordsSection = coordsConfig.getConfigurationSection("coordinates");
        if (coordsSection != null) {
            for (String id : coordsSection.getKeys(false)) {
                ConfigurationSection entry = coordsSection.getConfigurationSection(id);
                if (entry != null) {
                    try {
                        String name = entry.getString("name", "Unnamed");
                        double x = entry.getDouble("x");
                        double y = entry.getDouble("y");
                        double z = entry.getDouble("z");
                        String worldName = entry.getString("world");
                        String ownerUUID = entry.getString("owner");

                        World world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
                        UUID owner = (ownerUUID != null) ? UUID.fromString(ownerUUID) : null;

                        if (world != null) {
                            Coordinate coord = new Coordinate(id, name, x, y, z, worldName, owner);
                            coordinatesCache.put(id, coord);
                        } else {
                            plugin.getLogger().warning("Could not load coordinate '" + name + "' (ID: " + id + ") because world '" + worldName + "' is not loaded or does not exist.");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load coordinate entry with ID: " + id, e);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + coordinatesCache.size() + " coordinates from coords.yml.");
    }

    public void saveCoords() {
        ConfigurationSection coordsSection = coordsConfig.createSection("coordinates");

        for (Coordinate coord : coordinatesCache.values()) {
            ConfigurationSection entry = coordsSection.createSection(coord.getId());
            entry.set("name", coord.getName());
            entry.set("x", coord.getX());
            entry.set("y", coord.getY());
            entry.set("z", coord.getZ());
            entry.set("world", coord.getWorldName());
            entry.set("owner", coord.getOwnerUUID() != null ? coord.getOwnerUUID().toString() : null);
        }

        try {
            coordsConfig.save(coordsFile);
            plugin.getLogger().info("Saved " + coordinatesCache.size() + " coordinates to coords.yml.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save coords.yml!", e);
        }
    }

    public Coordinate addCoordinate(String name, double x, double y, double z, World world, Player player) {
        String id = generateUniqueCoordId();
        String worldName = world.getName();
        UUID ownerUUID = player.getUniqueId();

        Coordinate newCoord = new Coordinate(id, name, x, y, z, worldName, ownerUUID);
        coordinatesCache.put(id, newCoord);
        saveCoords();

        plugin.getWebhookNotifier().notifyCoordinateAdded(newCoord, player);

        return newCoord;
    }

    public Coordinate deleteCoordinate(String coordId, Player requester) {
        Coordinate coord = coordinatesCache.get(coordId);
        if (coord == null) {
            return null;
        }

        boolean isOwner = coord.getOwnerUUID() != null && coord.getOwnerUUID().equals(requester.getUniqueId());
        boolean isAdmin = requester.hasPermission("coordscatalog.admin") || requester.isOp(); // Adjust permission if needed

        if (!isAdmin && !isOwner) {
            return null;
        }

        Coordinate removedCoord = coordinatesCache.remove(coordId);
        if (removedCoord != null) {
            saveCoords();

            plugin.getWebhookNotifier().notifyCoordinateDeleted(removedCoord, requester);
        }
        return removedCoord;
    }


    public Coordinate getCoordinateById(String coordId) {
        return coordinatesCache.get(coordId);
    }

    public List<Coordinate> getAllCoordinates() {
        return new ArrayList<>(coordinatesCache.values());
    }

    public List<Coordinate> getCoordinatesByOwner(UUID ownerUUID) {
        return coordinatesCache.values().stream()
                .filter(coord -> ownerUUID.equals(coord.getOwnerUUID()))
                .collect(Collectors.toList());
    }

    public List<Coordinate> findCoordinatesByName(String searchName) {
        String lowerSearch = searchName.toLowerCase();
        return coordinatesCache.values().stream()
                .filter(coord -> coord.getName().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toList());
    }

    public List<Coordinate> findCoordinatesByIdOrName(String search) {
        String lowerSearch = search.toLowerCase();
        return coordinatesCache.values().stream()
                .filter(coord -> coord.getId().equals(lowerSearch) || coord.getName().toLowerCase().contains(lowerSearch))
                .sorted(Comparator.comparing(Coordinate::getName))
                .collect(Collectors.toList());
    }


    private String generateUniqueCoordId() {
        String id;
        do {
            id = CoordsUtil.generateCoordId();
        } while (coordinatesCache.containsKey(id));
        return id;
    }

    public List<Coordinate> getPaginatedCoordinates(List<Coordinate> allCoords, int page, int coordsPerPage) {
        if (page < 1) page = 1;
        int startIndex = (page - 1) * coordsPerPage;
        if (startIndex >= allCoords.size()) {
            return new ArrayList<>();
        }
        int endIndex = Math.min(startIndex + coordsPerPage, allCoords.size());
        return allCoords.subList(startIndex, endIndex);
    }

    public int getTotalPages(int totalCoords, int coordsPerPage) {
        if (coordsPerPage <= 0) return 1;
        return (int) Math.ceil((double) totalCoords / coordsPerPage);
    }
}