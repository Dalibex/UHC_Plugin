package me.dalibex.UHC_DBasic.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetPvpEpisodeCommandTest {
    @Test void acceptsInclusiveBoundariesOnly() {
        assertFalse(SetPvpEpisodeCommand.isValidEpisode(0));
        assertTrue(SetPvpEpisodeCommand.isValidEpisode(1));
        assertTrue(SetPvpEpisodeCommand.isValidEpisode(10));
        assertFalse(SetPvpEpisodeCommand.isValidEpisode(11));
    }
}
