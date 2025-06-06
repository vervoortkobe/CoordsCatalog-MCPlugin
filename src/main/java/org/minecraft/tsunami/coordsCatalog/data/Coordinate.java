package org.minecraft.tsunami.coordsCatalog.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class Coordinate {

    private final String id;
    private final String name;
    private final double x;
    private final double y;
    private final double z;
    private final String worldName;
    private final UUID ownerUUID;

    public Coordinate(String id, String name, double x, double y, double z, String worldName, UUID ownerUUID) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.ownerUUID = ownerUUID;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public String getWorldName() { return worldName; }
    public UUID getOwnerUUID() { return ownerUUID; }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public Location getLocation() {
        World world = getWorld();
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public String getOwnerName() {
        if (ownerUUID == null) {
            return "Server";
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        return owner.getName();
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", worldName='" + worldName + '\'' +
                ", ownerUUID=" + ownerUUID +
                '}';
    }
}