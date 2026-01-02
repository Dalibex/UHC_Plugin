package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CancelarStartCommand implements CommandExecutor {

    private final UHC_DBasic plugin;
    private final StartCommand startCommand;

    public CancelarStartCommand(UHC_DBasic plugin, StartCommand startCommand) {
        this.plugin = plugin;
        this.startCommand = startCommand;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.isOp()) return true;

        if (plugin.getGameManager().isPartidaIniciada()) {
            return true;
        }

        if (!startCommand.getConfirmacionPendiente()) {
            return true;
        }

        startCommand.setConfirmacionPendiente(false);
        plugin.getGameManager().setPartidaIniciada(false);

        player.sendMessage(plugin.getLang().get("start-menu.cancelled", player));

        return true;
    }
}
