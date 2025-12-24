package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;

public class ChatManager implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        String mensaje = event.getMessage();
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());

        event.setCancelled(true);

        // 1. CHAT GLOBAL FORZADO (Empieza con "!")
        if (mensaje.startsWith("!")) {
            String mensajeLimpio = mensaje.substring(1).trim();

            if (mensajeLimpio.isEmpty()) {
                p.sendMessage(ChatColor.RED + "Debes escribir un mensaje después del !");
                return;
            }

            enviarMensajeGlobal(p, team, mensajeLimpio);
            return;
        }

        // 2. LOGICA DE CHAT PRIVADO / EQUIPO
        if (team != null) {
            String formatoTeam = ChatColor.GRAY + "[" + ChatColor.AQUA + team.getDisplayName() + ChatColor.GRAY + "] "
                    + ChatColor.WHITE + p.getName() + ": " + ChatColor.WHITE + mensaje;

            for (String entry : team.getEntries()) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null && member.isOnline()) {
                    member.sendMessage(formatoTeam);
                }
            }
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + p.getName() + ": " + mensaje);

        } else {
            String formatoPrivado = ChatColor.GRAY + "[" + ChatColor.GREEN + "PRIVADO" + ChatColor.GRAY + "] "
                    + ChatColor.WHITE + p.getName() + ": " + ChatColor.WHITE + mensaje;

            p.sendMessage(formatoPrivado);
        }
    }

    private void enviarMensajeGlobal(Player p, Team team, String msg) {
        String prefix = (team != null) ? team.getDisplayName() : "";
        ChatColor colorName = (team != null) ? team.getColor() : ChatColor.GRAY;

        String formatoGlobal = ChatColor.GRAY + "[" + ChatColor.RED + "GLOBAL" + ChatColor.GRAY + "] "
                + "[" + colorName + prefix + ChatColor.GRAY + "] " + ChatColor.WHITE + p.getName() + ": " + ChatColor.GRAY + msg;

        Bukkit.broadcastMessage(formatoGlobal);
    }
}