package me.dalibex.UHC_DBasic.gamemodes.resourcerush;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns Resource Rush objective pools, active objectives, progress, and podium state. */
public final class ResourceRushObjectiveTracker {

    private static final int TARGET_OBJECTIVE_COUNT = 12;

    private final Map<Integer, List<Material>> poolsByChapter = new HashMap<>();
    private final List<Material> activeObjectives = new ArrayList<>();
    private final Set<Material> activeObjectiveSet = new LinkedHashSet<>();
    private final Map<String, LinkedHashSet<Material>> progressByKey = new HashMap<>();
    private final List<String> podium = new ArrayList<>();
    private final Set<String> podiumSet = new LinkedHashSet<>();

    public ResourceRushObjectiveTracker() {
        reset();
    }

    public void reset() {
        poolsByChapter.clear();
        activeObjectives.clear();
        activeObjectiveSet.clear();
        progressByKey.clear();
        podium.clear();
        podiumSet.clear();
        initializePools();
    }

    private void initializePools() {
        poolsByChapter.put(1, Arrays.asList(Material.DIAMOND_BLOCK, Material.GOLDEN_APPLE, Material.TNT, Material.SPYGLASS, Material.LAVA_BUCKET, Material.SADDLE, Material.ENDER_PEARL, Material.DIAMOND_HOE));
        poolsByChapter.put(2, Arrays.asList(Material.BLAZE_ROD, Material.GHAST_TEAR, Material.BREWING_STAND, Material.JUKEBOX, Material.GLOW_ITEM_FRAME, Material.MUSIC_DISC_TEARS, Material.GOLDEN_CARROT, Material.TARGET));
        poolsByChapter.put(3, Arrays.asList(Material.ANVIL, Material.ENCHANTING_TABLE, Material.PLAYER_HEAD, Material.PAINTING, Material.YELLOW_STAINED_GLASS, Material.MAGMA_CREAM, Material.LEAD, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
        poolsByChapter.put(4, Arrays.asList(Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.DRIED_GHAST, Material.DIAMOND_CHESTPLATE, Material.TURTLE_HELMET, Material.DEEPSLATE_GOLD_ORE, Material.PISTON, Material.FIRE_CHARGE));
        poolsByChapter.put(5, Arrays.asList(Material.NETHERITE_SCRAP, Material.RESPAWN_ANCHOR, Material.CAKE, Material.POISONOUS_POTATO, Material.COMPASS, Material.CROSSBOW, Material.PHANTOM_MEMBRANE, Material.PUMPKIN_PIE));
        poolsByChapter.put(6, Arrays.asList(Material.HONEY_BOTTLE, Material.RAW_GOLD_BLOCK, Material.RABBIT_FOOT, Material.NETHER_WART, Material.MAP, Material.HONEY_BLOCK, Material.CAMPFIRE, Material.DISPENSER));
        poolsByChapter.put(7, Arrays.asList(Material.GILDED_BLACKSTONE, Material.CLOCK, Material.AMETHYST_SHARD, Material.FERMENTED_SPIDER_EYE, Material.RECOVERY_COMPASS, Material.WARPED_FUNGUS_ON_A_STICK, Material.DETECTOR_RAIL, Material.LECTERN));
        poolsByChapter.put(8, Arrays.asList(Material.BEE_NEST, Material.LIGHTNING_ROD, Material.GLOW_BERRIES, Material.BOOKSHELF, Material.NAME_TAG, Material.SOUL_LANTERN, Material.NETHER_WART_BLOCK, Material.DIAMOND_AXE));

        for (List<Material> pool : poolsByChapter.values()) Collections.shuffle(pool);
    }

    public List<Material> addObjectivesForChapter(int chapter) {
        int objectivesToAdd = (chapter <= 3) ? 2 : (chapter <= 9) ? 1 : 0;
        if (objectivesToAdd == 0) return List.of();

        List<Material> chapterPool = poolsByChapter.getOrDefault(chapter, poolsByChapter.get(8));
        if (chapterPool == null) return List.of();

        List<Material> addedObjectives = new ArrayList<>();
        for (Material material : chapterPool) {
            if (addedObjectives.size() >= objectivesToAdd) break;
            if (activeObjectiveSet.add(material)) {
                activeObjectives.add(material);
                addedObjectives.add(material);
            }
        }
        return addedObjectives;
    }

    public ObjectiveCompletion completeObjective(String progressKey, Material material) {
        if (!activeObjectiveSet.contains(material)) return ObjectiveCompletion.notCompleted();

        LinkedHashSet<Material> achievements = progressByKey.computeIfAbsent(progressKey, key -> new LinkedHashSet<>());
        if (!achievements.add(material)) return ObjectiveCompletion.notCompleted();

        int done = achievements.size();
        boolean finished = done >= TARGET_OBJECTIVE_COUNT && podiumSet.add(progressKey);
        if (finished) podium.add(progressKey);
        return new ObjectiveCompletion(true, done, finished);
    }

    public void migrateProgress(String oldKey, String newKey) {
        if (!progressByKey.containsKey(oldKey)) return;
        LinkedHashSet<Material> oldProgress = progressByKey.get(oldKey);
        LinkedHashSet<Material> newProgress = progressByKey.computeIfAbsent(newKey, key -> new LinkedHashSet<>());
        newProgress.addAll(oldProgress);
        progressByKey.remove(oldKey);
    }

    public int progressCount(String progressKey) {
        Set<Material> achievements = progressByKey.get(progressKey);
        return achievements == null ? 0 : achievements.size();
    }

    public List<Material> achievements(String progressKey) {
        Set<Material> achievements = progressByKey.get(progressKey);
        return achievements == null ? List.of() : List.copyOf(achievements);
    }

    public List<Material> activeObjectives() {
        return List.copyOf(activeObjectives);
    }

    public int activeObjectiveCount() {
        return activeObjectives.size();
    }

    public boolean isActiveObjective(Material material) {
        return activeObjectiveSet.contains(material);
    }

    public List<String> podium() {
        return List.copyOf(podium);
    }

    public boolean podiumContains(String key) {
        return podiumSet.contains(key);
    }

    public boolean podiumEmpty() {
        return podium.isEmpty();
    }

    public String firstPodiumKey() {
        return podium.get(0);
    }

    public record ObjectiveCompletion(boolean completed, int done, boolean finished) {
        static ObjectiveCompletion notCompleted() {
            return new ObjectiveCompletion(false, 0, false);
        }
    }
}
