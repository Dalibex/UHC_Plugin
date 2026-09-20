package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la lógica pura de rotación de skines (SkinsManager):
 * asignación de identidades y paso de la cola de rotación.
 */
class SkinRotationTest {

    private static final int COMBATE_REINTENTOS_MAX = 6;

    // ---------------------------------------------------------------- asignacion

    @Test
    void assign_needsAtLeastTwo() {
        assertTrue(SkinsManager.assignNewSkins(List.of(), new HashMap<>()).isEmpty());
        assertTrue(SkinsManager.assignNewSkins(List.of("Steve"), new HashMap<>()).isEmpty());
    }

    @Test
    void assign_isPermutationWithoutSelfSkins() {
        List<String> vivos = List.of("Steve", "Alex", "Herobrine", "Notch", "Creeper", "P2");
        Map<String, String> ultima = new HashMap<>();
        ultima.put("steve", "Alex");
        ultima.put("alex", "Herobrine");
        ultima.put("herobrine", "Notch");
        ultima.put("notch", "Creeper");
        ultima.put("creeper", "P2");
        ultima.put("p2", "Steve");

        // El barajado es aleatorio: repetimos para cubrir varios resultados.
        for (int r = 0; r < 50; r++) {
            Map<String, String> asignacion = SkinsManager.assignNewSkins(vivos, ultima);

            assertEquals(new HashSet<>(vivos), new HashSet<>(asignacion.values()),
                    "La asignación debe usar todos (y solo) los nombres de vivos");
            assertEquals(vivos.size(), asignacion.size());
            Set<String> clavesEsperadas = new HashSet<>(vivos.stream().map(String::toLowerCase).toList());
            assertEquals(clavesEsperadas, asignacion.keySet(),
                    "Las claves se guardan siempre en minúsculas");

            for (String v : vivos) {
                assertFalse(asignacion.get(v.toLowerCase()).equalsIgnoreCase(v),
                        "Nadie debe llevar su propia skin (" + v + ")");
            }
        }
    }

    // ------------------------------------------------------------ paso de cola

    @Test
    void next_appliesOnlineNotRevealedNotInCombat() {
        ArrayDeque<String> cola = new ArrayDeque<>(List.of("Steve", "Alex"));
        Map<String, Integer> reintentos = new HashMap<>();
        Set<String> revelados = new HashSet<>();

        String nombre = SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> false);

        assertEquals("Steve", nombre);
        assertEquals(List.of("Alex"), new ArrayList<>(cola));
        assertNull(reintentos.get("steve"));
    }

    @Test
    void next_dropsOfflinePlayerWithoutApplying() {
        ArrayDeque<String> cola = new ArrayDeque<>(List.of("Offline", "Alex"));
        Map<String, Integer> reintentos = new HashMap<>();
        Set<String> revelados = new HashSet<>();

        String nombre = SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> n.equals("Alex"), n -> false);

        assertNull(nombre);
        assertEquals(List.of("Alex"), new ArrayList<>(cola));
    }

    @Test
    void next_dropsRevealedPlayerWithoutApplying() {
        ArrayDeque<String> cola = new ArrayDeque<>(List.of("steve", "alex"));
        Map<String, Integer> reintentos = new HashMap<>();
        Set<String> revelados = new HashSet<>(List.of("steve"));

        String nombre = SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> false);

        assertNull(nombre);
        assertEquals(List.of("alex"), new ArrayList<>(cola));
    }

    @Test
    void next_requeuesCombatPlayerUntilMaxThenDrops() {
        ArrayDeque<String> cola = new ArrayDeque<>(List.of("Steve"));
        Map<String, Integer> reintentos = new HashMap<>();
        Set<String> revelados = new HashSet<>();

        // En combate permanente: se reinserta hasta el tope de reintentos.
        for (int i = 1; i <= COMBATE_REINTENTOS_MAX; i++) {
            assertNull(SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> true));
            assertEquals(i, reintentos.get("steve"));
            assertEquals(i < COMBATE_REINTENTOS_MAX ? 1 : 0, cola.size(),
                    "Se descarta al llegar al tope (no se reinserta)");
        }

        // Con la cola vacía ya no hay nada que aplicar.
        assertNull(SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> true));
        assertTrue(cola.isEmpty());
    }

    @Test
    void next_appliesWhenCombatEndsWithinRetries() {
        ArrayDeque<String> cola = new ArrayDeque<>(List.of("Steve"));
        Map<String, Integer> reintentos = new HashMap<>();
        Set<String> revelados = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            assertNull(SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> true));
        }

        String nombre = SkinsManager.nextNameToSkin(cola, reintentos, revelados, n -> true, n -> false);

        assertEquals("Steve", nombre);
        assertTrue(cola.isEmpty());
        assertFalse(reintentos.containsKey("steve"));
    }

    // ---------------------------------------------------------- ventana combate

    @Test
    void combatWindow_edges() {
        assertFalse(SkinsManager.isCombatActive(null, 1_000L, 30_000L));
        assertTrue(SkinsManager.isCombatActive(1_000L, 30_999L, 30_000L));
        assertFalse(SkinsManager.isCombatActive(1_000L, 31_000L, 30_000L));
    }

    @Test
    void key_normalizesToLowerCase() {
        assertEquals("steve", SkinsManager.key("Steve"));
        assertEquals("alex", SkinsManager.key("ALEX"));
        assertSame("steve", SkinsManager.key("steve"));
    }
}