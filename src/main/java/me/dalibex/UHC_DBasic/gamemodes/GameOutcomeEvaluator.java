package me.dalibex.UHC_DBasic.gamemodes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Pure winner grouping policy shared by game modes and tests. */
public final class GameOutcomeEvaluator {
    public enum Status { IN_PROGRESS, NO_SURVIVORS, WINNER }

    public record Outcome(Status status, String winnerKey, boolean teamWinner) {
    }

    private GameOutcomeEvaluator() {
    }

    public static Outcome evaluate(Collection<String> participants, Set<String> eliminated,
            Map<String, String> teamByPlayer) {
        Set<String> entities = new HashSet<>();
        String solePlayer = null;
        boolean soleIsTeam = false;
        for (String player : participants) {
            if (eliminated.contains(player)) continue;
            String team = teamByPlayer.get(player);
            String entity = team == null ? "SOLO\u0000" + player : "TEAM\u0000" + team;
            entities.add(entity);
            solePlayer = team == null ? player : team;
            soleIsTeam = team != null;
        }
        if (entities.isEmpty()) return new Outcome(Status.NO_SURVIVORS, null, false);
        if (entities.size() == 1) return new Outcome(Status.WINNER, solePlayer, soleIsTeam);
        return new Outcome(Status.IN_PROGRESS, null, false);
    }
}
