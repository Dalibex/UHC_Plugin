package me.dalibex.UHC_DBasic.gamemodes.scoreboard;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Renders the Classic UHC sidebar lines. */
public class ClassicScoreboardRenderer {

    public List<String> render(UHC_DBasic plugin, GameManager gm, Objective objective, Player player,
                               String modeName, String chapterTime, String totalTime, boolean matchActive) {
        LanguageManager lang = plugin.getLang();
        List<String> keys = new ArrayList<>();
        if (!matchActive) {
            ScoreboardHelper.addLobbyScores(objective, keys, modeName, player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(30);
            ScoreboardHelper.addPhaseInfo(objective, next, keys, player, lang, gm);
            ScoreboardHelper.addTeamInfo(objective, next, keys, player, lang, plugin.getTeamManager(), gm);
            ScoreboardHelper.addTimers(objective, next, keys, chapterTime, totalTime, player, lang, gm);
        }
        return keys;
    }
}
