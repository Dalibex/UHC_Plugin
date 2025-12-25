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

import java.util.Arrays;

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
        ItemStack goldenHead = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = goldenHead.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§l§k! §e§lGOLDEN HEAD §6§l§k!");

            meta.setLore(Arrays.asList(
                    "§7§m-----------------------",
                    "§6§lArtefacto Legendario",
                    "§7Contiene la esencia de un caído.",
                    "",
                    "§6Efectos de Consumo:",
                    " §8» §fRegeneración §eII §7(12s)",
                    " §8» §fAbsorción §6II §7(5min)",
                    "§7§m-----------------------"
            ));

            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            goldenHead.setItemMeta(meta);
        }

        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
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
}
