package me.dalibex.UHC_DBasic.utils;

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

    @Test void borderDeltaSlotsMapExactly() {
        assertEquals(-10, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_MINUS_10));
        assertEquals(-100, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_MINUS_100));
        assertEquals(-500, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_MINUS_500));
        assertEquals(-1000, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_MINUS_1000));
        assertEquals(10, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_PLUS_10));
        assertEquals(100, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_PLUS_100));
        assertEquals(500, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_PLUS_500));
        assertEquals(1000, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_PLUS_1000));
        assertEquals(0, AdminSlots.borderDeltaForSlot(AdminSlots.BORDER_BACK));
    }

    @Test void timeDeltaSlotsMapExactly() {
        assertEquals(-1, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_MINUS_1));
        assertEquals(-5, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_MINUS_5));
        assertEquals(-10, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_MINUS_10));
        assertEquals(1, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_PLUS_1));
        assertEquals(5, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_PLUS_5));
        assertEquals(10, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_PLUS_10));
        assertEquals(0, AdminSlots.timeDeltaMinutesForSlot(AdminSlots.TIME_BACK));
    }

    @Test void publicSlotConstantsAreUniqueAndInPanelBounds() throws IllegalAccessException {
        Map<String, Integer> bounds = Map.of(
                "MAIN_", 9, "GAMEMODE_", 9, "GENERAL_", 27, "SHULKERS_", 27,
                "SHULKER_EPISODE_", 45, "TEAMS_EPISODE_", 45, "PVP_EPISODE_", 45,
                "RULES_", 36, "BORDER_", 36, "TIME_", 27);
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
