package org.minecraft.tsunami.coordsCatalog.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;

import java.util.Random;
import java.util.stream.Collectors;

public class CoordsUtil {

    public static double parseCoordinate(String arg, double relativeValue) throws IllegalArgumentException {
        if (arg.equals("~")) {
            return relativeValue;
        }

        if (arg.startsWith("~")) {
            try {
                double offset = Double.parseDouble(arg.substring(1));
                return relativeValue + offset;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                throw new IllegalArgumentException("Invalid relative coordinate: " + arg);
            }
        }

        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate value: " + arg);
        }
    }

    @Nullable
    public static World parseWorld(String worldArg, @Nullable Location senderLocation) {
        if (worldArg == null || worldArg.trim().isEmpty()) {
            return (senderLocation != null) ? senderLocation.getWorld() : null;
        }

        String lowerArg = worldArg.toLowerCase();

        World world = Bukkit.getWorld(worldArg);
        if (world != null) {
            return world;
        }

        world = Bukkit.getWorlds().stream()
                .filter(w -> w.getName().equalsIgnoreCase(worldArg))
                .findFirst().orElse(null);
        if (world != null) {
            return world;
        }

        World.Environment env;
        switch (lowerArg) {
            case "world":
            case "overworld":
                env = World.Environment.NORMAL;
                break;
            case "nether":
            case "the_nether":
                env = World.Environment.NETHER;
                break;
            case "end":
            case "the_end":
                env = World.Environment.THE_END;
                break;
            default:
                return null;
        }

        World.Environment finalEnv = env;
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == finalEnv)
                .findFirst()
                .orElse(null);

    }

    public static String generateCoordId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        return random.ints(5, 0, chars.length())
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    public static String formatCoordForDisplay(Coordinate coord, ConfigManager configManager) {
        if (coord == null) {
            return configManager.getMessage("invalid-entry");
        }

        return configManager.getFormattedMessage("list-entry",
                "id", coord.getId(),
                "name", coord.getName(),
                "x", String.format("%.2f", coord.getX()),
                "y", String.format("%.2f", coord.getY()),
                "z", String.format("%.2f", coord.getZ()),
                "world", coord.getWorldName()
        );
    }
}