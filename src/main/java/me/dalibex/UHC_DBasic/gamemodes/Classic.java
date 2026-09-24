package me.dalibex.UHC_DBasic.gamemodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import static org.bukkit.GameRules.PVP;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import net.kyori.adventure.title.Title;

public class Classic extends AbstractUHCGameMode {

    public Classic(UHC_DBasic plugin, GameManager gm) {
        super(plugin, gm);
    }

    @Override
    public String getName() {
        return "Classic UHC";
    }

    @Override
    protected void onChapterChange(int newChapter) {
        LanguageManager lang = plugin.getLang();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (newChapter < 10) {
                p.sendMessage(lang.get("game-events.chapter-start", p)
                        .replace("%prefix%", lang.get("general.prefix", p))
                        .replace("%chapter%", String.valueOf(newChapter)));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else if (newChapter == 10) {
                for (String s : lang.getList("game-events.final-phase", p)) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
            }
        }

        if (me.dalibex.UHC_DBasic.managers.SkinsManager.isRotationEpisode(newChapter)) {
            runSkinRotation();
        }

        maybeFormTeams(newChapter, null);

        if (newChapter == gm.getPvpEnabledEpisode()) {
            for (World w : Bukkit.getWorlds()) w.setGameRule(PVP, true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (String s : lang.getList("game-events.pvp-enabled", p)) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            }
        }
    }

    @Override
    public void updateScoreboard(Player player, String chapterTime, String totalTime, boolean matchActive) {
        LanguageManager lang = plugin.getLang();
        Scoreboard board = player.getScoreboard();

        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = getOrCreateSidebar(board, player, lang);
        List<String> keys = new ArrayList<>();

        ScoreboardHelper.syncTabHealthObjective(board, player, lang, matchActive);

        if (!matchActive) {
            ScoreboardHelper.addLobbyScores(obj, keys, getName(), player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(30);
            ScoreboardHelper.addPhaseInfo(obj, next, keys, player, lang, gm);
            ScoreboardHelper.addTeamInfo(obj, next, keys, player, lang, plugin.getTeamManager(), gm);
            ScoreboardHelper.addTimers(obj, next, keys, chapterTime, totalTime, player, lang, gm);
        }

        reconcileSidebarKeys(obj, player, keys);
    }

    @Override
    public void checkVictory() {
        if (!gm.isMatchActive() || gm.getTotalSeconds() <= 5) return;

        Map<String, String> teams = new java.util.HashMap<>();
        for (String name : gm.getInitialParticipants()) {
            Team team = plugin.getTeamManager().getPlayerTeam(name);
            if (team != null) teams.put(name, team.getName());
        }
        GameOutcomeEvaluator.Outcome outcome = GameOutcomeEvaluator.evaluate(
                gm.getInitialParticipants(), gm.getEliminatedPlayers(), teams);
        if (outcome.status() == GameOutcomeEvaluator.Status.NO_SURVIVORS) {
            finishGame(null);
            return;
        }
        if (outcome.status() == GameOutcomeEvaluator.Status.WINNER) {
            Team winnerTeam = outcome.teamWinner()
                    ? Bukkit.getScoreboardManager().getMainScoreboard().getTeam(outcome.winnerKey())
                    : createTempWinnerTeam(outcome.winnerKey());
            finishGame(winnerTeam);
        }
    }

    private Team createTempWinnerTeam(String playerName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team temp = board.getTeam("winner_temp");
        if (temp != null) temp.unregister();
        temp = board.registerNewTeam("winner_temp");
        temp.addEntry(playerName);
        temp.displayName(Component.text(playerName));
        temp.color(NamedTextColor.GOLD);
        return temp;
    }

    private void finishGame(Team winner) {
        LanguageManager lang = plugin.getLang();
        finishGameSession();

        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getSkinsManager().revealIdentity(online);
            plugin.getSkinsManager().updateVisualIdentity(online);
        }

        if (winner != null) {
            broadcastVictory(winner, lang);
            List<Player> winners = new ArrayList<>();
            for (String entry : winningEntries(winner)) {
                Player p = Bukkit.getPlayer(entry);
                if (p != null && !gm.getEliminatedPlayers().contains(entry)) winners.add(p);
            }
            applyVictoryEffects(winners);
        } else {
            Bukkit.broadcast(legacySection().deserialize(lang.get("victory.no-survivors", null)));
        }
    }

    private List<String> winningEntries(Team winner) {
        if (winner != null && winner.getName().startsWith(ScoreboardHelper.TEAM_PREFIX)) {
            return plugin.getTeamManager().getMemberNames(winner);
        }
        return winner == null ? List.of() : new ArrayList<>(winner.getEntries());
    }

    private void broadcastVictory(Team winner, LanguageManager lang) {
        String color = TextUtil.legacyColor(winner.color());
        String teamName = legacySection().serialize(winner.displayName());
        
        List<String> formattedNames = winningEntries(winner).stream()
                .map(entry -> gm.getEliminatedPlayers().contains(entry) ? "§7§m" + entry + "§r" : "§f" + entry)
                .collect(Collectors.toList());
        String membersList = String.join("§7, ", formattedNames);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(legacySection().deserialize(""));
            p.sendMessage(legacySection().deserialize(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", teamName)));
            p.sendMessage(legacySection().deserialize(lang.get("victory.team-members", p).replace("%members%", membersList)));
            p.sendMessage(legacySection().deserialize(lang.get("victory.broadcast-footer", p)));
            p.sendMessage(legacySection().deserialize(""));

            p.showTitle(Title.title(
                legacySection().deserialize(lang.get("victory.title", p)),
                legacySection().deserialize(lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", teamName)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            
            showPostGameScoreboard(p, winner, lang);
        }
    }

    private void showPostGameScoreboard(Player player, Team winner, LanguageManager lang) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", Criteria.DUMMY, lang.getComponent("victory.scoreboard-title", player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        String colorCode = TextUtil.legacyColor(winner.color());
        obj.getScore(lang.get("victory.scoreboard-winner", player)
                .replace("%color%", colorCode)
                .replace("%team%", legacySection().serialize(winner.displayName()))).setScore(1);
        player.setScoreboard(board);
    }
}
