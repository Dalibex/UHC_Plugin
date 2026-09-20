package me.dalibex.UHC_DBasic.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pruebas del parseo legacy centralizado (TextUtil).
 */
class TextUtilTest {

    @Test
    void deserialize_parsesAmpersandColors() {
        assertEquals("Hola", plainText().serialize(TextUtil.deserialize("&aHola")));
    }

    @Test
    void deserialize_acceptsLegacySectionCharacters() {
        assertEquals("Mundo", plainText().serialize(TextUtil.deserialize("§2Mundo")));
    }

    @Test
    void deserialize_emptyString() {
        assertEquals("", plainText().serialize(TextUtil.deserialize("")));
    }

    @Test
    void item_disablesItalic() {
        Component c = TextUtil.item("&lTitulo");
        assertEquals(TextDecoration.State.FALSE, c.decoration(TextDecoration.ITALIC));
    }

    @Test
    void legacyColor_namedColors() {
        assertEquals("§c", TextUtil.legacyColor(NamedTextColor.RED));
        assertEquals("§f", TextUtil.legacyColor(NamedTextColor.WHITE));
        assertEquals("§4", TextUtil.legacyColor(NamedTextColor.DARK_RED));
    }

    @Test
    void legacyColor_customHexDoesNotThrow() {
        // El serializer legacy por defecto no emite códigos hex: basta con que el
        // helper no lance y nunca devuelva texto con el marcador interno.
        String result = TextUtil.legacyColor(TextColor.color(0x123456));
        assertNotNull(result);
        assertFalse(result.contains("·"));
    }
}