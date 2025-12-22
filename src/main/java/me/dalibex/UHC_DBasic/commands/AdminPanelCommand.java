package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.managers.AdminPanel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminPanelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando es solo para jugadores.");
            return true;
        }

        Player player = (Player) sender;

        if(!player.isOp()) {
            sender.sendMessage("§cEste comando es solo para administradores.");
            return true;
        }

        AdminPanel.openMainAdminPanel(player);
        return true;
    }
}
