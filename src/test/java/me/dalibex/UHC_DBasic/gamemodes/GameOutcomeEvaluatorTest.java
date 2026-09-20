package me.dalibex.UHC_DBasic.gamemodes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameOutcomeEvaluatorTest {
    @Test void noSurvivors() {
        var result = GameOutcomeEvaluator.evaluate(List.of("A", "B"), Set.of("A", "B"), Map.of());
        assertEquals(GameOutcomeEvaluator.Status.NO_SURVIVORS, result.status());
        assertNull(result.winnerKey());
    }

    @Test void loneOfflineParticipantStillWins() {
        var result = GameOutcomeEvaluator.evaluate(List.of("Offline", "Dead"), Set.of("Dead"), Map.of());
        assertEquals(GameOutcomeEvaluator.Status.WINNER, result.status());
        assertEquals("Offline", result.winnerKey());
        assertFalse(result.teamWinner());
    }
    @Test void survivingMembersOfOneTeamWinTogether() {
        var result = GameOutcomeEvaluator.evaluate(List.of("A", "B", "C"), Set.of("C"),
                Map.of("A", "red", "B", "red", "C", "blue"));
        assertEquals(GameOutcomeEvaluator.Status.WINNER, result.status());
        assertEquals("red", result.winnerKey());
        assertTrue(result.teamWinner());
    }

    @Test void differentTeamsAndSolosKeepGameOpen() {
        assertEquals(GameOutcomeEvaluator.Status.IN_PROGRESS,
                GameOutcomeEvaluator.evaluate(List.of("A", "B"), Set.of(), Map.of("A", "red", "B", "blue")).status());
        assertEquals(GameOutcomeEvaluator.Status.IN_PROGRESS,
                GameOutcomeEvaluator.evaluate(List.of("A", "B"), Set.of(), Map.of("A", "red")).status());
    }
}
