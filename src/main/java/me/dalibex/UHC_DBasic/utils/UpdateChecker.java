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
 * Consulta primero la última release de GitHub; si el repositorio no tiene
 * releases publicadas, hace fallback a la lista de tags y usa la más alta.
 */
public class UpdateChecker {

    private final UHC_DBasic plugin;
    private final String currentVersion;
    private static final String RELEASES_URL = "https://api.github.com/repos/Dalibex/UHC_Plugin/releases/latest";
    private static final String TAGS_URL = "https://api.github.com/repos/Dalibex/UHC_Plugin/tags?per_page=100";

    private static volatile String latestVersionFound = null;
    private static volatile String currentVersionChecked = null;
    private static volatile boolean checkDone = false;

    public UpdateChecker(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    public static String getLatestVersionFound() {
        return latestVersionFound;
    }

    public static boolean isCheckDone() {
        return checkDone;
    }

    /** true si la comprobación terminó y existe una versión más reciente. */
    public static boolean isUpdateAvailable() {
        return checkDone && latestVersionFound != null && currentVersionChecked != null
                && compareVersions(latestVersionFound, currentVersionChecked) > 0;
    }

    /**
     * Comprueba la versión contra el repositorio de GitHub.
     */
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String latest = fetchLatestFromReleases();
                if (latest == null) {
                    latest = fetchHighestFromTags();
                }
                latestVersionFound = latest;
                currentVersionChecked = currentVersion;
                checkDone = true;

                if (latest == null) {
                    plugin.getLogger().warning("Could not get the latest version from GitHub.");
                    return;
                }

                if (compareVersions(latest, currentVersion) > 0) {
                    notifyUpdatedVersion(latest);
                } else {
                    plugin.getLogger().info(() -> "The plugin is up to date (v" + currentVersion + ").");
                }
            } catch (IOException e) {
                checkDone = true;
                plugin.getLogger().warning(() -> "Could not check for updates: " + e.getMessage());
            }
        });
    }

    private String fetchLatestFromReleases() throws IOException {
        String body = httpGet(RELEASES_URL);
        if (body == null) return null;
        // Parseo simple de JSON para encontrar el tag_name
        String marker = "\"tag_name\":\"";
        int start = body.indexOf(marker);
        if (start == -1) return null;
        String after = body.substring(start + marker.length());
        return after.substring(0, after.indexOf('"'));
    }

    private String fetchHighestFromTags() throws IOException {
        String body = httpGet(TAGS_URL);
        if (body == null) return null;
        String highest = null;
        // Parseo simple: cada \"name\":\"<tag>\"
        String marker = "\"name\":\"";
        int idx = 0;
        while ((idx = body.indexOf(marker, idx)) != -1) {
            String after = body.substring(idx + marker.length());
            String tag = after.substring(0, after.indexOf('"'));
            if (highest == null || compareVersions(tag, highest) > 0) {
                highest = tag;
            }
            idx = idx + marker.length();
        }
        return highest;
    }

    private String httpGet(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        connection.setRequestProperty("User-Agent", "UHC-Plugin-UpdateChecker");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);

        if (connection.getResponseCode() != 200) return null;

        StringBuilder response;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Compara dos versiones numéricamente (sin prefijo v ni sufijo -SNAPSHOT).
     * Devuelve >0 si a > b, 0 si iguales, <0 si a < b.
     */
    public static int compareVersions(String a, String b) {
        int[] pa = versionParts(a);
        int[] pb = versionParts(b);
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int[] versionParts(String version) {
        String v = version == null ? "" : version.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        int dash = v.indexOf('-');
        if (dash >= 0) v = v.substring(0, dash);
        String[] parts = v.split("[^0-9]+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                try {
                    result[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    result[i] = 0;
                }
            }
        }
        return result;
    }

    private void notifyUpdatedVersion(String latest) {
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§e [UHC UPDATE] A new version is available!"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Your version: §c" + currentVersion));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Latest version: §a" + latest));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Download it at: §bhttps://github.com/Dalibex/UHC_Plugin/releases"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
    }
}