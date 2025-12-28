package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().get("general.only-players", null));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(plugin.getLang().get("general.no-permission", player));
            return true;
        }

        World world = player.getWorld();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.clearActivePotionEffects();
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 255, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 999999, 255, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 255, false, false, false));
        }

        world.setDifficulty(Difficulty.HARD);
        plugin.getRightPanelManager().setStandBy();
        plugin.getTeamManager().borrarTodosLosEquipos();

        world.setTime(0L);
        world.setGameRule(ADVANCE_TIME, false);
        world.setGameRule(ADVANCE_WEATHER, false);
        world.setGameRule(NATURAL_HEALTH_REGENERATION, true);
        world.setGameRule(SPAWN_MONSTERS, false);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(5999984);

        String prefix = plugin.getLang().get("general.prefix", player);
        player.sendMessage(plugin.getLang().get("lobby.reset-success", player).replace("%prefix%", prefix));

        return true;
    }
}