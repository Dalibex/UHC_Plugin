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

    // ---- Panel de reglas generales (27 slots) ----
    public static final int GENERAL_SHULKER_1 = 11;
    public static final int GENERAL_SHULKER_2 = 15;
    public static final int GENERAL_BACK = 18;

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