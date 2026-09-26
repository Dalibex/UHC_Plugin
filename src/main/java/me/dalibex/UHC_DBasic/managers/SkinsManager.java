package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.skins.DeathHeadService;
import me.dalibex.UHC_DBasic.managers.skins.SkinApplyService;
import me.dalibex.UHC_DBasic.managers.skins.SkinAssignmentPolicy;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;

/**
 * Owns visual identity: skin rotation, identity reveal, reconnect resync, and real-skin death heads.
 * Identity state is keyed by lower-case player name, matching participants and eliminated players.
 */
public class SkinsManager {

    private static final long COMBAT_WINDOW_MS = 30_000L;
    private static final long ROTATION_INTERVAL_TICKS = 40L;
    private static final long ROTATION_START_TICKS = 40L;
    private final UHC_DBasic plugin;
    private final GameManager gm;
    private final SkinsRestorer skinsApi;
    private SkinApplyService skinApplyService;
    private DeathHeadService deathHeadService;

    private final Set<String> jugadoresRevelados = new HashSet<>();
    private final Map<String, String> ultimaSkinAsignada = new HashMap<>();
    private final Map<String, Long> combatTags = new HashMap<>();

    /** Real skin cache by lower-case name, populated during rotation and kept across matches. */
    private final Map<String, SkinProperty> skinRealPorNombre = new HashMap<>();

    private BukkitTask rotacionTask;
    /** Identity generation used to invalidate async work from previous rotations. */
    private volatile long generacion = 0;

    public SkinsManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
        this.skinsApi = SkinsRestorerProvider.get();
    }

    private SkinApplyService skinApplyService() {
        if (skinApplyService == null) {
            skinApplyService = new SkinApplyService(plugin, skinsApi, skinRealPorNombre,
                    () -> generacion,
                    name -> jugadoresRevelados.contains(key(name)),
                    this::updateVisualIdentity,
                    this::notifyIdentityChanged);
        }
        return skinApplyService;
    }

    private DeathHeadService deathHeadService() {
        if (deathHeadService == null) {
            deathHeadService = new DeathHeadService(plugin, skinsApi, skinRealPorNombre, () -> generacion);
        }
        return deathHeadService;
    }

    // ------------------------------------------------------------------
    // Pure logic, testable without a server.
    // ------------------------------------------------------------------

    /** Normalized key for a player name. */
    static String key(String name) {
        return SkinAssignmentPolicy.key(name);
    }

    /** Pure combat-window check. */
    static boolean isCombatActive(Long since, long now, long windowMs) {
        return SkinAssignmentPolicy.isCombatActive(since, now, windowMs);
    }

    /**
     * Returns the current match skin assignment: lower-case player name -> skin name.
     * Pure: avoids own skin and previous assigned skin when possible.
     */
    static Map<String, String> assignNewSkins(List<String> vivos, Map<String, String> ultimaPorNombre) {
        return SkinAssignmentPolicy.assignNewSkins(vivos, ultimaPorNombre);
    }

    static Map<String, String> assignNewSkins(List<String> vivos, Map<String, String> ultimaPorNombre,
            java.util.Random random) {
        return SkinAssignmentPolicy.assignNewSkins(vivos, ultimaPorNombre, random);
    }

    /** Repairs a live-player permutation so nobody keeps their own skin. */
    static void repairSelfAssignments(List<String> asignados, List<String> vivos) {
        SkinAssignmentPolicy.repairSelfAssignments(asignados, vivos);
    }

    public static boolean isRotationEpisode(int episode) {
        return episode >= 2 && episode <= 10;
    }

    /**
     * Processes one rotation queue tick and returns the player whose skin should be applied.
     * Pure: online/combat checks come from predicates; queue and retries are mutated intentionally.
     */
    static String nextNameToSkin(ArrayDeque<String> cola, Map<String, Integer> reintentos,
            Set<String> revelados, Predicate<String> isOnline, Predicate<String> isInCombat) {
        return SkinAssignmentPolicy.nextNameToSkin(cola, reintentos, revelados, isOnline, isInCombat);
    }

    // ------------------------------------------------------------------
    // Rotation.
    // ------------------------------------------------------------------

    /** Marks a player as recently hit so rotation can avoid changing them during combat. */
    public void markInCombat(Player p) {
        if (p == null) return;
        combatTags.put(key(p.getName()), System.currentTimeMillis());
    }

    private boolean isInCombat(String name) {
        return isCombatActive(combatTags.get(key(name)), System.currentTimeMillis(), COMBAT_WINDOW_MS);
    }

    /** Shuffles current-match identities and applies them gradually. */
    public void rotateSkins() {
        cancelarRotacion();
        generacion++;

        // Each rotation starts a new episode-level identity mask.
        jugadoresRevelados.clear();

        List<String> vivosNombres = gm.getInitialParticipants().stream()
                .filter(name -> !gm.getEliminatedPlayers().contains(name))
                .collect(Collectors.toList());

        if (vivosNombres.size() < 2) return;

        Map<String, String> nuevaAsignacion = assignNewSkins(vivosNombres, ultimaSkinAsignada);
        if (nuevaAsignacion.isEmpty()) return;
        ultimaSkinAsignada.clear();
        ultimaSkinAsignada.putAll(nuevaAsignacion);

        // Apply gradually: initial delay, then one player per interval.
        ArrayDeque<String> cola = new ArrayDeque<>(vivosNombres);
        Map<String, Integer> reintentos = new HashMap<>();
        rotacionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (cola.isEmpty() || rotacionTask == null) {
                    cancel();
                    return;
                }
                // Stop when the match ended or reset; fake skins are no longer meaningful.
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
                skinApplyService().applyByNameAsync(Bukkit.getPlayer(nombre), nombreSkinElegida, true);
            }
        }.runTaskTimer(plugin, ROTATION_START_TICKS, ROTATION_INTERVAL_TICKS);
    }

    /** Reapplies the correct reconnect skin without notifying the player. */
    public void reapplyCurrentSkin(Player p) {
        if (p == null) return;
        if (jugadoresRevelados.contains(key(p.getName()))) {
            restaurarSkinPropia(p);
        } else {
            skinApplyService().applyByNameAsync(p, ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName()), false);
        }
    }

    /** Reveals a player's real identity: real name and own skin. */
    public void revealIdentity(Player p) {
        if (p == null || jugadoresRevelados.contains(key(p.getName()))) return;

        jugadoresRevelados.add(key(p.getName()));
        updateVisualIdentity(p);
        restaurarSkinPropia(p);
    }

    /** Restores own identity without marking the player as revealed. */
    public void restoreOwnIdentity(Player p) {
        if (p == null) return;
        restaurarSkinPropia(p);
    }

    /** Restores the player's real skin from the local cache, falling back to async lookup. */
    private void restaurarSkinPropia(Player p) {
        skinApplyService().restoreOwnSkin(p);
    }

    private void notifyIdentityChanged(Player p, String skinName) {
        skinApplyService().notifyIdentityChanged(p, skinName);
    }

    /** Applies the victim's real skin to their death head, never the fake skin they were wearing. */
    public void applyOwnHead(Skull skull, Player victim) {
        deathHeadService().applyOwnHead(skull, victim);
    }

    public void updateVisualIdentity(Player p) {
        if (p == null) return;

        plugin.getTABManager().updateTabIdentity(p);
        // TAB may move the entry to its own scoreboard team; restore h_* membership after refresh.
        plugin.getTeamManager().resyncPlayerEntry(p.getName());
        // Skin/TAB refresh may reset client compass target to spawn; force the next real target update.
        plugin.getItemsListener().clearCompassTarget(p);

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

    /** Returns lower-case names whose real identity has been revealed. */
    public Set<String> getRevealedPlayers() {
        return Set.copyOf(jugadoresRevelados);
    }

    /** Returns fake skin assignment by lower-case player name. */
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

    /** Clears per-match identity state. */
    public void reset() {
        cancelarRotacion();
        generacion++;
        jugadoresRevelados.clear();
        ultimaSkinAsignada.clear();
        combatTags.clear();
    }
}
