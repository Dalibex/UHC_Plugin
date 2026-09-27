package me.dalibex.UHC_DBasic.managers.skins;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/** Pure identity-assignment and rotation queue policy for fake skins. */
public final class SkinAssignmentPolicy {

    private static final int SHUFFLE_ATTEMPTS = 30;
    private static final int SHUFFLE_FALLBACK_ATTEMPTS = 100;
    private static final int MAX_COMBAT_RETRIES = 6;

    private SkinAssignmentPolicy() {
    }

    public static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    public static boolean isCombatActive(Long since, long now, long windowMs) {
        return since != null && now - since < windowMs;
    }

    public static Map<String, String> assignNewSkins(List<String> aliveNames, Map<String, String> lastByName) {
        return assignNewSkins(aliveNames, lastByName, new Random());
    }

    public static Map<String, String> assignNewSkins(List<String> aliveNames, Map<String, String> lastByName, Random random) {
        Map<String, String> assignment = new HashMap<>();
        if (aliveNames.size() < 2) return assignment;

        List<String> assigned = new ArrayList<>(aliveNames);
        boolean valid = false;
        int attempts = 0;
        while (!valid && attempts < SHUFFLE_ATTEMPTS) {
            Collections.shuffle(assigned, random);
            valid = true;
            for (int i = 0; i < aliveNames.size(); i++) {
                String skin = assigned.get(i);
                if (skin.equalsIgnoreCase(aliveNames.get(i))
                        || skin.equalsIgnoreCase(lastByName.get(key(aliveNames.get(i))))) {
                    valid = false;
                    break;
                }
            }
            attempts++;
        }

        if (!valid) {
            for (int i = 0; i < SHUFFLE_FALLBACK_ATTEMPTS && !valid; i++) {
                Collections.shuffle(assigned, random);
                valid = true;
                for (int j = 0; j < aliveNames.size(); j++) {
                    if (assigned.get(j).equalsIgnoreCase(aliveNames.get(j))) {
                        valid = false;
                        break;
                    }
                }
            }
        }

        repairSelfAssignments(assigned, aliveNames);
        for (int i = 0; i < aliveNames.size(); i++) assignment.put(key(aliveNames.get(i)), assigned.get(i));
        return assignment;
    }

    public static Map<String, String> assignNewSkins(List<String> aliveNames, List<String> skinSources,
            Map<String, String> lastByName, Random random) {
        Map<String, String> assignment = new HashMap<>();
        if (aliveNames.size() < 2) return assignment;

        List<String> sources = distinctNames(skinSources);
        if (sources.isEmpty()) return assignment;

        Map<String, String> strict = buildSourceAssignment(aliveNames, sources, lastByName, random, true);
        if (strict.size() == aliveNames.size()) return strict;
        Map<String, String> loose = buildSourceAssignment(aliveNames, sources, lastByName, random, false);
        return loose.size() > strict.size() ? loose : strict;
    }

    private static List<String> distinctNames(List<String> names) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            if (seen.add(key(name))) result.add(name);
        }
        return result;
    }

    private static Map<String, String> buildSourceAssignment(List<String> aliveNames, List<String> sources,
            Map<String, String> lastByName, Random random, boolean avoidPrevious) {
        boolean uniqueSources = sources.size() >= aliveNames.size();
        Map<String, String> bestAssignment = Map.of();
        for (int attempt = 0; attempt < SHUFFLE_FALLBACK_ATTEMPTS; attempt++) {
            List<String> shuffled = new ArrayList<>(sources);
            Collections.shuffle(shuffled, random);
            Map<String, String> assignment = new HashMap<>();
            Set<String> usedSources = new HashSet<>();

            for (String playerName : aliveNames) {
                String picked = pickSource(playerName, shuffled, usedSources, uniqueSources, lastByName, avoidPrevious);
                if (picked == null) continue;
                assignment.put(key(playerName), picked);
                if (uniqueSources) usedSources.add(key(picked));
            }
            if (assignment.size() == aliveNames.size()) return assignment;
            if (assignment.size() > bestAssignment.size()) bestAssignment = assignment;
        }
        return bestAssignment;
    }

    private static String pickSource(String playerName, List<String> sources, Set<String> usedSources,
            boolean uniqueSources, Map<String, String> lastByName, boolean avoidPrevious) {
        String playerKey = key(playerName);
        String previous = lastByName.get(playerKey);
        for (String source : sources) {
            String sourceKey = key(source);
            if (uniqueSources && usedSources.contains(sourceKey)) continue;
            if (source.equalsIgnoreCase(playerName)) continue;
            if (avoidPrevious && previous != null && source.equalsIgnoreCase(previous)) continue;
            return source;
        }
        if (avoidPrevious) return pickSource(playerName, sources, usedSources, uniqueSources, lastByName, false);
        return null;
    }

    public static void repairSelfAssignments(List<String> assigned, List<String> aliveNames) {
        List<Integer> selfAssigned = new ArrayList<>();
        for (int i = 0; i < aliveNames.size(); i++) {
            if (assigned.get(i).equalsIgnoreCase(aliveNames.get(i))) selfAssigned.add(i);
        }
        if (selfAssigned.isEmpty()) return;

        for (int i = 0; i + 1 < selfAssigned.size(); i += 2) {
            Collections.swap(assigned, selfAssigned.get(i), selfAssigned.get(i + 1));
        }

        if (selfAssigned.size() % 2 == 1) {
            int solo = selfAssigned.get(selfAssigned.size() - 1);
            for (int i = 0; i < aliveNames.size(); i++) {
                if (i != solo && !assigned.get(i).equalsIgnoreCase(aliveNames.get(solo))) {
                    Collections.swap(assigned, solo, i);
                    break;
                }
            }
        }
    }

    public static String nextNameToSkin(ArrayDeque<String> queue, Map<String, Integer> retries,
            Set<String> revealed, Predicate<String> isOnline, Predicate<String> isInCombat) {
        String name = queue.poll();
        if (name == null) return null;
        if (!isOnline.test(name)) return null;
        if (revealed.contains(key(name))) return null;
        if (isInCombat.test(name)) {
            int attempt = retries.merge(key(name), 1, Integer::sum);
            if (attempt < MAX_COMBAT_RETRIES) queue.addLast(name);
            return null;
        }
        retries.remove(key(name));
        return name;
    }
}
