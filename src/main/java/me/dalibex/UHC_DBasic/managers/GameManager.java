package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.storage.PlayerStorage;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    private final UHC_DBasic plugin;
    private UHCGameMode modoActual;
    private final SkinsRestorer skinsApi;

    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20 * 60;

    private BukkitTask partidaTask;
    private boolean partidaIniciada = false;
    private boolean pausado = false;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();

    private final Set<UUID> jugadoresRevelados = new HashSet<>();
    private final Map<UUID, String> ultimaSkinAsignada = new HashMap<>();

    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
        this.skinsApi = SkinsRestorerProvider.get();
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
            actualizarIdentidadVisual(p);
            rotarSkins();

            if (p.getGameMode() == GameMode.SURVIVAL) {
                String nombreSkinNueva = getUltimaSkinAsignada()
                        .getOrDefault(p.getUniqueId(), "???");
                String rawMsg = plugin.getLang()
                        .get("game-events.skins.identity-changed", p);
                String mensajePersonalizado =
                        rawMsg.replace("%player%", nombreSkinNueva);
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', mensajePersonalizado));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            }

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

        limpiarEquiposScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            revelarIdentidad(p);
        }

        // Lógica de Lobby (Teletransporte al centro)
        World world = Bukkit.getWorlds().get(0);
        int y = world.getHighestBlockYAt(0, 0);
        Location spawnLoc = new Location(world, 0.5, (y < 60 ? 100 : y + 1), 0.5);

        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (managerBoard.getObjective("uhc") != null) managerBoard.getObjective("uhc").unregister();
        if (managerBoard.getObjective("vida_tab") != null) managerBoard.getObjective("vida_tab").unregister();

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

    public void limpiarEquiposScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : board.getTeams()) {
            team.unregister();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            p.setPlayerListName(p.getName());
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

    // -------------------- LOGICA PARA SKINS / IDENTIDAD --------------------
    public void rotarSkins() {
        List<Player> vivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .collect(Collectors.toList());

        if (vivos.size() < 2) return;
        List<String> poolNombres = vivos.stream().map(Player::getName).collect(Collectors.toList());
        jugadoresRevelados.clear();

        boolean asignacionValida = false;
        int intentos = 0;
        while (!asignacionValida && intentos < 10) {
            Collections.shuffle(poolNombres);
            asignacionValida = true;
            for (int i = 0; i < vivos.size(); i++) {
                String skinAsignada = poolNombres.get(i);
                String skinAnterior = ultimaSkinAsignada.get(vivos.get(i).getUniqueId());
                if (skinAsignada.equals(skinAnterior)) {
                    asignacionValida = false;
                    break;
                }
            }
            intentos++;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int i = 0; i < vivos.size(); i++) {
                Player p = vivos.get(i);
                String nombreSkinElegida = poolNombres.get(i);
                ultimaSkinAsignada.put(p.getUniqueId(), nombreSkinElegida);

                try {
                    SkinStorage skinStorage = skinsApi.getSkinStorage();
                    PlayerStorage playerStorage = skinsApi.getPlayerStorage();
                    Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(nombreSkinElegida);

                    if (result.isPresent()) {
                        playerStorage.setSkinIdOfPlayer(p.getUniqueId(), result.get().getIdentifier());
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            try {
                                skinsApi.getSkinApplier(Player.class).applySkin(p);
                            } catch (DataRequestException e) {
                                plugin.getLogger().warning("Error aplicando skin: " + e.getMessage());
                            }
                            actualizarIdentidadVisual(p);
                        });
                    }
                } catch (DataRequestException | MineSkinException e) {
                    plugin.getLogger().warning("Error al rotar skin para " + p.getName() + ": " + e.getMessage());
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void revelarIdentidad(Player p) {
        if (jugadoresRevelados.contains(p.getUniqueId())) return;

        jugadoresRevelados.add(p.getUniqueId());

        actualizarIdentidadVisual(p);

        try {
            PlayerStorage playerStorage = skinsApi.getPlayerStorage();
            SkinStorage skinStorage = skinsApi.getSkinStorage();
            Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(p.getName());
            if (result.isPresent()) {
                playerStorage.setSkinIdOfPlayer(p.getUniqueId(), result.get().getIdentifier());
                skinsApi.getSkinApplier(Player.class).applySkin(p);
            }
        } catch (DataRequestException | MineSkinException e) {
            plugin.getLogger().warning("Error al revelar identidad de " + p.getName() + ": " + e.getMessage());
        }
    }

    public void actualizarIdentidadVisual(Player p) {
        if (p == null) return;

        TabAPI tabApi = TabAPI.getInstance();
        me.neznamy.tab.api.TabPlayer tabPlayer = tabApi.getPlayer(p.getUniqueId());
        if (tabPlayer == null) return;

        TabListFormatManager tfm = tabApi.getTabListFormatManager();
        NameTagManager ntm = tabApi.getNameTagManager();

        if (partidaIniciada) {
            if (tfm != null) {
                tfm.setName(tabPlayer, "%rel_uhc_identidad%");
                ntm.setPrefix(tabPlayer, "%%rel_nametag_color%");
            }
        }
        if (partidaIniciada) {
            if (jugadoresRevelados.contains(p.getUniqueId())) {
                String nombreRojo = "§c" + p.getName();
                p.setDisplayName(nombreRojo);
            } else {
                String nombreFalso = ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName());
                p.setDisplayName("§c" + nombreFalso);
            }
        } else {
            p.setDisplayName("§f" + p.getName());
        }
    }
    // -----------------------------------------------------------------------

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

    public Set<UUID> getJugadoresRevelados() {
        return jugadoresRevelados;
    }

    public Map<UUID, String> getUltimaSkinAsignada() {
        return ultimaSkinAsignada;
    }

    public void cambiarModo(UHCGameMode nuevoModo) {
        this.modoActual = nuevoModo;
        this.modoActual.onReset();

        if (!partidaIniciada) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                modoActual.updateScoreboard(p, "00:00", "00:00", false);
            }
        }
    }

    public UHCGameMode getModoActual() { return modoActual; }
}