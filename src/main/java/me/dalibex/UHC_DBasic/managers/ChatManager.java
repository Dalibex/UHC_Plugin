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
        boolean partidaActiva = plugin.getGameManager().isPartidaIniciada();

        event.setCancelled(true);

        // 1. CHAT GLOBAL (Empieza con "!")
        if (mensaje.startsWith("!")) {
            String mensajeLimpio = mensaje.substring(1).trim();

            if (mensajeLimpio.isEmpty()) {
                p.sendMessage(lang.get("chat.empty-global-error", p));
                return;
            }

            enviarMensajeGlobal(p, team, mensajeLimpio, lang, partidaActiva);
            return;
        }

        // --- PARA CHAT DE EQUIPO Y PRIVADO ---
        String nombreBlanco = "§f" + p.getName() + "§r";

        // 2. LOGICA DE CHAT DE EQUIPO
        if (team != null && team.getEntries().size() > 1) {
            for (String entry : team.getEntries()) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null && member.isOnline()) {
                    String formatoTeam = lang.get("chat.format-team", member)
                            .replace("%team%", team.getDisplayName())
                            .replace("%player%", nombreBlanco) // Blanco
                            .replace("%msg%", mensaje);
                    member.sendMessage(ChatColor.translateAlternateColorCodes('&', formatoTeam));
                }
            }
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + team.getName() + " - " + p.getName() + ": " + mensaje);

        } else {
            // CHAT PRIVADO SIN EQUIPO
            String formatoPrivado = lang.get("chat.format-private", p)
                    .replace("%tag%", lang.get("chat.private-tag", p))
                    .replace("%player%", nombreBlanco) // Blanco
                    .replace("%msg%", mensaje);

            p.sendMessage(ChatColor.translateAlternateColorCodes('&', formatoPrivado));
        }
    }

    private void enviarMensajeGlobal(Player p, Team team, String msg, LanguageManager lang, boolean partidaActiva) {
        String tagGlobal = lang.get("chat.global-tag", null);
        String modoActual = plugin.getRightPanelManager().getModoActual().getName();

        String textoAMostrar;

        if (partidaActiva) {
            if (modoActual.equalsIgnoreCase("Resource Rush")) {
                textoAMostrar = "§k" + ((team != null) ? team.getDisplayName() : "SOLO");
            } else {
                textoAMostrar = "§kUHCELOUD";
            }
        } else {
            textoAMostrar = p.getName();
        }

        String formatoFinal = ChatColor.translateAlternateColorCodes('&',
                "&8[&c" + tagGlobal + "&8] &6" + textoAMostrar + "§r: &7" + msg);

        for (Player receptor : Bukkit.getOnlinePlayers()) {
            receptor.sendMessage(formatoFinal);
        }

        Bukkit.getConsoleSender().sendMessage("[GlobalChat] [" + p.getName() + "]: " + msg);
    }
}