package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.dalibex.UHC_DBasic.UHC_DBasic;

public class SpecialCraftsManager {

    private final UHC_DBasic plugin;
    private final NamespacedKey goldenHeadKey;

    public SpecialCraftsManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.goldenHeadKey = new NamespacedKey(plugin, "golden_head_item");
        registerRecipes();
    }

    private void registerRecipes() {
        registerGoldenHead();
    }

    private void registerGoldenHead() {
        LanguageManager lang = plugin.getLang();
        ItemStack goldenHead = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = goldenHead.getItemMeta();

        // Al ser una receta global, usamos null para obtener el idioma por defecto de la config
        if (meta != null) {
            meta.displayName(lang.getComponent("crafts.golden-head.name", null));
            meta.lore(lang.getComponentList("crafts.golden-head.lore", null));

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(goldenHeadKey, PersistentDataType.BYTE, (byte) 1);

            goldenHead.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        if (Bukkit.getRecipe(key) != null) {
            Bukkit.removeRecipe(key);
        }

        ShapedRecipe recipe = new ShapedRecipe(key, goldenHead);

        recipe.shape("GGG", "GHG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('H', Material.PLAYER_HEAD);

        Bukkit.addRecipe(recipe);
    }

    public void updateRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        if (Bukkit.getRecipe(key) != null) {
            Bukkit.removeRecipe(key);
        }
        registerGoldenHead();
    }

    public boolean isGoldenHead(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_APPLE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(goldenHeadKey, PersistentDataType.BYTE);
    }
}
