package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssignTeamCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;

    public AssignTeamCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cEste comando es solo para jugadores.");
            return true;
        }

        if (!plugin.isAdmin(admin)) {
            admin.sendMessage(plugin.getLang().get("general.no-permission", admin));
            return true;
        }

        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        if (!tm.isCustomTeamsEnabled()) {
            admin.sendMessage(lang.get("game.assign-team-disabled", admin));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        if (args.length < 2) {
            admin.sendMessage(lang.get("game.assign-team-usage", admin));
            return true;
        }

        String targetName = args[0];
        String colorInput = args[1];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(lang.get("game.assign-team-player-not-found", admin)
                    .replace("%player%", targetName));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        Team team = tm.getTeamByColorSearch(colorInput);
        if (team == null) {
            admin.sendMessage(lang.get("game.assign-team-not-found", admin)
                    .replace("%team%", colorInput));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        if (tm.getMemberCount(team) >= tm.getTeamSize()) {
            admin.sendMessage(lang.get("menus.team-selector.already-full", admin));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        tm.movePlayerToTeam(target, team);

        String teamDisplay = legacySection().serialize(team.displayName().color(team.color()));
        String successMsg = lang.get("game.assign-team-success", admin)
                .replace("%player%", target.getName())
                .replace("%team%", teamDisplay);
        admin.sendMessage(successMsg);
        admin.playSound(admin.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        String joinedMsg = lang.get("menus.team-selector.joined", target)
                .replace("%name%", teamDisplay);
        target.sendMessage(joinedMsg);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Team team : plugin.getTeamManager().getTeams()) {
                String colorKey = TeamManager.normalizeColorInput(team.getName());
                if (colorKey.startsWith(partial)) completions.add(colorKey);
            }
        }

        return completions;
    }
}
