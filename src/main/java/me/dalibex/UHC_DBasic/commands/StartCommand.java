package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.CommandTabs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class StartCommand implements CommandExecutor, TabCompleter {

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

        if (!plugin.isAdmin(player)) {
            player.sendMessage(lang.get("general.no-permission", player));
            return true;
        }

        int size = validate(player, args);
        if (size == -1) return true;

        if (!plugin.getGameManager().requestStart(player.getUniqueId(), size)) {
            player.sendMessage(plugin.getLang().get("start-menu.already-starting", player)
                    .replace("%error-prefix%", plugin.getLang().get("general.error-prefix", player)));
            return true;
        }
        sendConfirmationMenu(player, size);

        return true;
    }

    /**
     * Valida argumentos, formato numérico, rango mínimo y estado del juego.
     * @return el tamaño (size) si es válido, -1 si falla (enviando mensaje de error).
     */
    int validate(Player player, String[] args) {
        LanguageManager lang = plugin.getLang();
        String errorPrefix = lang.get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(lang.get("start-menu.usage", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        int size;
        try {
            size = Integer.parseInt(args[0]);
            if (!isValidBorderSize(size)) {
                player.sendMessage(lang.get("menus.barrier.min-size-error", player).replace("%error-prefix%", errorPrefix));
                return -1;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get("game.invalid-number", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        if (!isStartablePhase(plugin.getGameManager().getPhase())) {
            player.sendMessage(errorPrefix + lang.get("game.game-already-started", player));
            return -1;
        }

        if (plugin.getGameManager().hasPendingStart()) {
            player.sendMessage(lang.get("start-menu.already-starting", player).replace("%error-prefix%", errorPrefix));
            return -1;
        }

        return size;
    }

    static boolean isValidBorderSize(int size) { return size >= 20; }

    static boolean isStartablePhase(me.dalibex.UHC_DBasic.managers.GamePhase phase) {
        return phase == me.dalibex.UHC_DBasic.managers.GamePhase.LOBBY;
    }

    /**
     * Construye y envía el mensaje interactivo con los botones SÍ/NO.
     */
    private void sendConfirmationMenu(Player player, int size) {
        LanguageManager lang = plugin.getLang();
        Component mensaje = legacySection().deserialize(lang.get("general.prefix", player));

        Component botonSi = legacySection().deserialize(lang.get("start-menu.buttons.confirm.text", player))
                .clickEvent(ClickEvent.runCommand("/confirmstart " + size))
                .hoverEvent(HoverEvent.showText(legacySection().deserialize(lang.get("start-menu.buttons.confirm.hover", player))));

        Component botonNo = legacySection().deserialize(lang.get("start-menu.buttons.cancel.text", player))
                .clickEvent(ClickEvent.runCommand("/cancelstart"))
                .hoverEvent(HoverEvent.showText(legacySection().deserialize(lang.get("start-menu.buttons.cancel.hover", player))));

        mensaje = mensaje.append(legacySection().deserialize(" "))
                .append(botonSi)
                .append(legacySection().deserialize("   "))
                .append(botonNo);

        player.sendMessage(mensaje);
    }

    public boolean hasPendingConfirmation() {
        return plugin.getGameManager().hasPendingStart();
    }

    public UUID getConfirmerUuid() {
        return plugin.getGameManager().getStartupOwner();
    }

    public int getPendingSize() {
        return plugin.getGameManager().getPendingBorderSize();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return CommandTabs.prefixFilter(Arrays.asList("500", "1000", "2000", "3000"), args[0]);
        }
        return new ArrayList<>();
    }
}
