package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class SpecialCraftsManager {

    private final UHC_DBasic plugin;

    public SpecialCraftsManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        registrarRecetas();
    }

    private void registrarRecetas() {
        registrarGoldenHead();
        // ...
    }

    private void registrarGoldenHead() {
        LanguageManager lang = plugin.getLang();
        ItemStack goldenHead = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = goldenHead.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(lang.get("crafts.golden-head.name"));
            meta.setLore(lang.getList("crafts.golden-head.lore"));

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            goldenHead.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        if (Bukkit.getRecipe(key) != null) {
            Bukkit.removeRecipe(key);
        }

        ShapedRecipe recipe = new ShapedRecipe(key, goldenHead);

        /*
          G G G
          G H G  G = Oro, H = Cabeza de jugador
          G G G
         */
        recipe.shape("GGG", "GHG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('H', Material.PLAYER_HEAD);

        Bukkit.addRecipe(recipe);
    }

    // Por cambio de idioma
    public void actualizarReceta() {
        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        if (Bukkit.getRecipe(key) != null) {
            Bukkit.removeRecipe(key);
        }
        registrarGoldenHead();
    }
}