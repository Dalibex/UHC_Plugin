package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Responsable de la identidad visual de los jugadores: rotación de skins
 * (escalonada, respetando combate), revelación de identidad, re-sincronización
 * al reconectar y la cabeza de muerte con la skin REAL del fallecido.
 *
 * <p>La identidad se indexa internamente por NOMBRE del jugador en minúsculas
 * (clave canónica del plugin, igual que participantes/eliminados de
 * {@link GameManager}): {@link #jugadoresRevelados}, {@link #ultimaSkinAsignada}
 * y {@link #combatTags} usan names en minúsculas.
 */
public class SkinsManager {

    /** Ventana de combate durante la cual no se cambia la skin a un jugador. */
    private static final long COMBAT_WINDOW_MS = 30_000L;
    /** Intervalo entre jugador y jugador en la rotación (5 segundos). */
    private static final long ROTATION_INTERVAL_TICKS = 100L;
    /** Retardo inicial de la rotación tras empezar (10 segundos). */
    private static final long ROTATION_START_TICKS = 200L;
    /** Reintentos máximos de un jugador en combate antes de saltarlo en la rotación. */
    private static final int COMBATE_REINTENTOS_MAX = 6;
    /** Intentos máximos de barajado cumpliendo todas las reglas anti-repetición. */
    private static final int SHUFFLE_INTENTOS = 30;
    /** Intentos máximos del fallback solo no-propia. */
    private static final int SHUFFLE_FALLBACK_INTENTOS = 100;

    private final UHC_DBasic plugin;
    private final GameManager gm;
    private final SkinsRestorer skinsApi;

    /** Jugadores revelados (su identidad real ya se mostró), por nombre en minúsculas. */
    private final Set<String> jugadoresRevelados = new HashSet<>();
    /** Skin falsa asignada por jugador (nombre en minúsculas → nombre de la skin). */
    private final Map<String, String> ultimaSkinAsignada = new HashMap<>();
    /** Timestamp del último golpe por jugador (nombre en minúsculas → ms). */
    private final Map<String, Long> combatTags = new HashMap<>();

    /** Caché de la skin REAL de cada jugador por nombre (minúsculas). Se
     *  alimenta al resolver skins durante la rotación y es la fuente fiable
     *  para restaurar la skin propia en el reset. Persiste entre partidas. */
    private final Map<String, SkinProperty> skinRealPorNombre = new HashMap<>();

    /** Tarea de rotación activa (una por partida/fase); se cancela al resetear o al rotar de nuevo. */
    private BukkitTask rotacionTask;
    /** Generación de identidad: invalida aplicaciones asíncronas pendientes de partidas/rotaciones previas. */
    private volatile long generacion = 0;

    public SkinsManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
        this.skinsApi = SkinsRestorerProvider.get();
    }

    // ------------------------------------------------------------------
    // Lógica pura (testeable sin servidor)
    // ------------------------------------------------------------------

    /** Clave normalizada de un nombre de jugador. */
    static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * Comprueba si el último golpe del jugador está dentro de la ventana de combate.
     * Pura: no usa Bukkit.
     */
    static boolean isCombatActive(Long since, long now, long windowMs) {
        return since != null && now - since < windowMs;
    }

    /**
     * Baraja las identidades de la partida y devuelve la asginación
     * (nombre minúsculas → nombre de skin, con la capitalización original).
     *
     * <p>Pura (sin Bukkit). Reglas anti-repetición: ni la propia skin ni la que el
     * jugador llevaba en la rotación anterior (en {@code ultimaPorNombre}). Con
     * menos de 2 vivos devuelve un mapa vacío.
     */
    static Map<String, String> assignNewSkins(List<String> vivos, Map<String, String> ultimaPorNombre) {
        return assignNewSkins(vivos, ultimaPorNombre, new Random());
    }

    static Map<String, String> assignNewSkins(List<String> vivos, Map<String, String> ultimaPorNombre,
            Random random) {
        Map<String, String> asignacion = new HashMap<>();
        if (vivos.size() < 2) return asignacion;

        List<String> asignados = new ArrayList<>(vivos);
        boolean asignacionValida = false;
        int intentos = 0;
        while (!asignacionValida && intentos < SHUFFLE_INTENTOS) {
            Collections.shuffle(asignados, random);
            asignacionValida = true;
            for (int i = 0; i < vivos.size(); i++) {
                String skinAsignada = asignados.get(i);
                if (skinAsignada.equalsIgnoreCase(vivos.get(i)) ||
                        skinAsignada.equalsIgnoreCase(ultimaPorNombre.get(key(vivos.get(i))))) {
                    asignacionValida = false;
                    break;
                }
            }
            intentos++;
        }

        // Esfuerzo final: aunque cumpla las reglas, al menos evitar la propia skin.
        if (!asignacionValida) {
            for (int k = 0; k < SHUFFLE_FALLBACK_INTENTOS && !asignacionValida; k++) {
                Collections.shuffle(asignados, random);
                asignacionValida = true;
                for (int i = 0; i < vivos.size(); i++) {
                    if (asignados.get(i).equalsIgnoreCase(vivos.get(i))) {
                        asignacionValida = false;
                        break;
                    }
                }
            }
        }

        // Garantía determinista: nadie acaba con su propia skin, sea cual sea el
        // resultado de los barajados anteriores.
        repairSelfAssignments(asignados, vivos);

        for (int i = 0; i < vivos.size(); i++) {
            asignacion.put(key(vivos.get(i)), asignados.get(i));
        }
        return asignacion;
    }

    /**
     * Repara una asignación (permutación de los vivos) para que ningún jugador
     * acabe con su propia skin, intercambiando skins. Pura y determinista.
     */
    static void repairSelfAssignments(List<String> asignados, List<String> vivos) {
        List<Integer> conPropia = new ArrayList<>();
        for (int i = 0; i < vivos.size(); i++) {
            if (asignados.get(i).equalsIgnoreCase(vivos.get(i))) conPropia.add(i);
        }
        if (conPropia.isEmpty()) return;

        // Emparejar los que llevan su propia skin: intercambiar sus skins entre sí.
        for (int j = 0; j + 1 < conPropia.size(); j += 2) {
            Collections.swap(asignados, conPropia.get(j), conPropia.get(j + 1));
        }

        // El último sin pareja lo intercambia con otro jugador que no le devuelva
        // su propia skin (con n >= 2 siempre existe: esa skin solo la lleva él).
        if (conPropia.size() % 2 == 1) {
            int solo = conPropia.get(conPropia.size() - 1);
            for (int i = 0; i < vivos.size(); i++) {
                if (i != solo && !asignados.get(i).equalsIgnoreCase(vivos.get(solo))) {
                    Collections.swap(asignados, solo, i);
                    break;
                }
            }
        }
    }

    public static boolean isRotationEpisode(int episode) {
        return episode >= 2 && episode <= 10;
    }

    /**
     * Procesa un tick de rotación para el siguiente jugador de la cola y
     * devuelve el nombre cuya skin hay que aplicar, o {@code null} si este tick
     * no hay nada que aplicar.
     *
     * <p>Pura (sin Bukkit): las comprobaciones de conexión/combate entran como
     * predicados. Muta {@code cola} y {@code reintentos}:
     * desconectado → descartado; ya revelado → descartado; en combate →
     * reintentado al final de la cola (máx. {@link #COMBATE_REINTENTOS_MAX}
     * intentos, luego saltado).
     */
    static String nextNameToSkin(ArrayDeque<String> cola, Map<String, Integer> reintentos,
            Set<String> revelados, Predicate<String> isOnline, Predicate<String> isInCombat) {
        String name = cola.poll();
        if (name == null) return null;
        if (!isOnline.test(name)) return null;              // desconectado: se descarta
        if (revelados.contains(key(name))) return null;     // ya revelado: no volver a camuflar
        if (isInCombat.test(name)) {
            int intento = reintentos.merge(key(name), 1, Integer::sum);
            if (intento < COMBATE_REINTENTOS_MAX) {
                cola.addLast(name); // reintentar más tarde sin tocar su skin
            }
            return null;
        }
        reintentos.remove(key(name));
        return name;
    }

    // ------------------------------------------------------------------
    // Rotación
    // ------------------------------------------------------------------

    /**
     * Marca a un jugador como recién golpeado. Mientras esté dentro de la
     * ventana {@link #COMBAT_WINDOW_MS}, la rotación respetará su skin actual.
     */
    public void markInCombat(Player p) {
        if (p == null) return;
        combatTags.put(key(p.getName()), System.currentTimeMillis());
    }

    private boolean isInCombat(String name) {
        return isCombatActive(combatTags.get(key(name)), System.currentTimeMillis(), COMBAT_WINDOW_MS);
    }

    /**
     * Baraja las identidades de la partida actual y las aplica de forma
     * escalonada: un jugador cada {@link #ROTATION_INTERVAL_TICKS}.
     *
     * <p>Un jugador en combate se reintenta (máx. 30 s, el mismo tamaño que la
     * ventana de combate) y, si sigue ocupado, se salta para que la cola avance.
     * Un jugador desconectado se descarta: al reconectar se re-sincroniza su
     * skin (reapplyCurrentSkin / onPlayerJoin).
     */
    public void rotateSkins() {
        cancelarRotacion();
        generacion++;

        // Una revelación es global durante el episodio actual. Cada rotación
        // empieza un episodio nuevo y vuelve a enmascarar todas las identidades.
        jugadoresRevelados.clear();

        List<String> vivosNombres = gm.getInitialParticipants().stream()
                .filter(name -> !gm.getEliminatedPlayers().contains(name))
                .collect(Collectors.toList());

        if (vivosNombres.size() < 2) return;

        Map<String, String> nuevaAsignacion = assignNewSkins(vivosNombres, ultimaSkinAsignada);
        if (nuevaAsignacion.isEmpty()) return;
        ultimaSkinAsignada.clear();
        ultimaSkinAsignada.putAll(nuevaAsignacion);

        // Aplicar escalonado: emite a los 10 segundos y luego 1 jugador cada
        // 5 segundos (ver nextNameToSkin para el detalle de combate/offline).
        ArrayDeque<String> cola = new ArrayDeque<>(vivosNombres);
        Map<String, Integer> reintentos = new HashMap<>();
        rotacionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (cola.isEmpty() || rotacionTask == null) {
                    cancel();
                    return;
                }
                // Si la partida terminó o se reseteó, la rotación ya no tiene
                // sentido: se cancela en vez de seguir reapicando fakes.
                if (gm.getPhase() != GamePhase.RUNNING && gm.getPhase() != GamePhase.PAUSED) {
                    cancel();
                    return;
                }
                String nombre = nextNameToSkin(cola, reintentos, jugadoresRevelados,
                        n -> {
                            Player p = Bukkit.getPlayer(n);
                            return p != null && p.isOnline();
                        },
                        SkinsManager.this::isInCombat);
                if (nombre == null) return;
                String nombreSkinElegida = ultimaSkinAsignada.get(key(nombre));
                if (nombreSkinElegida == null) return;
                applySkinByNameAsync(Bukkit.getPlayer(nombre), nombreSkinElegida, true);
            }
        }.runTaskTimer(plugin, ROTATION_START_TICKS, ROTATION_INTERVAL_TICKS);
    }

    /**
     * Aplica una skin por nombre a un jugador (búsqueda asíncrona, posible
     * red). Actualiza el nombre visual y, si {@code notify}, avisa al jugador.
     */
    private void applySkinByNameAsync(Player p, String nombreSkin, boolean notify) {
        UUID playerId = p.getUniqueId();
        String playerName = p.getName();
        boolean esPropia = nombreSkin.equalsIgnoreCase(playerName);
        long generacionSolicitada = generacion;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkinStorage skinStorage = skinsApi.getSkinStorage();
                Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(nombreSkin);
                if (!result.isPresent()) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (generacionSolicitada != generacion) return;
                    Player current = Bukkit.getPlayer(playerId);
                    if (current == null || !current.isOnline() || !current.getName().equals(playerName)) return;
                    // Nunca recamuflejar a un jugador ya revelado con una skin ajena.
                    if (!esPropia && jugadoresRevelados.contains(key(playerName))) return;
                    // Tanto la caché local como el ID persistente se mutan solo
                    // en main y únicamente para la generación que los solicitó.
                    skinRealPorNombre.put(key(nombreSkin), result.get().getProperty());
                    PlayerStorage playerStorage = skinsApi.getPlayerStorage();
                    playerStorage.setSkinIdOfPlayer(playerId, result.get().getIdentifier());
                    try {
                        skinsApi.getSkinApplier(Player.class).applySkin(current);
                    } catch (DataRequestException e) {
                        plugin.getLogger().warning(() -> "Error aplicando skin: " + e.getMessage());
                    }
                    updateVisualIdentity(current);
                    if (notify) notifyIdentityChanged(current);
                });
            } catch (DataRequestException | MineSkinException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger()
                        .warning(() -> "Error al aplicar skin a " + playerName + ": " + e.getMessage()));
            }
        });
    }

    /**
     * Re-aplica la skin correcta a un jugador que reconecta: la falsa asignada
     * si aún no fue revelado, o su propia skin si ya lo fue. Sin mensaje: debe
     * ser transparente.
     */
    public void reapplyCurrentSkin(Player p) {
        if (p == null) return;
        if (jugadoresRevelados.contains(key(p.getName()))) {
            restaurarSkinPropia(p);
        } else {
            applySkinByNameAsync(p, ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName()), false);
        }
    }

    /**
     * Revela la identidad real de un jugador (muerte por combate o fin de
     * partida): nombre real + su propia skin.
     */
    public void revealIdentity(Player p) {
        if (p == null || jugadoresRevelados.contains(key(p.getName()))) return;

        jugadoresRevelados.add(key(p.getName()));
        updateVisualIdentity(p);
        restaurarSkinPropia(p);
    }

    /**
     * Restaura la identidad propia sin marcar al jugador como revelado. Se usa
     * fuera de una partida (lobby/reset), donde no existe revelación de episodio.
     */
    public void restoreOwnIdentity(Player p) {
        if (p == null) return;
        restaurarSkinPropia(p);
    }

    /**
     * Restaura la skin REAL del jugador de forma fiable y sin red. Prioriza la
     * caché propia (alimentada durante la rotación, donde la skin de cada
     * jugador se resuelve por nombre) y aplica directamente la SkinProperty;
     * si no está en caché cae a la caché de SkinsRestorer y solo como último
     * recurso a la búsqueda asíncrona.
     */
    private void restaurarSkinPropia(Player p) {
        SkinProperty propia = skinRealPorNombre.get(key(p.getName()));
        if (propia == null) {
            applySkinByNameAsync(p, p.getName(), false);
            return;
        }

        skinRealPorNombre.put(key(p.getName()), propia);
        // Reapuntar el almacén persistente a su propia skin para que
        // futuros rejoin/respawn mantengan la skin real.
        skinsApi.getPlayerStorage().setSkinIdOfPlayer(p.getUniqueId(), SkinIdentifier.ofPlayer(p.getUniqueId()));
        // Aplicar la SkinProperty ya resuelta: sin Mojang ni resolución.
        skinsApi.getSkinApplier(Player.class).applySkin(p, propia);
        updateVisualIdentity(p);
    }

    private void notifyIdentityChanged(Player p) {
        String nombreSkinNueva = ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName());
        String rawMsg = plugin.getLang().get("game-events.skins.identity-changed", p);
        p.sendMessage(legacySection().deserialize(rawMsg.replace("%player%", nombreSkinNueva)));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    /**
     * Aplica a la cabeza de jugador la skin REAL del fallecido (la suya), no
     * la falsa que llevaba puesta mientras andaba camuflado.
     *
     * <p>Como todas las skins falsas son las reales de otros participantes, la
     * de la víctima ya suele estar en la caché de SkinsRestorer y se aplica
     * en el mismo tick por su nombre. Si no, se resuelve en segundo plano.
     */
    public void applyOwnHead(Skull skull, Player victim) {
        String victimName = victim.getName();
        UUID victimId = victim.getUniqueId();
        UUID worldId = skull.getWorld().getUID();
        int x = skull.getX();
        int y = skull.getY();
        int z = skull.getZ();
        long generacionSolicitada = generacion;
        SkinProperty cached = skinRealPorNombre.get(key(victimName));
        if (cached != null) {
            setHeadProfile(worldId, x, y, z, victimId, victimName, cached);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<InputDataResult> result = skinsApi.getSkinStorage().findOrCreateSkinData(victimName);
                if (result.isPresent()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (generacionSolicitada != generacion) return;
                        skinRealPorNombre.put(key(victimName), result.get().getProperty());
                        setHeadProfile(worldId, x, y, z, victimId, victimName, result.get().getProperty());
                    });
                }
            } catch (DataRequestException | MineSkinException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger()
                        .warning(() -> "Cabeza de " + victimName + " no resuelta: " + e.getMessage()));
            }
        });
    }

    private void setHeadProfile(UUID worldId, int x, int y, int z, UUID victimId,
            String victimName, SkinProperty property) {
        try {
            if (Bukkit.getWorld(worldId) == null) return;
            Location loc = new Location(Bukkit.getWorld(worldId), x, y, z);
            if (!(loc.getBlock().getState() instanceof Skull skull)) return;
            if (loc.getBlock().getType() != Material.PLAYER_HEAD) return;
            PlayerProfile perfil = Bukkit.createProfile(victimId, victimName);
            perfil.setProperty(new ProfileProperty("textures", property.getValue(), property.getSignature()));
            skull.setProfile(ResolvableProfile.resolvableProfile(perfil));
            skull.update();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(() -> "Perfil de cabeza inválido para " + victimName);
        }
    }

    public void updateVisualIdentity(Player p) {
        if (p == null) return;

        plugin.getTABManager().updateTabIdentity(p);
        // TAB (scoreboard-teams) puede reasignar la entry del jugador a un equipo
        // propio al refrescar su nametag/prefix; se re-incorpora al equipo h_*
        // para que el scoreboard refleje de nuevo la pertenencia real.
        plugin.getTeamManager().resyncPlayerEntry(p.getName());

        GamePhase phase = gm.getPhase();
        if (phase != GamePhase.LOBBY && phase != GamePhase.ENDING) {
            if (jugadoresRevelados.contains(key(p.getName()))) {
                p.displayName(Component.text(p.getName(), NamedTextColor.RED));
            } else {
                String nombreFalso = ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName());
                p.displayName(Component.text(nombreFalso, NamedTextColor.RED));
            }
        } else {
            p.displayName(Component.text(p.getName()));
        }
    }

    /**
     * Jugadores cuya identidad real ya fue revelada (nombres en minúsculas).
     */
    public Set<String> getRevealedPlayers() {
        return Set.copyOf(jugadoresRevelados);
    }

    /**
     * Mapa de skin falsa asignada por jugador (nombre minúsculas → nombre de skin).
     */
    public Map<String, String> getLastAssignedSkin() {
        return Map.copyOf(ultimaSkinAsignada);
    }

    public boolean isIdentityRevealed(String playerName) {
        return jugadoresRevelados.contains(key(playerName));
    }

    public String getAssignedSkin(String playerName) {
        return ultimaSkinAsignada.getOrDefault(key(playerName), playerName);
    }

    private void cancelarRotacion() {
        if (rotacionTask != null) {
            rotacionTask.cancel();
            rotacionTask = null;
        }
    }

    /**
     * Limpia el estado de identidad entre partidas: revelados por combate,
     * historial de skins y marcas de combate.
     */
    public void reset() {
        cancelarRotacion();
        generacion++;
        jugadoresRevelados.clear();
        ultimaSkinAsignada.clear();
        combatTags.clear();
    }
}
