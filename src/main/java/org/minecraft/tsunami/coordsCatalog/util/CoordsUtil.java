package org.minecraft.tsunami.coordsCatalog.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Random;
import java.util.stream.Collectors;

public class CoordsUtil {
    public static double parseCoordinate(String arg, double defaultValue) {
        if (arg.equals("~")) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate: " + arg);
        }
    }

    public static World parseWorld(String worldArg) {
        worldArg = worldArg.toLowerCase();
        switch (worldArg) {
            case "overworld":
                return Bukkit.getWorld(String.valueOf(World.Environment.NORMAL));
            case "nether":
                return Bukkit.getWorld(String.valueOf(World.Environment.NETHER));
            case "end":
                return Bukkit.getWorld(String.valueOf(World.Environment.THE_END));
            default:
                throw new IllegalArgumentException("Invalid world: " + worldArg);
        }
    }

    public static String generateCoordId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        return random.ints(5, 0, chars.length())
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}
