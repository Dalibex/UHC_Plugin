package me.dalibex.UHC_DBasic.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LanguageManager {
    private final UHC_DBasic plugin;
    private final Map<String, FileConfiguration> langConfigs = new HashMap<>();
    private final Map<UUID, String> playerPreferences = new HashMap<>();
    private File preferencesFile;
    private FileConfiguration preferencesConfig;

    public LanguageManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        loadAllLanguages();
        loadPlayerPreferences();
    }

    private void loadAllLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        String[] supported = {"es", "en"};
        for (String lang : supported) {
            String fileName = "lang/messages_" + lang + ".yml";
            File file = new File(plugin.getDataFolder(), fileName);

            if (!file.exists()) {
                plugin.saveResource(fileName, false);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            langConfigs.put(lang, config);
        }
    }

    private void loadPlayerPreferences() {
        preferencesFile = new File(plugin.getDataFolder(), "player_langs.yml");
        if (!preferencesFile.exists()) {
            try { preferencesFile.createNewFile(); } catch (IOException e) {}
        }
        preferencesConfig = YamlConfiguration.loadConfiguration(preferencesFile);

        for (String uuidStr : preferencesConfig.getKeys(false)) {
            playerPreferences.put(UUID.fromString(uuidStr), preferencesConfig.getString(uuidStr));
        }
    }

    public void setPlayerLanguage(Player player, String lang) {
        if (!langConfigs.containsKey(lang)) return;

        playerPreferences.put(player.getUniqueId(), lang);
        preferencesConfig.set(player.getUniqueId().toString(), lang);
        try { preferencesConfig.save(preferencesFile); } catch (IOException e) {}
    }

    public String getPlayerLang(Player player) {
        if (player == null) {
            return plugin.getConfig().getString("language", "es");
        }
        return playerPreferences.getOrDefault(player.getUniqueId(), plugin.getConfig().getString("language", "es"));
    }

    public String get(String path, Player player) {
        String lang = getPlayerLang(player);
        FileConfiguration config = langConfigs.get(lang);

        if (config == null) {
            config = langConfigs.get("es");
        }

        if (config == null) return "§cLanguage files missing";

        String message = config.getString(path);
        if (message == null) return "§cKey not found: " + path;

        return message.replace('&', '§');
    }

    public Component getComponent(String path, Player player) {
        return LegacyComponentSerializer.legacySection().deserialize(get(path, player)).decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> getComponentList(String path, Player player) {
        List<String> list = getList(path, player);
        List<Component> result = new ArrayList<>();
        for (String s : list) {
            result.add(LegacyComponentSerializer.legacySection().deserialize(s).decoration(TextDecoration.ITALIC, false));
        }
        return result;
    }

    public List<String> getList(String path, Player player) {
        String lang = getPlayerLang(player);
        FileConfiguration config = langConfigs.get(lang);

        if (config == null) {
            config = langConfigs.get("es");
        }

        if (config == null) return Collections.singletonList("§cLanguage files missing");

        List<String> list = config.getStringList(path);

        // Seguridad: Si la lista está vacía o no existe
        if (list.isEmpty()) {
            return Collections.singletonList("§cList not found: " + path);
        }

        list.replaceAll(line -> line.replace('&', '§'));
        return list;
    }
}