package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameManager {

    private final UHC_DBasic plugin;
    private UHCGameMode modoActual;

    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20 * 60;

    private BukkitTask partidaTask;
    private boolean partidaIniciada = false;
    private boolean pausado = false;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();

    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
    }

    public void iniciarPartida() {
        if (partidaTask != null) return;

        this.partidaIniciada = true;
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.pausado = false;
        this.jugadoresEliminados.clear();

        this.modoActual.onReset();

        registrarParticipantes();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName(p.getName());
            p.damage(0.01);

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
                if (pausado) return;

                cronometroSegundos++;
                tiempoTotalSegundos++;

                // DELEGACIÓN EVENTOS
                modoActual.onTick(cronometroSegundos, tiempoTotalSegundos);

                int restante = segundosPorCapitulo - (cronometroSegundos % segundosPorCapitulo);
                String fRestante = formatTime(restante);
                String fTotal = formatTime(tiempoTotalSegundos);

                // DELEGACIÓN SCOREBOARDS
                for (Player p : Bukkit.getOnlinePlayers()) {
                    modoActual.updateScoreboard(p, fRestante, fTotal, true);
                }

                // DELEGACIÓN VICTORIA
                modoActual.checkVictory();

                // Funcionamiento Brújula
                plugin.getEventHandler().onCompassTrack();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void setStandBy() {
        detenerPartidaTask();
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.partidaIniciada = false;
        this.pausado = false;

        this.modoActual.onReset();
        this.jugadoresEliminados.clear();
        this.participantesIniciales.clear();

        // Lógica de Lobby (Teletransporte al centro)
        World world = Bukkit.getWorlds().get(0);
        int y = world.getHighestBlockYAt(0, 0);
        Location spawnLoc = new Location(world, 0.5, (y < 60 ? 100 : y + 1), 0.5);

        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (managerBoard.getObjective("uhc") != null) managerBoard.getObjective("uhc").unregister();
        if (managerBoard.getObjective("vida_tab") != null) managerBoard.getObjective("vida_tab").unregister();

        // Limpieza de Nametags temporales
        for (Team team : new HashSet<>(managerBoard.getTeams())) {
            if (team.getName().startsWith("h_")) team.unregister();
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(spawnLoc);
            p.setPlayerListName(p.getName());
            modoActual.updateScoreboard(p, "00:00", "00:00", false);
        }
    }

    public void detenerPartidaTask() {
        if (this.partidaTask != null) {
            this.partidaTask.cancel();
            this.partidaTask = null;
        }
    }

    public void registrarParticipantes() {
        participantesIniciales.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                participantesIniciales.add(p.getName());
            }
        }
    }

    private String formatTime(int s) {
        int h = s / 3600; int m = (s % 3600) / 60; int sec = s % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    // --- GETTERS Y SETTERS ---
    public int getCapitulo() { return capitulo; }
    public void setCapitulo(int capitulo) { this.capitulo = capitulo; }
    public int getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public int getSegundosPorCapitulo() { return segundosPorCapitulo; }
    public void setSegundosPorCapitulo(int s) { this.segundosPorCapitulo = s; }
    public Set<String> getJugadoresEliminados() { return jugadoresEliminados; }
    public List<String> getParticipantesIniciales() { return participantesIniciales; }
    public boolean isPausado() { return pausado; }
    public void setPausado(boolean pausado) { this.pausado = pausado; }
    public boolean isPartidaIniciada() { return partidaIniciada; }
    public void setPartidaIniciada(boolean estado) {
        this.partidaIniciada = estado;
    }
    public void setModoActual(UHCGameMode modo) { this.modoActual = modo; }
    public UHCGameMode getModoActual() { return modoActual; }
}