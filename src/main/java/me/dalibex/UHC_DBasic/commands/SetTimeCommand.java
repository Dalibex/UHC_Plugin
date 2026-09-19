package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.TimeUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetTimeCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public SetTimeCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LanguageManager lang = plugin.getLang();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(lang.get("general.no-permission", player));
            return true;
        }

        int totalSegundos = validate(sender, args, player);
        if (totalSegundos == -1) return true;

        GameManager rpm = plugin.getGameManager();
        rpm.setSecondsPerChapter(totalSegundos);

        sendFeedback(sender, args, player, totalSegundos);

        return true;
    }

    /**
     * Valida argumentos, formato, límites y estado de la partida.
     * @return total de segundos si es válido, -1 si falla.
     */
    private int validate(CommandSender sender, String[] args, Player player) {
        LanguageManager lang = plugin.getLang();

        if (args.length != 3) {
            List<String> usage = lang.getList("timer.usage", player);
            for (String line : usage) sender.sendMessage(line);
            return -1;
        }

        if (plugin.getGameManager().getTotalSeconds() > 0) {
            sender.sendMessage(lang.get("timer.already-started", player));
            return -1;
        }

        try {
            int h = Integer.parseInt(args[0]);
            int m = Integer.parseInt(args[1]);
            int s = Integer.parseInt(args[2]);

            if (h < 0 || m < 0 || s < 0) {
                sender.sendMessage(lang.get("timer.negative-values", player));
                return -1;
            }

            if (h > 99 || m > 99 || s > 99) {
                sender.sendMessage(lang.get("timer.max-value-limit", player));
                return -1;
            }

            int total = (h * 3600) + (m * 60) + s;

            if (total <= 0) {
                sender.sendMessage(lang.get("timer.too-short", player));
                return -1;
            }

            return total;

        } catch (NumberFormatException e) {
            sender.sendMessage(lang.get("timer.invalid-number", player));
            return -1;
        }
    }

    private void sendFeedback(CommandSender sender, String[] args, Player player, int total) {
        LanguageManager lang = plugin.getLang();

        String tiempoFormateado = TimeUtil.formatHms(total);

        String successMsg = lang.get("timer.success", player)
                .replace("%prefix%", lang.get("general.prefix", player))
                .replace("%time%", tiempoFormateado);

        sender.sendMessage(successMsg);

        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        }
    }
}