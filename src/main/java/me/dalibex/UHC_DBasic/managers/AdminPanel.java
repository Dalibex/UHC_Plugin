package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.GameRules.*;

public class AdminPanel {

    public static boolean combate18 = false;
    public static boolean bloquearManoSecundaria = false;

    // ------------------------------ DISEÑO PANELES ------------------------------
    public static void openMainAdminPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        Inventory mainGui = Bukkit.createInventory(null, 9, lang.get("menus.main-admin.title"));
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta ameta = pvpItem.getItemMeta();
        ameta.setDisplayName(lang.get("menus.main-admin.combat-item.name"));
        List<String> pvpLore = new ArrayList<>();
        String s18 = combate18 ? lang.get("menus.common.enabled") : lang.get("menus.common.disabled");
        String sOff = bloquearManoSecundaria ? lang.get("admin-messages.state-blocked") : lang.get("admin-messages.state-allowed");

        for (String line : lang.getList("menus.main-admin.combat-item.lore")) {
            pvpLore.add(line.replace("%status18%", s18).replace("%statusOffhand%", sOff));
        }
        ameta.setLore(pvpLore);
        pvpItem.setItemMeta(ameta);
        mainGui.setItem(0, pvpItem);

        ItemStack rulesItem = new ItemStack(Material.BOOK);
        ItemMeta meta = rulesItem.getItemMeta();
        meta.setDisplayName(lang.get("menus.main-admin.rules-item.name"));
        meta.setLore(lang.getList("menus.main-admin.rules-item.lore"));
        rulesItem.setItemMeta(meta);
        mainGui.setItem(2, rulesItem);

        ItemStack borderItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta bMeta = borderItem.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.main-admin.border-item.name"));
        bMeta.setLore(lang.getList("menus.main-admin.border-item.lore"));
        borderItem.setItemMeta(bMeta);
        mainGui.setItem(4, borderItem);

        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta tMeta = timeItem.getItemMeta();
        tMeta.setDisplayName(lang.get("menus.main-admin.time-item.name"));
        tMeta.setLore(lang.getList("menus.main-admin.time-item.lore"));
        timeItem.setItemMeta(tMeta);
        mainGui.setItem(6, timeItem);

        RightPanelManager rpm = plugin.getRightPanelManager();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;

        ItemStack teamItem = new ItemStack(partidaEnCurso ? Material.BARRIER : Material.WHITE_BANNER);
        ItemMeta cMeta = teamItem.getItemMeta();

        if (partidaEnCurso) {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name-locked"));
            List<String> lore = new ArrayList<>();
            lore.add(lang.get("menus.common.locked"));
            lore.add(lang.get("menus.common.locked-lore"));
            cMeta.setLore(lore);
            cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            cMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name"));
            String sizeStr = (teamSize == 1) ? lang.get("menus.main-admin.teams-item.size-solos") :
                    lang.get("menus.main-admin.teams-item.size-teams").replace("%n%", String.valueOf(teamSize));
            int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);

            List<String> lore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.teams-item.lore")) {
                lore.add(line.replace("%size%", sizeStr).replace("%online%", String.valueOf(jugadoresOnline)).replace("%total%", String.valueOf(numEquipos)));
            }
            if (teamSize > 1 && jugadoresOnline % teamSize != 0 && jugadoresOnline > 0) {
                lore.add("");
                lore.add(lang.get("menus.main-admin.teams-item.balance-warning"));
            }
            cMeta.setLore(lore);
        }
        teamItem.setItemMeta(cMeta);
        mainGui.setItem(8, teamItem);

        player.openInventory(mainGui);
    }
    public void openGameRulesPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        Inventory rulesGui = Bukkit.createInventory(null, 36, lang.get("menus.gamerules.title"));

        rulesGui.setItem(10, createRuleItem(Material.GOLDEN_APPLE, lang.get("menus.rules.nat-regen"),
                player.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION), lang));
        rulesGui.setItem(11, createRuleItem(Material.NETHERITE_SWORD, lang.get("menus.rules.pvp"),
                player.getWorld().getGameRuleValue(PVP), lang));
        rulesGui.setItem(12, createRuleItem(Material.PUFFERFISH, lang.get("menus.rules.day-night"),
                player.getWorld().getGameRuleValue(ADVANCE_TIME), lang));
        rulesGui.setItem(13, createRuleItem(Material.ZOMBIE_HEAD, lang.get("menus.rules.monsters"),
                player.getWorld().getGameRuleValue(SPAWN_MONSTERS), lang));
        rulesGui.setItem(14, createRuleItem(Material.CRAFTING_TABLE, lang.get("menus.rules.advancements"),
                player.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES), lang));
        rulesGui.setItem(15, createRuleItem(Material.VILLAGER_SPAWN_EGG, lang.get("menus.rules.trader"),
                player.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS), lang));
        rulesGui.setItem(16, createRuleItem(Material.COMPASS, lang.get("menus.rules.locator"),
                player.getWorld().getGameRuleValue(LOCATOR_BAR), lang));

        // Botón Volver
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.get("menus.common.back"));
        back.setItemMeta(backMeta);
        rulesGui.setItem(27, back);

        player.openInventory(rulesGui);
    }
    public static void openBarrierRulesPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Inventory barrierGui = Bukkit.createInventory(null, 36, lang.get("menus.barrier.title"));

        double currentSize = player.getWorld().getWorldBorder().getSize();

        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(lang.get("menus.barrier.current-size.name"));
        iMeta.setLore(Arrays.asList(lang.get("menus.barrier.current-size.lore").replace("%size%", String.valueOf((int)currentSize))));
        info.setItemMeta(iMeta);
        barrierGui.setItem(13, info);

        barrierGui.setItem(10, createBorderItem(Material.RED_STAINED_GLASS_PANE, "§c-10", -10, lang));
        barrierGui.setItem(11, createBorderItem(Material.RED_WOOL, "§c-100", -100, lang));
        barrierGui.setItem(19, createBorderItem(Material.RED_CONCRETE_POWDER, "§c-500", -500, lang));
        barrierGui.setItem(20, createBorderItem(Material.RED_CONCRETE, "§c-1000", -1000, lang));

        barrierGui.setItem(15, createBorderItem(Material.GREEN_STAINED_GLASS_PANE, "§a+10", 10, lang));
        barrierGui.setItem(16, createBorderItem(Material.GREEN_WOOL, "§a+100", 100, lang));
        barrierGui.setItem(24, createBorderItem(Material.GREEN_CONCRETE_POWDER, "§a+500", 500, lang));
        barrierGui.setItem(25, createBorderItem(Material.GREEN_CONCRETE, "§a+1000", 1000, lang));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.get("menus.common.back"));
        back.setItemMeta(backMeta);
        barrierGui.setItem(31, back);

        player.openInventory(barrierGui);
    }
    public static void openTimePanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Inventory timeGui = Bukkit.createInventory(null, 27, lang.get("menus.time.title"));

        RightPanelManager rpm = plugin.getRightPanelManager();
        boolean estaPausado = rpm.isPausado();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;
        int totalSecs = rpm.getSegundosPorCapitulo();

        String tiempoVisual = String.format("%02dh %02dm %02ds", totalSecs / 3600, (totalSecs % 3600) / 60, totalSecs % 60);

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(lang.get("menus.time.info-item.name"));
        List<String> lore = new ArrayList<>();
        for (String line : lang.getList("menus.time.info-item.lore")) {
            lore.add(line.replace("%time%", tiempoVisual));
        }
        iMeta.setLore(lore);
        info.setItemMeta(iMeta);
        timeGui.setItem(13, info);

        timeGui.setItem(10, createTimeBtn(Material.RED_STAINED_GLASS_PANE, "§c-1 m", -1, partidaEnCurso, lang));
        timeGui.setItem(11, createTimeBtn(Material.RED_WOOL, "§c-5 m", -5, partidaEnCurso, lang));
        timeGui.setItem(12, createTimeBtn(Material.RED_CONCRETE, "§c-10 m", -10, partidaEnCurso, lang));

        timeGui.setItem(14, createTimeBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+1 m", 1, partidaEnCurso, lang));
        timeGui.setItem(15, createTimeBtn(Material.GREEN_WOOL, "§a+5 m", 5, partidaEnCurso, lang));
        timeGui.setItem(16, createTimeBtn(Material.GREEN_CONCRETE, "§a+10 m", 10, partidaEnCurso, lang));

        ItemStack pauseBtn = new ItemStack(estaPausado ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta pMeta = pauseBtn.getItemMeta();
        pMeta.setDisplayName(estaPausado ? lang.get("menus.time.resume") : lang.get("menus.time.pause"));
        pauseBtn.setItemMeta(pMeta);
        timeGui.setItem(21, pauseBtn);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.common.back"));
        back.setItemMeta(bMeta);
        timeGui.setItem(18, back);

        player.openInventory(timeGui);
    }
    // ----------------------------------------------------------------------------

    private static ItemStack createBorderItem(Material mat, String name, int amount, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        meta.setLore(Arrays.asList(lang.get("menus.barrier.change-lore")
                .replace("%amount%", String.valueOf(Math.abs(amount)))));

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createTimeBtn(Material mat, String name, int amount, boolean bloqueado, LanguageManager lang) {
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();

        if (bloqueado) {
            meta.setDisplayName("§7§m" + name);
            meta.setLore(Arrays.asList(lang.get("menus.common.locked"), lang.get("menus.common.locked-lore")));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lang.get("menus.time.change-lore")
                    .replace("%amount%", String.valueOf(Math.abs(amount)))));
        }

        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------ COMBATE 1.8  ------------------------------
    public static void toggleCombate18(Player admin) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        combate18 = !combate18;
        double speedValue = combate18 ? 100.0 : 4.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(speedValue);
        }
        String estado = combate18 ? plugin.getLang().get("admin-messages.state-enabled") : plugin.getLang().get("admin-messages.state-disabled");
        Bukkit.broadcastMessage(plugin.getLang().get("admin-messages.combat-toggle").replace("%prefix%", plugin.getLang().get("general.prefix")).replace("%state%", estado));
    }
    public static void toggleManoSecundaria(Player admin) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        bloquearManoSecundaria = !bloquearManoSecundaria;
        String estado = bloquearManoSecundaria ? plugin.getLang().get("admin-messages.state-blocked") : plugin.getLang().get("admin-messages.state-allowed");
        Bukkit.broadcastMessage(plugin.getLang().get("admin-messages.offhand-toggle").replace("%prefix%", plugin.getLang().get("general.prefix")).replace("%state%", estado));
    }
    // --------------------------------------------------------------------------

    private ItemStack createRuleItem(Material mat, String name, Boolean state, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        String status = state ? lang.get("menus.common.enabled") : lang.get("menus.common.disabled");
        meta.setLore(Arrays.asList(lang.get("menus.common.status") + status, "", lang.get("menus.common.click-to-toggle")));
        item.setItemMeta(meta);
        return item;
    }
}