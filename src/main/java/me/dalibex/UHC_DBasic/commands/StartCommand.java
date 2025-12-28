package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
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
        LanguageManager lang = plugin.getLang();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("general.only-players", null));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(lang.get("general.no-permission", player));
            return true;
        }

        if (args.length == 0) {
            String errorMsg = lang.get("start-menu.usage", player)
                    .replace("%error-prefix%", lang.get("general.error-prefix", player));
            player.sendMessage(errorMsg);
            return true;
        }

        String size = args[0];

        TextComponent mensaje = new TextComponent(lang.get("general.prefix", player));

        // BOTÓN SI
        TextComponent botonSi = new TextComponent(lang.get("start-menu.buttons.confirm.text", player));
        botonSi.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmarstart " + size));
        botonSi.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(lang.get("start-menu.buttons.confirm.hover", player))));

        // BOTÓN NO
        TextComponent botonNo = new TextComponent(lang.get("start-menu.buttons.cancel.text", player));
        botonNo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(lang.get("start-menu.buttons.cancel.hover", player))));

        mensaje.addExtra(botonSi);
        mensaje.addExtra(" ");
        mensaje.addExtra(botonNo);
        player.spigot().sendMessage(mensaje);

        return true;
    }
}