package me.dalibex.UHC_DBasic;

import me.dalibex.UHC_DBasic.commands.*;
import me.dalibex.UHC_DBasic.listeners.ResourceRushListener;
import me.dalibex.UHC_DBasic.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class UHC_DBasic extends JavaPlugin {

    private GameManager gameManager;
    private TeamManager teamManager;
    private AdminPanelManager adminPanelManager;
    private ChatManager chatManager;
    private UHC_EventManager eventHandler;
    private SpecialCraftsManager specialCraftsManager;
    private LanguageManager languageManager;
    private ResourceRushListener resourceRushListener;

    @Override
    public void onEnable() {
        // 1. CARGAR CONFIGURACIÓN E IDIOMA
        saveDefaultConfig();

        // El LanguageManager ahora carga todos los archivos de la carpeta /lang
        languageManager = new LanguageManager(this);

        // 3. INICIALIZAR RESTO DE MANAGERS
        teamManager = new TeamManager();
        gameManager = new GameManager(this);
        adminPanelManager = new AdminPanelManager(this);
        chatManager = new ChatManager(this, gameManager);
        eventHandler = new UHC_EventManager(this);
        specialCraftsManager = new SpecialCraftsManager(this);
        resourceRushListener = new ResourceRushListener(this);

        // 4. REGISTRAR EVENTOS
        getServer().getPluginManager().registerEvents(eventHandler, this);
        getServer().getPluginManager().registerEvents(resourceRushListener, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        // 5. REGISTRAR COMANDOS
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand(this));
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("uhccommands").setExecutor(new GCommandsCommand(this));
        getCommand("nequipo").setExecutor(new NEquipoCommand(this));
        getCommand("tpartes").setExecutor(new TiempoPartesCommand(this));

        StartCommand startCmd = new StartCommand(this);
        getCommand("start").setExecutor(startCmd);
        getCommand("confirmarstart").setExecutor(new ConfirmStartCommand(this, startCmd));
        getCommand("cancelarstart").setExecutor(new CancelarStartCommand(this, startCmd));

        // COMANDO DE IDIOMA
        getCommand("lang").setExecutor(new LangCommand(this));
        getCommand("lang").setTabCompleter(new LangCommand(this));

        getCommand("test").setExecutor(((sender, command, s, strings) -> {
            sender.sendMessage("§a¡Plugin works fine!");
            return true;
        }));

        getLogger().info("UHC ELOUD Plugin Enabled");
    }

    public GameManager getRightPanelManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public AdminPanelManager getAdminPanel() {
        return adminPanelManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public SpecialCraftsManager getSpecialCraftsManager() {
        return specialCraftsManager;
    }

    public LanguageManager getLang() {
        return languageManager;
    }

    public UHC_EventManager getEventHandler() {
        return eventHandler;
    }

    @Override
    public void onDisable() {
        getLogger().info("UHC_DBasic Plugin Disabled");
    }
}