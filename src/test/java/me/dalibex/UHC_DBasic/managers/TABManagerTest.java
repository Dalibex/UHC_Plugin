package me.dalibex.UHC_DBasic.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TABManagerTest {

    @Test
    void inactiveMatch_showsWhiteRealName() {
        assertEquals("§fAlex", TABManager.resolveIdentityText("Steve", "Alex", false, false, false, false, "Notch"));
        assertEquals("§f", TABManager.resolveIdentityText("Steve", "Alex", true, false, false, false, "Notch"));
    }

    @Test
    void teammate_showsGreenRealName() {
        assertEquals("§aAlex", TABManager.resolveIdentityText("Steve", "Alex", false, true, true, false, "Notch"));
        assertEquals("§a", TABManager.resolveIdentityText("Steve", "Alex", true, true, true, false, "Notch"));
    }

    @Test
    void hiddenEnemy_showsPinkFakeNameInTab() {
        assertEquals("§dNotch", TABManager.resolveIdentityText("Steve", "Alex", false, true, false, false, "Notch"));
        assertEquals("§c", TABManager.resolveIdentityText("Steve", "Alex", true, true, false, false, "Notch"));
    }

    @Test
    void revealedEnemy_showsRedRealName() {
        assertEquals("§cAlex", TABManager.resolveIdentityText("Steve", "Alex", false, true, false, true, "Notch"));
        assertEquals("§c", TABManager.resolveIdentityText("Steve", "Alex", true, true, false, true, "Notch"));
    }
}
