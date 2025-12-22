package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NEquipoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length == 0) {
            p.sendMessage("§c¡Debes escribir un nombre! /nequipo <nombre>");
            return true;
        }

        String nombre = String.join(" ", args);
        boolean exito = UHC_DBasic.getPlugin(UHC_DBasic.class).getTeamManager().renombrarEquipo(p, nombre);

        if (!exito) {
            p.sendMessage("§cNo puedes renombrar un equipo si no estás en uno.");
        }
        return true;
    }
}
