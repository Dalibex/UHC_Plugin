package me.dalibex.UHC_DBasic.managers.teams;

import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Locale;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Owns team colors, display names, prefixes, and color input normalization. */
public final class TeamPalette {

    private static final Material[] TEAM_DYES = {
            Material.RED_DYE, Material.BLUE_DYE, Material.GREEN_DYE,
            Material.YELLOW_DYE, Material.ORANGE_DYE, Material.PURPLE_DYE,
            Material.CYAN_DYE, Material.PINK_DYE, Material.LIME_DYE,
            Material.LIGHT_BLUE_DYE, Material.MAGENTA_DYE, Material.WHITE_DYE
    };

    private static final String[] TEAM_COLOR_KEYS = {
            "red", "blue", "green", "yellow", "orange", "purple",
            "cyan", "pink", "lime", "light_blue", "magenta", "white"
    };

    private static final NamedTextColor[] TEAM_NAMED_COLORS = {
            NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.DARK_GREEN,
            NamedTextColor.YELLOW, NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_AQUA, NamedTextColor.LIGHT_PURPLE, NamedTextColor.GREEN,
            NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE, NamedTextColor.WHITE
    };

    private static final String[] TEAM_LEGACY_CODES = {
            "§c", "§9", "§2", "§e", "§6", "§5", "§3", "§d", "§a", "§b", "§d", "§f"
    };

    private TeamPalette() {
    }

    public static int size() {
        return TEAM_COLOR_KEYS.length;
    }

    public static String colorKey(int index) {
        return TEAM_COLOR_KEYS[index % TEAM_COLOR_KEYS.length];
    }

    public static Material dye(int index) {
        return TEAM_DYES[index % TEAM_DYES.length];
    }

    public static String legacyCode(int index) {
        return TEAM_LEGACY_CODES[index % TEAM_LEGACY_CODES.length];
    }

    public static Team createTeam(Scoreboard board, LanguageManager lang, int index) {
        String colorKey = colorKey(index);
        Team team = board.registerNewTeam(ScoreboardHelper.TEAM_PREFIX + colorKey);
        team.color(TEAM_NAMED_COLORS[index % TEAM_NAMED_COLORS.length]);

        String localizedName = lang.get("teams.colors." + colorKey, null);
        team.displayName(Component.text(localizedName != null ? localizedName : colorKey));
        applyPrefix(team, lang);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        return team;
    }

    public static void applyPrefix(Team team, LanguageManager lang) {
        String name = legacySection().serialize(team.displayName());
        String prefix = lang.get("teams.prefix-format", null)
                .replace("%color%", legacyCode(indexFor(team)))
                .replace("%name%", name);
        team.prefix(TextUtil.deserialize(prefix));
    }

    public static int indexFor(Team team) {
        String name = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length()).toLowerCase(Locale.ROOT);
        for (int i = 0; i < TEAM_COLOR_KEYS.length; i++) {
            if (TEAM_COLOR_KEYS[i].equalsIgnoreCase(name)) return i;
        }
        return 0;
    }

    public static String normalizeColorInput(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ScoreboardHelper.TEAM_PREFIX)) {
            lower = lower.substring(ScoreboardHelper.TEAM_PREFIX.length());
        }
        return lower;
    }

    public static boolean isDefaultName(Team team, LanguageManager lang, String displayNameToCheck) {
        if (team == null || !isPluginTeam(team)) return true;
        String name = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length()).toLowerCase(Locale.ROOT);
        for (String key : TEAM_COLOR_KEYS) {
            if (key.equalsIgnoreCase(name)) {
                String localizedDefault = lang.get("teams.colors." + key, null);
                return displayNameToCheck.equalsIgnoreCase(localizedDefault) || displayNameToCheck.equalsIgnoreCase(key);
            }
        }
        return name.startsWith("team_");
    }

    public static boolean isPluginTeam(Team team) {
        return team != null && team.getName().startsWith(ScoreboardHelper.TEAM_PREFIX);
    }
}
