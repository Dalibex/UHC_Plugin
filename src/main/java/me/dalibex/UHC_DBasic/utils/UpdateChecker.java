package me.dalibex.UHC_DBasic.utils;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

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
        this.currentVersion = plugin.getDescription().getVersion();
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
                URL url = new URL(githubUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "UHC-Plugin-UpdateChecker");

                if (connection.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // Parseo simple de JSON para encontrar el tag_name
                    String json = response.toString();
                    if (json.contains("\"tag_name\":\"")) {
                        String latestVersion = json.split("\"tag_name\":\"")[1].split("\"")[0];
                        latestVersionFound = latestVersion;
                        
                        if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                            notifyUpdatedVersion(latestVersion);
                        } else {
                            plugin.getLogger().info("§aEl plugin está actualizado (v" + currentVersion + ").");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo verificar la versión: " + e.getMessage());
            }
        });
    }

    private void notifyUpdatedVersion(String latest) {
        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + " [UHC UPDATE] ¡Nueva versión disponible!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + " Versión actual: " + ChatColor.RED + currentVersion);
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + " Versión nueva: " + ChatColor.GREEN + latest);
        Bukkit.getConsoleSender().sendMessage(ChatColor.WHITE + " Descárgala en: " + ChatColor.AQUA + "https://github.com/Dalibex/UHC_Plugin/releases");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage(" ");
    }
}
