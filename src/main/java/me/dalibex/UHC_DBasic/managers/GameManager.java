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

import static org.bukkit.GameRule.*;

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
    private final Map<UUID, String> penultimaSkinAsignada = new HashMap<>();

    public GameManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.modoActual = new Classic(plugin, this);
        this.skinsApi = SkinsRestorerProvider.get();
    }

    public void iniciarPartida() {
        if (partidaTask != null) return;

        // Limpiar ítems de selector de equipo personalizados
        TeamManager tm = plugin.getTeamManager();
        tm.removeAllSelectorItems();

        this.partidaIniciada = true;
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.pausado = false;
        this.jugadoresEliminados.clear();

        this.modoActual.onReset();

        registrarParticipantes();

        // 1. Rotar identidades Sincrónicamente antes de empezar
        rotarSkins();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName(p.getName());
            p.damage(0.01);
            actualizarIdentidadVisual(p);

            if (p.getGameMode() == GameMode.SURVIVAL) {
                String nombreSkinNueva = ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName());
                String rawMsg = plugin.getLang().get("game-events.skins.identity-changed", p);
                String mensajePersonalizado = rawMsg.replace("%player%", nombreSkinNueva);
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
                plugin.getItemsListener().updateTrackingCompasses();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void fullReset() {
        detenerPartidaTask();
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 0;
        this.partidaIniciada = false;
        this.pausado = false;

        this.modoActual.onReset();
        this.jugadoresEliminados.clear();
        this.participantesIniciales.clear();

        // 1. Resetear Mundos
        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.setTime(0L);
            world.setThundering(false);
            world.setStorm(false);
            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(5999984);
            
            // Usando los nombres de GameRule de Bukkit (DO_DAYLIGHT_CYCLE, etc.)
            // que es lo que parece esperar el proyecto si usa GameRule (singular)
            world.setGameRule(DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(DO_WEATHER_CYCLE, false);
            world.setGameRule(NATURAL_REGENERATION, true);
            world.setGameRule(DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.PVP, false);
        }

        // 2. Resetear Jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyLobbySettings(p);
        }

        // 3. Resetear Managers
        TeamManager tm = plugin.getTeamManager();
        tm.borrarTodosLosEquipos();
        if (tm.isCustomTeamsEnabled()) {
            tm.initializeCustomTeams();
        }
        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (managerBoard.getObjective("uhc") != null) managerBoard.getObjective("uhc").unregister();
        if (managerBoard.getObjective("vida_tab") != null) managerBoard.getObjective("vida_tab").unregister();

        for (Team team : new HashSet<>(managerBoard.getTeams())) {
            if (team.getName().startsWith("h_")) team.unregister();
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
        p.setPlayerListName(p.getName());

        // Efectos de Lobby
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, false, false, false));
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false, false));
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 200, 255, false, false, false));

        // Teletransporte al centro del spawn si la partida no ha comenzado
        World world = p.getWorld();
        int y = world.getHighestBlockYAt(0, 0);
        Location spawnLoc = new Location(world, 0.5, Math.max(y, 60) + 1, 0.5);
        p.teleport(spawnLoc);
        
        revelarIdentidad(p);
        if (modoActual != null) {
            modoActual.updateScoreboard(p, "00:00", "00:00", false);
        }

        // Entrega de selector de equipo si está habilitado
        TeamManager tm = plugin.getTeamManager();
        if (tm.isCustomTeamsEnabled() && tm.getTeamSize() > 1) {
            tm.giveTeamSelectorItem(p);
        }
    }

    public void setStandBy() {
        fullReset();
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (String name : vivosNombres) {
                Player p = Bukkit.getPlayer(name);
                if (p == null || !p.isOnline()) continue;

                String nombreSkinElegida = ultimaSkinAsignada.get(p.getUniqueId());
                if (nombreSkinElegida == null) continue;

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

                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
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