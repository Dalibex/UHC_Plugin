package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GCommandsCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public GCommandsCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LanguageManager lang = plugin.getLang();

        Player player = (sender instanceof Player) ? (Player) sender : null;

        sender.sendMessage(lang.get("help.header", player));

        List<String> lines = lang.getList("help.commands_list", player);
        for (String line : lines) {
            sender.sendMessage(line);
        }

        sender.sendMessage(lang.get("help.footer", player));

        return true;
    }
}