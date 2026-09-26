package me.dalibex.UHC_DBasic.utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Centralizes sidebar line building across game modes.
 * Each method also tracks generated keys so modes can remove stale lines between ticks.
 */
public class ScoreboardHelper {

    public static final String SIDEBAR_OBJECTIVE = "uhc";
    public static final String HEALTH_OBJECTIVE = "health_tab";
    public static final String TEAM_PREFIX = "h_";

    private static final int FINAL_CHAPTER = 10;
    private static final double HIGH_HEALTH_THRESHOLD = 15.0;
    private static final double MEDIUM_HEALTH_THRESHOLD = 10.0;
    private static final double LOW_HEALTH_THRESHOLD = 5.0;

    /** Ensures the health objective rendered in the TAB player list exists without resending it every tick. */
    public static void ensureTabHealthObjective(Scoreboard board, Player player, LanguageManager lang) {
        Objective healthObjective = board.getObjective(HEALTH_OBJECTIVE);
        if (healthObjective == null) {
            healthObjective = board.registerNewObjective(HEALTH_OBJECTIVE, Criteria.HEALTH,
                    lang.getComponent("scoreboard.health-icon", player), RenderType.HEARTS);
        }
        if (healthObjective.getDisplaySlot() != DisplaySlot.PLAYER_LIST) {
            healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }

    /** Removes the TAB health objective from a player's scoreboard when leaving active gameplay. */
    public static void removeTabHealthObjective(Scoreboard board) {
        Objective healthObjective = board.getObjective(HEALTH_OBJECTIVE);
        if (healthObjective != null) healthObjective.unregister();
    }

    /** Adds lobby sidebar lines: mode, waiting status, and online players. */
    public static void addLobbyScores(Objective obj, List<String> keys, String modeName, Player player, LanguageManager lang) {
        score(obj, "§1 ", 7, keys);
        score(obj, lang.get("scoreboard.mode-label", player).replace("%mode%", modeName), 6, keys);
        score(obj, "§2 ", 5, keys);
        score(obj, lang.get("scoreboard.waiting", player), 4, keys);
        score(obj, "§3 ", 3, keys);
        score(obj, lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), 2, keys);
        score(obj, "§4 ", 1, keys);
    }

    /** Adds phase and PVP status lines. */
    public static void addPhaseInfo(Objective obj, AtomicInteger next, List<String> keys, Player player, LanguageManager lang, GameManager gm) {
        int chapter = gm.getChapter();
        String pvpStatus = (chapter < gm.getPvpEnabledEpisode())
                ? lang.get("scoreboard.pvp-pact", player)
                : lang.get("scoreboard.pvp-active", player);

        score(obj, "§1 ", next.getAndDecrement(), keys);
        if (chapter < FINAL_CHAPTER) {
            score(obj, lang.get("scoreboard.phase", player).replace("%chapter%", String.valueOf(chapter)), next.getAndDecrement(), keys);
        } else {
            score(obj, lang.get("scoreboard.finalized", player), next.getAndDecrement(), keys);
            if (gm.getCurrentMode() instanceof me.dalibex.UHC_DBasic.gamemodes.Classic) {
                score(obj, lang.get("scoreboard.go-center", player), next.getAndDecrement(), keys);
                score(obj, "§2 ", next.getAndDecrement(), keys);
            }
        }
        score(obj, lang.get("scoreboard.pvp-label", player).replace("%status%", pvpStatus), next.getAndDecrement(), keys);
        score(obj, "§3 ", next.getAndDecrement(), keys);
    }

    /** Adds team and teammate status lines. */
    public static void addTeamInfo(Objective obj, AtomicInteger next, List<String> keys, Player player, LanguageManager lang, TeamManager tm, GameManager gm) {
        Team team = tm.getPlayerTeam(player.getName());
        int teamSize = tm.getTeamSize();
        int chapter = gm.getChapter();

        if (teamSize == 1) {
            String line = (team != null && !tm.isDefaultName(team)) ?
                    lang.get("scoreboard.team-label", player).replace("%color%", TextUtil.legacyColor(team.color())).replace("%name%", legacySection().serialize(team.displayName())) 
                    : lang.get("scoreboard.team-rename-warn", player);
            score(obj, line, next.getAndDecrement(), keys);
        } else {
            boolean manual = tm.isCustomTeamsEnabled();
            if (chapter < tm.getTeamsFormedEpisode() && !manual) {
                for (int i = 1; i < teamSize; i++) {
                    score(obj, " §d👥 §f: §k??????" + (" ".repeat(i)), next.getAndDecrement(), keys);
                }
            } else {
                String line = (team != null && !tm.isDefaultName(team)) ?
                        lang.get("scoreboard.team-mates-label", player).replace("%color%", TextUtil.legacyColor(team.color())).replace("%name%", legacySection().serialize(team.displayName())) 
                        : (team != null ? lang.get("scoreboard.team-rename-warn", player) : lang.get("scoreboard.team-assigning", player));
                
                score(obj, line, next.getAndDecrement(), keys);
                if (team != null) {
                    for (String entry : tm.getMemberNames(team)) {
                        if (entry.equalsIgnoreCase(player.getName())) continue;
                        addMateLine(obj, next, keys, player, entry, lang, gm);
                    }
                }
            }
        }
    }

    private static void addMateLine(Objective obj, AtomicInteger next, List<String> keys, Player viewer, String entry, LanguageManager lang, GameManager gm) {
        String healthText;
        String colorPrefix = "§f";
        String displayName = entry;
        Player mate = Bukkit.getPlayer(entry);

        if (gm.getEliminatedPlayers().contains(entry)) {
            colorPrefix = "§7§m";
            healthText = lang.get("scoreboard.mate-dead", viewer);
        } else {
            if (mate != null && mate.isOnline()) {
                displayName = mate.getName();
                colorPrefix = "§f";
                double health = mate.getHealth();
                String healthColor = (health > HIGH_HEALTH_THRESHOLD) ? "§a"
                        : (health > MEDIUM_HEALTH_THRESHOLD) ? "§2"
                        : (health > LOW_HEALTH_THRESHOLD) ? "§e" : "§c";
                healthText = " " + healthColor + (int) health + "§4❤";
            } else {
                healthText = lang.get("scoreboard.mate-offline", viewer);
            }
        }
        score(obj, "§6> " + colorPrefix + displayName + healthText, next.getAndDecrement(), keys);
    }

    /** Adds total time and next-chapter timers. */
    public static void addTimers(Objective obj, AtomicInteger next, List<String> keys, String chapterTime, String totalTime, Player player, LanguageManager lang, GameManager gm) {
        score(obj, "§6 ", next.getAndDecrement(), keys);
        score(obj, lang.get("scoreboard.time-total-label", player), next.getAndDecrement(), keys);
        score(obj, "§6> §f" + totalTime, next.getAndDecrement(), keys);
        score(obj, "§7 ", next.getAndDecrement(), keys);
        
        if (gm.getChapter() < FINAL_CHAPTER) {
            score(obj, lang.get("scoreboard.time-next-label", player), next.getAndDecrement(), keys);
            score(obj, "§6> §f" + chapterTime, next.getAndDecrement(), keys);
        }
    }

    private static void score(Objective obj, String key, int value, List<String> keys) {
        obj.getScore(key).setScore(value);
        keys.add(key);
    }
}
