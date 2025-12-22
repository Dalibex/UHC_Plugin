package me.dalibex.UHC_DBasic.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GCommandsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        sender.sendMessage("§8§m--------------§r §6§lUHC DBASIC COMMANDS §8§m--------------");
        sender.sendMessage("");
        sender.sendMessage("§e/reset §7- §fPrepara el mundo, reglas y panel. (Usa esto al iniciar el servidor)");
        sender.sendMessage("");
        sender.sendMessage("§e/start <tamaño> §7- §fInicia la cuenta atrás tras una confirmación, teletransporta a los jugadores ya ajusta las reglas para iniciar la partida");
        sender.sendMessage("");
        sender.sendMessage("§e/test §7- §fVerifica si el plugin responde correctamente.");
        sender.sendMessage("");
        sender.sendMessage("§e/uhcadmin §7- §fAbre panel de administrador.");
        sender.sendMessage("");
        sender.sendMessage("§e/nequipo <nombre> §7- §fRenombra o crea el nombre de tu equipo.");
        sender.sendMessage("");
        sender.sendMessage("§8§m---------------------------------------------------");

        return true;
    }
}
