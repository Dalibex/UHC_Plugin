package me.dalibex.UHC_DBasic.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.dalibex.UHC_DBasic.UHC_DBasic;

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

        if (plugin.getGameManager().isGameStarted()) {
            return true;
        }

        if (!startCommand.hasPendingConfirmation()) {
            return true;
        }

        startCommand.setConfirmationPending(false);
        plugin.getGameManager().setGameStarted(false);

        player.sendMessage(plugin.getLang().get("start-menu.cancelled", player));

        return true;
    }
}
