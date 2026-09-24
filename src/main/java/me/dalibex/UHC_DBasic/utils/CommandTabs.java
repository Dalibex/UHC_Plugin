package me.dalibex.UHC_DBasic.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared command tab-completion helpers. */
public final class CommandTabs {

    private CommandTabs() {
    }

    /** Empty TabCompleter, preventing default player-name suggestions. */
    public static final TabCompleter NO_SUGGESTIONS = new TabCompleter() {
        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
            return new ArrayList<>();
        }
    };

    /** Filters options by case-insensitive prefix, returning all options for an empty token. */
    public static List<String> prefixFilter(List<String> options, String token) {
        String t = token.toLowerCase(Locale.ROOT);
        Predicate<String> match = o -> o.toLowerCase(Locale.ROOT).startsWith(t);
        List<String> result = new ArrayList<>();
        if (t.isEmpty()) {
            result.addAll(options);
            return result;
        }
        for (String option : options) {
            if (match.test(option)) result.add(option);
        }
        return result;
    }
}
