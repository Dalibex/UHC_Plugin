package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GCommandsCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public GCommandsCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        sender.sendMessage(plugin.getLang().get("help.header"));

        List<String> lines = plugin.getLang().getList("help.commands_list");
        for (String line : lines) {
            sender.sendMessage(line);
        }

        sender.sendMessage(plugin.getLang().get("help.footer"));

        return true;
    }
}
