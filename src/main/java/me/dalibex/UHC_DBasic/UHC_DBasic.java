package me.dalibex.UHC_DBasic;

import me.dalibex.UHC_DBasic.commands.*;
import me.dalibex.UHC_DBasic.listeners.ResourceRushListener;
import me.dalibex.UHC_DBasic.listeners.UHC_EventManagerListener;
import me.dalibex.UHC_DBasic.managers.*;
import me.neznamy.tab.api.TabAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class UHC_DBasic extends JavaPlugin {

    private GameManager gameManager;
    private TeamManager teamManager;
    private AdminPanelManager adminPanelManager;
    private ChatManager chatManager;
    private UHC_EventManagerListener eventHandler;
    private SpecialCraftsManager specialCraftsManager;
    private LanguageManager languageManager;
    private ResourceRushListener resourceRushListener;

    @Override
    public void onEnable() {
        // VERIFICAR DEPENDENCIAS | TAB Y SKINRESTORER
        if (!verificarDependencias()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        logBanner(); // Banner de UHC en console

        // INICIALIZAR MANAGERS
        teamManager = new TeamManager();
        gameManager = new GameManager(this);

        crearAnimacionesTab();
        configurarTabAutomaticamente();

        // CONFIGURACIÓN E IDIOMA
        saveDefaultConfig();
        languageManager = new LanguageManager(this);

        // INICIALIZAR RESTO DE COMPONENTES
        adminPanelManager = new AdminPanelManager(this);
        chatManager = new ChatManager(this);
        eventHandler = new UHC_EventManagerListener(this);
        specialCraftsManager = new SpecialCraftsManager(this);
        resourceRushListener = new ResourceRushListener(this);

        // REGISTRAR EVENTOS
        getServer().getPluginManager().registerEvents(eventHandler, this);
        getServer().getPluginManager().registerEvents(resourceRushListener, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        // REGISTRAR COMANDOS
        registrarComandos();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            registrarPlaceholder();
        }, 60L);

        getLogger().info("§aUHC ELOUD Plugin Enabled.");
    }

    private boolean verificarDependencias() {
        boolean tab = Bukkit.getPluginManager().getPlugin("TAB") != null;
        boolean skins = Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null;

        if (!tab || !skins) {
            getLogger().severe("--------------------------------------------------");
            getLogger().severe("   [UHC ERROR] MISSING REQUIRED DEPENDENCIES!");
            if (!tab)   getLogger().severe("   > TAB Plugin: NOT FOUND");
            if (!skins) getLogger().severe("   > SkinsRestorer Plugin: NOT FOUND");
            getLogger().severe("   The plugin will be disabled for safety.");
            getLogger().severe("--------------------------------------------------");
            return false;
        }
        return true;
    }

    private void registrarComandos() {
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand(this));
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("uhccommands").setExecutor(new GCommandsCommand(this));
        getCommand("nequipo").setExecutor(new NEquipoCommand(this));
        getCommand("tpartes").setExecutor(new TiempoPartesCommand(this));

        StartCommand startCmd = new StartCommand(this);
        getCommand("start").setExecutor(startCmd);
        getCommand("confirmarstart").setExecutor(new ConfirmStartCommand(this, startCmd));
        getCommand("cancelarstart").setExecutor(new CancelarStartCommand(this, startCmd));

        getCommand("lang").setExecutor(new LangCommand(this));
        getCommand("lang").setTabCompleter(new LangCommand(this));

        getCommand("test").setExecutor(((sender, command, s, strings) -> {
            sender.sendMessage("§a[UHC] Plugin and its dependencies (TAB/SkinsRestorer) working perfectly!");
            return true;
        }));
    }

    private void registrarPlaceholder() {
        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_uhc_identidad%", 500, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            Player v = Bukkit.getPlayer(viewer.getUniqueId());
            Player t = Bukkit.getPlayer(target.getUniqueId());
            if (v == null || t == null) return "";
            if (!gameManager.isPartidaIniciada()) { return "§f" + t.getName(); }
            if (v.equals(t) || teamManager.areInSameTeam(v, t)) { return "§a" + t.getName(); }
            if (gameManager.getJugadoresRevelados().contains(t.getUniqueId())) { return "§c" + t.getName(); }
            String nombreFalso = gameManager.getUltimaSkinAsignada().getOrDefault(t.getUniqueId(), t.getName());
            return "§d" + nombreFalso;
        });

        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_nametag_color%", 500, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            Player v = Bukkit.getPlayer(viewer.getUniqueId());
            Player t = Bukkit.getPlayer(target.getUniqueId());
            if (v == null || t == null) return "";
            if (!gameManager.isPartidaIniciada()) {return "§f";}
            if (v.equals(t) || teamManager.areInSameTeam(v, t)) {return "§a";}
            if (gameManager.getJugadoresRevelados().contains(t.getUniqueId())) {return "§c"; }
            return "§c";
        });
    }

    private void configurarTabAutomaticamente() {
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
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            }, 40L);
        } catch (IOException e) {
            getLogger().severe("ERROR SAVING TAB CONFIG: " + e.getMessage());
        }
    }

    private void crearAnimacionesTab() {
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
            getLogger().severe("ERROR SAVING animations.yml: " + e.getMessage());
        }
    }

    private void logBanner() {
        String prefix = "§6[UHC ELOUD] ";
        Bukkit.getConsoleSender().sendMessage("§6--------------------------------------------------");
        Bukkit.getConsoleSender().sendMessage("§e   _  _ _  _ ____    ____ _    ____ _  _ ___  ");
        Bukkit.getConsoleSender().sendMessage("§e   |  | |__| |       |___ |    |  | |  | |  \\ ");
        Bukkit.getConsoleSender().sendMessage("§e   |__| |  | |___    |___ |___ |__| |__| |__/ ");
        Bukkit.getConsoleSender().sendMessage("§6");
        Bukkit.getConsoleSender().sendMessage("§f   Developed by: §bDalibex");
        Bukkit.getConsoleSender().sendMessage("§f   Version: §a" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage("§f   Status: §2§lONLINE");
        Bukkit.getConsoleSender().sendMessage("§6--------------------------------------------------");
    }

    // --- GETTERS ---
    public GameManager getGameManager() { return gameManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public AdminPanelManager getAdminPanel() { return adminPanelManager; }
    public ChatManager getChatManager() { return chatManager; }
    public SpecialCraftsManager getSpecialCraftsManager() { return specialCraftsManager; }
    public LanguageManager getLang() { return languageManager; }
    public UHC_EventManagerListener getEventHandler() { return eventHandler; }

    @Override
    public void onDisable() {
        getLogger().info("UHC_DBasic Plugin Disabled");
    }
}