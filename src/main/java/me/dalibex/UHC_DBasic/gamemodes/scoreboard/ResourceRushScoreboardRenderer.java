package me.dalibex.UHC_DBasic.gamemodes.scoreboard;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.resourcerush.ResourceRushObjectiveTracker;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Renders Resource Rush sidebar lines, including objective progress. */
public class ResourceRushScoreboardRenderer {

    public List<String> render(UHC_DBasic plugin, GameManager gm, ResourceRushObjectiveTracker tracker,
                               Objective objective, Player player, String modeName,
                               String chapterTime, String totalTime, boolean matchActive) {
        LanguageManager lang = plugin.getLang();
        List<String> keys = new ArrayList<>();
        if (!matchActive) {
            ScoreboardHelper.addLobbyScores(objective, keys, modeName, player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(35);
            ScoreboardHelper.addPhaseInfo(objective, next, keys, player, lang, gm);
            ScoreboardHelper.addTeamInfo(objective, next, keys, player, lang, plugin.getTeamManager(), gm);
            addProgress(objective, next, keys, plugin, tracker, player, lang);
            ScoreboardHelper.addTimers(objective, next, keys, chapterTime, totalTime, player, lang, gm);
        }
        return keys;
    }

    private void addProgress(Objective objective, AtomicInteger next, List<String> keys, UHC_DBasic plugin,
                             ResourceRushObjectiveTracker tracker, Player player, LanguageManager lang) {
        keys.add("§8 ");
        objective.getScore("§8 ").setScore(next.getAndDecrement());
        Team team = plugin.getTeamManager().getPlayerTeam(player.getName());
        String progressKey = team != null ? team.getName() : player.getName();
        String counter = lang.get("scoreboard-rr.rr-counter", player)
                .replace("%done%", String.valueOf(tracker.progressCount(progressKey)))
                .replace("%total%", String.valueOf(tracker.activeObjectiveCount()));
        keys.add(counter);
        objective.getScore(counter).setScore(next.getAndDecrement());
    }
}
