package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando de abandono de partida: marca a un jugador como eliminado
 * (quedándose fuera esté conectado o no). Un jugador puede abandonar por
 * sí mismo con /abandon o un administrador puede marcarlo con
 * /abandon <jugador> si se desconectó sin avisar.
 */
public class AbandonCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;

    public AbandonCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage("§cThis command is only for players.");
            return true;
        }

        GameManager gm = plugin.getGameManager();
        LanguageManager lang = plugin.getLang();

        if (!gm.isMatchActive()) {
            executor.sendMessage(lang.get("game.abandon-game-not-started", executor));
            executor.playSound(executor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        boolean porAdmin = args.length > 0;
        String targetName;
        if (porAdmin) {
            if (!plugin.isAdmin(executor)) {
                executor.sendMessage(lang.get("general.no-permission", executor));
                return true;
            }
            targetName = args[0];
        } else {
            targetName = executor.getName();
        }

        if (!gm.getInitialParticipants().contains(targetName)) {
            executor.sendMessage(lang.get("game.abandon-not-participant", executor).replace("%player%", targetName));
            executor.playSound(executor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        if (gm.getEliminatedPlayers().contains(targetName)) {
            executor.sendMessage(lang.get("game.abandon-already", executor).replace("%player%", targetName));
            executor.playSound(executor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }

        gm.eliminatePlayer(targetName);

        Player target = Bukkit.getPlayer(targetName);
        if (target != null && target.isOnline()) {
            target.setGameMode(GameMode.SPECTATOR);
            target.sendMessage(lang.get("game.abandon-self", target));
            target.playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1, 1);
        }

        Bukkit.broadcast(legacySection().deserialize(lang.get("game.abandon-broadcast", null).replace("%player%", targetName)));

        if (porAdmin) {
            executor.sendMessage(lang.get("game.abandon-admin-set", executor).replace("%player%", targetName));
            executor.playSound(executor.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        // Verificar victoria tras procesar el abandono
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getGameManager().getCurrentMode().checkVictory();
            }
        }.runTaskLater(plugin, 1L);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender instanceof Player p && plugin.isAdmin(p) && args.length == 1) {
            String partial = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return completions;
    }
}
