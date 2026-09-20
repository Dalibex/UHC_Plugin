package me.dalibex.UHC_DBasic.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetTeamEpisodeCommandTest {
    @Test void acceptsInclusiveBoundariesOnly() {
        assertFalse(SetTeamEpisodeCommand.isValidEpisode(0));
        assertTrue(SetTeamEpisodeCommand.isValidEpisode(1));
        assertTrue(SetTeamEpisodeCommand.isValidEpisode(10));
        assertFalse(SetTeamEpisodeCommand.isValidEpisode(11));
    }
}
