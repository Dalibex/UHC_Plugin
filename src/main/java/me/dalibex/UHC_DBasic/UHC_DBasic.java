package me.dalibex.UHC_DBasic;

import me.dalibex.UHC_DBasic.commands.*;
import me.dalibex.UHC_DBasic.managers.AdminPanel;
import me.dalibex.UHC_DBasic.managers.ChatManager;
import me.dalibex.UHC_DBasic.managers.RightPanelManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class UHC_DBasic extends JavaPlugin implements Listener {

    private RightPanelManager rightPanelManager;
    private TeamManager teamManager;

    @Override
    public void onEnable() {
        getLogger().info("UHC_DBasic Plugin Enabled");

        // Registrar Managers
        rightPanelManager = new RightPanelManager(this);
        teamManager = new TeamManager();

        // Registrar Eventos
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new AdminPanel(), this);
        getServer().getPluginManager().registerEvents(new ChatManager(), this);
        getServer().getPluginManager().registerEvents(rightPanelManager, this);

        // Registrar Comandos
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand());
        getCommand("start").setExecutor(new StartCommand());
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("confirmarstart").setExecutor(new ConfirmCommand(this));
        getCommand("uhccommands").setExecutor(new GCommandsCommand());
        getCommand("nequipo").setExecutor(new NEquipoCommand());

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

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("UHC_DBasic Plugin Disabled");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
