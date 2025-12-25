package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.RightPanelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TiempoPartesCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public TiempoPartesCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("uhc.admin")) {
            sender.sendMessage("§cNo tienes permiso.");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage("§cUso correcto: /tpartes <horas> <minutos> <segundos>");
            sender.sendMessage("§7Ejemplo para 10 seg: §f/tpartes 0 0 10");
            sender.sendMessage("§7Ejemplo para 20 min: §f/tpartes 0 20 0");
            return true;
        }

        RightPanelManager rpm = plugin.getRightPanelManager();

        if (rpm.getTiempoTotalSegundos() > 0) {
            sender.sendMessage("§cLa partida ya ha comenzado, no puedes cambiar esto ahora.");
            return true;
        }

        try {
            int h = Integer.parseInt(args[0]);
            int m = Integer.parseInt(args[1]);
            int s = Integer.parseInt(args[2]);

            int totalSegundos = (h * 3600) + (m * 60) + s;

            if (totalSegundos <= 0) {
                sender.sendMessage("§cEl tiempo total debe ser mayor a 0 segundos.");
                return true;
            }

            rpm.setSegundosPorCapitulo(totalSegundos);

            String tiempoFormateado = String.format("%02dh %02dm %02ds", h, m, s);
            sender.sendMessage("§e§lUHC ELOUD> §fDuración de cada parte ajustada a: §e" + tiempoFormateado);

            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cError: Debes introducir números enteros.");
        }

        return true;
    }
}