package me.dalibex.UHC_DBasic.managers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSlotsTest {
    @Test void episodeSlotsMapExactly() {
        int[] slots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        for (int i = 0; i < slots.length; i++) assertEquals(i + 1, AdminSlots.episodeForSlot(slots[i]));
        assertEquals(-1, AdminSlots.episodeForSlot(-1));
        assertEquals(-1, AdminSlots.episodeForSlot(19));
        assertEquals(-1, AdminSlots.episodeForSlot(34));
        assertEquals(-1, AdminSlots.episodeForSlot(45));
    }

    @Test void publicSlotConstantsAreUniqueAndInPanelBounds() throws IllegalAccessException {
        Map<String, Integer> bounds = Map.of(
                "MAIN_", 9, "GAMEMODE_", 9, "GENERAL_", 27, "SHULKERS_", 27,
                "TEAMS_EPISODE_", 45, "RULES_", 36, "BORDER_", 36, "TIME_", 27);
        for (Map.Entry<String, Integer> panel : bounds.entrySet()) {
            Set<Integer> occupied = new HashSet<>();
            for (Field field : AdminSlots.class.getFields()) {
                if (field.getType() != int.class || !Modifier.isStatic(field.getModifiers())
                        || !field.getName().startsWith(panel.getKey())) continue;
                int slot = field.getInt(null);
                assertTrue(slot >= 0 && slot < panel.getValue(), field.getName() + " out of bounds");
                assertTrue(occupied.add(slot), field.getName() + " duplicates slot " + slot);
            }
        }
    }
}
