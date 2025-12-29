package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.GameRules.*;

public class PrepareWorldCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public PrepareWorldCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LanguageManager lang = plugin.getLang();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("general.only-players", null));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(lang.get("general.no-permission", player));
            return true;
        }

        resetearJugadores();
        resetearMundos();

        plugin.getRightPanelManager().setStandBy();
        plugin.getTeamManager().borrarTodosLosEquipos();

        String prefix = lang.get("general.prefix", player);
        player.sendMessage(lang.get("lobby.reset-success", player).replace("%prefix%", prefix));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);

        return true;
    }

    private void resetearJugadores() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.clearActivePotionEffects();
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setExp(0);
            p.setLevel(0);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 255, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 255, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 255, false, false, false));
        }
    }

    private void resetearMundos() {
        for (World world : Bukkit.getWorlds()) {
            world.setDifficulty(Difficulty.HARD);
            world.setTime(0L);
            world.setThundering(false);
            world.setStorm(false);

            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(5999984);

            world.setGameRule(ADVANCE_TIME, false);
            world.setGameRule(ADVANCE_WEATHER, false);
            world.setGameRule(NATURAL_HEALTH_REGENERATION, true);
            world.setGameRule(SPAWN_MONSTERS, false);
            world.setGameRule(PVP, false);
        }
    }
}