package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
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

    private final UHC_DBasic plugin;

    public StartCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().get("general.only-players"));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(plugin.getLang().get("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            String errorMsg = plugin.getLang().get("start-menu.usage")
                    .replace("%error-prefix%", plugin.getLang().get("general.error-prefix"));
            player.sendMessage(errorMsg);
            return true;
        }

        String size = args[0];

        TextComponent mensaje = new TextComponent(plugin.getLang().get("general.prefix"));

        // BOTÓN SI
        TextComponent botonSi = new TextComponent(plugin.getLang().get("start-menu.buttons.confirm.text"));
        botonSi.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmarstart " + size));
        botonSi.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(plugin.getLang().get("start-menu.buttons.confirm.hover"))));

        // BOTÓN NO
        TextComponent botonNo = new TextComponent(plugin.getLang().get("start-menu.buttons.cancel.text"));
        botonNo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getLang().get("start-menu.buttons.cancel.hover"))));

        mensaje.addExtra(botonSi);
        mensaje.addExtra(" ");
        mensaje.addExtra(botonNo);
        player.spigot().sendMessage(mensaje);

        return true;
    }
}