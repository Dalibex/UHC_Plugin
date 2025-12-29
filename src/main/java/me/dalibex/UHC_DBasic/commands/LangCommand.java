package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
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
        LanguageManager lang = plugin.getLang();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("general.only-players", null));
            return true;
        }

        String langCode = validarTodo(player, args);
        if (langCode == null) return true;

        lang.setPlayerLanguage(player, langCode);

        if (player.getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null) {
            plugin.getRightPanelManager().actualizarScoreboard(player, "00:00", "00:00", false);
        }

        String prefix = lang.get("general.prefix", player);
        String confirmMsg = lang.get("lang.switch", player).replace("%prefix%", prefix);
        player.sendMessage(confirmMsg);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        return true;
    }

    /**
     * Valida argumentos, estado del juego y disponibilidad del idioma.
     * @return El código del idioma si es válido, null si falla.
     */
    private String validarTodo(Player player, String[] args) {
        LanguageManager lang = plugin.getLang();
        String errorPrefix = lang.get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(lang.get("lang.usage", player).replace("%error-prefix%", errorPrefix));
            return null;
        }

        if (plugin.getRightPanelManager().getTiempoTotalSegundos() > 0) {
            player.sendMessage(lang.get("lang.already-started", player).replace("%error-prefix%", errorPrefix));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return null;
        }

        String targetLang = args[0].toLowerCase();
        if (!targetLang.equals("es") && !targetLang.equals("en")) {
            player.sendMessage(lang.get("lang.invalid", player).replace("%error-prefix%", errorPrefix));
            return null;
        }

        return targetLang;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("es", "en");
        }
        return null;
    }
}