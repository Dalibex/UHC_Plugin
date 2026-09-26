package me.dalibex.UHC_DBasic.managers.skins;

import me.dalibex.UHC_DBasic.managers.SkinsManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests pure fake-skin assignment and rotation queue policy. */
class SkinRotationTest {

    private static final int MAX_COMBAT_RETRIES = 6;

    @Test
    void assign_needsAtLeastTwo() {
        assertTrue(SkinAssignmentPolicy.assignNewSkins(List.of(), new HashMap<>()).isEmpty());
        assertTrue(SkinAssignmentPolicy.assignNewSkins(List.of("Steve"), new HashMap<>()).isEmpty());
    }

    @Test
    void assign_isPermutationWithoutSelfSkins() {
        List<String> alive = List.of("Steve", "Alex", "Herobrine", "Notch", "Creeper", "P2");
        Map<String, String> last = new HashMap<>();
        last.put("steve", "Alex");
        last.put("alex", "Herobrine");
        last.put("herobrine", "Notch");
        last.put("notch", "Creeper");
        last.put("creeper", "P2");
        last.put("p2", "Steve");

        for (int seed = 0; seed < 10; seed++) {
            Map<String, String> assignment = SkinAssignmentPolicy.assignNewSkins(alive, last, new Random(seed));

            assertEquals(new HashSet<>(alive), new HashSet<>(assignment.values()));
            assertEquals(alive.size(), assignment.size());
            assertEquals(new HashSet<>(alive.stream().map(String::toLowerCase).toList()), assignment.keySet());
            for (String name : alive) assertFalse(assignment.get(name.toLowerCase()).equalsIgnoreCase(name));
        }
    }

    @Test
    void assign_sameSeedProducesSameAssignment() {
        List<String> players = List.of("A", "B", "C", "D", "E");
        assertEquals(SkinAssignmentPolicy.assignNewSkins(players, Map.of(), new Random(42)),
                SkinAssignmentPolicy.assignNewSkins(players, Map.of(), new Random(42)));
    }

    @Test
    void repair_twoSelfAssignmentsSwapsThem() {
        List<String> assigned = new ArrayList<>(List.of("A", "B"));
        SkinAssignmentPolicy.repairSelfAssignments(assigned, List.of("A", "B"));
        assertEquals(List.of("B", "A"), assigned);
    }

    @Test
    void repair_allSelfAssignmentsProducesDerangement() {
        List<String> players = List.of("A", "B", "C", "D");
        List<String> assigned = new ArrayList<>(players);
        SkinAssignmentPolicy.repairSelfAssignments(assigned, players);
        assertEquals(new HashSet<>(players), new HashSet<>(assigned));
        for (int i = 0; i < players.size(); i++) assertFalse(players.get(i).equals(assigned.get(i)));
    }

    @Test
    void repair_oddSelfAssignmentsUsesNonSelfPartner() {
        List<String> players = List.of("A", "B", "C", "D", "E");
        List<String> assigned = new ArrayList<>(List.of("A", "B", "C", "E", "D"));
        SkinAssignmentPolicy.repairSelfAssignments(assigned, players);
        assertEquals(new HashSet<>(players), new HashSet<>(assigned));
        for (int i = 0; i < players.size(); i++) assertFalse(players.get(i).equals(assigned.get(i)));
    }

    @Test
    void rotationEpisodesAreTwoThroughTen() {
        assertFalse(SkinsManager.isRotationEpisode(1));
        assertTrue(SkinsManager.isRotationEpisode(2));
        assertTrue(SkinsManager.isRotationEpisode(10));
        assertFalse(SkinsManager.isRotationEpisode(11));
    }

    @Test
    void next_appliesOnlineNotRevealedNotInCombat() {
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("Steve", "Alex"));
        Map<String, Integer> retries = new HashMap<>();
        Set<String> revealed = new HashSet<>();

        String name = SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> false);

        assertEquals("Steve", name);
        assertEquals(List.of("Alex"), new ArrayList<>(queue));
        assertNull(retries.get("steve"));
    }

    @Test
    void next_dropsOfflinePlayerWithoutApplying() {
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("Offline", "Alex"));
        Map<String, Integer> retries = new HashMap<>();
        Set<String> revealed = new HashSet<>();

        String name = SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> n.equals("Alex"), n -> false);

        assertNull(name);
        assertEquals(List.of("Alex"), new ArrayList<>(queue));
    }

    @Test
    void next_dropsRevealedPlayerWithoutApplying() {
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("steve", "alex"));
        Map<String, Integer> retries = new HashMap<>();
        Set<String> revealed = new HashSet<>(List.of("steve"));

        String name = SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> false);

        assertNull(name);
        assertEquals(List.of("alex"), new ArrayList<>(queue));
    }

    @Test
    void next_requeuesCombatPlayerUntilMaxThenDrops() {
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("Steve"));
        Map<String, Integer> retries = new HashMap<>();
        Set<String> revealed = new HashSet<>();

        for (int attempt = 1; attempt <= MAX_COMBAT_RETRIES; attempt++) {
            assertNull(SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> true));
            assertEquals(attempt, retries.get("steve"));
            assertEquals(attempt < MAX_COMBAT_RETRIES ? 1 : 0, queue.size());
        }

        assertNull(SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> true));
        assertTrue(queue.isEmpty());
    }

    @Test
    void next_appliesWhenCombatEndsWithinRetries() {
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("Steve"));
        Map<String, Integer> retries = new HashMap<>();
        Set<String> revealed = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            assertNull(SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> true));
        }

        String name = SkinAssignmentPolicy.nextNameToSkin(queue, retries, revealed, n -> true, n -> false);

        assertEquals("Steve", name);
        assertTrue(queue.isEmpty());
        assertFalse(retries.containsKey("steve"));
    }

    @Test
    void combatWindow_edges() {
        assertFalse(SkinAssignmentPolicy.isCombatActive(null, 1_000L, 30_000L));
        assertTrue(SkinAssignmentPolicy.isCombatActive(1_000L, 30_999L, 30_000L));
        assertFalse(SkinAssignmentPolicy.isCombatActive(1_000L, 31_000L, 30_000L));
    }

    @Test
    void key_normalizesToLowerCase() {
        assertEquals("steve", SkinAssignmentPolicy.key("Steve"));
        assertEquals("alex", SkinAssignmentPolicy.key("ALEX"));
        assertSame("steve", SkinAssignmentPolicy.key("steve"));
    }
}
