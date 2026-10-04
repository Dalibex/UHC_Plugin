package me.dalibex.UHC_DBasic.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ItemRouletteAnimationTest {

    private static final List<Material> CANDIDATES = List.of(
            Material.DIAMOND, Material.GOLDEN_APPLE, Material.BREEZE_ROD);

    @Test void select_returnsCandidateWithoutMutatingPool() {
        List<Material> original = List.copyOf(CANDIDATES);
        Material selected = ItemRouletteAnimation.select(CANDIDATES, new Random(3));

        assertTrue(CANDIDATES.contains(selected));
        assertEquals(original, CANDIDATES);
    }

    @Test void frames_haveConfiguredSizeAndPredeterminedResult() {
        List<Material> frames = ItemRouletteAnimation.frames(
                CANDIDATES, Material.TOTEM_OF_UNDYING, 24, new Random(7));

        assertEquals(24, frames.size());
        assertEquals(Material.TOTEM_OF_UNDYING, frames.getLast());
    }

    @Test void frames_avoidAdjacentDuplicatesWhenAlternativesExist() {
        List<Material> frames = ItemRouletteAnimation.frames(
                CANDIDATES, Material.DIAMOND, 30, new Random(1));

        for (int i = 1; i < frames.size(); i++) {
            assertFalse(frames.get(i - 1) == frames.get(i), "duplicate at frame " + i);
        }
    }

    @Test void delays_slowDownAndIncludeWinnerHold() {
        assertEquals(2L, ItemRouletteAnimation.delayAfterFrame(0, 24, 2L, 10L));
        assertTrue(ItemRouletteAnimation.delayAfterFrame(20, 24, 2L, 10L)
                > ItemRouletteAnimation.delayAfterFrame(5, 24, 2L, 10L));
        assertEquals(40L, ItemRouletteAnimation.totalDurationTicks(1, 2L, 10L, 40L));
    }

    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemRouletteAnimation.select(List.of(), new Random()));
        assertThrows(IllegalArgumentException.class,
                () -> ItemRouletteAnimation.frames(CANDIDATES, Material.DIAMOND, 0, new Random()));
    }
}
