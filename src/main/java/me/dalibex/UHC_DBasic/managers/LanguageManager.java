package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class LanguageManager {
    private final UHC_DBasic plugin;
    private FileConfiguration langConfig;

    public LanguageManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        String lang = plugin.getConfig().getString("language", "es");
        File langFile = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/messages_" + lang + ".yml", false);
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String get(String path) {
        String message = langConfig.getString(path);
        if (message == null) return "§cKey not found: " + path;

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> getList(String path) {
        List<String> list = langConfig.getStringList(path);
        list.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line));
        return list;
    }
}
