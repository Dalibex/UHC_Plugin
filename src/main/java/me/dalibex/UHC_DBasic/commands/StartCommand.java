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
    private boolean confirmacionPendiente = false;

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

        int size = validarTodo(player, args);
        if (size == -1) return true;

        this.confirmacionPendiente = true;
        enviarMenuConfirmacion(player, size);

        return true;
    }

    /**
     * Valida argumentos, formato numérico, rango mínimo y estado del juego.
     * @return el tamaño (size) si es válido, -1 si falla (enviando mensaje de error).
     */
    private int validarTodo(Player player, String[] args) {
        LanguageManager lang = plugin.getLang();
        String errorPrefix = lang.get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(lang.get("start-menu.usage", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        int size;
        try {
            size = Integer.parseInt(args[0]);
            if (size < 20) {
                player.sendMessage(lang.get("menus.barrier.min-size-error", player).replace("%error-prefix%", errorPrefix));
                return -1;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get("game.invalid-number", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        if (plugin.getRightPanelManager().isPartidaIniciada()) {
            player.sendMessage(errorPrefix + lang.get("game.game-already-started", player));
            return -1;
        }

        if (confirmacionPendiente) {
            player.sendMessage(lang.get("start-menu.already-starting", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        return size;
    }

    /**
     * Construye y envía el mensaje interactivo con los botones SÍ/NO.
     */
    private void enviarMenuConfirmacion(Player player, int size) {
        LanguageManager lang = plugin.getLang();
        TextComponent mensaje = new TextComponent(lang.get("general.prefix", player));

        // BOTÓN SI
        TextComponent botonSi = new TextComponent(lang.get("start-menu.buttons.confirm.text", player));
        botonSi.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/confirmarstart " + size));
        botonSi.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(lang.get("start-menu.buttons.confirm.hover", player))));

        // BOTÓN NO
        TextComponent botonNo = new TextComponent(lang.get("start-menu.buttons.cancel.text", player));
        botonNo.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cancelarstart"));
        botonNo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(lang.get("start-menu.buttons.cancel.hover", player))));

        mensaje.addExtra(" ");
        mensaje.addExtra(botonSi);
        mensaje.addExtra("   ");
        mensaje.addExtra(botonNo);

        player.spigot().sendMessage(mensaje);
    }

    public void setConfirmacionPendiente(boolean estado) {
        this.confirmacionPendiente = estado;
    }

    public boolean getConfirmacionPendiente() {
        return confirmacionPendiente;
    }
}