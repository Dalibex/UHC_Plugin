package me.dalibex.UHC_DBasic.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.CommandTabs;

/**
 * Permite al admin elegir en qué episodio (parte) se forman los equipos:
 * cuándo se entregan las brújulas y se anuncia la formación.
 */
public class SetTeamEpisodeCommand implements CommandExecutor, TabCompleter {

    public static final int MIN_EPISODE = 1;
    public static final int MAX_EPISODE = 10;

    private final UHC_DBasic plugin;

    public SetTeamEpisodeCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!plugin.isAdmin(sender)) {
            sender.sendMessage(plugin.getLang().get("general.no-permission", null));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.getLang().get("teams.episode.usage", null)
                    .replace("%error-prefix%", plugin.getLang().get("general.error-prefix", null)));
            return true;
        }

        if (plugin.getGameManager().isGameStarted()) {
            sender.sendMessage(plugin.getLang().get("game.game-already-started", null));
            return true;
        }

        int episode;
        try {
            episode = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLang().get("game.invalid-number", null)
                    .replace("%error-prefix%", plugin.getLang().get("general.error-prefix", null)));
            return true;
        }

        if (episode < MIN_EPISODE || episode > MAX_EPISODE) {
            sender.sendMessage(plugin.getLang().get("teams.episode.range", null)
                    .replace("%error-prefix%", plugin.getLang().get("general.error-prefix", null)));
            return true;
        }

        plugin.getTeamManager().setTeamsFormedEpisode(episode);

        sender.sendMessage(plugin.getLang().get("teams.episode.success", null)
                .replace("%prefix%", plugin.getLang().get("general.prefix", null))
                .replace("%episode%", String.valueOf(episode)));
        if (sender instanceof Player p) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (int i = MIN_EPISODE; i <= MAX_EPISODE; i++) options.add(String.valueOf(i));
            return CommandTabs.prefixFilter(options, args[0]);
        }
        return new ArrayList<>();
    }
}