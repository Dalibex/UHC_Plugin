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
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;

import me.dalibex.UHC_DBasic.utils.UpdateChecker;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

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
        UHCGameMode modo = gm.getModoActual();
        // 1. Manejo de estados de juego (Partida iniciada vs Lobby)
        if (gm.isPartidaIniciada()) {
            handleInGameJoin(p, gm);
        } else {
            handleLobbyJoin(p);
        }

        // 2. Aplicar mecánicas globales (Velocidad de ataque)
        double attackSpeedValue = AdminPanelManager.combate18 ? 1024.0 : 4.0;
        if (p.getAttribute(Attribute.ATTACK_SPEED) != null) {
            var attackSpeed = p.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(attackSpeedValue);
            }
        }

        // 3. Sincronización de Scoreboard
        updateAllScoreboards(gm, modo);

        // 4. Notificación de actualización para administradores
        if (p.isOp() || p.hasPermission("uhc.admin")) {
            String latest = UpdateChecker.getLatestVersionFound();
            if (latest != null && !latest.equalsIgnoreCase(plugin.getPluginMeta().getVersion())) {
                p.sendMessage(Component.empty());
                p.sendMessage(legacySection().deserialize("&6[UHC] &a¡Hay una nueva versión disponible! (&e" + latest + "&6)"));
                p.sendMessage(legacySection().deserialize("&eDescárgala en: &bhttps://github.com/Dalibex/UHC_Plugin/releases"));
                p.sendMessage(Component.empty());
            }
        }
    }

    /**
     * Gestiona el ingreso del jugador cuando hay una partida activa.
     */
    private void handleInGameJoin(Player p, GameManager gm) {
        boolean eraParticipante = gm.getParticipantesIniciales().contains(p.getName());
        boolean estaEliminado = gm.getJugadoresEliminados().contains(p.getName());

        if (!eraParticipante || estaEliminado) {
            p.setGameMode(GameMode.SPECTATOR);
        } else {
            p.setGameMode(GameMode.SURVIVAL);
            // Restaurar identidad visual si no ha sido revelado
            if (!gm.getJugadoresRevelados().contains(p.getUniqueId())) {
                gm.actualizarIdentidadVisual(p);
            }
        }
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
        int crono = gm.getTiempoTotalSegundos();
        String timeStr = formatTime(crono);
        boolean active = crono > 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            modo.updateScoreboard(online, "...", timeStr, active);
        }
    }

    private String formatTime(int s) {
        int h = s / 3600; int m = (s % 3600) / 60; int sec = s % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }
}
