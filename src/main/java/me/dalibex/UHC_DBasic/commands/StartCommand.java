package me.dalibex.UHC_DBasic.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StartCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cNo tienes permisos para ejecutar este comando.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§c§lERROR > §7Uso correcto: /start <tamaño>");
            return true;
        }

        String size = args[0];

        TextComponent mensaje = new TextComponent("§e§lUHC ELOUD ");

        // BOTON PARA EMPEZAR
        TextComponent botonSi = new TextComponent("§a[EMPEZAR UHC]");
        botonSi.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmarstart " + size));
        botonSi.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7EMPEZAR e iniciar la cuenta atrás")));

        // Botón NO (Simplemente avisa que se canceló)
        TextComponent botonNo = new TextComponent("§c[CANCELAR]");
        botonNo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7No hacer nada, cancela la acción")));

        mensaje.addExtra("§e§l> ");
        mensaje.addExtra(botonSi);
        mensaje.addExtra(" ");
        mensaje.addExtra(botonNo);
        player.spigot().sendMessage(mensaje);

        return true;
    }
}
