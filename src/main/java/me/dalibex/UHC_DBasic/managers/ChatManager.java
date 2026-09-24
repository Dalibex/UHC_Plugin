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
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (message.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        // AsyncChatEvent runs on Netty threads; plugin state must be touched on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> processMessage(player, message));
    }

    private void processMessage(Player p, String message) {
        LanguageManager lang = plugin.getLang();
        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        boolean partidaActiva = plugin.getGameManager().isMatchActive();

        if (message.startsWith("!")) {
            String cleanMessage = message.substring(1).trim();

            if (cleanMessage.isEmpty()) {
                p.sendMessage(lang.get("chat.empty-global-error", p));
                return;
            }

            sendGlobalMessage(p, team, cleanMessage, lang, partidaActiva);
            return;
        }

        String whiteName = "§f" + p.getName() + "§r";

        if (team != null && plugin.getTeamManager().getMemberCount(team) > 1) {
            for (String entry : plugin.getTeamManager().getMemberNames(team)) {
                Player member = Bukkit.getPlayer(entry);
                if (member != null && member.isOnline()) {
                    String teamFormat = lang.get("chat.format-team", member)
                            .replace("%team%", legacySection().serialize(team.displayName()))
                            .replace("%player%", whiteName)
                            .replace("%msg%", message);
                    member.sendMessage(TextUtil.deserialize(teamFormat));
                }
            }
            Bukkit.getConsoleSender().sendMessage("[TeamChat] " + team.getName() + " - " + p.getName() + ": " + message);

        } else {
            String privateFormat = lang.get("chat.format-private", p)
                    .replace("%tag%", lang.get("chat.private-tag", p))
                    .replace("%player%", whiteName)
                    .replace("%msg%", message);

            p.sendMessage(TextUtil.deserialize(privateFormat));
        }
    }

    private void sendGlobalMessage(Player p, Team team, String msg, LanguageManager lang, boolean partidaActiva) {
        String globalTag = lang.get("chat.global-tag", null);

        Component finalFormat = Component.empty()
                .color(NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.OBFUSCATED, false)
                .append(TextUtil.deserialize("&8[&c" + globalTag + "&8] ")
                        .decoration(TextDecoration.OBFUSCATED, false));

        if (partidaActiva) {
            finalFormat = finalFormat
                    .append(Component.text(msg, NamedTextColor.GRAY)
                            .decoration(TextDecoration.OBFUSCATED, false));
        } else {
            Component name = Component.text(p.getName(), NamedTextColor.GOLD)
                    .decoration(TextDecoration.OBFUSCATED, false);
            finalFormat = finalFormat
                    .append(name)
                    .append(Component.text(": ", NamedTextColor.GRAY)
                            .decoration(TextDecoration.OBFUSCATED, false))
                    .append(Component.text(msg, NamedTextColor.GRAY)
                            .decoration(TextDecoration.OBFUSCATED, false));
        }

        for (Player receiver : Bukkit.getOnlinePlayers()) {
            receiver.sendMessage(finalFormat);
        }

        String consoleMessage = "&7[GlobalChat] [&f" + p.getName() + "&7]: &f" + msg;
        Bukkit.getConsoleSender().sendMessage(TextUtil.deserialize(consoleMessage));
    }
}
