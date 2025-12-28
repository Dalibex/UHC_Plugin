package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.RightPanelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TiempoPartesCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public TiempoPartesCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LanguageManager lang = plugin.getLang();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (!sender.hasPermission("uhc.admin")) {
            sender.sendMessage(lang.get("general.no-permission", player));
            return true;
        }

        if (args.length != 3) {
            List<String> usage = lang.getList("timer.usage", player);
            for (String line : usage) {
                sender.sendMessage(line);
            }
            return true;
        }

        RightPanelManager rpm = plugin.getRightPanelManager();

        if (rpm.getTiempoTotalSegundos() > 0) {
            sender.sendMessage(lang.get("timer.already-started", player));
            return true;
        }

        try {
            int h = Integer.parseInt(args[0]);
            int m = Integer.parseInt(args[1]);
            int s = Integer.parseInt(args[2]);

            int totalSegundos = (h * 3600) + (m * 60) + s;

            if (totalSegundos <= 0) {
                sender.sendMessage(lang.get("timer.too-short", player));
                return true;
            }

            rpm.setSegundosPorCapitulo(totalSegundos);

            String tiempoFormateado = String.format("%02dh %02dm %02ds", h, m, s);

            String successMsg = lang.get("timer.success", player)
                    .replace("%prefix%", lang.get("general.prefix", player))
                    .replace("%time%", tiempoFormateado);

            sender.sendMessage(successMsg);

            if (player != null) {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(lang.get("timer.invalid-number", player));
        }

        return true;
    }
}