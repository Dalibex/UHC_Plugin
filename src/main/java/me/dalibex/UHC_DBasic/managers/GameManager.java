package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import static org.bukkit.GameRules.ADVANCE_TIME;
import static org.bukkit.GameRules.ADVANCE_WEATHER;
import static org.bukkit.GameRules.NATURAL_HEALTH_REGENERATION;
import static org.bukkit.GameRules.PVP;
import static org.bukkit.GameRules.SPAWN_MONSTERS;
import org.bukkit.Location;
import org.bukkit.Sound;
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
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;

public class GameManager {

    private final UHC_DBasic plugin;
    private UHCGameMode modoActual;
    private final SkinsRestorer skinsApi;

    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20 * 60;

    private BukkitTask partidaTask;
    private GamePhase phase = GamePhase.LOBBY;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();

    private final Set<UUID> jugadoresRevelados = new HashSet<>();
    private final Map<UUID, String> ultimaSkinAsignada = new HashMap<>();
    private final Map<UUID, String> penultimaSkinAsignada = new HashMap<>();

    @SuppressWarnings("this-escape")
    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
        this.skinsApi = SkinsRestorerProvider.get();
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
        rotateSkins();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playerListName(Component.text(p.getName()));
            p.damage(0.01);
            updateVisualIdentity(p);

            if (p.getGameMode() == GameMode.SURVIVAL) {
                String nombreSkinNueva = ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName());
                String mensajePersonalizado = plugin.getLang().get("game-events.skins.identity-changed", p).replace("%player%", nombreSkinNueva);
                p.sendMessage(mensajePersonalizado);
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
        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.setGameRule(ADVANCE_TIME, true);
            world.setGameRule(ADVANCE_WEATHER, true);
            world.setThundering(false);
            world.setStorm(false);
            world.setGameRule(ADVANCE_WEATHER, false);
            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(5999984);

            world.setGameRule(NATURAL_HEALTH_REGENERATION, true);
            world.setGameRule(SPAWN_MONSTERS, false);
            world.setGameRule(PVP, false);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL || world.getEnvironment() == World.Environment.THE_END) {
                    world.setTime(0L);
                }
                world.setGameRule(ADVANCE_TIME, false);
            }
        });

        // 2. Resetear Jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyLobbySettings(p);
        }

        // 3. Resetear Managers
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

    private World getMainWorld() {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().get(0));
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
        World world = getMainWorld();
        int y = world.getHighestBlockYAt(0, 0);
        Location spawnLoc = new Location(world, 0.5, Math.max(y, 60) + 1, 0.5);
        p.teleport(spawnLoc);
        
        revealIdentity(p);
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

    // -------------------- LOGICA PARA SKINS / IDENTIDAD --------------------
    public void rotateSkins() {
        // Obtenemos todos los jugadores que siguen "vivos" internamente
        List<String> vivosNombres = participantesIniciales.stream()
                .filter(name -> !jugadoresEliminados.contains(name))
                .collect(Collectors.toList());

        if (vivosNombres.size() < 2) return;
        
        List<String> poolNombres = new ArrayList<>(vivosNombres);
        jugadoresRevelados.clear();

        boolean asignacionValida = false;
        int intentos = 0;
        
        while (!asignacionValida && intentos < 30) {
            Collections.shuffle(poolNombres);
            asignacionValida = true;
            
            for (int i = 0; i < vivosNombres.size(); i++) {
                String skinAsignada = poolNombres.get(i);
                UUID uuid = Bukkit.getOfflinePlayer(vivosNombres.get(i)).getUniqueId();
                
                // Evitar su propia skin, la última y la penúltima skin si es posible
                if (skinAsignada.equalsIgnoreCase(vivosNombres.get(i)) ||
                    skinAsignada.equalsIgnoreCase(ultimaSkinAsignada.get(uuid)) || 
                    skinAsignada.equalsIgnoreCase(penultimaSkinAsignada.get(uuid))) {
                    asignacionValida = false;
                    break;
                }
            }
            intentos++;
        }

        // Si después de 30 intentos no hay suerte (ej: muy pocos jugadores para tantas reglas),
        // hacemos un último esfuerzo para al menos evitar la propia skin.
        if (!asignacionValida) {
            for (int k = 0; k < 100; k++) {
                Collections.shuffle(poolNombres);
                boolean noPropia = true;
                for (int i = 0; i < vivosNombres.size(); i++) {
                    if (poolNombres.get(i).equalsIgnoreCase(vivosNombres.get(i))) {
                        noPropia = false;
                        break;
                    }
                }
                if (noPropia) break;
            }
        }

        // Guardar historial y asignar nombres inmediatamente (Sincrónico)
        for (int i = 0; i < vivosNombres.size(); i++) {
            UUID uuid = Bukkit.getOfflinePlayer(vivosNombres.get(i)).getUniqueId();
            penultimaSkinAsignada.put(uuid, ultimaSkinAsignada.get(uuid));
            ultimaSkinAsignada.put(uuid, poolNombres.get(i));
        }

        // Aplicar skins asíncronamente solo a los que están online
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= vivosNombres.size()) {
                    cancel();
                    return;
                }

                String name = vivosNombres.get(index++);
                Player p = Bukkit.getPlayer(name);
                if (p == null || !p.isOnline()) return;

                String nombreSkinElegida = ultimaSkinAsignada.get(p.getUniqueId());
                if (nombreSkinElegida == null) return;

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
                                plugin.getLogger().warning(() -> "Error aplicando skin: " + e.getMessage());
                            }
                            updateVisualIdentity(p);
                        });
                    }
                } catch (DataRequestException | MineSkinException e) {
                    plugin.getLogger().warning(() -> "Error al rotar skin para " + p.getName() + ": " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 4L);
    }


    public void revealIdentity(Player p) {
        if (jugadoresRevelados.contains(p.getUniqueId())) return;

        jugadoresRevelados.add(p.getUniqueId());

        updateVisualIdentity(p);

        // La búsqueda de skin puede hacer una petición de red (MineSkin) con
        // bloques: se hace fuera del hilo principal y se aplica en el principal.
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerStorage playerStorage = skinsApi.getPlayerStorage();
                SkinStorage skinStorage = skinsApi.getSkinStorage();
                Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(name);
                if (result.isPresent()) {
                    playerStorage.setSkinIdOfPlayer(uuid, result.get().getIdentifier());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            skinsApi.getSkinApplier(Player.class).applySkin(p);
                        } catch (DataRequestException e) {
                            plugin.getLogger().warning(() -> "Error aplicando skin: " + e.getMessage());
                        }
                    });
                }
            } catch (DataRequestException | MineSkinException e) {
                plugin.getLogger().warning(() -> "Error al revelar identidad de " + name + ": " + e.getMessage());
            }
        });
    }

    public void updateVisualIdentity(Player p) {
        if (p == null) return;

        TabAPI tabApi = TabAPI.getInstance();
        me.neznamy.tab.api.TabPlayer tabPlayer = tabApi.getPlayer(p.getUniqueId());
        if (tabPlayer == null) return;

        TabListFormatManager tfm = tabApi.getTabListFormatManager();
        NameTagManager ntm = tabApi.getNameTagManager();

        if (phase != GamePhase.LOBBY && phase != GamePhase.ENDING) {
            if (tfm != null) {
                tfm.setName(tabPlayer, "%rel_uhc_identidad%");
            }
            if (ntm != null) {
                ntm.setPrefix(tabPlayer, "%%rel_nametag_color%");
            }
        }
        if (phase != GamePhase.LOBBY && phase != GamePhase.ENDING) {
            if (jugadoresRevelados.contains(p.getUniqueId())) {
                p.displayName(Component.text(p.getName(), NamedTextColor.RED));
            } else {
                String nombreFalso = ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName());
                p.displayName(Component.text(nombreFalso, NamedTextColor.RED));
            }
        } else {
            p.displayName(Component.text(p.getName()));
        }
    }
    // -----------------------------------------------------------------------

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

    public Set<UUID> getRevealedPlayers() {
        return jugadoresRevelados;
    }

    public Map<UUID, String> getLastAssignedSkin() {
        return ultimaSkinAsignada;
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