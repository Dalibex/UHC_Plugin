package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private final UHC_DBasic plugin;
    private final GameManager gm;
    private final SkinsRestorer skinsApi;

    private final Set<UUID> jugadoresRevelados = new HashSet<>();
    private final Map<UUID, String> ultimaSkinAsignada = new HashMap<>();
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();

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

    private void cancelarRotacion() {
        if (rotacionTask != null) {
            rotacionTask.cancel();
            rotacionTask = null;
        }
    }

    /**
     * Marca a un jugador como recién golpeado. Mientras esté dentro de la
     * ventana {@link #COMBAT_WINDOW_MS}, la rotación respetará su skin actual.
     */
    public void markInCombat(Player p) {
        if (p == null) return;
        combatTags.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private boolean isInCombat(Player p) {
        Long since = combatTags.get(p.getUniqueId());
        return since != null && System.currentTimeMillis() - since < COMBAT_WINDOW_MS;
    }

    /**
     * Baraja las identidades de la partida actual y las aplica de forma
     * escalonada: un jugador cada {@link #ROTATION_INTERVAL_TICKS}.
     *
     * Reglas anti-repetición: ni la propia skin ni la que el jugador llevaba
     * antes de esta rotación. Un jugador en combate (o desconectado) se
     * re-coloca al final de la cola y se reintenta sin tocar su skin.
     */
    public void rotateSkins() {
        cancelarRotacion();
        generacion++;

        List<String> vivosNombres = gm.getInitialParticipants().stream()
                .filter(name -> !gm.getEliminatedPlayers().contains(name))
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
                if (skinAsignada.equalsIgnoreCase(vivosNombres.get(i)) ||
                        skinAsignada.equalsIgnoreCase(ultimaSkinAsignada.get(uuid))) {
                    asignacionValida = false;
                    break;
                }
            }
            intentos++;
        }

        // Esfuerzo final: aunque cumpla las reglas, al menos evitar la propia skin.
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

        for (int i = 0; i < vivosNombres.size(); i++) {
            UUID uuid = Bukkit.getOfflinePlayer(vivosNombres.get(i)).getUniqueId();
            ultimaSkinAsignada.put(uuid, poolNombres.get(i));
        }

        // Aplicar escalonado: emite a los 10 segundos y luego 1 jugador cada
        // 5 segundos. Un jugador en combate se reintenta (máx. 30s, el mismo
        // tamaño que la ventana de combate) y, si sigue ocupado, se salta para
        // que la cola avance. Un jugador desconectado se descarta: al reconectar
        // se re-sincroniza su skin (reapplyCurrentSkin / onPlayerJoin).
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
                String name = cola.poll();
                Player p = Bukkit.getPlayer(name);
                if (p == null || !p.isOnline()) {
                    return; // desconectado: se descarta, el rejoin le re-sincroniza la skin
                }
                UUID uuid = p.getUniqueId();
                if (jugadoresRevelados.contains(uuid)) return; // ya revelado: no volver a camuflar
                if (isInCombat(p)) {
                    int intento = reintentos.merge(uuid.toString(), 1, Integer::sum);
                    if (intento < COMBATE_REINTENTOS_MAX) {
                        cola.addLast(name); // reintentar más tarde sin tocar su skin
                    }
                    return;
                }
                reintentos.remove(uuid.toString());
                String nombreSkinElegida = ultimaSkinAsignada.get(uuid);
                if (nombreSkinElegida == null) return;
                applySkinByNameAsync(p, nombreSkinElegida, true);
            }
        }.runTaskTimerAsynchronously(plugin, ROTATION_START_TICKS, ROTATION_INTERVAL_TICKS);
    }

    /**
     * Aplica una skin por nombre a un jugador (búsqueda asíncrona, posible
     * red). Actualiza el nombre visual y, si {@code notify}, avisa al jugador.
     */
    private void applySkinByNameAsync(Player p, String nombreSkin, boolean notify) {
        UUID uuid = p.getUniqueId();
        boolean esPropia = nombreSkin.equalsIgnoreCase(p.getName());
        long generacionSolicitada = generacion;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerStorage playerStorage = skinsApi.getPlayerStorage();
                SkinStorage skinStorage = skinsApi.getSkinStorage();
                Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(nombreSkin);
                if (!result.isPresent()) return;
                // Guardar la skin real resuelta de ese jugador para restaurarla
                // de forma inmediata y sin red en el reset.
                skinRealPorNombre.put(nombreSkin.toLowerCase(), result.get().getProperty());
                // Si hubo un reset o una rotación posterior, esta aplicación está obsoleta.
                if (generacionSolicitada != generacion) return;
                playerStorage.setSkinIdOfPlayer(uuid, result.get().getIdentifier());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (generacionSolicitada != generacion) return;
                    // Nunca recamuflejar a un jugador ya revelado con una skin ajena.
                    if (!esPropia && jugadoresRevelados.contains(uuid)) return;
                    try {
                        skinsApi.getSkinApplier(Player.class).applySkin(p);
                    } catch (DataRequestException e) {
                        plugin.getLogger().warning(() -> "Error aplicando skin: " + e.getMessage());
                    }
                    updateVisualIdentity(p);
                    if (notify) notifyIdentityChanged(p);
                });
            } catch (DataRequestException | MineSkinException e) {
                plugin.getLogger().warning(() -> "Error al aplicar skin a " + p.getName() + ": " + e.getMessage());
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
        if (jugadoresRevelados.contains(p.getUniqueId())) {
            restaurarSkinPropia(p);
        } else {
            applySkinByNameAsync(p, ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName()), false);
        }
    }

    /**
     * Revela la identidad real de un jugador (muerte por combate o fin de
     * partida): nombre real + su propia skin.
     */
    public void revealIdentity(Player p) {
        if (p == null || jugadoresRevelados.contains(p.getUniqueId())) return;

        jugadoresRevelados.add(p.getUniqueId());
        updateVisualIdentity(p);
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
        SkinProperty propia = skinRealPorNombre.get(p.getName().toLowerCase());
        if (propia == null) {
            propia = skinsApi.getSkinStorage().findSkinData(p.getName())
                    .map(InputDataResult::getProperty).orElse(null);
        }
        if (propia == null) {
            applySkinByNameAsync(p, p.getName(), false);
            return;
        }

        skinRealPorNombre.put(p.getName().toLowerCase(), propia);
        // Reapuntar el almacén persistente a su propia skin para que
        // futuros rejoin/respawn mantengan la skin real.
        skinsApi.getPlayerStorage().setSkinIdOfPlayer(p.getUniqueId(), SkinIdentifier.ofPlayer(p.getUniqueId()));
        // Aplicar la SkinProperty ya resuelta: sin Mojang ni resolución.
        skinsApi.getSkinApplier(Player.class).applySkin(p, propia);
        updateVisualIdentity(p);
    }

    private void notifyIdentityChanged(Player p) {
        String nombreSkinNueva = ultimaSkinAsignada.getOrDefault(p.getUniqueId(), p.getName());
        String rawMsg = plugin.getLang().get("game-events.skins.identity-changed", p);
        p.sendMessage(legacySection().deserialize(rawMsg.replace("%player%", nombreSkinNueva)));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    /**
     * Aplica a la cabeza de jugador la skin REAL del fallecido (la suya), no
     * la falsa que llevaba puesta mientras andaba camuflado.
     *
     * Como todas las skins falsas son las reales de otros participantes, la
     * de la víctima ya suele estar en la caché de SkinsRestorer y se aplica
     * en el mismo tick por su nombre. Si no, se resuelve en segundo plano.
     */
    public void applyOwnHead(Skull skull, Player victim) {
        Optional<SkinProperty> cached = skinsApi.getSkinStorage()
                .findSkinData(victim.getName())
                .map(InputDataResult::getProperty);
        if (cached.isPresent()) {
            setHeadProfile(skull, victim, cached.get());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<InputDataResult> result = skinsApi.getSkinStorage().findOrCreateSkinData(victim.getName());
                if (result.isPresent()) {
                    skinRealPorNombre.put(victim.getName().toLowerCase(), result.get().getProperty());
                    setHeadProfile(skull, victim, result.get().getProperty());
                }
            } catch (DataRequestException | MineSkinException e) {
                plugin.getLogger().warning(() -> "Cabeza de " + victim.getName() + " no resuelta: " + e.getMessage());
            }
        });
    }

    private void setHeadProfile(Skull skull, Player victim, SkinProperty property) {
        try {
            PlayerProfile perfil = Bukkit.createProfile(victim.getUniqueId(), victim.getName());
            perfil.setProperty(new ProfileProperty("textures", property.getValue(), property.getSignature()));
            Location loc = skull.getLocation();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (loc.getWorld() == null) return;
                if (!(loc.getBlock().getState() instanceof Skull s)) return;
                if (loc.getBlock().getType() != Material.PLAYER_HEAD) return;
                s.setProfile(ResolvableProfile.resolvableProfile(perfil));
                s.update();
            });
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(() -> "Perfil de cabeza inválido para " + victim.getName());
        }
    }

    public void updateVisualIdentity(Player p) {
        if (p == null) return;

        plugin.getTABManager().updateTabIdentity(p);

        GamePhase phase = gm.getPhase();
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

    public Set<UUID> getRevealedPlayers() {
        return jugadoresRevelados;
    }

    public Map<UUID, String> getLastAssignedSkin() {
        return ultimaSkinAsignada;
    }
}