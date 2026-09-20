package me.dalibex.UHC_DBasic.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import me.dalibex.UHC_DBasic.managers.GameManager;

import me.dalibex.UHC_DBasic.utils.TimeUtil;
import me.dalibex.UHC_DBasic.utils.UpdateChecker;
import net.kyori.adventure.text.Component;

/**
 * Listener especializado en el manejo de conexiones y desconexiones de jugadores.
 * Gestiona el spawn inicial, reconexiones a partidas en curso y sincronización de scoreboards.
 */
public class PlayerConnectionListener implements Listener {

    private final UHC_DBasic plugin;

    public PlayerConnectionListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        UHCGameMode modo = gm.getCurrentMode();
        // 1. Manejo de estados de juego (Partida iniciada vs Lobby)
        if (gm.isGameStarted()) {
            handleInGameJoin(p, gm);
        } else {
            handleLobbyJoin(p);
        }

        // 2. Aplicar mecánicas globales (Velocidad de ataque)
        double attackSpeedValue = plugin.getAdminPanel().isCombate18() ? 1024.0 : 4.0;
        if (p.getAttribute(Attribute.ATTACK_SPEED) != null) {
            var attackSpeed = p.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(attackSpeedValue);
            }
        }

        // 3. Sincronización de Scoreboard
        updateAllScoreboards(gm, modo);

        // 4. Notificación de actualizaciones para todos los jugadores
        handleUpdateNotice(p);
    }

    /**
     * Maneja el ingreso al juego cuando hay una partida activa.
     */
    private void handleInGameJoin(Player p, GameManager gm) {
        boolean eraParticipante = gm.getInitialParticipants().contains(p.getName());
        boolean estaEliminado = gm.getEliminatedPlayers().contains(p.getName());

        if (!eraParticipante || estaEliminado) {
            p.setGameMode(GameMode.SPECTATOR);
        } else {
            p.setGameMode(GameMode.SURVIVAL);
            // Synch identidad: re-aplica la skin correcta (falsa si aún no fue
            // revelado, la propia si ya lo fue). Durante PlayerJoinEvent TAB
            // aún no ha cargado al jugador y lanzaría IllegalStateException;
            // se difiere la actualización visual a los 1 y 20 ticks.
            plugin.getSkinsManager().reapplyCurrentSkin(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.getSkinsManager().updateVisualIdentity(p);
            }, 1L);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.getSkinsManager().updateVisualIdentity(p);
            }, 20L);
        }
    }

    /**
     * Avisa en el chat si hay una versión más reciente del plugin.
     * Si el check asíncrono aún no ha terminado, reintenta una vez en breve.
     */
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
        }, 80L);
    }

    private void sendUpdateNotice(Player p) {
        String latest = UpdateChecker.getLatestVersionFound();
        String actual = plugin.getPluginMeta().getVersion();
        String url = "https://github.com/Dalibex/UHC_Plugin/releases";
        p.sendMessage(Component.empty());
        p.sendMessage(plugin.getLang().get("general.update-available", p).replace("%latest%", latest).replace("%current%", actual));
        p.sendMessage(plugin.getLang().get("general.update-download", p).replace("%url%", url));
        p.sendMessage(Component.empty());
    }

    /**
     * Gestiona el ingreso del jugador en el lobby previo a la partida.
     */
    private void handleLobbyJoin(Player p) {
        plugin.getGameManager().applyLobbySettings(p);
    }

    /**
     * Fuerza la actualización del scoreboard para todos los jugadores online.
     */
    private void updateAllScoreboards(GameManager gm, UHCGameMode modo) {
        int crono = gm.getTotalSeconds();
        String timeStr = TimeUtil.formatClock(crono);
        boolean active = crono > 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            modo.updateScoreboard(online, "...", timeStr, active);
        }
    }
}
