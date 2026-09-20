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

    private BukkitTask partidaTask;
    private GamePhase phase = GamePhase.LOBBY;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();

    @SuppressWarnings("this-escape")
    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
    }

    public void startGame() {
        if (partidaTask != null) return;

        // Limpiar ítems de selector de equipo personalizados
        TeamManager tm = plugin.getTeamManager();
        tm.removeAllSelectorItems();

        this.phase = GamePhase.RUNNING;
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.jugadoresEliminados.clear();

        this.modoActual.onReset();

        registerParticipants();

        // 1. Rotar identidades Sincrónicamente antes de empezar
        plugin.getSkinsManager().rotateSkins();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playerListName(Component.text(p.getName()));
            p.damage(0.01);
            plugin.getSkinsManager().updateVisualIdentity(p);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        p.setHealth(20.0);
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
        stopGameTask();
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 0;
        this.phase = GamePhase.LOBBY;

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
        tm.deleteAllTeams();
        if (tm.isCustomTeamsEnabled()) {
            tm.initializeCustomTeams();
        }
        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective uhcObjective = managerBoard.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
        if (uhcObjective != null) uhcObjective.unregister();
        Objective vidaTabObjective = managerBoard.getObjective(ScoreboardHelper.HEALTH_OBJECTIVE);
        if (vidaTabObjective != null) vidaTabObjective.unregister();

        for (Team team : new HashSet<>(managerBoard.getTeams())) {
            if (team.getName().startsWith(ScoreboardHelper.TEAM_PREFIX)) team.unregister();
        }
    }

    public void applyLobbySettings(Player p) {
        p.clearActivePotionEffects();
        if (!p.getInventory().isEmpty()) p.getInventory().clear();
        
        // Forzar modo aventura para todos (incluyendo ex-espectadores)
        p.setGameMode(GameMode.ADVENTURE);
        
        p.setHealth(20.0);
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

        plugin.getSkinsManager().revealIdentity(p);
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

    public void registerParticipants() {
        participantesIniciales.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                participantesIniciales.add(p.getName());
            }
        }
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

    public GamePhase getPhase() { return phase; }

    public void setPhase(GamePhase nueva) { this.phase = nueva; }

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
        return phase == GamePhase.RUNNING || phase == GamePhase.PAUSED;
    }

    public void setGameStarted(boolean estado) {
        this.phase = estado ? GamePhase.RUNNING : GamePhase.LOBBY;
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