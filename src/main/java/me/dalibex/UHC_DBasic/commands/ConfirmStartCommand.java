package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import me.dalibex.UHC_DBasic.services.MatchStartService;
import me.dalibex.UHC_DBasic.utils.CommandTabs;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfirmStartCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;
    private final StartCommand startCmd;
    private final MatchStartService matchStartService;

    public ConfirmStartCommand(UHC_DBasic plugin, StartCommand startCmd) {
        this.plugin = plugin;
        this.startCmd = startCmd;
        this.matchStartService = new MatchStartService(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String errorPrefix = plugin.getLang().get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(plugin.getLang().get("start-menu.usage", player).replace("%error-prefix%", errorPrefix));
            return true;
        }

        if (!startCmd.hasPendingConfirmation() || plugin.getGameManager().isGameStarted()) return true;
        if (!canConfirmStart(player)) return true;
        if (!validateCustomTeams(player)) return true;
        if (!plugin.getGameManager().beginPreparation(player.getUniqueId())) return true;

        matchStartService.start(plugin.getGameManager().getPendingBorderSize());
        return true;
    }

    private boolean canConfirmStart(Player player) {
        if (!plugin.isAdmin(player)) {
            player.sendMessage(plugin.getLang().get("general.no-permission", player));
            return false;
        }

        UUID initiator = startCmd.getConfirmerUuid();
        if (initiator != null && !initiator.equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLang().get("start-menu.not-initiator", player));
            return false;
        }
        return true;
    }

    private boolean validateCustomTeams(Player player) {
        TeamManager tm = plugin.getTeamManager();
        if (!tm.isCustomTeamsEnabled() || tm.getTeamSize() <= 1) return true;

        if (!tm.allPlayersHaveTeam()) {
            player.sendMessage(plugin.getLang().get("game.start-blocked-custom-teams", player));
            rejectStart(player);
            return false;
        }

        int onlinePlayers = (int) Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .count();
        int minRequired = (int) Math.ceil((double) onlinePlayers / tm.getTeamSize());
        int usedTeams = tm.getTeamsWithPlayersCount();

        if (usedTeams <= minRequired) return true;

        player.sendMessage(plugin.getLang().get("game.start-blocked-team-count", player)
                .replace("%min%", String.valueOf(minRequired))
                .replace("%n%", String.valueOf(tm.getTeamSize())));
        rejectStart(player);
        return false;
    }

    private void rejectStart(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
        plugin.getGameManager().cancelStartup();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && startCmd.hasPendingConfirmation()) {
            return CommandTabs.prefixFilter(List.of(String.valueOf(startCmd.getPendingSize())), args[0]);
        }
        return new ArrayList<>();
    }
}
