package me.dalibex.UHC_DBasic.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Utilidad única de parseo legado.
 *
 * `legacySection()` solo entiende el carácter '§' como código de formato.
 * Esta clase normaliza '&' -> '§' antes de deserializar para que ningún
 * texto quede mostrando el '&' literal y para que el proyecto no dependa
 * de recordar esa conversión en cada call-site.
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * Deserializa una cadena legacy usando '&' o '§' como carácter de formato.
     */
    public static Component deserialize(String legacyOrAmpersand) {
        return legacySection().deserialize(legacyOrAmpersand.replace('&', '§'));
    }

    /**
     * Como {@link #deserialize(String)} pero desactivando la cursiva.
     * A partir de MC 1.21 los ítems se muestran en cursiva salvo que el
     * Component declare TextDecoration.ITALIC=false explícitamente.
     */
    public static Component item(String legacyOrAmpersand) {
        return deserialize(legacyOrAmpersand).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Devuelve el código legacy (§+letra/hex) de un {@link TextColor},
     * por ejemplo {@code "§c"} para rojo.
     */
    public static String legacyColor(TextColor color) {
        return legacySection().serialize(Component.text('·', color)).replace("·", "");
    }
}