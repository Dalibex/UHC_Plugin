package me.dalibex.UHC_DBasic.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.bukkit.Bukkit;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Utilidad para verificar actualizaciones del plugin de forma asíncrona.
 */
public class UpdateChecker {

    private final UHC_DBasic plugin;
    private final String currentVersion;
    private final String githubUrl = "https://api.github.com/repos/Dalibex/UHC_Plugin/releases/latest";
    private static String latestVersionFound = null;

    public UpdateChecker(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    public static String getLatestVersionFound() {
        return latestVersionFound;
    }

    /**
     * Comprueba la versión contra el repositorio de GitHub.
     */
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(githubUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "UHC-Plugin-UpdateChecker");

                if (connection.getResponseCode() == 200) {
                    StringBuilder response;
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                    }

                    // Parseo simple de JSON para encontrar el tag_name
                    String json = response.toString();
                    if (json.contains("\"tag_name\":\"")) {
                        String latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
                        latestVersionFound = latestVersion;
                        
                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            notifyUpdatedVersion(latestVersion);
                        } else {
                            plugin.getLogger().info(() -> "§aThe plugin is updated (v" + currentVersion + ").");
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning(() -> "Could not check version: " + e.getMessage());
            }
        });
    }

    private void notifyUpdatedVersion(String latest) {
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§e [UHC UPDATE] ¡New version avaliable!"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Current version: §c" + currentVersion));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f New version: §a" + latest));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Download it at: §bhttps://github.com/Dalibex/UHC_Plugin/releases"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
    }
}
