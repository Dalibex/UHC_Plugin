package me.dalibex.UHC_DBasic.services;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Coordinates border setup, scatter teleporting, and the match countdown. */
public class MatchStartService {

    private static final int MIN_SCATTER_POSITIONS = 4;
    private static final int SCATTER_PRELOAD_RADIUS_CHUNKS = 2;
    private static final double SCATTER_EDGE_INSET = 0.5;
    private static final double SPAWN_VERTICAL_OFFSET = 1.5;
    private static final long SCATTER_INTERVAL_TICKS = 40L;
    private static final long WORLD_LOAD_DELAY_TICKS = 120L;
    private static final long COUNTDOWN_PERIOD_TICKS = 20L;
    private static final int COUNTDOWN_SECONDS = 10;
    private static final int START_EFFECT_DURATION_TICKS = 3600;
    private static final int MAX_EFFECT_AMPLIFIER = 255;
    private static final Material RESCUE_PLATFORM_MATERIAL = Material.GLASS;
    private static final Material RESCUE_BOAT_MATERIAL = Material.OAK_BOAT;

    private final UHC_DBasic plugin;

    public MatchStartService(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public void start(int size) {
        World world = plugin.getGameManager().getStartupWorld();
        LanguageManager lang = plugin.getLang();
        long generation = plugin.getGameManager().getStartupGeneration();

        plugin.getTeamManager().removeAllSelectorItems();

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);

        List<UUID> players = new ArrayList<>(plugin.getGameManager().getEligibleRoster());
        int totalPlayers = players.size();
        int positionCount = Math.max(MIN_SCATTER_POSITIONS, totalPlayers);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < positionCount; i++) indices.add(i);
        java.util.Collections.shuffle(indices);

        BukkitRunnable scatter = new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (!plugin.getGameManager().isStartupGeneration(generation)) {
                    cancel();
                    return;
                }
                if (current < totalPlayers) {
                    UUID playerId = players.get(current);
                    ScatterPoint scatterPoint = calculateScatterPoint(indices.get(current), positionCount, size);
                    preloadScatterChunks(world, scatterPoint);
                    Location location = resolveScatterLocation(world, scatterPoint);
                    plugin.getGameManager().setPlannedScatterLocation(playerId, location);
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) prepareAndTeleport(player, location);

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(lang.get("game.teleporting-progress", online)
                                .replace("%current%", String.valueOf(current + 1))
                                .replace("%total%", String.valueOf(totalPlayers)));
                    }
                    current++;
                } else {
                    cancel();
                    for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(lang.get("game.loading-world", online));
                    BukkitRunnable delay = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (plugin.getGameManager().enterCountdown(generation)) startCountdown(lang, generation);
                        }
                    };
                    plugin.getGameManager().trackStartupTask(delay.runTaskLater(plugin, WORLD_LOAD_DELAY_TICKS));
                }
            }
        };
        plugin.getGameManager().trackStartupTask(scatter.runTaskTimer(plugin, 0L, SCATTER_INTERVAL_TICKS));
    }

    private ScatterPoint calculateScatterPoint(int index, int positionCount, int size) {
        double radius = size / 2.0;
        double angle = (2 * Math.PI * index / positionCount) + (Math.PI / 4);
        double circularX = Math.cos(angle);
        double circularZ = Math.sin(angle);

        double finalX;
        double finalZ;
        if (Math.abs(circularX) > Math.abs(circularZ)) {
            finalX = (circularX > 0) ? radius : -radius;
            finalZ = radius * (circularZ / Math.abs(circularX));
        } else {
            finalZ = (circularZ > 0) ? radius : -radius;
            finalX = radius * (circularX / Math.abs(circularZ));
        }

        if (finalX > 0) finalX -= SCATTER_EDGE_INSET; else finalX += SCATTER_EDGE_INSET;
        if (finalZ > 0) finalZ -= SCATTER_EDGE_INSET; else finalZ += SCATTER_EDGE_INSET;

        int blockX = (int) Math.floor(finalX);
        int blockZ = (int) Math.floor(finalZ);
        return new ScatterPoint(blockX, blockZ, blockX + SCATTER_EDGE_INSET, blockZ + SCATTER_EDGE_INSET);
    }

    private void preloadScatterChunks(World world, ScatterPoint point) {
        int centerChunkX = point.blockX() >> 4;
        int centerChunkZ = point.blockZ() >> 4;
        for (int chunkX = centerChunkX - SCATTER_PRELOAD_RADIUS_CHUNKS; chunkX <= centerChunkX + SCATTER_PRELOAD_RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = centerChunkZ - SCATTER_PRELOAD_RADIUS_CHUNKS; chunkZ <= centerChunkZ + SCATTER_PRELOAD_RADIUS_CHUNKS; chunkZ++) {
                world.loadChunk(chunkX, chunkZ, true);
            }
        }
    }

    private Location resolveScatterLocation(World world, ScatterPoint point) {
        int blockY = world.getHighestBlockYAt(point.blockX(), point.blockZ());
        Block ground = world.getBlockAt(point.blockX(), blockY, point.blockZ());

        if (ground.isLiquid() || ground.getType().toString().contains("AIR")) {
            if (ground.isLiquid()) {
                blockY += 1;
                world.getBlockAt(point.blockX(), blockY, point.blockZ()).setType(RESCUE_PLATFORM_MATERIAL);
            }
        }

        return new Location(world, point.spawnX(), blockY + SPAWN_VERTICAL_OFFSET, point.spawnZ());
    }

    private void prepareAndTeleport(Player player, Location location) {
        player.teleport(location);
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0f);
        giveBoatIfWaterRescueSpawn(player, location);

        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, START_EFFECT_DURATION_TICKS, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
    }

    private void giveBoatIfWaterRescueSpawn(Player player, Location location) {
        Block platform = location.clone().subtract(0, 1, 0).getBlock();
        if (platform.getType() != RESCUE_PLATFORM_MATERIAL || !platform.getRelative(0, -1, 0).isLiquid()) return;

        ItemStack boat = new ItemStack(RESCUE_BOAT_MATERIAL);
        ItemMeta meta = boat.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLang().getComponent("items.rescue-boat.name", player));
            boat.setItemMeta(meta);
        }
        player.getInventory().addItem(boat).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(plugin.getLang().get("game.water-spawn-boat", player));
    }

    private void startCountdown(LanguageManager lang, long generation) {
        BukkitRunnable countdown = new BukkitRunnable() {
            int seconds = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!plugin.getGameManager().isStartupGeneration(generation)) {
                    cancel();
                    return;
                }
                if (seconds > 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String title = lang.get("game.countdown-title", player).replace("%time%", String.valueOf(seconds));
                        String subtitle = lang.get("game.countdown-subtitle", player);
                        player.showTitle(Title.title(
                                legacySection().deserialize(title),
                                legacySection().deserialize(subtitle),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    seconds--;
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setGameMode(plugin.getGameManager().getEligibleRoster().contains(player.getUniqueId())
                            ? GameMode.SURVIVAL : GameMode.SPECTATOR);
                    for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());

                    player.showTitle(Title.title(
                            legacySection().deserialize(lang.get("game.started-title", player)),
                            legacySection().deserialize(lang.get("game.started-subtitle", player)),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                }

                plugin.getWorldManager().applyFinalMatchRules();
                plugin.getGameManager().startGame();
                plugin.getGameManager().clearCompletedStartup();
                cancel();
            }
        };
        plugin.getGameManager().trackStartupTask(countdown.runTaskTimer(plugin, COUNTDOWN_PERIOD_TICKS, COUNTDOWN_PERIOD_TICKS));
    }

    private record ScatterPoint(int blockX, int blockZ, double spawnX, double spawnZ) {
    }
}
