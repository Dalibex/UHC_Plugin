package me.dalibex.UHC_DBasic.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.dalibex.UHC_DBasic.UHC_DBasic;

/** Runs native item-sprite roulettes in player action bars. */
public final class ItemRoulette {

    private static final int DEFAULT_STEPS = 24;
    private static final long DEFAULT_INITIAL_DELAY = 2L;
    private static final long DEFAULT_FINAL_DELAY = 10L;
    private static final long DEFAULT_RESULT_HOLD = 40L;
    private static final int MAX_STEPS = 200;
    private static final long MAX_DELAY_TICKS = 200L;
    private static final long MAX_RESULT_HOLD_TICKS = 1200L;

    private final UHC_DBasic plugin;
    private final Map<UUID, ActiveRoulette> activeByPlayer = new java.util.HashMap<>();
    private final int steps;
    private final long initialDelay;
    private final long finalDelay;
    private final long resultHold;

    public ItemRoulette(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.steps = clamp(plugin.getConfig().getInt("item-roulette.steps", DEFAULT_STEPS), 1, MAX_STEPS);
        this.initialDelay = clamp(plugin.getConfig().getLong(
                "item-roulette.initial-delay-ticks", DEFAULT_INITIAL_DELAY), 1L, MAX_DELAY_TICKS);
        this.finalDelay = clamp(plugin.getConfig().getLong(
                "item-roulette.final-delay-ticks", DEFAULT_FINAL_DELAY), initialDelay, MAX_DELAY_TICKS);
        this.resultHold = clamp(plugin.getConfig().getLong(
                "item-roulette.result-hold-ticks", DEFAULT_RESULT_HOLD), 0L, MAX_RESULT_HOLD_TICKS);
    }

    public void start(Player player, List<Material> candidates, Consumer<Material> onSelected) {
        List<Material> pool = validatedPool(candidates);
        Material result = ItemRouletteAnimation.select(pool, ThreadLocalRandom.current());
        start(player, pool, result, onSelected);
    }

    public void start(Player player, List<Material> candidates, Material result,
                      Consumer<Material> onSelected) {
        requirePrimaryThread();
        Objects.requireNonNull(player, "player");
        List<Material> pool = validatedPool(candidates);
        ItemSpriteResolver.resolve(result);
        Consumer<Material> callback = Objects.requireNonNull(onSelected, "onSelected");

        if (!player.isOnline()) return;

        UUID playerId = player.getUniqueId();
        cancelInternal(playerId);
        List<Material> frames = ItemRouletteAnimation.frames(
                pool, result, steps, ThreadLocalRandom.current());
        ActiveRoulette roulette = new ActiveRoulette(frames, result, callback);
        activeByPlayer.put(playerId, roulette);
        showNextFrame(playerId, roulette);
    }

    public boolean isActive(UUID playerId) {
        return activeByPlayer.containsKey(playerId);
    }

    public long totalDurationTicks() {
        return ItemRouletteAnimation.totalDurationTicks(
                steps, initialDelay, finalDelay, resultHold);
    }

    public void cancel(UUID playerId) {
        requirePrimaryThread();
        cancelInternal(playerId);
    }

    public void cancelAll() {
        requirePrimaryThread();
        for (ActiveRoulette roulette : activeByPlayer.values()) roulette.cancelTask();
        activeByPlayer.clear();
    }

    public void shutdown() {
        for (ActiveRoulette roulette : activeByPlayer.values()) roulette.cancelTask();
        activeByPlayer.clear();
    }

    private void showNextFrame(UUID playerId, ActiveRoulette roulette) {
        if (activeByPlayer.get(playerId) != roulette) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            cancelInternal(playerId);
            return;
        }

        Material material = roulette.frames.get(roulette.frameIndex);
        player.sendActionBar(ItemRouletteRenderer.render(material));
        if (material != roulette.lastShown) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.25f, 1.2f);
            roulette.lastShown = material;
        }
        roulette.frameIndex++;

        if (roulette.frameIndex < roulette.frames.size()) {
            long delay = ItemRouletteAnimation.delayAfterFrame(
                    roulette.frameIndex - 1, roulette.frames.size(), initialDelay, finalDelay);
            roulette.task = Bukkit.getScheduler().runTaskLater(plugin,
                    () -> showNextFrame(playerId, roulette), delay);
        } else {
            roulette.task = Bukkit.getScheduler().runTaskLater(plugin,
                    () -> complete(playerId, roulette), resultHold);
        }
    }

    private void complete(UUID playerId, ActiveRoulette roulette) {
        if (!activeByPlayer.remove(playerId, roulette)) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        try {
            roulette.callback.accept(roulette.result);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Item roulette callback failed for " + player.getName()
                    + ": " + exception.getMessage());
        }
    }

    private void cancelInternal(UUID playerId) {
        ActiveRoulette previous = activeByPlayer.remove(playerId);
        if (previous != null) previous.cancelTask();
    }

    private static List<Material> validatedPool(List<Material> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        LinkedHashSet<Material> unique = new LinkedHashSet<>();
        for (Material material : candidates) {
            ItemSpriteResolver.resolve(material);
            unique.add(material);
        }
        if (unique.isEmpty()) throw new IllegalArgumentException("Candidates cannot be empty");
        return List.copyOf(unique);
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Item roulettes must be controlled from the server thread");
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class ActiveRoulette {
        private final List<Material> frames;
        private final Material result;
        private final Consumer<Material> callback;
        private int frameIndex;
        private Material lastShown;
        private BukkitTask task;

        private ActiveRoulette(List<Material> frames, Material result, Consumer<Material> callback) {
            this.frames = frames;
            this.result = result;
            this.callback = callback;
        }

        private void cancelTask() {
            if (task != null) task.cancel();
        }
    }
}
