package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class HelpCommand implements CommandExecutor {

    private static final String ADMIN_PAGE = "admin";
    private static final String USER_PAGE = "user";

    private final UHC_DBasic plugin;

    public HelpCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        LanguageManager lang = plugin.getLang();
        Player player = (sender instanceof Player) ? (Player) sender : null;
        String page = args.length > 0 && ADMIN_PAGE.equalsIgnoreCase(args[0]) ? ADMIN_PAGE : USER_PAGE;

        sender.sendMessage(lang.get("help.header", player));
        sender.sendMessage(lang.get("help." + page + "-title", player));

        List<String> lines = lang.getList("help." + page + "-list", player);
        for (String line : lines) {
            sender.sendMessage(line);
        }

        sender.sendMessage(lang.get("help.footer", player));
        sendNavigation(sender, player, lang);

        return true;
    }

    private void sendNavigation(CommandSender sender, Player player, LanguageManager lang) {
        if (player == null) {
            sender.sendMessage(lang.get("help.nav-user", null) + " " + lang.get("help.nav-admin", null));
            return;
        }

        Component userButton = legacySection().deserialize(lang.get("help.nav-user", player))
                .clickEvent(ClickEvent.runCommand("/uhccommands user"))
                .hoverEvent(HoverEvent.showText(legacySection().deserialize(lang.get("help.nav-user-hover", player))));

        Component separator = Component.text(" ");

        Component adminButton = legacySection().deserialize(lang.get("help.nav-admin", player))
                .clickEvent(ClickEvent.runCommand("/uhccommands admin"))
                .hoverEvent(HoverEvent.showText(legacySection().deserialize(lang.get("help.nav-admin-hover", player))));

        player.sendMessage(userButton.append(separator).append(adminButton));
    }
}
