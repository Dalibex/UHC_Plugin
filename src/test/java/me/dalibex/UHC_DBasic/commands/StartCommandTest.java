package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.utils.GamePhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartCommandTest {
    @Test void borderMinimumIsInclusive() {
        assertFalse(StartCommand.isValidBorderSize(19));
        assertTrue(StartCommand.isValidBorderSize(20));
        assertTrue(StartCommand.isValidBorderSize(Integer.MAX_VALUE));
    }

    @Test void onlyLobbyIsStartable() {
        for (GamePhase phase : GamePhase.values()) {
            assertTrue(StartCommand.isStartablePhase(phase) == (phase == GamePhase.LOBBY), phase.name());
        }
        assertFalse(StartCommand.isStartablePhase(null));
    }
}
