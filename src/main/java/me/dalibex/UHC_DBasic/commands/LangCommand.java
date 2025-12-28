package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class LangCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;

    public LangCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.getLang().get("general.only-players", null));
            return true;
        }

        if (plugin.getRightPanelManager().getTiempoTotalSegundos() > 0) {
            p.sendMessage(plugin.getLang().get("timer.already-started", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (args.length != 1) {
            p.sendMessage("§cUsage: /lang <language>");
            return true;
        }

        String langCode = args[0].toLowerCase();

        if (langCode.equals("es") || langCode.equals("en")) {
            plugin.getLang().setPlayerLanguage(p, langCode);

            String prefix = plugin.getLang().get("general.prefix", p);
            String confirmMsg = plugin.getLang().get("lang-switch", p).replace("%prefix%", prefix);
            p.sendMessage(confirmMsg);

            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        } else {
            p.sendMessage("§cInvalid language");
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("es", "en");
        }
        return null;
    }
}