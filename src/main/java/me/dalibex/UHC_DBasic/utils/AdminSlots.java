package me.dalibex.UHC_DBasic.utils;

/**
 * Single source of truth for admin panel inventory slots.
 * Builders and click handlers must use these constants to avoid drift.
 */
public final class AdminSlots {

    private AdminSlots() {
    }

    // ---- Main panel (9 slots) ----
    public static final int MAIN_COMBAT = 0;
    public static final int MAIN_GENERAL_RULES = 1;
    public static final int MAIN_GAME_RULES = 2;
    public static final int MAIN_GAMEMODE = 3;
    public static final int MAIN_BORDER = 4;
    public static final int MAIN_TIME = 6;
    public static final int MAIN_CUSTOM_TEAMS = 7;
    public static final int MAIN_TEAMS_SIZE = 8;

    // ---- Gamemode panel (9 slots) ----
    public static final int GAMEMODE_BACK = 0;
    public static final int GAMEMODE_CLASSIC = 2;
    public static final int GAMEMODE_RESOURCE_RUSH = 4;

    // ---- General rules hub (27 slots) ----
    public static final int GENERAL_SHULKERS_MENU = 11;
    public static final int GENERAL_PVP_EPISODE_MENU = 13;
    public static final int GENERAL_TEAMS_EPISODE_MENU = 15;
    public static final int GENERAL_BACK = 18;

    // ---- Episode shulkers panel (27 slots) ----
    public static final int SHULKERS_TOGGLE_1 = 10;
    public static final int SHULKERS_EPISODE_1 = 12;
    public static final int SHULKERS_EPISODE_2 = 14;
    public static final int SHULKERS_TOGGLE_2 = 16;
    public static final int SHULKERS_BACK = 18;

    public static final int[] EPISODE_BUTTONS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    // ---- Shulker episode selector (45 slots) ----
    public static final int SHULKER_EPISODE_INFO = 13;
    public static final int SHULKER_EPISODE_BACK = 40;
    public static final int[] SHULKER_EPISODE_BUTTONS = EPISODE_BUTTONS;

    // ---- Team formation episode selector (45 slots) ----
    public static final int TEAMS_EPISODE_INFO = 13;
    public static final int TEAMS_EPISODE_BACK = 40;
    public static final int[] TEAMS_EPISODE_BUTTONS = EPISODE_BUTTONS;

    // ---- PVP episode selector (45 slots) ----
    public static final int PVP_EPISODE_INFO = 13;
    public static final int PVP_EPISODE_BACK = 40;
    public static final int[] PVP_EPISODE_BUTTONS = TEAMS_EPISODE_BUTTONS;

    /**
     * Returns the episode (1-10) mapped to a selector slot, or -1 otherwise.
     */
    public static int episodeForSlot(int slot) {
        for (int i = 0; i < EPISODE_BUTTONS.length; i++) {
            if (EPISODE_BUTTONS[i] == slot) return i + 1;
        }
        return -1;
    }

    // ---- Gamerules panel (36 slots) ----
    public static final int RULES_NATURAL_REGENERATION = 10;
    public static final int RULES_PVP = 11;
    public static final int RULES_DAY_NIGHT = 12;
    public static final int RULES_MONSTERS = 13;
    public static final int RULES_ADVANCEMENTS = 14;
    public static final int RULES_TRADER = 15;
    public static final int RULES_LOCATOR = 16;
    public static final int RULES_BACK = 27;

    // ---- Border panel (36 slots) ----
    public static final int BORDER_INFO = 13;
    public static final int BORDER_MINUS_10 = 10;
    public static final int BORDER_MINUS_100 = 11;
    public static final int BORDER_MINUS_500 = 19;
    public static final int BORDER_MINUS_1000 = 20;
    public static final int BORDER_PLUS_10 = 15;
    public static final int BORDER_PLUS_100 = 16;
    public static final int BORDER_PLUS_500 = 24;
    public static final int BORDER_PLUS_1000 = 25;
    public static final int BORDER_BACK = 31;

    public static int borderDeltaForSlot(int slot) {
        return switch (slot) {
            case BORDER_MINUS_10 -> -10;
            case BORDER_MINUS_100 -> -100;
            case BORDER_MINUS_500 -> -500;
            case BORDER_MINUS_1000 -> -1000;
            case BORDER_PLUS_10 -> 10;
            case BORDER_PLUS_100 -> 100;
            case BORDER_PLUS_500 -> 500;
            case BORDER_PLUS_1000 -> 1000;
            default -> 0;
        };
    }

    // ---- Time panel (27 slots) ----
    public static final int TIME_INFO = 13;
    public static final int TIME_MINUS_1 = 10;
    public static final int TIME_MINUS_5 = 11;
    public static final int TIME_MINUS_10 = 12;
    public static final int TIME_PLUS_1 = 14;
    public static final int TIME_PLUS_5 = 15;
    public static final int TIME_PLUS_10 = 16;
    public static final int TIME_PAUSE = 21;
    public static final int TIME_BACK = 18;

    public static int timeDeltaMinutesForSlot(int slot) {
        return switch (slot) {
            case TIME_MINUS_1 -> -1;
            case TIME_MINUS_5 -> -5;
            case TIME_MINUS_10 -> -10;
            case TIME_PLUS_1 -> 1;
            case TIME_PLUS_5 -> 5;
            case TIME_PLUS_10 -> 10;
            default -> 0;
        };
    }
}
