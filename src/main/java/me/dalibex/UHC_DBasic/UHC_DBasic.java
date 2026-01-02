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
import java.util.Collections;
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
        // 1. VERIFICAR DEPENDENCIAS | TAB Y SKINRESTORER
        if (!verificarDependencias()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        configurarTabAutomaticamente();

        // 2. CONFIGURACIÓN E IDIOMA
        saveDefaultConfig();
        languageManager = new LanguageManager(this);

        // 3. INICIALIZAR MANAGERS
        teamManager = new TeamManager();
        gameManager = new GameManager(this);

        // 5. INICIALIZAR RESTO DE COMPONENTES
        adminPanelManager = new AdminPanelManager(this);
        chatManager = new ChatManager(this);
        eventHandler = new UHC_EventManagerListener(this);
        specialCraftsManager = new SpecialCraftsManager(this);
        resourceRushListener = new ResourceRushListener(this);

        // 6. REGISTRAR EVENTOS
        getServer().getPluginManager().registerEvents(eventHandler, this);
        getServer().getPluginManager().registerEvents(resourceRushListener, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        // 7. REGISTRAR COMANDOS
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

            if (!gameManager.isPartidaIniciada()) {
                return "§f" + t.getName();
            }
            if (v.equals(t) || teamManager.areInSameTeam(v, t)) {
                return "§a" + t.getName();
            }
            if (gameManager.getJugadoresRevelados().contains(t.getUniqueId())) {
                return "§c" + t.getName();
            }
            String nombreFalso = gameManager.getUltimaSkinAsignada().getOrDefault(t.getUniqueId(), t.getName());
            return "§c" + nombreFalso;
        });
    }

    private void configurarTabAutomaticamente() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null) {
            getLogger().severe("TAB no encontrado. No se puede configurar.");
            return;
        }

        File configFile = new File(tabPlugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        boolean cambiosRealizados = false;

        int refreshActual = config.getInt("placeholder-refresh-intervals.%rel_uhc_identidad%", -1);
        if (refreshActual <= 0) {
            config.set("placeholder-refresh-intervals.%rel_uhc_identidad%", 50);
            cambiosRealizados = true;
            getLogger().info("Activado refresco de placeholders relacionales en TAB.");
        }

        if (config.contains("placeholder-refresh-intervals.relational-placeholders-refresh")) {
            config.set("placeholder-refresh-intervals.relational-placeholders-refresh", null); // Al poner null, se borra
            cambiosRealizados = true;
            getLogger().info("Eliminada configuración obsoleta/errónea de TAB.");
        }

        if (!config.getBoolean("unlimited-nametag-mode.enabled")) {
            config.set("unlimited-nametag-mode.enabled", true);

            config.set("unlimited-nametag-mode.disable-condition", "AN_IMPOSSIBLE_CONDITION");

            config.set("unlimited-nametag-mode.use-alternate-armor-stand-names", false);

            cambiosRealizados = true;
            getLogger().info("Activado Unlimited Nametag Mode en TAB.");
        }

        List<String> sortingList = config.getStringList("group-sorting-priority-list");
        if (!sortingList.contains("none")) {
            sortingList.add("none");
            config.set("group-sorting-priority-list", sortingList);
            cambiosRealizados = true;
        }

        List<String> sortingTypes = config.getStringList("sorting-types");
        if (sortingTypes.isEmpty() || !sortingTypes.get(0).equals("CASE_SENSITIVE_A_TO_Z")) {
            config.set("sorting-types", Collections.singletonList("CASE_SENSITIVE_A_TO_Z"));
            cambiosRealizados = true;
        }

        if (cambiosRealizados) {
            try {
                config.save(configFile);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
                    getLogger().info("Configuración de TAB actualizada y recargada automáticamente.");
                }, 40L);
            } catch (IOException e) {
                getLogger().severe("No se pudo guardar la configuración de TAB: " + e.getMessage());
            }
        }
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