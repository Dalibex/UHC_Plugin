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

    private final UHC_DBasic plugin;

    public ChatManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
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
        if (team != null && team.getEntries().size() > 1) {
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
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + team.getName() + " - " + p.getName() + ": " + mensaje);

        } else {
            String formatoPrivado = lang.get("chat.format-private", p)
                    .replace("%tag%", lang.get("chat.private-tag", p))
                    .replace("%player%", p.getName())
                    .replace("%msg%", mensaje);

            p.sendMessage(formatoPrivado);
        }
    }

    private void enviarMensajeGlobal(Player p, Team team, String msg, LanguageManager lang) {
        ChatColor colorName = (team != null) ? team.getColor() : ChatColor.WHITE;

        for (Player receptor : Bukkit.getOnlinePlayers()) {
            String formatoGlobal = lang.get("chat.format-global", receptor);

            formatoGlobal = formatoGlobal.replace("[%team%]", "").replace("[%team% ]", "").replace("%team%", "");

            formatoGlobal = formatoGlobal.replace("%tag%", lang.get("chat.global-tag", receptor))
                    .replace("%color%", colorName.toString())
                    .replace("%player%", p.getName())
                    .replace("%msg%", msg);

            receptor.sendMessage(formatoGlobal.replace("  ", " ").trim());
        }

        Bukkit.getConsoleSender().sendMessage("[GlobalChat] " + p.getName() + ": " + msg);
    }
}