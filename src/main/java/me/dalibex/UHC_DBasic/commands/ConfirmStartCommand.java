package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.CommandTabs;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import static org.bukkit.GameRules.*;

public class ConfirmStartCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;
    private final StartCommand startCmd;
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

    public ConfirmStartCommand(UHC_DBasic plugin, StartCommand startCmd) {
        this.plugin = plugin;
        this.startCmd = startCmd;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String errorPrefix = plugin.getLang().get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(plugin.getLang().get("start-menu.usage", player).replace("%error-prefix%", errorPrefix));
            return true;
        }

        if (!startCmd.hasPendingConfirmation() || plugin.getGameManager().isGameStarted()) {
            return true;
        }

        if (plugin.isAdmin(player)) {
            UUID initiator = startCmd.getConfirmerUuid();
            if (initiator != null && !initiator.equals(player.getUniqueId())) {
                player.sendMessage(plugin.getLang().get("start-menu.not-initiator", player));
                return true;
            }
        } else {
            player.sendMessage(plugin.getLang().get("general.no-permission", player));
            return true;
        }

        TeamManager tm = plugin.getTeamManager();
        if (tm.isCustomTeamsEnabled() && tm.getTeamSize() > 1) {
            if (!tm.allPlayersHaveTeam()) {
                player.sendMessage(plugin.getLang().get("game.start-blocked-custom-teams", player));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                plugin.getGameManager().cancelStartup();
                return true;
            }
            
            int onlinePlayers = (int) Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count();
            int minRequired = (int) Math.ceil((double) onlinePlayers / tm.getTeamSize());
            int usedTeams = tm.getTeamsWithPlayersCount();
            
            if (usedTeams > minRequired) {
                player.sendMessage(plugin.getLang().get("game.start-blocked-team-count", player)
                        .replace("%min%", String.valueOf(minRequired))
                        .replace("%n%", String.valueOf(tm.getTeamSize())));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                plugin.getGameManager().cancelStartup();
                return true;
            }
        }

        if (!plugin.getGameManager().beginPreparation(player.getUniqueId())) return true;

        startUHCProcess(plugin.getGameManager().getPendingBorderSize());

        return true;
    }

    /**
     * Coordinates border setup, scatter teleporting, and countdown.
     */
    private void startUHCProcess(int size) {
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
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) prepareAndTeleport(p, location);

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(lang.get("game.teleporting-progress", online)
                                .replace("%current%", String.valueOf(current + 1))
                                .replace("%total%", String.valueOf(totalPlayers)));
                    }
                    current++;
                } else {
                    this.cancel();

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(lang.get("game.loading-world", online));
                    }

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
        double spawnX = blockX + SCATTER_EDGE_INSET;
        double spawnZ = blockZ + SCATTER_EDGE_INSET;

        return new ScatterPoint(blockX, blockZ, spawnX, spawnZ);
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
        int blockX = point.blockX();
        int blockZ = point.blockZ();
        int blockY = world.getHighestBlockYAt(blockX, blockZ);
        Block ground = world.getBlockAt(blockX, blockY, blockZ);

        if (ground.isLiquid() || ground.getType().toString().contains("AIR")) {
            if (ground.isLiquid()) {
                blockY += 1;
                world.getBlockAt(blockX, blockY, blockZ).setType(RESCUE_PLATFORM_MATERIAL);
            }
        }

        return new Location(world, point.spawnX(), blockY + SPAWN_VERTICAL_OFFSET, point.spawnZ());
    }

    private void prepareAndTeleport(Player p, Location loc) {
        p.teleport(loc);
        p.setVelocity(new Vector(0, 0, 0));
        p.setFallDistance(0f);
        giveBoatIfWaterRescueSpawn(p, loc);

        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, START_EFFECT_DURATION_TICKS, 1, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, START_EFFECT_DURATION_TICKS, MAX_EFFECT_AMPLIFIER, false, false, false));
    }

    private void giveBoatIfWaterRescueSpawn(Player p, Location loc) {
        Block platform = loc.clone().subtract(0, 1, 0).getBlock();
        if (platform.getType() != RESCUE_PLATFORM_MATERIAL || !platform.getRelative(0, -1, 0).isLiquid()) return;

        ItemStack boat = new ItemStack(RESCUE_BOAT_MATERIAL);
        ItemMeta meta = boat.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLang().getComponent("items.rescue-boat.name", p));
            boat.setItemMeta(meta);
        }
        p.getInventory().addItem(boat).values().forEach(leftover -> p.getWorld().dropItemNaturally(p.getLocation(), leftover));
        p.sendMessage(plugin.getLang().get("game.water-spawn-boat", p));
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
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        String title = lang.get("game.countdown-title", p).replace("%time%", String.valueOf(seconds));
                        String subtitle = lang.get("game.countdown-subtitle", p);
                        p.showTitle(Title.title(
                            legacySection().deserialize(title),
                            legacySection().deserialize(subtitle),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    seconds--;
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (plugin.getGameManager().getEligibleRoster().contains(p.getUniqueId())) {
                            p.setGameMode(GameMode.SURVIVAL);
                        } else {
                            p.setGameMode(GameMode.SPECTATOR);
                        }
                        for (PotionEffect effect : p.getActivePotionEffects()) {
                            p.removePotionEffect(effect.getType());
                        }

                        String startTitle = lang.get("game.started-title", p);
                        String startSubtitle = lang.get("game.started-subtitle", p);
                        p.showTitle(Title.title(
                            legacySection().deserialize(startTitle),
                            legacySection().deserialize(startSubtitle),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }

                    // Apply final match gamerules to every loaded dimension.
                    for (World w : Bukkit.getWorlds()) {
                        w.setGameRule(ADVANCE_TIME, true);
                        w.setGameRule(PVP, false);
                        w.setGameRule(ADVANCE_WEATHER, true);
                        w.setGameRule(NATURAL_HEALTH_REGENERATION, false);
                        w.setGameRule(SPAWN_MONSTERS, true);
                        w.setDifficulty(Difficulty.HARD);
                    }

                    plugin.getGameManager().startGame();
                    plugin.getGameManager().clearCompletedStartup();
                    this.cancel();
                }
            }
        };
        plugin.getGameManager().trackStartupTask(countdown.runTaskTimer(plugin, COUNTDOWN_PERIOD_TICKS, COUNTDOWN_PERIOD_TICKS));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && startCmd.hasPendingConfirmation()) {
            return CommandTabs.prefixFilter(List.of(String.valueOf(startCmd.getPendingSize())), args[0]);
        }
        return new ArrayList<>();
    }

    private record ScatterPoint(int blockX, int blockZ, double spawnX, double spawnZ) {
    }
}
