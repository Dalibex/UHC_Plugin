package me.dalibex.UHC_DBasic.managers;

/**
 * Estado global de la partida. Sustituye al par de booleanos
 * (partidaIniciada / pausado) como ÚNICA fuente de verdad.
 * INITIALIZING -> Plugin cargando antes del reset automático.
 * LOBBY        -> Partida no iniciada (menú de admin, espera de jugadores).
 * PREPARING    -> Roster cerrado y jugadores siendo distribuidos.
 * COUNTDOWN    -> Cuenta atrás previa al inicio efectivo.
 * RUNNING      -> Partida en curso, tiempo avanzando.
 * PAUSED       -> Partida en curso pero con el cronómetro pausado (DYE del panel).
 * ENDING       -> Partida finalizándose.
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
