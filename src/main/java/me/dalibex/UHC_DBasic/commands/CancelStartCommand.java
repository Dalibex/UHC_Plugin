package me.dalibex.UHC_DBasic.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GamePhase;

public class CancelStartCommand implements CommandExecutor {

    private final UHC_DBasic plugin;
    private final StartCommand startCommand;

    public CancelStartCommand(UHC_DBasic plugin, StartCommand startCommand) {
        this.plugin = plugin;
        this.startCommand = startCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!plugin.isAdmin(player)) return true;

        GamePhase phase = plugin.getGameManager().getPhase();
        if (!startCommand.hasPendingConfirmation()
                && phase != GamePhase.PREPARING
                && phase != GamePhase.COUNTDOWN) {
            return true;
        }

        if (phase == GamePhase.PREPARING || phase == GamePhase.COUNTDOWN) {
            plugin.getGameManager().fullReset();
        } else {
            plugin.getGameManager().cancelStartup();
        }

        player.sendMessage(plugin.getLang().get("start-menu.cancelled", player));

        return true;
    }
}
