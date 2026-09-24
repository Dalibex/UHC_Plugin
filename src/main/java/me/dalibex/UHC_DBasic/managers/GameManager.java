package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.AbstractUHCGameMode;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TimeUtil;
import net.kyori.adventure.text.Component;

public class GameManager {

    private final UHC_DBasic plugin;
    private UHCGameMode modoActual;

    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20 * 60;
    private int pvpEnabledEpisode = 4;

    private BukkitTask partidaTask;
    private GamePhase phase = GamePhase.INITIALIZING;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();
    private final List<BukkitTask> startupTasks = new ArrayList<>();
    private Set<UUID> eligibleRoster = Set.of();
    private final Map<UUID, String> eligibleRosterNames = new java.util.HashMap<>();
    private final Map<UUID, Location> plannedScatterLocations = new java.util.HashMap<>();
    private UUID startupOwner;
    private int pendingBorderSize;
    private long startupGeneration;

    @SuppressWarnings("this-escape")
    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
    }

    public void startGame() {
        if (partidaTask != null || phase != GamePhase.COUNTDOWN) return;

        // Limpiar ítems de selector de equipo personalizados
        TeamManager tm = plugin.getTeamManager();
        tm.removeAllSelectorItems();

        this.phase = GamePhase.RUNNING;
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.jugadoresEliminados.clear();

        this.modoActual.onReset();

        registerParticipants(eligibleRoster);

        // 1. Rotar identidades Sincrónicamente antes de empezar
        plugin.getSkinsManager().rotateSkins();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!eligibleRoster.contains(p.getUniqueId())) {
                p.setGameMode(GameMode.SPECTATOR);
                continue;
            }
            p.playerListName(Component.text(p.getName()));
            modoActual.updateScoreboard(p, "00:00", "00:00", true);
            p.damage(0.01);
            plugin.getSkinsManager().updateVisualIdentity(p);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        double maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                                ? p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                                : 20.0;
                        p.setHealth(Math.min(20.0, maxHealth));
                        p.setFoodLevel(20);
                        p.setSaturation(20f);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }

        partidaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (phase == GamePhase.PAUSED) return;

                cronometroSegundos++;
                tiempoTotalSegundos++;

                // DELEGACIÓN EVENTOS
                modoActual.onTick(cronometroSegundos, tiempoTotalSegundos);

                int restante = segundosPorCapitulo - (cronometroSegundos % segundosPorCapitulo);
                String fRestante = TimeUtil.formatClock(restante);
                String fTotal = TimeUtil.formatClock(tiempoTotalSegundos);

                // DELEGACIÓN SCOREBOARDS
                for (Player p : Bukkit.getOnlinePlayers()) {
                    modoActual.updateScoreboard(p, fRestante, fTotal, true);
                }

                // DELEGACIÓN VICTORIA
                modoActual.checkVictory();

                // Funcionamiento Brújula
                plugin.getItemsListener().updateTrackingCompasses();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void fullReset() {
        cancelStartup();
        this.phase = GamePhase.INITIALIZING;
        stopGameTask();
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 0;
        this.modoActual.onReset();
        this.jugadoresEliminados.clear();
        this.participantesIniciales.clear();

        // 1. Resetear Mundos
        plugin.getWorldManager().resetWorlds();

        // 2. Resetear estado de identidad ANTES de reiniciar jugadores: si se hace
        // al revés, revealIdentity (llamado desde applyLobbySettings) haría early-return
        // por los revelados de la partida anterior y no se re-aplicaría la skin real.
        plugin.getSkinsManager().reset();

        // 3. Resetear Jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyLobbySettings(p);
        }

// 4. Resetear Managers
        TeamManager tm = plugin.getTeamManager();
        tm.migrateLegacyTeamsOnce();
        // Conservar las membresías seleccionadas ANTES del reset para restaurarlas
        // justo después de recrear los equipos (los equipos custom los elige el
        // jugador y no deberían perderse con un /reset).
        Map<String, String> equiposPrevios = tm.isCustomTeamsEnabled() ? tm.snapshotTeamMembers() : Map.of();
        tm.deleteAllTeams();
        if (tm.isCustomTeamsEnabled()) {
            tm.initializeCustomTeams();
            tm.restoreTeamMembers(equiposPrevios);
        }
Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective uhcObjective = managerBoard.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
        if (uhcObjective != null) uhcObjective.unregister();
        Objective vidaTabObjective = managerBoard.getObjective(ScoreboardHelper.HEALTH_OBJECTIVE);
        if (vidaTabObjective != null) vidaTabObjective.unregister();

        this.phase = GamePhase.LOBBY;
    }

    public void applyLobbySettings(Player p) {
        p.clearActivePotionEffects();
        if (!p.getInventory().isEmpty()) p.getInventory().clear();
        
        // Forzar modo aventura para todos (incluyendo ex-espectadores)
        p.setGameMode(GameMode.ADVENTURE);
        
        double maxHealth = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                : 20.0;
        p.setHealth(Math.min(20.0, maxHealth));
        p.setFoodLevel(20);
        p.setExp(0);
        p.setLevel(0);
        p.playerListName(Component.text(p.getName()));

        // Efectos de Lobby
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, false, false, false));
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false, false));
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 200, 255, false, false, false));

        // Teletransporte al centro del spawn del mundo principal (overworld),
        // incluso si el jugador está en otra dimensión (nether/end)
        plugin.getWorldManager().teleportToSpawn(p);

        plugin.getSkinsManager().restoreOwnIdentity(p);
        if (modoActual != null) {
            modoActual.updateScoreboard(p, "00:00", "00:00", false);
        }

        // Entrega de selector de equipo si está habilitado
        TeamManager tm = plugin.getTeamManager();
        if (tm.isCustomTeamsEnabled() && tm.getTeamSize() > 1) {
            tm.giveTeamSelectorItem(p);
        }
    }

    public void stopGameTask() {
        if (this.partidaTask != null) {
            this.partidaTask.cancel();
            this.partidaTask = null;
        }
    }

    private void registerParticipants(Set<UUID> roster) {
        participantesIniciales.clear();
        for (UUID uuid : roster) {
            String name = eligibleRosterNames.get(uuid);
            if (name == null) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                name = player.getName();
            }
            if (name != null) participantesIniciales.add(name);
        }
    }

    public boolean requestStart(UUID owner, int borderSize) {
        if (phase != GamePhase.LOBBY || startupOwner != null) return false;
        startupOwner = owner;
        pendingBorderSize = borderSize;
        return true;
    }

    public boolean beginPreparation(UUID confirmer) {
        if (phase != GamePhase.LOBBY || startupOwner == null || !startupOwner.equals(confirmer)) return false;
        eligibleRoster = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .map(Player::getUniqueId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        eligibleRosterNames.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (eligibleRoster.contains(player.getUniqueId())) {
                eligibleRosterNames.put(player.getUniqueId(), player.getName());
            }
        }
        plannedScatterLocations.clear();
        phase = GamePhase.PREPARING;
        return true;
    }

    public boolean hasPendingStart() { return startupOwner != null && phase == GamePhase.LOBBY; }
    public UUID getStartupOwner() { return startupOwner; }
    public int getPendingBorderSize() { return pendingBorderSize; }
    public Set<UUID> getEligibleRoster() { return eligibleRoster; }
    public boolean isEligibleRosterMember(UUID uuid) { return eligibleRoster.contains(uuid); }
    public void setPlannedScatterLocation(UUID uuid, Location location) {
        plannedScatterLocations.put(uuid, location.clone());
    }
    public Location getPlannedScatterLocation(UUID uuid) {
        Location location = plannedScatterLocations.get(uuid);
        return location == null ? null : location.clone();
    }
    public World getStartupWorld() { return plugin.getWorldManager().getMainWorld(); }
    public long getStartupGeneration() { return startupGeneration; }
    public boolean isStartupGeneration(long generation) { return generation == startupGeneration; }
    public void trackStartupTask(BukkitTask task) { startupTasks.add(task); }

    public boolean enterCountdown(long generation) {
        if (!isStartupGeneration(generation) || phase != GamePhase.PREPARING) return false;
        phase = GamePhase.COUNTDOWN;
        return true;
    }

    public void clearCompletedStartup() {
        startupTasks.clear();
        startupOwner = null;
        pendingBorderSize = 0;
    }

    public void cancelStartup() {
        startupGeneration++;
        for (BukkitTask task : startupTasks) task.cancel();
        startupTasks.clear();
        startupOwner = null;
        pendingBorderSize = 0;
        eligibleRoster = Set.of();
        eligibleRosterNames.clear();
        plannedScatterLocations.clear();
        if (phase == GamePhase.PREPARING || phase == GamePhase.COUNTDOWN) phase = GamePhase.LOBBY;
    }

    public void enterEnding() {
        cancelStartup();
        stopGameTask();
        phase = GamePhase.ENDING;
    }

    /**
     * Único punto de mutación externa de la lista de eliminados.
     */
    public void eliminatePlayer(String nombre) {
        jugadoresEliminados.add(nombre);
    }

    // --- GETTERS Y SETTERS ---
    public int getChapter() { return capitulo; }

    public void setChapter(int capitulo) { this.capitulo = capitulo; }

    public int getTotalSeconds() { return tiempoTotalSegundos; }

    public int getSecondsPerChapter() { return segundosPorCapitulo; }

    public void setSecondsPerChapter(int s) { this.segundosPorCapitulo = s; }

    /** Episodio (parte) en el que termina el pacto de caballeros y se activa PVP. */
    public int getPvpEnabledEpisode() { return pvpEnabledEpisode; }

    public void setPvpEnabledEpisode(int episode) { this.pvpEnabledEpisode = episode; }

    public GamePhase getPhase() { return phase; }

    public Set<String> getEliminatedPlayers() { return Collections.unmodifiableSet(jugadoresEliminados); }

    public List<String> getInitialParticipants() { return Collections.unmodifiableList(participantesIniciales); }

    public boolean isPaused() { return phase == GamePhase.PAUSED; }

    public void setPaused(boolean pausado) {
        if (pausado && phase == GamePhase.RUNNING) {
            this.phase = GamePhase.PAUSED;
        } else if (!pausado && phase == GamePhase.PAUSED) {
            this.phase = GamePhase.RUNNING;
        }
    }

    public boolean isGameStarted() {
        return phase == GamePhase.PREPARING || phase == GamePhase.COUNTDOWN
                || phase == GamePhase.RUNNING || phase == GamePhase.PAUSED || phase == GamePhase.ENDING;
    }

    /** Partida con gameplay activo; excluye preparación y cierre. */
    public boolean isMatchActive() {
        return phase == GamePhase.RUNNING || phase == GamePhase.PAUSED;
    }

    public void setGameStarted(boolean estado) {
        if (estado) {
            throw new IllegalStateException("Use the explicit startup transitions");
        }
        // Compatibility for existing game modes; new finish paths should call enterEnding().
        enterEnding();
    }

    public void changeMode(UHCGameMode nuevoModo) {
        // Transferir la caché de claves de sidebar del modo anterior para que la
        // nueva instancia pueda limpiar las líneas obsoletas del scoreboard.
        Map<UUID, Set<String>> transfer = null;
        if (modoActual instanceof AbstractUHCGameMode anterior) {
            transfer = anterior.takeSidebarKeys();
        }

        this.modoActual = nuevoModo;
        this.modoActual.onReset();

        if (transfer != null && modoActual instanceof AbstractUHCGameMode nuevo) {
            nuevo.adoptSidebarKeys(transfer);
        }

        if (!isGameStarted()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                modoActual.updateScoreboard(p, "00:00", "00:00", false);
            }
        }
    }

    public UHCGameMode getCurrentMode() { return modoActual; }
}
