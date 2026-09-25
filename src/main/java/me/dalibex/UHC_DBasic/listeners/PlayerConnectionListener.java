package me.dalibex.UHC_DBasic.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.GamePhase;

import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TimeUtil;
import me.dalibex.UHC_DBasic.utils.UpdateChecker;
import net.kyori.adventure.text.Component;

/** Handles joins, active-match reconnects, and scoreboard synchronization. */
public class PlayerConnectionListener implements Listener {

    private static final double ATTACK_SPEED_1_8 = 1024.0;
    private static final double DEFAULT_ATTACK_SPEED = 4.0;
    private static final long FIRST_SKIN_SYNC_DELAY_TICKS = 1L;
    private static final long SECOND_SKIN_SYNC_DELAY_TICKS = 20L;
    private static final long UPDATE_NOTICE_RETRY_DELAY_TICKS = 80L;
    private static final String RELEASES_URL = "https://github.com/Dalibex/UHC_Plugin/releases";

    private final UHC_DBasic plugin;

    public PlayerConnectionListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        UHCGameMode mode = gm.getCurrentMode();

        if (gm.isGameStarted()) {
            handleInGameJoin(p, gm);
        } else {
            handleLobbyJoin(p);
        }

        double attackSpeedValue = plugin.getAdminPanel().isCombate18() ? ATTACK_SPEED_1_8 : DEFAULT_ATTACK_SPEED;
        if (p.getAttribute(Attribute.ATTACK_SPEED) != null) {
            var attackSpeed = p.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(attackSpeedValue);
            }
        }

        updateAllScoreboards(gm, mode);
        handleUpdateNotice(p);
    }

    /** Handles joins while the game has already started. */
    private void handleInGameJoin(Player p, GameManager gm) {
        GamePhase phase = gm.getPhase();
        if (phase == GamePhase.PREPARING || phase == GamePhase.COUNTDOWN) {
            p.setGameMode(GameMode.SPECTATOR);
            if (gm.isEligibleRosterMember(p.getUniqueId())) {
                Location planned = gm.getPlannedScatterLocation(p.getUniqueId());
                if (planned != null) p.teleport(planned);
            }
            return;
        }
        boolean wasParticipant = gm.getInitialParticipants().contains(p.getName());
        boolean isEliminated = gm.getEliminatedPlayers().contains(p.getName());

        if (!wasParticipant || isEliminated) {
            p.setGameMode(GameMode.SPECTATOR);
        } else {
            p.setGameMode(GameMode.SURVIVAL);
            // TAB may not be ready during PlayerJoinEvent, so visual identity is synced twice.
            plugin.getSkinsManager().reapplyCurrentSkin(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.getSkinsManager().updateVisualIdentity(p);
            }, FIRST_SKIN_SYNC_DELAY_TICKS);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.getSkinsManager().updateVisualIdentity(p);
            }, SECOND_SKIN_SYNC_DELAY_TICKS);
        }
    }

    /** Sends an update notice, retrying once if the async check is still running. */
    private void handleUpdateNotice(Player p) {
        if (UpdateChecker.isCheckDone()) {
            if (UpdateChecker.isUpdateAvailable()) {
                sendUpdateNotice(p);
            }
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && UpdateChecker.isUpdateAvailable()) {
                sendUpdateNotice(p);
            }
        }, UPDATE_NOTICE_RETRY_DELAY_TICKS);
    }

    private void sendUpdateNotice(Player p) {
        String latest = UpdateChecker.getLatestVersionFound();
        String actual = plugin.getPluginMeta().getVersion();
        p.sendMessage(Component.empty());
        p.sendMessage(plugin.getLang().get("general.update-available", p).replace("%latest%", latest).replace("%current%", actual));
        p.sendMessage(plugin.getLang().get("general.update-download", p).replace("%url%", RELEASES_URL));
        p.sendMessage(Component.empty());
    }

    /** Handles lobby joins before the match starts. */
    private void handleLobbyJoin(Player p) {
        plugin.getGameManager().applyLobbySettings(p);
    }

    /** Forces scoreboard refresh for every online player. */
    private void updateAllScoreboards(GameManager gm, UHCGameMode mode) {
        int totalSeconds = gm.getTotalSeconds();
        String timeStr = TimeUtil.formatClock(totalSeconds);
        boolean active = gm.isMatchActive();

        for (Player online : Bukkit.getOnlinePlayers()) {
            mode.updateScoreboard(online, "...", timeStr, active);
            if (active) {
                ScoreboardHelper.ensureTabHealthObjective(online.getScoreboard(), online, plugin.getLang());
            } else {
                ScoreboardHelper.removeTabHealthObjective(online.getScoreboard());
            }
        }
    }
}
