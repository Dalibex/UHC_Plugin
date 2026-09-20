package me.dalibex.UHC_DBasic.utils;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del autocompletado de comandos (CommandTabs).
 * El filtro es lógica pura; NO_SUGGESTIONS ni siquiera toca el sender.
 */
class CommandTabsTest {

    @Test
    void prefixFilter_emptyToken_returnsAll() {
        List<String> options = Arrays.asList("classic", "resource-rush", "solos");
        assertEquals(options, CommandTabs.prefixFilter(options, ""));
    }

    @Test
    void prefixFilter_isCaseInsensitive() {
        List<String> options = Arrays.asList("Classic", "Resource-Rush", "Solos");
        List<String> result = CommandTabs.prefixFilter(options, "res");
        assertEquals(List.of("Resource-Rush"), result);
    }

    @Test
    void prefixFilter_preservesOrder() {
        List<String> options = Arrays.asList("bb", "aa", "bc");
        assertEquals(Arrays.asList("bb", "bc"), CommandTabs.prefixFilter(options, "b"));
    }

    @Test
    void prefixFilter_noMatch_returnsEmpty() {
        List<String> options = Arrays.asList("classic", "solos");
        assertTrue(CommandTabs.prefixFilter(options, "zzz").isEmpty());
    }

    @Test
    void prefixFilter_doesNotMutateInput() {
        List<String> options = Arrays.asList("classic", "solos");
        CommandTabs.prefixFilter(options, "");
        assertEquals(Arrays.asList("classic", "solos"), options);
    }

    @Test
    void noSuggestions_returnsEmptyList() {
        List<String> result = CommandTabs.NO_SUGGESTIONS.onTabComplete(null, null, null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}