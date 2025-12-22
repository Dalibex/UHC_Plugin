package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
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

        // 1. CHAT GLOBAL FORZADO (Empieza con "!")
        if (mensaje.startsWith("!")) {
            String mensajeLimpio = mensaje.substring(1).trim();
            if (mensajeLimpio.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            enviarMensajeGlobal(event, team, mensajeLimpio);
            return;
        }

        // 2. CHAT PRIVADO
        event.setCancelled(true);
        String nombreAMostrar = (team != null && !team.getDisplayName().equals("SIN_NOMBRE"))
                ? team.getDisplayName() : "PRIVADO";

        String colorChat = (team != null) ? team.getColor().toString() : "§f";
        String formato = "§8[§a" + nombreAMostrar + "§8] " + colorChat + p.getName() + ": §f" + mensaje;

        if (team != null) {
            for (String entry : team.getEntries()) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null) member.sendMessage(formato);
            }
        } else {
            p.sendMessage(formato);
        }
    }

    private void enviarMensajeGlobal(AsyncPlayerChatEvent event, Team team, String msg) {
        String prefix = (team != null) ? team.getPrefix() : "§7";
        event.setFormat("§8[§bGLOBAL§8] " + prefix + "§f%1$s: §7%2$s");
        event.setMessage(msg);
    }
}