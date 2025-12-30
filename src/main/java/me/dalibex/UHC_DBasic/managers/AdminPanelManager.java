package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.GameRules.*;

public class AdminPanelManager {

    private final UHC_DBasic plugin;
    public static boolean combate18 = false;
    public static boolean bloquearManoSecundaria = false;
    private boolean shulkerOneEnabled = true;
    private boolean shulkerTwoEnabled = true;

    public AdminPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    // ------------------------------ DISEÑO PANELES ------------------------------
    public void openMainAdminPanel(Player player) {
        if (!player.isOp()) return;

        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        Inventory mainGui = Bukkit.createInventory(null, 9, lang.get("menus.main-admin.title", player));
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        // Item de Combate
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

        // Otros Items
        mainGui.setItem(1, createSimpleItem(Material.BELL, "menus.main-admin.general-rules-item", player));
        mainGui.setItem(2, createSimpleItem(Material.BOOK, "menus.main-admin.rules-item", player));
        mainGui.setItem(4, createSimpleItem(Material.EMERALD_BLOCK, "menus.main-admin.border-item", player));
        mainGui.setItem(6, createSimpleItem(Material.CLOCK, "menus.main-admin.time-item", player));

        // Item de Equipos (Bloqueado si hay partida)
        GameManager rpm = plugin.getRightPanelManager();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;

        ItemStack teamItem = new ItemStack(partidaEnCurso ? Material.BARRIER : Material.WHITE_BANNER);
        ItemMeta cMeta = teamItem.getItemMeta();

        if (partidaEnCurso) {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name-locked", player));
            cMeta.setLore(Arrays.asList(lang.get("menus.common.locked", player), lang.get("menus.common.locked-lore", player)));
        } else {
            cMeta.setDisplayName(lang.get("menus.main-admin.teams-item.name", player));
            String sizeStr = (teamSize == 1) ? lang.get("menus.main-admin.teams-item.size-solos", player) :
                    lang.get("menus.main-admin.teams-item.size-teams", player).replace("%n%", String.valueOf(teamSize));
            int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);

            List<String> lore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.teams-item.lore", player)) {
                lore.add(line.replace("%size%", sizeStr).replace("%online%", String.valueOf(jugadoresOnline)).replace("%total%", String.valueOf(numEquipos)));
            }
            cMeta.setLore(lore);
        }
        teamItem.setItemMeta(cMeta);
        mainGui.setItem(8, teamItem);

        player.openInventory(mainGui);
    }

    public void openGeneralRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, lang.get("menus.generalrules.title", player));

        inv.setItem(11, createShulkerBtn(Material.ORANGE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-1", isShulkerOneEnabled(), player));
        inv.setItem(15, createShulkerBtn(Material.LIGHT_BLUE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-2", isShulkerTwoEnabled(), player));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(bMeta);
        inv.setItem(18, back);

        player.openInventory(inv);
    }

    public void openGameRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory rulesGui = Bukkit.createInventory(null, 36, lang.get("menus.gamerules.title", player));

        // Usamos siempre el mundo 0 (Overworld) para que los valores no cambien según donde esté el admin
        World w = Bukkit.getWorlds().get(0);

        rulesGui.setItem(10, createRuleItem(Material.GOLDEN_APPLE, lang.get("menus.rules.nat-regen", player), w.getGameRuleValue(NATURAL_HEALTH_REGENERATION), player, lang));
        rulesGui.setItem(11, createRuleItem(Material.NETHERITE_SWORD, lang.get("menus.rules.pvp", player), w.getGameRuleValue(PVP), player, lang));
        rulesGui.setItem(12, createRuleItem(Material.PUFFERFISH, lang.get("menus.rules.day-night", player), w.getGameRuleValue(ADVANCE_TIME), player, lang));
        rulesGui.setItem(13, createRuleItem(Material.ZOMBIE_HEAD, lang.get("menus.rules.monsters", player), w.getGameRuleValue(SPAWN_MONSTERS), player, lang));
        rulesGui.setItem(14, createRuleItem(Material.CRAFTING_TABLE, lang.get("menus.rules.advancements", player), w.getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES), player, lang));
        rulesGui.setItem(15, createRuleItem(Material.VILLAGER_SPAWN_EGG, lang.get("menus.rules.trader", player), w.getGameRuleValue(SPAWN_WANDERING_TRADERS), player, lang));
        rulesGui.setItem(16, createRuleItem(Material.COMPASS, lang.get("menus.rules.locator", player), w.getGameRuleValue(LOCATOR_BAR), player, lang));

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(lang.get("menus.common.back", player));
        back.setItemMeta(backMeta);
        rulesGui.setItem(27, back);

        player.openInventory(rulesGui);
    }

    public void openBarrierRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory barrierGui = Bukkit.createInventory(null, 36, lang.get("menus.barrier.title", player));

        double currentSize = Bukkit.getWorlds().get(0).getWorldBorder().getSize();

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

    public void openTimePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory timeGui = Bukkit.createInventory(null, 27, lang.get("menus.time.title", player));

        GameManager rpm = plugin.getRightPanelManager();
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

    // ------------------------------ MÉTODOS AUXILIARES ------------------------------

    private ItemStack createSimpleItem(Material mat, String langKey, Player p) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getLang().get(langKey + ".name", p));
        meta.setLore(plugin.getLang().getList(langKey + ".lore", p));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createShulkerBtn(Material mat, String key, boolean enabled, Player p) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getLang().get(key + ".name", p));
        List<String> lore = new ArrayList<>();
        String status = enabled ? plugin.getLang().get("menus.common.enabled", p) : plugin.getLang().get("menus.common.disabled", p);
        for(String s : plugin.getLang().getList(key + ".lore", p)) {
            lore.add(s.replace("%status%", status));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem(Material mat, String name, int amount, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lang.get("menus.barrier.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount)))));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTimeBtn(Material mat, String name, int amount, boolean bloqueado, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();
        if (bloqueado) {
            meta.setDisplayName("§7§m" + name);
            meta.setLore(Arrays.asList(lang.get("menus.common.locked", player), lang.get("menus.common.locked-lore", player)));
        } else {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lang.get("menus.time.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount)))));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRuleItem(Material mat, String name, Boolean state, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        String status = state ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        meta.setLore(Arrays.asList(lang.get("menus.common.status", player) + status, "", lang.get("menus.common.click-to-toggle", player)));
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------ LÓGICA DE TOGGLES ------------------------------
    public void toggleCombate18() {
        combate18 = !combate18;
        double speedValue = combate18 ? 1024.0 : 4.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(speedValue);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            String estado = combate18 ? plugin.getLang().get("admin-messages.state-enabled", p) : plugin.getLang().get("admin-messages.state-disabled", p);
            p.sendMessage(plugin.getLang().get("admin-messages.combat-toggle", p).replace("%prefix%", plugin.getLang().get("general.prefix", p)).replace("%state%", estado));
        }
    }

    public void toggleManoSecundaria() {
        bloquearManoSecundaria = !bloquearManoSecundaria;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String estado = bloquearManoSecundaria ? plugin.getLang().get("admin-messages.state-blocked", p) : plugin.getLang().get("admin-messages.state-allowed", p);
            p.sendMessage(plugin.getLang().get("admin-messages.offhand-toggle", p).replace("%prefix%", plugin.getLang().get("general.prefix", p)).replace("%state%", estado));
        }
    }

    public boolean isShulkerOneEnabled() { return shulkerOneEnabled; }
    public void setShulkerOneEnabled(boolean e) { this.shulkerOneEnabled = e; }
    public boolean isShulkerTwoEnabled() { return shulkerTwoEnabled; }
    public void setShulkerTwoEnabled(boolean e) { this.shulkerTwoEnabled = e; }
}