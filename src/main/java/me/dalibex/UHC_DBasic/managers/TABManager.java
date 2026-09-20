package me.dalibex.UHC_DBasic.managers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;

/**
 * Integración con el plugin TAB: config automática (animaciones y header/footer),
 * placeholders relacionales de identidad y el formato de nombre en el tablist.
 */
public class TABManager {

    private final UHC_DBasic plugin;

    public TABManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        createTABAnimations();
        setupTABAutomatically();
    }

    public void registerPlaceholders() {
        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_uhc_identidad%", 500, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            Player v = Bukkit.getPlayer(viewer.getUniqueId());
            Player t = Bukkit.getPlayer(target.getUniqueId());
            if (v == null || t == null) return "";
            if (!plugin.getGameManager().isGameStarted()) { return "§f" + t.getName(); }
            if (v.equals(t) || plugin.getTeamManager().areInSameTeam(v, t)) { return "§a" + t.getName(); }
            if (plugin.getSkinsManager().getRevealedPlayers().contains(t.getUniqueId())) { return "§c" + t.getName(); }
            String nombreFalso = plugin.getSkinsManager().getLastAssignedSkin().getOrDefault(t.getUniqueId(), t.getName());
            return "§d" + nombreFalso;
        });

        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_nametag_color%", 500, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            Player v = Bukkit.getPlayer(viewer.getUniqueId());
            Player t = Bukkit.getPlayer(target.getUniqueId());
            if (v == null || t == null) return "";
            if (!plugin.getGameManager().isGameStarted()) {return "§f";}
            if (v.equals(t) || plugin.getTeamManager().areInSameTeam(v, t)) {return "§a";}
            if (plugin.getSkinsManager().getRevealedPlayers().contains(t.getUniqueId())) {return "§c"; }
            return "§c";
        });
    }

    public void updateTabIdentity(Player p) {
        if (p == null) return;

        TabAPI tabApi = TabAPI.getInstance();
        me.neznamy.tab.api.TabPlayer tabPlayer = tabApi.getPlayer(p.getUniqueId());
        if (tabPlayer == null) return;

        GamePhase phase = plugin.getGameManager().getPhase();
        if (phase != GamePhase.LOBBY && phase != GamePhase.ENDING) {
            TabListFormatManager tfm = tabApi.getTabListFormatManager();
            NameTagManager ntm = tabApi.getNameTagManager();
            if (tfm != null) {
                tfm.setName(tabPlayer, "%rel_uhc_identidad%");
            }
            if (ntm != null) {
                ntm.setPrefix(tabPlayer, "%%rel_nametag_color%");
            }
        }
    }

    private void setupTABAutomatically() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null) return;

        File configFile = new File(tabPlugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        config.set("scoreboard-teams.sorting-types", Arrays.asList("PLACEHOLDER_A_TO_Z:%player%", "GROUPS:owner,admin,mod,helper,builder,vip,default,none"));
        config.set("playerlist-objective.enabled", false);
        config.set("playerlist-objective.value", 0);

        // CONFIGURACIÓN HEADER Y FOOTER
        config.set("header-footer.enabled", true);
        List<String> header = Arrays.asList(
                "<#FFFFFF>&m                                       </#FFFF00>",
                "",
                "%animation:UHC-Brillo%",
                ""
        );
        List<String> footer = Arrays.asList(
                "",
                "&fPing: &6%ping%ms",
                "%animation:Firma-Brillo%",
                "",
                "<#FFFFFF>&m                                       </#FFFF00>"
        );
        config.set("header-footer.designs.default.header", header);
        config.set("header-footer.designs.default.footer", footer);

        try {
            config.save(configFile);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            }, 40L);
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "ERROR SAVING TAB CONFIG: " + e.getMessage());
        }
    }

    private void createTABAnimations() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null) return;

        File animFile = new File(tabPlugin.getDataFolder(), "animations.yml");
        FileConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);

        List<String> framesTitulo = Arrays.asList(
                "&6&lUHC ELOUD", "&e&lUHC ELOUD", "&f&lU&e&lHC ELOUD",
                "&6&lU&f&lH&e&lC ELOUD", "&6&lUH&f&lC&e&l ELOUD", "&6&lUHC &f&lE&e&lLOUD",
                "&6&lUHC E&f&lL&e&lOUD", "&6&lUHC EL&f&lO&e&lUD", "&6&lUHC ELO&f&lU&e&lD",
                "&6&lUHC ELOU&f&lD", "&6&lUHC ELOUD"
        );
        animConfig.set("UHC-Brillo.texts", framesTitulo);
        animConfig.set("UHC-Brillo.change-interval", 100);

        List<String> framesFirma = Arrays.asList(
                "&6made by Dalibex", "&emade by Dalibex", "&fmade by Dalibex",
                "&6made by Dalibex", "&6made by Dalibex"
        );
        animConfig.set("Firma-Brillo.texts", framesFirma);
        animConfig.set("Firma-Brillo.change-interval", 100);

        try {
            animConfig.save(animFile);
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "ERROR SAVING animations.yml: " + e.getMessage());
        }
    }
}