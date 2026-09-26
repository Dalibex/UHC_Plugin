package me.dalibex.UHC_DBasic.admin;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Builds reusable Admin Panel items. */
class AdminItemFactory {

    private final UHC_DBasic plugin;

    AdminItemFactory(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    Component text(String legacy) {
        return TextUtil.item(legacy);
    }

    ItemStack backButton(Player player) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(plugin.getLang().getComponent("menus.common.back", player));
        back.setItemMeta(meta);
        return back;
    }

    ItemStack lockedItem(Player player, String displayNameKey) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(displayNameKey, player));
        meta.lore(List.of(plugin.getLang().getComponent("menus.common.locked", player),
                plugin.getLang().getComponent("menus.common.locked-lore", player)));
        item.setItemMeta(meta);
        return item;
    }

    ItemStack simpleItem(Material material, String langKey, Player player) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(langKey + ".name", player));
        meta.lore(plugin.getLang().getComponentList(langKey + ".lore", player));
        item.setItemMeta(meta);
        return item;
    }

    ItemStack shulkerButton(Material material, String key, boolean enabled, int episode, Player player) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(key + ".name", player));

        List<Component> lore = new ArrayList<>();
        String status = enabled ? plugin.getLang().get("menus.common.enabled", player)
                : plugin.getLang().get("menus.common.disabled", player);
        for (String line : plugin.getLang().getList(key + ".lore", player)) {
            lore.add(text(line.replace("%status%", status).replace("%episode%", String.valueOf(episode))));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack borderButton(Material material, String name, int amount, boolean locked, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(locked ? Material.BARRIER : material);
        ItemMeta meta = item.getItemMeta();
        if (locked) {
            meta.displayName(text("§7§m" + name));
            meta.lore(List.of(lang.getComponent("menus.common.locked", player), lang.getComponent("game.border-not-started", player)));
        } else {
            meta.displayName(text(name));
            meta.lore(List.of(text(lang.get("menus.barrier.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount))))));
        }
        item.setItemMeta(meta);
        return item;
    }

    ItemStack timeButton(Material material, String name, int amount, boolean locked, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(locked ? Material.BARRIER : material);
        ItemMeta meta = item.getItemMeta();
        if (locked) {
            meta.displayName(text("§7§m" + name));
            meta.lore(List.of(lang.getComponent("menus.common.locked", player), lang.getComponent("menus.common.locked-lore", player)));
        } else {
            meta.displayName(text(name));
            meta.lore(List.of(text(lang.get("menus.time.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount))))));
        }
        item.setItemMeta(meta);
        return item;
    }

    ItemStack ruleItem(Material material, String name, Boolean state, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(name));
        String status = state ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        meta.lore(Arrays.asList(text(lang.get("menus.common.status", player) + status), Component.empty(),
                lang.getComponent("menus.common.click-to-toggle", player)));
        item.setItemMeta(meta);
        return item;
    }

    ItemStack gamemodeItem(Material material, String modeKey, boolean selected, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = lang.get("menus.gamemode." + modeKey + ".name", player);
        meta.displayName(text((selected ? "§6§l" : "§f") + name));

        List<Component> lore = new ArrayList<>();
        if (selected) {
            lore.add(lang.getComponent("menus.gamemode.selected-status", player));
            lore.add(Component.empty());
            for (String line : lang.getList("menus.gamemode." + modeKey + ".lore", player)) lore.add(text(line));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(lang.getComponent("menus.gamemode.not-selected-status", player));
            lore.add(Component.empty());
            for (String line : lang.getList("menus.gamemode." + modeKey + ".lore", player)) lore.add(text(line));
            lore.add(Component.empty());
            lore.add(lang.getComponent("menus.gamemode.click-to-select", player));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    void populateEpisodeSelector(org.bukkit.inventory.Inventory inv, Player player, LanguageManager lang,
                                 Material infoMaterial, String menuKey, int selected, boolean locked,
                                 int infoSlot, int[] episodeSlots, int backSlot, int shulkerNumber) {
        inv.setItem(infoSlot, episodeInfoItem(infoMaterial, menuKey, selected, player, lang, shulkerNumber));
        for (int i = 0; i < episodeSlots.length; i++) {
            inv.setItem(episodeSlots[i], episodeButton(i + 1, selected, locked, player, lang, menuKey));
        }
        inv.setItem(backSlot, backButton(player));
    }

    private ItemStack episodeInfoItem(Material material, String menuKey, int selected, Player player,
                                      LanguageManager lang, int shulkerNumber) {
        ItemStack info = new ItemStack(material);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(lang.getComponent(menuKey + ".info.name", player));

        List<Component> lore = new ArrayList<>();
        for (String line : lang.getList(menuKey + ".info.lore", player)) {
            String text = line.replace("%episode%", String.valueOf(selected));
            if (shulkerNumber > 0) text = text.replace("%shulker%", String.valueOf(shulkerNumber));
            lore.add(text(text));
        }

        meta.lore(lore);
        info.setItemMeta(meta);
        return info;
    }

    private ItemStack episodeButton(int episode, int selected, boolean locked, Player player, LanguageManager lang, String menuKey) {
        boolean isSelected = episode == selected;
        ItemStack item = new ItemStack(isSelected ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text((isSelected ? "§6§l" : "§f")
                + lang.get(menuKey + ".button.name", player).replace("%episode%", String.valueOf(episode))));

        List<Component> lore = new ArrayList<>();
        for (String line : lang.getList(menuKey + ".button.lore", player)) {
            lore.add(text(line.replace("%episode%", String.valueOf(episode))));
        }
        if (isSelected) {
            lore.add(Component.empty());
            lore.add(lang.getComponent(menuKey + ".selected-status", player));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (locked) {
            lore.add(Component.empty());
            lore.add(lang.getComponent("menus.common.locked", player));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
