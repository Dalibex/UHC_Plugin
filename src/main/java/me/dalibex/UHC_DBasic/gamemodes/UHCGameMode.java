package me.dalibex.UHC_DBasic.gamemodes;

import org.bukkit.entity.Player;

public interface UHCGameMode {

    /** Returns the internal mode name. */
    String getName();

    /** Runs every second for timed mode events. */
    void onTick(int cronometroSegundos, int tiempoTotalSegundos);

    /** Updates the mode-specific scoreboard. */
    void updateScoreboard(Player player, String chapterTime, String totalTime, boolean partidaActiva);

    /** Checks mode-specific victory conditions. */
    void checkVictory();

    /** Runs when the plugin resets to lobby. */
    void onReset();
}
