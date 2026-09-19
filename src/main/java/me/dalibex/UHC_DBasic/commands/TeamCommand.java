package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public TeamCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) {
            p.sendMessage(plugin.getLang().get("teams.name-required", p));
            return true;
        }

        String nombre = String.join(" ", args);
        boolean exito = plugin.getTeamManager().renameTeam(p, nombre);

        if (!exito) {
            p.sendMessage(plugin.getLang().get("teams.no-team", p));
        }

        return true;
    }
}