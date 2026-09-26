package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/** Owns configurable gameplay settings that are edited from the Admin Panel. */
public class MatchSettingsManager {

    private boolean combat18 = false;
    private boolean offhandLocked = false;
    private boolean shulkerOneEnabled = true;
    private boolean shulkerTwoEnabled = true;
    private int shulkerOneEpisode = 1;
    private int shulkerTwoEpisode = 8;

    public void toggleCombat18() {
        combat18 = !combat18;
        double speedValue = combat18 ? 1024.0 : 4.0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) attackSpeed.setBaseValue(speedValue);
        }
    }

    public void toggleOffhandLock() {
        offhandLocked = !offhandLocked;
    }

    public boolean isCombat18() { return combat18; }
    public boolean isOffhandLocked() { return offhandLocked; }
    public boolean isShulkerOneEnabled() { return shulkerOneEnabled; }
    public void setShulkerOneEnabled(boolean enabled) { this.shulkerOneEnabled = enabled; }
    public boolean isShulkerTwoEnabled() { return shulkerTwoEnabled; }
    public void setShulkerTwoEnabled(boolean enabled) { this.shulkerTwoEnabled = enabled; }
    public int getShulkerOneEpisode() { return shulkerOneEpisode; }
    public void setShulkerOneEpisode(int episode) { this.shulkerOneEpisode = episode; }
    public int getShulkerTwoEpisode() { return shulkerTwoEpisode; }
    public void setShulkerTwoEpisode(int episode) { this.shulkerTwoEpisode = episode; }
}
