package me.dalibex.UHC_DBasic.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ItemSpriteResolverTest {

    @Test void ordinaryItemUsesItemsAtlasAndMaterialPath() {
        ItemSpriteResolver.SpriteReference sprite = ItemSpriteResolver.defaultReference(
                "minecraft", "breeze_rod", false);

        assertEquals("minecraft:items", sprite.atlas().asString());
        assertEquals("minecraft:item/breeze_rod", sprite.sprite().asString());
    }

    @Test void ordinaryBlockUsesBlocksAtlas() {
        ItemSpriteResolver.SpriteReference sprite = ItemSpriteResolver.defaultReference(
                "minecraft", "diamond_block", true);

        assertEquals("minecraft:blocks", sprite.atlas().asString());
        assertEquals("minecraft:block/diamond_block", sprite.sprite().asString());
    }

    @Test void dynamicAndModelExceptionsUseExistingVanillaSprites() {
        assertEquals("minecraft:item/recovery_compass_16",
                ItemSpriteResolver.resolve(Material.RECOVERY_COMPASS).sprite().asString());
        assertEquals("minecraft:item/brewing_stand",
                ItemSpriteResolver.resolve(Material.BREWING_STAND).sprite().asString());
        assertEquals("minecraft:block/damaged_anvil_top",
                ItemSpriteResolver.resolve(Material.DAMAGED_ANVIL).sprite().asString());
        assertEquals("minecraft:block/ancient_debris_side",
                ItemSpriteResolver.resolve(Material.ANCIENT_DEBRIS).sprite().asString());
        assertEquals("minecraft:container/slot/helmet",
                ItemSpriteResolver.resolve(Material.PLAYER_HEAD).sprite().asString());
    }

    @Test void airCannotBeDisplayed() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemSpriteResolver.resolve(Material.AIR));
    }
}
