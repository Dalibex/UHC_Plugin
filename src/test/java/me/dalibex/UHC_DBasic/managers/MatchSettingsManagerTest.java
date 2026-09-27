package me.dalibex.UHC_DBasic.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSettingsManagerTest {

    @Test
    void skinRotation_defaultsEnabledAndToggles() {
        MatchSettingsManager settings = new MatchSettingsManager();

        assertTrue(settings.isSkinRotationEnabled());
        settings.toggleSkinRotation();
        assertFalse(settings.isSkinRotationEnabled());
        settings.toggleSkinRotation();
        assertTrue(settings.isSkinRotationEnabled());
    }
}
