package me.dalibex.UHC_DBasic.listeners;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameLogicListenerTest {

    @Test
    void legacyAxeDamage_matchesCombat18Values() {
        assertEquals(3.0, GameLogicListener.legacyAxeDamage(Material.WOODEN_AXE));
        assertEquals(3.0, GameLogicListener.legacyAxeDamage(Material.GOLDEN_AXE));
        assertEquals(4.0, GameLogicListener.legacyAxeDamage(Material.STONE_AXE));
        assertEquals(5.0, GameLogicListener.legacyAxeDamage(Material.IRON_AXE));
        assertEquals(6.0, GameLogicListener.legacyAxeDamage(Material.DIAMOND_AXE));
        assertEquals(7.0, GameLogicListener.legacyAxeDamage(Material.NETHERITE_AXE));
    }

    @Test
    void legacyAxeDamage_ignoresNonAxes() {
        assertEquals(0.0, GameLogicListener.legacyAxeDamage(Material.DIAMOND_SWORD));
        assertEquals(0.0, GameLogicListener.legacyAxeDamage(Material.AIR));
    }
}
