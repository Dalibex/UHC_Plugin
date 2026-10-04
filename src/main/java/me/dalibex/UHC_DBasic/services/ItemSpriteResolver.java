package me.dalibex.UHC_DBasic.services;

import java.util.Map;

import org.bukkit.Material;

import net.kyori.adventure.key.Key;

/** Resolves vanilla materials to sprites available in the client atlases. */
public final class ItemSpriteResolver {

    static final Key ITEMS_ATLAS = Key.key("minecraft:items");
    static final Key BLOCKS_ATLAS = Key.key("minecraft:blocks");
    static final Key GUI_ATLAS = Key.key("minecraft:gui");

    private static final Map<Material, SpriteReference> EXCEPTIONS = Map.ofEntries(
            Map.entry(Material.ANVIL, block("anvil")),
            Map.entry(Material.ANCIENT_DEBRIS, block("ancient_debris_side")),
            Map.entry(Material.CHIPPED_ANVIL, block("chipped_anvil_top")),
            Map.entry(Material.DAMAGED_ANVIL, block("damaged_anvil_top")),
            Map.entry(Material.BEE_NEST, block("bee_nest_front")),
            Map.entry(Material.BREWING_STAND, item("brewing_stand")),
            Map.entry(Material.CAKE, item("cake")),
            Map.entry(Material.CAMPFIRE, item("campfire")),
            Map.entry(Material.CLOCK, item("clock_00")),
            Map.entry(Material.COMPASS, item("compass_16")),
            Map.entry(Material.CROSSBOW, item("crossbow_standby")),
            Map.entry(Material.DISPENSER, block("dispenser_front")),
            Map.entry(Material.DRIED_GHAST, block("dried_ghast_hydration_0_south")),
            Map.entry(Material.ENCHANTING_TABLE, block("enchanting_table_top")),
            Map.entry(Material.HONEY_BLOCK, block("honey_block_side")),
            Map.entry(Material.JUKEBOX, block("jukebox_side")),
            Map.entry(Material.LECTERN, block("lectern_front")),
            Map.entry(Material.PISTON, block("piston_side")),
            Map.entry(Material.PLAYER_HEAD, gui("container/slot/helmet")),
            Map.entry(Material.RECOVERY_COMPASS, item("recovery_compass_16")),
            Map.entry(Material.RESPAWN_ANCHOR, block("respawn_anchor_side0")),
            Map.entry(Material.SOUL_LANTERN, item("soul_lantern")),
            Map.entry(Material.TARGET, block("target_side")),
            Map.entry(Material.TNT, block("tnt_side"))
    );

    private ItemSpriteResolver() {
    }

    public static SpriteReference resolve(Material material) {
        requireDisplayableItem(material);
        SpriteReference exception = EXCEPTIONS.get(material);
        if (exception != null) return exception;

        return defaultReference(material.key().namespace(), material.key().value(), material.isBlock());
    }

    public static String translationKey(Material material) {
        requireDisplayableItem(material);
        return material.translationKey();
    }

    private static void requireDisplayableItem(Material material) {
        if (material == null || material == Material.AIR
                || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            throw new IllegalArgumentException("Material must be a displayable item: " + material);
        }
    }

    static SpriteReference defaultReference(String namespace, String id, boolean blockMaterial) {
        String folder = blockMaterial ? "block/" : "item/";
        return new SpriteReference(blockMaterial ? BLOCKS_ATLAS : ITEMS_ATLAS,
                Key.key(namespace, folder + id));
    }

    private static SpriteReference item(String path) {
        return new SpriteReference(ITEMS_ATLAS, Key.key("minecraft", "item/" + path));
    }

    private static SpriteReference block(String path) {
        return new SpriteReference(BLOCKS_ATLAS, Key.key("minecraft", "block/" + path));
    }

    private static SpriteReference gui(String path) {
        return new SpriteReference(GUI_ATLAS, Key.key("minecraft", path));
    }

    public record SpriteReference(Key atlas, Key sprite) {
    }
}
