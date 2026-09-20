package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la normalización del input de color de equipos.
 * El prefijo interno {@code h_} no debe exigirse al usuario: se aceptan
 * "red", "h_red" y "RED" (y el nombre interno) como equivalentes.
 */
class TeamManagerTest {

    @Test
    void normalizeColorInput_plainColor_isLowercased() {
        assertEquals("red", TeamManager.normalizeColorInput("Red"));
        assertEquals("red", TeamManager.normalizeColorInput("RED"));
        assertEquals("light_blue", TeamManager.normalizeColorInput("Light_Blue"));
    }

    @Test
    void normalizeColorInput_withInternalPrefix_stripsPrefix() {
        assertEquals("red", TeamManager.normalizeColorInput("h_red"));
        assertEquals("red", TeamManager.normalizeColorInput("H_RED"));
    }

    @Test
    void normalizeColorInput_fullInternalName_stripsPrefix() {
        String fullName = ScoreboardHelper.TEAM_PREFIX + "blue";
        assertEquals("blue", TeamManager.normalizeColorInput(fullName));
    }

    @Test
    void normalizeColorInput_null_returnsEmpty() {
        assertEquals("", TeamManager.normalizeColorInput(null));
    }

    @Test
    void normalizeColorInput_neverReturnsPrefix() {
        assertTrue(!TeamManager.normalizeColorInput("h_red").startsWith(ScoreboardHelper.TEAM_PREFIX));
        assertTrue(!TeamManager.normalizeColorInput("red").startsWith(ScoreboardHelper.TEAM_PREFIX));
    }
}