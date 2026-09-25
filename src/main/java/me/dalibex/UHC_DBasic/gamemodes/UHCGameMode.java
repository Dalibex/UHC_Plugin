package me.dalibex.UHC_DBasic.gamemodes;

import org.bukkit.entity.Player;

public interface UHCGameMode {

    /** Returns the internal mode name. */
    String getName();

    /** Runs every second for timed mode events. */
    void onTick(int chapterSeconds, int totalSeconds);

    /** Updates the mode-specific scoreboard. */
    void updateScoreboard(Player player, String chapterTime, String totalTime, boolean matchActive);

    /** Checks mode-specific victory conditions. */
    void checkVictory();

    /** Runs when the plugin resets to lobby. */
    void onReset();
}
