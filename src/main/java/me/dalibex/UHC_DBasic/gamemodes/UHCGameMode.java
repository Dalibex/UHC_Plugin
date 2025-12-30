package me.dalibex.UHC_DBasic.gamemodes;

import org.bukkit.entity.Player;

public interface UHCGameMode {

    /**
     * Devuelve el nombre interno del modo.
     */
    String getName();

    /**
     * Se ejecuta cada segundo. Aquí va la lógica de eventos de tiempo
     * (entregar shulkers, activar pvp, sonidos de capítulos).
     */
    void onTick(int cronometroSegundos, int tiempoTotalSegundos);

    /**
     * Define cómo se ve el Scoreboard para este modo específico.
     */
    void updateScoreboard(Player player, String chapterTime, String totalTime, boolean partidaActiva);

    /**
     * Lógica de detección de ganadores (puede variar según el modo).
     */
    void checkVictory();

    /**
     * Se ejecuta cuando el plugin se resetea (lobby).
     */
    void onReset();
}
