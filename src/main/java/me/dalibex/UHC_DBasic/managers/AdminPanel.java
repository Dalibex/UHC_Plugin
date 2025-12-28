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
    private boolean shulkerOneEnabled = true;
    private boolean shulkerTwoEnabled = true;

    // ------------------------------ DISEÑO PANELES ------------------------------
    public static void openMainAdminPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        Inventory mainGui = Bukkit.createInventory(null, 9, lang.get("menus.main-admin.title", player));
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta ameta = pvpItem.getItemMeta();
        ameta.setDisplayName(lang.get("menus.main-admin.combat-item.name", player));
        List<String> pvpLore = new ArrayList<>();
        String s18 = combate18 ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        String sOff = bloquearManoSecundaria ? lang.get("admin-messages.state-blocked", player) : lang.get("admin-messages.state-allowed", player);

        for (String line : lang.getList("menus.main-admin.combat-item.lore", player)) {
            pvpLore.add(line.replace("%status18%", s18).replace("%statusOffhand%", sOff));
        }
        ameta.setLore(pvpLore);
        pvpItem.setItemMeta(ameta);
        mainGui.setItem(0, pvpItem);

        ItemStack rulesItem = new ItemStack(Material.BOOK);
        ItemMeta meta = rulesItem.getItemMeta();
        meta.setDisplayName(lang.get("menus.main-admin.rules-item.name", player));
        meta.setLore(lang.getList("menus.main-admin.rules-item.lore", player));
        rulesItem.setItemMeta(meta);
        mainGui.setItem(2, rulesItem);

        ItemStack generalRulesItem = new ItemStack(Material.BELL);
        ItemMeta xmeta = generalRulesItem.getItemMeta();
        xmeta.setDisplayName(lang.get("menus.main-admin.general-rules-item.name", player));
        xmeta.setLore(lang.getList("menus.main-admin.general-rules-item.lore", player));
        generalRulesItem.setItemMeta(xmeta);
        mainGui.setItem(1, generalRulesItem);

        ItemStack borderItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta bMeta = borderItem.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.main-admin.border-item.name", player));
        bMeta.setLore(lang.getList("menus.main-admin.border-item.lore", player));
        borderItem.setItemMeta(bMeta);
        mainGui.setItem(4, borderItem);

        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta tMeta = timeItem.getItemMeta();
        tMeta.setDisplayName(lang.get("menus.main-admin.time-item.name", player));
        tMeta.setLore(lang.getList("menus.main-admin.time-item.lore", player));
        timeItem.setItemMeta(tMeta);
        mainGui.setItem(6, timeItem);

        RightPanelManager rpm = plugin.getRightPanelManager();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;

        ItemStack teamItem = new ItemStack(partidaEnCurso ? Material.BARRIER : Material.WHITE_BANNER);
        ItemMeta cMeta = teamItem.getItemMeta();

        if (partidaEnCurso) {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name-locked", player));
            List<String> lore = new ArrayList<>();
            lore.add(lang.get("menus.common.locked", player));
            lore.add(lang.get("menus.common.locked-lore", player));
            cMeta.setLore(lore);
            cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            cMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name", player));
            String sizeStr = (teamSize == 1) ? lang.get("menus.main-admin.teams-item.size-solos", player) :
                    lang.get("menus.main-admin.teams-item.size-teams", player).replace("%n%", String.valueOf(teamSize));
            int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);

            List<String> lore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.teams-item.lore", player)) {
                lore.add(line.replace("%size%", sizeStr).replace("%online%", String.valueOf(jugadoresOnline)).replace("%total%", String.valueOf(numEquipos)));
            }
            if (teamSize > 1 && jugadoresOnline % teamSize != 0 && jugadoresOnline > 0) {
                lore.add("");
                lore.add(lang.get("menus.main-admin.teams-item.balance-warning", player));
            }
            cMeta.setLore(lore);
        }
        teamItem.setItemMeta(cMeta);
        mainGui.setItem(8, teamItem);

        player.openInventory(mainGui);
    }

    public void openGeneralRulesPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, lang.get("menus.generalrules.title", player));

        ItemStack pouch = new ItemStack(Material.ORANGE_SHULKER_BOX);
        ItemMeta pMeta = pouch.getItemMeta();
        pMeta.setDisplayName(lang.get("menus.generalrules.settings.shulker-item-1.name", player));

        List<String> pLore = new ArrayList<>();
        String statusP = isShulkerOneEnabled() ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        for(String s : lang.getList("menus.generalrules.settings.shulker-item-1.lore", player)) {
            pLore.add(s.replace("%status%", statusP));
        }
        pMeta.setLore(pLore);
        pouch.setItemMeta(pMeta);

        ItemStack shulker = new ItemStack(Material.LIGHT_BLUE_SHULKER_BOX);
        ItemMeta sMeta = shulker.getItemMeta();
        sMeta.setDisplayName(lang.get("menus.generalrules.settings.shulker-item-2.name", player));

        List<String> sLore = new ArrayList<>();
        String statusS = isShulkerTwoEnabled() ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        for(String s : lang.getList("menus.generalrules.settings.shulker-item-2.lore", player)) {
            sLore.add(s.replace("%status%", statusS));
        }
        sMeta.setLore(sLore);
        shulker.setItemMeta(sMeta);

        inv.setItem(11, pouch);
        inv.setItem(15, shulker);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(bMeta);
        inv.setItem(18, back);

        player.openInventory(inv);
    }

    public void openGameRulesPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        Inventory rulesGui = Bukkit.createInventory(null, 36, lang.get("menus.gamerules.title", player));

        rulesGui.setItem(10, createRuleItem(Material.GOLDEN_APPLE, lang.get("menus.rules.nat-regen", player),
                player.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION), player, lang));
        rulesGui.setItem(11, createRuleItem(Material.NETHERITE_SWORD, lang.get("menus.rules.pvp", player),
                player.getWorld().getGameRuleValue(PVP), player, lang));
        rulesGui.setItem(12, createRuleItem(Material.PUFFERFISH, lang.get("menus.rules.day-night", player),
                player.getWorld().getGameRuleValue(ADVANCE_TIME), player, lang));
        rulesGui.setItem(13, createRuleItem(Material.ZOMBIE_HEAD, lang.get("menus.rules.monsters", player),
                player.getWorld().getGameRuleValue(SPAWN_MONSTERS), player, lang));
        rulesGui.setItem(14, createRuleItem(Material.CRAFTING_TABLE, lang.get("menus.rules.advancements", player),
                player.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES), player, lang));
        rulesGui.setItem(15, createRuleItem(Material.VILLAGER_SPAWN_EGG, lang.get("menus.rules.trader", player),
                player.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS), player, lang));
        rulesGui.setItem(16, createRuleItem(Material.COMPASS, lang.get("menus.rules.locator", player),
                player.getWorld().getGameRuleValue(LOCATOR_BAR), player, lang));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(backMeta);
        rulesGui.setItem(27, back);

        player.openInventory(rulesGui);
    }

    public static void openBarrierRulesPanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Inventory barrierGui = Bukkit.createInventory(null, 36, lang.get("menus.barrier.title", player));

        double currentSize = player.getWorld().getWorldBorder().getSize();

        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(lang.get("menus.barrier.current-size.name", player));
        iMeta.setLore(Arrays.asList(lang.get("menus.barrier.current-size.lore", player).replace("%size%", String.valueOf((int)currentSize))));
        info.setItemMeta(iMeta);
        barrierGui.setItem(13, info);

        barrierGui.setItem(10, createBorderItem(Material.RED_STAINED_GLASS_PANE, "§c-10", -10, player, lang));
        barrierGui.setItem(11, createBorderItem(Material.RED_WOOL, "§c-100", -100, player, lang));
        barrierGui.setItem(19, createBorderItem(Material.RED_CONCRETE_POWDER, "§c-500", -500, player, lang));
        barrierGui.setItem(20, createBorderItem(Material.RED_CONCRETE, "§c-1000", -1000, player, lang));

        barrierGui.setItem(15, createBorderItem(Material.GREEN_STAINED_GLASS_PANE, "§a+10", 10, player, lang));
        barrierGui.setItem(16, createBorderItem(Material.GREEN_WOOL, "§a+100", 100, player, lang));
        barrierGui.setItem(24, createBorderItem(Material.GREEN_CONCRETE_POWDER, "§a+500", 500, player, lang));
        barrierGui.setItem(25, createBorderItem(Material.GREEN_CONCRETE, "§a+1000", 1000, player, lang));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(backMeta);
        barrierGui.setItem(31, back);

        player.openInventory(barrierGui);
    }

    public static void openTimePanel(Player player) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Inventory timeGui = Bukkit.createInventory(null, 27, lang.get("menus.time.title", player));

        RightPanelManager rpm = plugin.getRightPanelManager();
        boolean estaPausado = rpm.isPausado();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;
        int totalSecs = rpm.getSegundosPorCapitulo();

        String tiempoVisual = String.format("%02dh %02dm %02ds", totalSecs / 3600, (totalSecs % 3600) / 60, totalSecs % 60);

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(lang.get("menus.time.info-item.name", player));
        List<String> lore = new ArrayList<>();
        for (String line : lang.getList("menus.time.info-item.lore", player)) {
            lore.add(line.replace("%time%", tiempoVisual));
        }
        iMeta.setLore(lore);
        info.setItemMeta(iMeta);
        timeGui.setItem(13, info);

        timeGui.setItem(10, createTimeBtn(Material.RED_STAINED_GLASS_PANE, "§c-1 m", -1, partidaEnCurso, player, lang));
        timeGui.setItem(11, createTimeBtn(Material.RED_WOOL, "§c-5 m", -5, partidaEnCurso, player, lang));
        timeGui.setItem(12, createTimeBtn(Material.RED_CONCRETE, "§c-10 m", -10, partidaEnCurso, player, lang));

        timeGui.setItem(14, createTimeBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+1 m", 1, partidaEnCurso, player, lang));
        timeGui.setItem(15, createTimeBtn(Material.GREEN_WOOL, "§a+5 m", 5, partidaEnCurso, player, lang));
        timeGui.setItem(16, createTimeBtn(Material.GREEN_CONCRETE, "§a+10 m", 10, partidaEnCurso, player, lang));

        ItemStack pauseBtn = new ItemStack(estaPausado ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta pMeta = pauseBtn.getItemMeta();
        pMeta.setDisplayName(estaPausado ? lang.get("menus.time.resume", player) : lang.get("menus.time.pause", player));
        pauseBtn.setItemMeta(pMeta);
        timeGui.setItem(21, pauseBtn);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(bMeta);
        timeGui.setItem(18, back);

        player.openInventory(timeGui);
    }

    private static ItemStack createBorderItem(Material mat, String name, int amount, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lang.get("menus.barrier.change-lore", player)
                .replace("%amount%", String.valueOf(Math.abs(amount)))));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createTimeBtn(Material mat, String name, int amount, boolean bloqueado, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();

        if (bloqueado) {
            meta.setDisplayName("§7§m" + name);
            meta.setLore(Arrays.asList(lang.get("menus.common.locked", player), lang.get("menus.common.locked-lore", player)));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lang.get("menus.time.change-lore", player)
                    .replace("%amount%", String.valueOf(Math.abs(amount)))));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void toggleCombate18(Player admin) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        combate18 = !combate18;
        double speedValue = combate18 ? 100.0 : 4.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(speedValue);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            String estado = combate18 ? plugin.getLang().get("admin-messages.state-enabled", p) : plugin.getLang().get("admin-messages.state-disabled", p);
            p.sendMessage(plugin.getLang().get("admin-messages.combat-toggle", p).replace("%prefix%", plugin.getLang().get("general.prefix", p)).replace("%state%", estado));
        }
    }

    public static void toggleManoSecundaria(Player admin) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        bloquearManoSecundaria = !bloquearManoSecundaria;

        for (Player p : Bukkit.getOnlinePlayers()) {
            String estado = bloquearManoSecundaria ? plugin.getLang().get("admin-messages.state-blocked", p) : plugin.getLang().get("admin-messages.state-allowed", p);
            p.sendMessage(plugin.getLang().get("admin-messages.offhand-toggle", p).replace("%prefix%", plugin.getLang().get("general.prefix", p)).replace("%state%", estado));
        }
    }

    public boolean isShulkerOneEnabled() { return shulkerOneEnabled; }
    public void setShulkerOneEnabled(boolean shulkerOEnabled) { this.shulkerOneEnabled = shulkerOEnabled; }
    public boolean isShulkerTwoEnabled() { return shulkerTwoEnabled; }
    public void setShulkerTwoEnabled(boolean shulkerTEnabled) { this.shulkerTwoEnabled = shulkerTEnabled; }

    private ItemStack createRuleItem(Material mat, String name, Boolean state, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        String status = state ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        meta.setLore(Arrays.asList(lang.get("menus.common.status", player) + status, "", lang.get("menus.common.click-to-toggle", player)));
        item.setItemMeta(meta);
        return item;
    }
}