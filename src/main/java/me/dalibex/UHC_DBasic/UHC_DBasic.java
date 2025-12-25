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

    @Override
    public void onEnable() {
        getLogger().info("UHC_DBasic Plugin Enabled");

        // Registrar Managers
        rightPanelManager = new RightPanelManager(this);
        teamManager = new TeamManager();
        adminPanel = new AdminPanel();
        chatManager = new ChatManager();
        eventHandler = new UHC_EventManager(this);
        specialCraftsManager = new SpecialCraftsManager(this);

        // Registrar Eventos
        getServer().getPluginManager().registerEvents(eventHandler, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        // Registrar Comandos
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand());
        getCommand("start").setExecutor(new StartCommand());
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("confirmarstart").setExecutor(new ConfirmCommand(this));
        getCommand("uhccommands").setExecutor(new GCommandsCommand());
        getCommand("nequipo").setExecutor(new NEquipoCommand());
        getCommand("tpartes").setExecutor(new TiempoPartesCommand(this));

        // Comando de prueba TEST
        getCommand("test").setExecutor(((sender, command, s, strings) -> {
            sender.sendMessage("§a¡El plugin de UHC está funcionando perfectamente!");
            return true;
        }));
    }

    @Override
    public @NotNull Path getDataPath() {
        return super.getDataPath();
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

    public UHC_EventManager getEventHandler() {
        return eventHandler;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("UHC_DBasic Plugin Disabled");
    }
}
