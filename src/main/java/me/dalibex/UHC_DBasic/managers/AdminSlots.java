package me.dalibex.UHC_DBasic.managers;

/**
 * Única fuente de verdad para los slots de los inventarios del panel de admin.
 * Tanto AdminPanelManager (construcción de los menús) como AdminPanelListener
 * (manejo de clics) deben referenciar estas constantes en lugar de números
 * mágicos, para evitar la deriva entre lo que se dibuja y lo que se interpreta
 */
public final class AdminSlots {

    private AdminSlots() {
    }

    // ---- Panel principal (9 slots) ----
    public static final int MAIN_COMBAT = 0;
    public static final int MAIN_GENERAL_RULES = 1;
    public static final int MAIN_GAME_RULES = 2;
    public static final int MAIN_GAMEMODE = 3;
    public static final int MAIN_BORDER = 4;
    public static final int MAIN_TIME = 6;
    public static final int MAIN_CUSTOM_TEAMS = 7;
    public static final int MAIN_TEAMS_SIZE = 8;

    // ---- Panel de modos de juego (9 slots) ----
    public static final int GAMEMODE_BACK = 0;
    public static final int GAMEMODE_CLASSIC = 2;
    public static final int GAMEMODE_RESOURCE_RUSH = 4;

    // ---- Panel de reglas generales (hub, 27 slots) ----
    public static final int GENERAL_SHULKERS_MENU = 11;
    public static final int GENERAL_TEAMS_EPISODE_MENU = 15;
    public static final int GENERAL_BACK = 18;

    // ---- Sub-panel de shulkers de episodio (27 slots) ----
    public static final int SHULKERS_TOGGLE_1 = 11;
    public static final int SHULKERS_TOGGLE_2 = 15;
    public static final int SHULKERS_BACK = 18;

    // ---- Sub-panel de episodio de formación de equipos (45 slots) ----
    public static final int TEAMS_EPISODE_INFO = 13;
    public static final int TEAMS_EPISODE_BACK = 40;
    public static final int[] TEAMS_EPISODE_BUTTONS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};

    /**
     * Devuelve el episodio (1-10) asociado a un slot del sub-panel, o -1 si
     * el slot no es un botón de episodio.
     */
    public static int episodeForSlot(int slot) {
        for (int i = 0; i < TEAMS_EPISODE_BUTTONS.length; i++) {
            if (TEAMS_EPISODE_BUTTONS[i] == slot) return i + 1;
        }
        return -1;
    }

    // ---- Panel de gamerules (36 slots) ----
    public static final int RULES_NATURAL_REGENERATION = 10;
    public static final int RULES_PVP = 11;
    public static final int RULES_DAY_NIGHT = 12;
    public static final int RULES_MONSTERS = 13;
    public static final int RULES_ADVANCEMENTS = 14;
    public static final int RULES_TRADER = 15;
    public static final int RULES_LOCATOR = 16;
    public static final int RULES_BACK = 27;

    // ---- Panel del borde (36 slots) ----
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

    // ---- Panel de tiempo (27 slots) ----
    public static final int TIME_INFO = 13;
    public static final int TIME_MINUS_1 = 10;
    public static final int TIME_MINUS_5 = 11;
    public static final int TIME_MINUS_10 = 12;
    public static final int TIME_PLUS_1 = 14;
    public static final int TIME_PLUS_5 = 15;
    public static final int TIME_PLUS_10 = 16;
    public static final int TIME_PAUSE = 21;
    public static final int TIME_BACK = 18;
}