package me.dalibex.UHC_DBasic.managers;

import static org.bukkit.GameRules.ADVANCE_TIME;
import static org.bukkit.GameRules.ADVANCE_WEATHER;
import static org.bukkit.GameRules.NATURAL_HEALTH_REGENERATION;
import static org.bukkit.GameRules.PVP;
import static org.bukkit.GameRules.SPAWN_MONSTERS;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import me.dalibex.UHC_DBasic.UHC_DBasic;

/**
 * Responsable del estado de los mundos: localización del mundo principal,
 * reset de gamerules/tiempo y teletransporte de jugadores al spawn del lobby.
 */
public class WorldManager {

    private final UHC_DBasic plugin;
    private static final int RESET_WORLD_BORDER_SIZE = 5999984;
    private static final int MIN_SPAWN_Y = 60;
    private static final double SPAWN_CENTER_OFFSET = 0.5;

    public WorldManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public World getMainWorld() {
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().get(0));
    }

    public void setGameRuleForAllWorlds(GameRule<Boolean> rule, boolean value) {
        for (World world : Bukkit.getWorlds()) world.setGameRule(rule, value);
    }

    public void applyFinalMatchRules() {
        setGameRuleForAllWorlds(ADVANCE_TIME, true);
        setGameRuleForAllWorlds(PVP, false);
        setGameRuleForAllWorlds(ADVANCE_WEATHER, true);
        setGameRuleForAllWorlds(NATURAL_HEALTH_REGENERATION, false);
        setGameRuleForAllWorlds(SPAWN_MONSTERS, true);
        for (World world : Bukkit.getWorlds()) world.setDifficulty(Difficulty.HARD);
    }

    public void resetWorlds() {
        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.setGameRule(ADVANCE_TIME, true);
            world.setGameRule(ADVANCE_WEATHER, true);
            world.setThundering(false);
            world.setStorm(false);
            world.setGameRule(ADVANCE_WEATHER, false);
            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(RESET_WORLD_BORDER_SIZE);

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
    }

    public void teleportToSpawn(Player p) {
        World world = getMainWorld();
        int y = world.getHighestBlockYAt(0, 0);
        Location spawnLoc = new Location(world, SPAWN_CENTER_OFFSET, Math.max(y, MIN_SPAWN_Y) + 1, SPAWN_CENTER_OFFSET);
        p.teleport(spawnLoc);
    }
}
