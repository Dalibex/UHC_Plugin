package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
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
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        Player p = event.getPlayer();
        String mensaje = event.getMessage();
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());

        event.setCancelled(true);

        // 1. CHAT GLOBAL (Empieza con "!")
        if (mensaje.startsWith("!")) {
            String mensajeLimpio = mensaje.substring(1).trim();

            if (mensajeLimpio.isEmpty()) {
                p.sendMessage(lang.get("chat.empty-global-error", p));
                return;
            }

            enviarMensajeGlobal(p, team, mensajeLimpio, lang);
            return;
        }

        // 2. LOGICA DE CHAT DE EQUIPO
        if (team != null) {
            for (String entry : team.getEntries()) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null && member.isOnline()) {
                    String formatoTeam = lang.get("chat.format-team", member)
                            .replace("%team%", team.getDisplayName())
                            .replace("%player%", p.getName())
                            .replace("%msg%", mensaje);
                    member.sendMessage(formatoTeam);
                }
            }
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + p.getName() + ": " + mensaje);

        } else {
            // Mensaje para jugadores sin equipo
            String formatoPrivado = lang.get("chat.format-private", p)
                    .replace("%tag%", lang.get("chat.private-tag", p))
                    .replace("%player%", p.getName())
                    .replace("%msg%", mensaje);

            p.sendMessage(formatoPrivado);
        }
    }

    private void enviarMensajeGlobal(Player p, Team team, String msg, LanguageManager lang) {
        ChatColor colorName = (team != null) ? team.getColor() : ChatColor.GRAY;

        for (Player receptor : Bukkit.getOnlinePlayers()) {
            String formatoGlobal = lang.get("chat.format-global", receptor)
                    .replace("%tag%", lang.get("chat.global-tag", receptor))
                    .replace("%color%", colorName.toString())
                    .replace("%team%", "")
                    .replace("[]", "")
                    .replace("%player%", p.getName())
                    .replace("%msg%", msg);
            receptor.sendMessage(formatoGlobal);
        }

        Bukkit.getConsoleSender().sendMessage("[GlobalChat] " + p.getName() + ": " + msg);
    }
}