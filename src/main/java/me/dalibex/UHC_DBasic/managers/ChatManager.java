package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class ChatManager implements Listener {

    private final UHC_DBasic plugin;

    public ChatManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String mensaje = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (mensaje.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Player jugador = event.getPlayer();
        // AsyncChatEvent se dispara en el hilo Netty: todo lo que toque scoreboard o
        // estado del plugin debe ejecutarse en el hilo principal.
        Bukkit.getScheduler().runTask(plugin, () -> processMessage(jugador, mensaje));
    }

    private void processMessage(Player p, String mensaje) {
        LanguageManager lang = plugin.getLang();
        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        boolean partidaActiva = plugin.getGameManager().isMatchActive();

        // 1. CHAT GLOBAL (Empieza con "!")
        if (mensaje.startsWith("!")) {
            String mensajeLimpio = mensaje.substring(1).trim();

            if (mensajeLimpio.isEmpty()) {
                p.sendMessage(lang.get("chat.empty-global-error", p));
                return;
            }

            sendGlobalMessage(p, team, mensajeLimpio, lang, partidaActiva);
            return;
        }

        // --- PARA CHAT DE EQUIPO Y PRIVADO ---
        String nombreBlanco = "§f" + p.getName() + "§r";

        // 2. LOGICA DE CHAT DE EQUIPO
        if (team != null && plugin.getTeamManager().getMemberCount(team) > 1) {
            for (String entry : plugin.getTeamManager().getMemberNames(team)) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null && member.isOnline()) {
                    String formatoTeam = lang.get("chat.format-team", member)
                            .replace("%team%", legacySection().serialize(team.displayName()))
                            .replace("%player%", nombreBlanco)
                            .replace("%msg%", mensaje);
                    member.sendMessage(TextUtil.deserialize(formatoTeam));
                }
            }
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + team.getName() + " - " + p.getName() + ": " + mensaje);

        } else {
            // CHAT PRIVADO SIN EQUIPO
            String formatoPrivado = lang.get("chat.format-private", p)
                    .replace("%tag%", lang.get("chat.private-tag", p))
                    .replace("%player%", nombreBlanco)
                    .replace("%msg%", mensaje);

            p.sendMessage(TextUtil.deserialize(formatoPrivado));
        }
    }

    private void sendGlobalMessage(Player p, Team team, String msg, LanguageManager lang, boolean partidaActiva) {
        String tagGlobal = lang.get("chat.global-tag", null);
        String modoActual = plugin.getGameManager().getCurrentMode().getName();

        String textoAMostrar;

        if (partidaActiva) {
            // Solo el bloque entre §k y §r queda ofuscado; el §r final evita que
            // el texto del usuario (a continuación) herede la decoración.
            if (modoActual.equalsIgnoreCase("Resource Rush")) {
                String clave = (team != null) ? legacySection().serialize(team.displayName()) : "SOLO";
                textoAMostrar = "§k" + clave + "§r";
            } else {
                textoAMostrar = "§kUHCELOUD§r";
            }
        } else {
            textoAMostrar = p.getName();
        }

        // La línea se arma por componentes: el nombre puede llevar §k (obfuscado)
        // para ocultar la identidad en partida, pero el mensaje propio NO debe
        // heredar esa decoración, así que se añade con OBFUSCATED off.
        Component nombre = TextUtil.deserialize("&6" + textoAMostrar);
        Component formatoFinal = Component.empty()
                .color(NamedTextColor.DARK_GRAY)
                .append(TextUtil.deserialize("&8[&c" + tagGlobal + "&8] "))
                .append(nombre)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(msg, NamedTextColor.GRAY)
                        .decoration(TextDecoration.OBFUSCATED, false));

        for (Player receptor : Bukkit.getOnlinePlayers()) {
            receptor.sendMessage(formatoFinal);
        }

        String consola = "&7[GlobalChat] [&f" + p.getName() + "&7]: &f" + msg;
        Bukkit.getConsoleSender().sendMessage(TextUtil.deserialize(consola));
    }
}
