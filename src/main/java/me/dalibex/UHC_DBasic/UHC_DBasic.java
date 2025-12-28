package me.dalibex.UHC_DBasic;

import me.dalibex.UHC_DBasic.commands.*;
import me.dalibex.UHC_DBasic.managers.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class UHC_DBasic extends JavaPlugin {

    private RightPanelManager rightPanelManager;
    private TeamManager teamManager;
    private AdminPanel adminPanel;
    private ChatManager chatManager;
    private UHC_EventManager eventHandler;
    private SpecialCraftsManager specialCraftsManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        // 1. CARGAR CONFIGURACIÓN E IDIOMA
        saveDefaultConfig();

        // El LanguageManager ahora carga todos los archivos de la carpeta /lang
        languageManager = new LanguageManager(this);

        // 3. INICIALIZAR RESTO DE MANAGERS
        teamManager = new TeamManager();
        rightPanelManager = new RightPanelManager(this);
        adminPanel = new AdminPanel(this);
        chatManager = new ChatManager(this);
        eventHandler = new UHC_EventManager(this);
        specialCraftsManager = new SpecialCraftsManager(this);

        // 4. REGISTRAR EVENTOS
        getServer().getPluginManager().registerEvents(eventHandler, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        // 5. REGISTRAR COMANDOS
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand(this));
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("confirmarstart").setExecutor(new ConfirmCommand(this));
        getCommand("uhccommands").setExecutor(new GCommandsCommand(this));
        getCommand("nequipo").setExecutor(new NEquipoCommand(this));
        getCommand("tpartes").setExecutor(new TiempoPartesCommand(this));

        // Comando de idioma
        getCommand("lang").setExecutor(new LangCommand(this));
        getCommand("lang").setTabCompleter(new LangCommand(this));

        getCommand("test").setExecutor(((sender, command, s, strings) -> {
            sender.sendMessage("§a¡Plugin works fine!");
            return true;
        }));

        getLogger().info("UHC ELOUD Plugin Enabled");
    }

    public RightPanelManager getRightPanelManager() {
        return rightPanelManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public AdminPanel getAdminPanel() {
        return adminPanel;
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

    public void setGlobalLanguage(String langCode) {
        this.getConfig().set("language", langCode);
        this.saveConfig();
        this.languageManager = new LanguageManager(this);
        specialCraftsManager.actualizarReceta();
    }

    @Override
    public void onDisable() {
        getLogger().info("UHC_DBasic Plugin Disabled");
    }
}