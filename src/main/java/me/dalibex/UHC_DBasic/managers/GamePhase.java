package me.dalibex.UHC_DBasic.managers;

/**
 * Estado global de la partida. Sustituye al par de booleanos
 * (partidaIniciada / pausado) como ÚNICA fuente de verdad.
 * LOBBY   -> Partida no iniciada (menú de admin, espera de jugadores).
 * RUNNING -> Partida en curso, tiempo avanzando.
 * PAUSED  -> Partida en curso pero con el cronómetro pausado (DYE del panel).
 * ENDING  -> Partida finalizándose.
 */
public enum GamePhase {
    LOBBY,
    RUNNING,
    PAUSED,
    ENDING
}