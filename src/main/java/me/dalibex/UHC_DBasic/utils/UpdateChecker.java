package me.dalibex.UHC_DBasic.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Utilidad para verificar actualizaciones del plugin de forma asíncrona.
 * Consulta primero la última release de GitHub; si el repositorio no tiene
 * releases publicadas, hace fallback a la lista de tags y usa la más alta.
 */
public class UpdateChecker {
    private static final Pattern VERSION_NUMBER = Pattern.compile("\\d+");
    private static final Pattern RELEASE_TAG = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TAG_NAME = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

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
        latestVersionFound = null;
        currentVersionChecked = null;
        checkDone = false;

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
        return parseReleaseTag(body);
    }

    private String fetchHighestFromTags() throws IOException {
        String body = httpGet(TAGS_URL);
        if (body == null) return null;
        return parseHighestTag(body);
    }

    static String parseReleaseTag(String body) {
        if (body == null) return null;
        Matcher matcher = RELEASE_TAG.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    static String parseHighestTag(String body) {
        if (body == null) return null;
        String highest = null;
        Matcher matcher = TAG_NAME.matcher(body);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (highest == null || compareVersions(tag, highest) > 0) {
                highest = tag;
            }
        }
        return highest;
    }

    private String httpGet(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
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
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Compara dos versiones numéricamente (sin prefijo v ni sufijos pre-release).
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
        List<Integer> parts = new ArrayList<>();
        Matcher matcher = VERSION_NUMBER.matcher(v);
        while (matcher.find()) {
            try {
                parts.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException e) {
                parts.add(0);
            }
        }
        return parts.stream().mapToInt(Integer::intValue).toArray();
    }

    private void notifyUpdatedVersion(String latest) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§e [UHC UPDATE] A new version is available!"));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Your version: §c" + currentVersion));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Latest version: §a" + latest));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§f Download it at: §bhttps://github.com/Dalibex/UHC_Plugin/releases"));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize("§6--------------------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(legacySection().deserialize(" "));
        });
    }
}
