package me.dalibex.UHC_DBasic.managers.skins;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Resolves and applies SkinsRestorer skin data off the main thread. */
public class SkinApplyService {

    private final UHC_DBasic plugin;
    private final SkinsRestorer skinsApi;
    private final Map<String, SkinProperty> realSkinCache;
    private final LongSupplier generation;
    private final Predicate<String> isRevealed;
    private final Consumer<Player> visualRefresh;
    private final BiConsumer<Player, String> notifyChanged;

    public SkinApplyService(UHC_DBasic plugin, SkinsRestorer skinsApi, Map<String, SkinProperty> realSkinCache,
                            LongSupplier generation, Predicate<String> isRevealed,
                            Consumer<Player> visualRefresh, BiConsumer<Player, String> notifyChanged) {
        this.plugin = plugin;
        this.skinsApi = skinsApi;
        this.realSkinCache = realSkinCache;
        this.generation = generation;
        this.isRevealed = isRevealed;
        this.visualRefresh = visualRefresh;
        this.notifyChanged = notifyChanged;
    }

    public void applyByNameAsync(Player player, String skinName, boolean notify) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        boolean ownSkin = skinName.equalsIgnoreCase(playerName);
        long requestedGeneration = generation.getAsLong();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkinStorage skinStorage = skinsApi.getSkinStorage();
                Optional<InputDataResult> result = skinStorage.findOrCreateSkinData(skinName);
                if (!result.isPresent()) return;
                Bukkit.getScheduler().runTask(plugin, () -> applyResolvedSkin(
                        playerId, playerName, skinName, ownSkin, notify, requestedGeneration, result.get()));
            } catch (DataRequestException | MineSkinException e) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger()
                        .warning(() -> "Error applying skin to " + playerName + ": " + e.getMessage()));
            }
        });
    }

    public boolean applyCachedSkin(Player player, String skinName, SkinProperty property, boolean notify) {
        if (player == null || property == null) return false;
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        long requestedGeneration = generation.getAsLong();
        if (requestedGeneration != generation.getAsLong()) return false;
        Player current = Bukkit.getPlayer(playerId);
        if (current == null || !current.isOnline() || !current.getName().equals(playerName)) return false;
        if (!skinName.equalsIgnoreCase(playerName) && isRevealed.test(playerName)) return false;

        try {
            realSkinCache.put(SkinAssignmentPolicy.key(skinName), property);
            skinsApi.getSkinApplier(Player.class).applySkin(current, property);
        } catch (RuntimeException e) {
            plugin.getLogger().warning(() -> "Error applying cached skin: " + e.getMessage());
            return false;
        }
        visualRefresh.accept(current);
        if (notify) notifyChanged.accept(current, skinName);
        return true;
    }

    private void applyResolvedSkin(UUID playerId, String playerName, String skinName, boolean ownSkin,
                                   boolean notify, long requestedGeneration, InputDataResult result) {
        if (requestedGeneration != generation.getAsLong()) return;
        Player current = Bukkit.getPlayer(playerId);
        if (current == null || !current.isOnline() || !current.getName().equals(playerName)) return;
        if (!ownSkin && isRevealed.test(playerName)) return;

        realSkinCache.put(SkinAssignmentPolicy.key(skinName), result.getProperty());
        PlayerStorage playerStorage = skinsApi.getPlayerStorage();
        playerStorage.setSkinIdOfPlayer(playerId, result.getIdentifier());
        try {
            skinsApi.getSkinApplier(Player.class).applySkin(current);
        } catch (DataRequestException e) {
            plugin.getLogger().warning(() -> "Error applying skin: " + e.getMessage());
        }
        visualRefresh.accept(current);
        if (notify) notifyChanged.accept(current, skinName);
    }

    public void restoreOwnSkin(Player player) {
        SkinProperty ownSkin = realSkinCache.get(SkinAssignmentPolicy.key(player.getName()));
        if (ownSkin == null) {
            restoreOwnSkinByNameOrDefault(player);
            return;
        }

        realSkinCache.put(SkinAssignmentPolicy.key(player.getName()), ownSkin);
        skinsApi.getPlayerStorage().setSkinIdOfPlayer(player.getUniqueId(), SkinIdentifier.ofPlayer(player.getUniqueId()));
        skinsApi.getSkinApplier(Player.class).applySkin(player, ownSkin);
        visualRefresh.accept(player);
    }

    private void restoreOwnSkinByNameOrDefault(Player player) {
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        long requestedGeneration = generation.getAsLong();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Optional<InputDataResult> result = skinsApi.getSkinStorage().findOrCreateSkinData(playerName);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result.isPresent()) {
                        applyResolvedSkin(playerId, playerName, playerName, true, false, requestedGeneration, result.get());
                    } else {
                        restoreDefaultSkin(playerId, playerName);
                    }
                });
            } catch (DataRequestException | MineSkinException e) {
                Bukkit.getScheduler().runTask(plugin, () -> restoreDefaultSkin(playerId, playerName));
            }
        });
    }

    private void restoreDefaultSkin(UUID playerId, String playerName) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline() || !player.getName().equals(playerName)) return;
        skinsApi.getPlayerStorage().removeSkinIdOfPlayer(playerId);
        try {
            skinsApi.getSkinApplier(Player.class).applySkin(player);
        } catch (DataRequestException e) {
            plugin.getLogger().warning(() -> "Error restoring default skin: " + e.getMessage());
        }
        visualRefresh.accept(player);
    }

    public void notifyIdentityChanged(Player player, String skinName) {
        String rawMsg = plugin.getLang().get("game-events.skins.identity-changed", player);
        player.sendMessage(legacySection().deserialize(rawMsg.replace("%player%", skinName)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }
}
