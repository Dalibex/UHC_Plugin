package me.dalibex.UHC_DBasic.gamemodes.resourcerush;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ResourceRushObjectiveTrackerTest {

    @Test void reservedObjectivesStayHiddenUntilRouletteFinishes() {
        ResourceRushObjectiveTracker tracker = new ResourceRushObjectiveTracker();
        List<Material> selected = tracker.reserveObjectivesForChapter(1);

        assertEquals(2, selected.size());
        assertEquals(0, tracker.activeObjectiveCount());
        assertFalse(tracker.isActiveObjective(selected.getFirst()));

        assertTrue(tracker.activateObjective(selected.getFirst()));
        assertEquals(1, tracker.activeObjectiveCount());
        assertTrue(tracker.isActiveObjective(selected.getFirst()));
        assertFalse(tracker.activateObjective(selected.getFirst()));
    }

    @Test void chapterCandidatesAreDefensiveCopy() {
        ResourceRushObjectiveTracker tracker = new ResourceRushObjectiveTracker();
        List<Material> candidates = tracker.candidatesForChapter(2);

        assertEquals(8, candidates.size());
        assertThrowsUnsupported(candidates);
    }

    private static void assertThrowsUnsupported(List<Material> candidates) {
        try {
            candidates.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("candidate list must be immutable");
    }
}
