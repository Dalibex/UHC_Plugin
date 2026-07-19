package me.dalibex.UHC_DBasic.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;

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

        // Delegar toda la lógica al GameManager
        plugin.getGameManager().fullReset();

        String prefix = lang.get("general.prefix", player);
        player.sendMessage(lang.get("lobby.reset-success", player).replace("%prefix%", prefix));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);

        return true;
    }

}