package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.AdminPanel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminPanelCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public AdminPanelCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLang().get("general.only-players"));
            return true;
        }

        Player player = (Player) sender;

        if(!player.isOp()) {
            sender.sendMessage(plugin.getLang().get("general.no-permission"));
            return true;
        }

        AdminPanel.openMainAdminPanel(player);
        return true;
    }
}
