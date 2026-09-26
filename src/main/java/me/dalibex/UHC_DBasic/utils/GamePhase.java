package me.dalibex.UHC_DBasic.utils;

/**
 * Global game phase and single source of truth for startup/running/pause state.
 * INITIALIZING -> Plugin loading before the automatic reset.
 * LOBBY -> Match not started; admin menu and player waiting state.
 * PREPARING -> Roster locked and players being scattered.
 * COUNTDOWN -> Countdown before gameplay starts.
 * RUNNING -> Active match with time advancing.
 * PAUSED -> Active match with the timer paused.
 * ENDING -> Match finishing.
 */
public enum GamePhase {
    INITIALIZING,
    LOBBY,
    PREPARING,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    ENDING
}
