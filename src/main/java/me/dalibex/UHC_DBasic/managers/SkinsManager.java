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
 * Owns visual identity: skin rotation, identity reveal, reconnect resync, and real-skin death heads.
 * Identity state is keyed by lower-case player name, matching participants and eliminated players.
 */
public class SkinsManager {

    private static final long COMBAT_WINDOW_MS = 30_000L;
    private static final long ROTATION_INTERVAL_TICKS = 100L;
    private static final long ROTATION_START_TICKS = 200L;
    private static final int MAX_COMBAT_RETRIES = 6;
    private static final int SHUFFLE_ATTEMPTS = 30;
    private static final int SHUFFLE_FALLBACK_ATTEMPTS = 100;

    private final UHC_DBasic plugin;
    private final GameManager gm;
    private final SkinsRestorer skinsApi;

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

    // ------------------------------------------------------------------
    // Pure logic, testable without a server.
    // ------------------------------------------------------------------

    /** Normalized key for a player name. */
    static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /** Pure combat-window check. */
    static boolean isCombatActive(Long since, long now, long windowMs) {
        return since != null && now - since < windowMs;
    }

    /**
     * Returns the current match skin assignment: lower-case player name -> skin name.
     * Pure: avoids own skin and previous assigned skin when possible.
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
        while (!asignacionValida && intentos < SHUFFLE_ATTEMPTS) {
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

        // Final effort: at least avoid assigning a player their own skin.
        if (!asignacionValida) {
            for (int k = 0; k < SHUFFLE_FALLBACK_ATTEMPTS && !asignacionValida; k++) {
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

        // Deterministic safety net: nobody keeps their own skin.
        repairSelfAssignments(asignados, vivos);

        for (int i = 0; i < vivos.size(); i++) {
            asignacion.put(key(vivos.get(i)), asignados.get(i));
        }
        return asignacion;
    }

    /** Repairs a live-player permutation so nobody keeps their own skin. */
    static void repairSelfAssignments(List<String> asignados, List<String> vivos) {
        List<Integer> conPropia = new ArrayList<>();
        for (int i = 0; i < vivos.size(); i++) {
            if (asignados.get(i).equalsIgnoreCase(vivos.get(i))) conPropia.add(i);
        }
        if (conPropia.isEmpty()) return;

        // Pair self-assigned players and swap their skins.
        for (int j = 0; j + 1 < conPropia.size(); j += 2) {
            Collections.swap(asignados, conPropia.get(j), conPropia.get(j + 1));
        }

        // The odd one swaps with someone who will not receive their own skin.
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
     * Processes one rotation queue tick and returns the player whose skin should be applied.
     * Pure: online/combat checks come from predicates; queue and retries are mutated intentionally.
     */
    static String nextNameToSkin(ArrayDeque<String> cola, Map<String, Integer> reintentos,
            Set<String> revelados, Predicate<String> isOnline, Predicate<String> isInCombat) {
        String name = cola.poll();
        if (name == null) return null;
        if (!isOnline.test(name)) return null;
        if (revelados.contains(key(name))) return null;
        if (isInCombat.test(name)) {
            int intento = reintentos.merge(key(name), 1, Integer::sum);
            if (intento < MAX_COMBAT_RETRIES) {
                cola.addLast(name);
            }
            return null;
        }
        reintentos.remove(key(name));
        return name;
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
                applySkinByNameAsync(Bukkit.getPlayer(nombre), nombreSkinElegida, true);
            }
        }.runTaskTimer(plugin, ROTATION_START_TICKS, ROTATION_INTERVAL_TICKS);
    }

    /** Applies a skin by name asynchronously, then refreshes visual identity and optionally notifies. */
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
                    // Never remask a revealed player with someone else's skin.
                    if (!esPropia && jugadoresRevelados.contains(key(playerName))) return;
                    // Local cache and persistent skin id mutate only on main for the active generation.
                    skinRealPorNombre.put(key(nombreSkin), result.get().getProperty());
                    PlayerStorage playerStorage = skinsApi.getPlayerStorage();
                    playerStorage.setSkinIdOfPlayer(playerId, result.get().getIdentifier());
                    try {
                        skinsApi.getSkinApplier(Player.class).applySkin(current);
                    } catch (DataRequestException e) {
                        plugin.getLogger().warning(() -> "Error applying skin: " + e.getMessage());
                    }
                    updateVisualIdentity(current);
                    if (notify) notifyIdentityChanged(current);
                });
            } catch (DataRequestException | MineSkinException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger()
                        .warning(() -> "Error applying skin to " + playerName + ": " + e.getMessage()));
            }
        });
    }

    /** Reapplies the correct reconnect skin without notifying the player. */
    public void reapplyCurrentSkin(Player p) {
        if (p == null) return;
        if (jugadoresRevelados.contains(key(p.getName()))) {
            restaurarSkinPropia(p);
        } else {
            applySkinByNameAsync(p, ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName()), false);
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
        SkinProperty propia = skinRealPorNombre.get(key(p.getName()));
        if (propia == null) {
            applySkinByNameAsync(p, p.getName(), false);
            return;
        }

        skinRealPorNombre.put(key(p.getName()), propia);
        // Point persistent storage back to their own skin for future rejoin/respawn.
        skinsApi.getPlayerStorage().setSkinIdOfPlayer(p.getUniqueId(), SkinIdentifier.ofPlayer(p.getUniqueId()));
        // Apply the already-resolved SkinProperty: no Mojang/network lookup.
        skinsApi.getSkinApplier(Player.class).applySkin(p, propia);
        updateVisualIdentity(p);
    }

    private void notifyIdentityChanged(Player p) {
        String nombreSkinNueva = ultimaSkinAsignada.getOrDefault(key(p.getName()), p.getName());
        String rawMsg = plugin.getLang().get("game-events.skins.identity-changed", p);
        p.sendMessage(legacySection().deserialize(rawMsg.replace("%player%", nombreSkinNueva)));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    /** Applies the victim's real skin to their death head, never the fake skin they were wearing. */
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
                        .warning(() -> "Could not resolve head for " + victimName + ": " + e.getMessage()));
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
            PlayerProfile profile = Bukkit.createProfile(victimId, victimName);
            profile.setProperty(new ProfileProperty("textures", property.getValue(), property.getSignature()));
            skull.setProfile(ResolvableProfile.resolvableProfile(profile));
            skull.update();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(() -> "Invalid head profile for " + victimName);
        }
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
