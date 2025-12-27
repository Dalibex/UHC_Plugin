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

        if (!sender.isOp()) {
            sender.sendMessage(plugin.getLang().get("general.no-permission"));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage/Uso: /lang <es|en>");
            return true;
        }

        String langCode = args[0].toLowerCase();

        if (langCode.equals("es") || langCode.equals("en")) {
            plugin.setLanguage(langCode);

            sender.sendMessage("§e§lUHC ELOUD > §fLanguage updated to: §a" + langCode.toUpperCase());

            if (sender instanceof Player p) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
        } else {
            sender.sendMessage("§cInvalid language. Use 'es' (Español) or 'en' (English).");
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
