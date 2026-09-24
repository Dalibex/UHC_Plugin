package me.dalibex.UHC_DBasic.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Single legacy text parsing helper.
 * legacySection() only treats '§' as a format code, so this class normalizes '&' first.
 */
public final class TextUtil {

    private TextUtil() {
    }

    /** Deserializes legacy text using '&' or '§' as format markers. */
    public static Component deserialize(String legacyOrAmpersand) {
        return legacySection().deserialize(legacyOrAmpersand.replace('&', '§'));
    }

    /** Like deserialize, but disables item italics for MC 1.21+ rendering. */
    public static Component item(String legacyOrAmpersand) {
        return deserialize(legacyOrAmpersand).decoration(TextDecoration.ITALIC, false);
    }

    /** Returns the legacy color code for a TextColor, for example {@code "§c"}. */
    public static String legacyColor(TextColor color) {
        return legacySection().serialize(Component.text('·', color)).replace("·", "");
    }
}
