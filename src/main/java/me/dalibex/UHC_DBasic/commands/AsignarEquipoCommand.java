package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
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

public class AsignarEquipoCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;

    public AsignarEquipoCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cEste comando es solo para jugadores.");
            return true;
        }

        if (!admin.isOp()) {
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

        // Buscar al jugador
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(lang.get("game.assign-team-player-not-found", admin)
                    .replace("%player%", targetName));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        // Buscar el equipo por color/ID
        Team team = tm.getTeamByColorSearch(colorInput);
        if (team == null) {
            admin.sendMessage(lang.get("game.assign-team-not-found", admin)
                    .replace("%team%", colorInput));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        // Verificar si el equipo está lleno
        if (team.getEntries().size() >= tm.getTeamSize()) {
            admin.sendMessage(lang.get("menus.team-selector.already-full", admin));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        // Quitar del equipo anterior si tiene uno
        Team currentTeam = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(target.getName());
        if (currentTeam != null) {
            currentTeam.removeEntry(target.getName());
        }

        // Asignar al equipo
        team.addEntry(target.getName());

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
            // Autocompletar nombres de jugadores
            String partial = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Autocompletar nombres de colores (Solo nombres internos/en inglés)
            String partial = args[1].toLowerCase();
            for (Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
                if (team.getName().toLowerCase().startsWith(partial)) completions.add(team.getName());
            }
        }

        return completions;
    }
}
