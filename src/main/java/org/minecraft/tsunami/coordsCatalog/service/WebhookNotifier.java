package org.minecraft.tsunami.coordsCatalog.service;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.minecraft.tsunami.coordsCatalog.Main;
import org.minecraft.tsunami.coordsCatalog.config.ConfigManager;
import org.minecraft.tsunami.coordsCatalog.data.Coordinate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public class WebhookNotifier {

    private final Main plugin;
    private final ConfigManager configManager;

    public WebhookNotifier(Main plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void notifyCoordinateAdded(Coordinate coord, Player player) {
        if (configManager.isWebhooksEnabled()) return;

        String playerName = (player != null) ? player.getName() : "Console";
        String message = String.format("""
                        ✅ **Coordinate Added**
                        **ID:** `%s`
                        **Name:** %s
                        **Location:** `%.2f %.2f %.2f`
                        **World:** %s
                        **Added by:** %s""",
                coord.getId(), coord.getName(), coord.getX(), coord.getY(), coord.getZ(),
                coord.getWorldName(), playerName);
        sendWebhookMessage(message);
    }

    public void notifyCoordinateDeleted(Coordinate coord, Player player) {
        if (configManager.isWebhooksEnabled()) return;

        String playerName = (player != null) ? player.getName() : "Console";
        String ownerName = coord.getOwnerName();
        String message = String.format("""
                        ❌ **Coordinate Deleted**
                        **ID:** `%s`
                        **Name:** %s (Owned by: %s)
                        **Location:** `%.2f %.2f %.2f`
                        **World:** %s
                        **Deleted by:** %s""",
                coord.getId(), coord.getName(), ownerName, coord.getX(), coord.getY(), coord.getZ(),
                coord.getWorldName(), playerName);
        sendWebhookMessage(message);
    }

    public void sendCustomWebhookMessage(String message) {
        if (configManager.isWebhooksEnabled()) return;
        sendWebhookMessage(message);
    }


    private void sendWebhookMessage(String messageContent) {
        List<String> webhookUrls = configManager.getWebhookUrls();
        if (webhookUrls == null || webhookUrls.isEmpty()) {
            return;
        }

        String escapedMessage = messageContent.replace("\\", "\\\\").replace("\"", "\\\"");
        String jsonPayload = String.format("{\"content\": \"%s\"}", escapedMessage);

        for (String urlString : webhookUrls) {
            if (urlString == null || urlString.trim().isEmpty()) continue;

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection connection = getHttpURLConnection(urlString, jsonPayload);

                        int responseCode = connection.getResponseCode();
                        if (responseCode < 200 || responseCode >= 300) {
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                                StringBuilder response = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response.append(responseLine.trim());
                                }
                                plugin.getLogger().warning("Webhook failed for URL " + urlString + ". Code: " + responseCode + ". Response: " + response);
                            } catch (IOException ioex) {
                                plugin.getLogger().warning("Webhook failed for URL " + urlString + ". Code: " + responseCode + ". Could not read error stream.");
                            }
                        }

                        connection.disconnect();

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to send webhook message to " + urlString, e);
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    @NotNull
    private static HttpURLConnection getHttpURLConnection(String urlString, String jsonPayload) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "CoordsCatalog MinecraftPlugin");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }
}