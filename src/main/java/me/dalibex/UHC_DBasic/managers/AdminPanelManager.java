package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import static org.bukkit.GameRules.ADVANCE_TIME;
import static org.bukkit.GameRules.LOCATOR_BAR;
import static org.bukkit.GameRules.NATURAL_HEALTH_REGENERATION;
import static org.bukkit.GameRules.PVP;
import static org.bukkit.GameRules.SHOW_ADVANCEMENT_MESSAGES;
import static org.bukkit.GameRules.SPAWN_MONSTERS;
import static org.bukkit.GameRules.SPAWN_WANDERING_TRADERS;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import me.dalibex.UHC_DBasic.utils.TimeUtil;
import net.kyori.adventure.text.Component;

public class AdminPanelManager {

    private final UHC_DBasic plugin;
    private boolean combate18 = false;
    private boolean bloquearManoSecundaria = false;
    private boolean shulkerOneEnabled = true;
    private boolean shulkerTwoEnabled = true;

    public AdminPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    private Component txt(String legacy) {
        return TextUtil.item(legacy);
    }

    private ItemStack createBackButton(Player player) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(plugin.getLang().getComponent("menus.common.back", player));
        back.setItemMeta(bMeta);
        return back;
    }

    private ItemStack createLockedItem(Player player, String displayNameKey) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(displayNameKey, player));
        meta.lore(List.of(plugin.getLang().getComponent("menus.common.locked", player), plugin.getLang().getComponent("menus.common.locked-lore", player)));
        item.setItemMeta(meta);
        return item;
    }

    public void openMainAdminPanel(Player player) {
        if (!plugin.isAdmin(player)) return;

        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        Inventory mainGui = Bukkit.createInventory(null, 9, lang.getComponent("menus.main-admin.title", player));
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta ameta = pvpItem.getItemMeta();
        ameta.displayName(lang.getComponent("menus.main-admin.combat-item.name", player));
        String s18 = combate18 ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        String sOff = bloquearManoSecundaria ? lang.get("admin-messages.state-blocked", player) : lang.get("admin-messages.state-allowed", player);

        List<Component> pvpLore = new ArrayList<>();
        for (String line : lang.getList("menus.main-admin.combat-item.lore", player)) {
            pvpLore.add(txt(line.replace("%status18%", s18).replace("%statusOffhand%", sOff)));
        }
        ameta.lore(pvpLore);
        pvpItem.setItemMeta(ameta);
        mainGui.setItem(AdminSlots.MAIN_COMBAT, pvpItem);

        mainGui.setItem(AdminSlots.MAIN_GENERAL_RULES, createSimpleItem(Material.PAPER, "menus.main-admin.general-rules-item", player));
        mainGui.setItem(AdminSlots.MAIN_GAME_RULES, createSimpleItem(Material.BOOK, "menus.main-admin.rules-item", player));
        mainGui.setItem(AdminSlots.MAIN_BORDER, createSimpleItem(Material.EMERALD_BLOCK, "menus.main-admin.border-item", player));
        mainGui.setItem(AdminSlots.MAIN_TIME, createSimpleItem(Material.CLOCK, "menus.main-admin.time-item", player));

        GameManager rpm = plugin.getGameManager();
        boolean partidaEnCurso = rpm.getPhase() != GamePhase.LOBBY;

        ItemStack gmItem = new ItemStack(partidaEnCurso ? Material.BARRIER : Material.NETHER_STAR);
        ItemMeta gmMeta = gmItem.getItemMeta();
        String gmNameKey = partidaEnCurso ? "menus.main-admin.gamemode-item.name-locked" : "menus.main-admin.gamemode-item.name";
        gmMeta.displayName(lang.getComponent(gmNameKey, player));

        List<Component> gmLore = new ArrayList<>();
        for (String line : lang.getList("menus.main-admin.gamemode-item.lore", player)) {
            gmLore.add(txt(line.replace("%mode%", rpm.getCurrentMode().getName())));
        }

        if (partidaEnCurso) {
            gmLore.add(Component.empty());
            gmLore.add(lang.getComponent("menus.common.locked", player));
        }
        gmMeta.lore(gmLore);
        gmItem.setItemMeta(gmMeta);
        mainGui.setItem(AdminSlots.MAIN_GAMEMODE, gmItem);

        ItemStack customTeamItem;
        if (partidaEnCurso) {
            customTeamItem = createLockedItem(player, "menus.main-admin.custom-teams-item.name-locked");
        } else {
            customTeamItem = new ItemStack(Material.PAINTING);
            ItemMeta ctMeta = customTeamItem.getItemMeta();
            ctMeta.displayName(lang.getComponent("menus.main-admin.custom-teams-item.name", player));
            String status = tm.isCustomTeamsEnabled() ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
            List<Component> ctLore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.custom-teams-item.lore", player)) {
                ctLore.add(txt(line.replace("%status%", status)));
            }
            if (teamSize <= 1) {
                ctLore.add(Component.empty());
                ctLore.add(txt(lang.get("game.custom-teams-solos-error", player)));
            }
            ctMeta.lore(ctLore);
            customTeamItem.setItemMeta(ctMeta);
        }
        mainGui.setItem(AdminSlots.MAIN_CUSTOM_TEAMS, customTeamItem);

        ItemStack teamItem;
        if (partidaEnCurso) {
            teamItem = createLockedItem(player, "menus.main-admin.teams-item.name-locked");
        } else {
            teamItem = new ItemStack(Material.WHITE_BANNER);
            ItemMeta cMeta = teamItem.getItemMeta();
            cMeta.displayName(lang.getComponent("menus.main-admin.teams-item.name", player));
            String sizeStr = (teamSize == 1) ? lang.get("menus.main-admin.teams-item.size-solos", player) :
                    lang.get("menus.main-admin.teams-item.size-teams", player).replace("%n%", String.valueOf(teamSize));
            int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);

            List<Component> lore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.teams-item.lore", player)) {
                lore.add(txt(line.replace("%size%", sizeStr).replace("%online%", String.valueOf(jugadoresOnline)).replace("%total%", String.valueOf(numEquipos))));
            }
            if (teamSize < 4 && jugadoresOnline < (teamSize + 1) * 2) {
                lore.add(Component.empty());
                lore.add(txt(lang.get("game.team-size-error", player).replace("%min%", String.valueOf((teamSize + 1) * 2)).replace("%n%", String.valueOf(teamSize + 1))));
            }
            cMeta.lore(lore);
            teamItem.setItemMeta(cMeta);
        }
        mainGui.setItem(AdminSlots.MAIN_TEAMS_SIZE, teamItem);

        player.openInventory(mainGui);
    }


    public void openGeneralRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, lang.getComponent("menus.generalrules.title", player));

        ItemStack shulkersBtn = new ItemStack(Material.PURPLE_SHULKER_BOX);
        ItemMeta sMeta = shulkersBtn.getItemMeta();
        sMeta.displayName(lang.getComponent("menus.generalrules.settings.shulkers-menu.name", player));
        sMeta.lore(lang.getComponentList("menus.generalrules.settings.shulkers-menu.lore", player));
        shulkersBtn.setItemMeta(sMeta);
        inv.setItem(AdminSlots.GENERAL_SHULKERS_MENU, shulkersBtn);

        ItemStack pvpEpBtn = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta pMeta = pvpEpBtn.getItemMeta();
        pMeta.displayName(lang.getComponent("menus.generalrules.settings.pvp-episode-menu.name", player));
        pMeta.lore(lang.getComponentList("menus.generalrules.settings.pvp-episode-menu.lore", player));
        pvpEpBtn.setItemMeta(pMeta);
        inv.setItem(AdminSlots.GENERAL_PVP_EPISODE_MENU, pvpEpBtn);

        ItemStack teamsEpBtn = new ItemStack(Material.BLUE_BANNER);
        ItemMeta tMeta = teamsEpBtn.getItemMeta();
        tMeta.displayName(lang.getComponent("menus.generalrules.settings.teams-episode-menu.name", player));
        tMeta.lore(lang.getComponentList("menus.generalrules.settings.teams-episode-menu.lore", player));
        teamsEpBtn.setItemMeta(tMeta);
        inv.setItem(AdminSlots.GENERAL_TEAMS_EPISODE_MENU, teamsEpBtn);

        ItemStack back = createBackButton(player);
        inv.setItem(AdminSlots.GENERAL_BACK, back);

        player.openInventory(inv);
    }

    public void openShulkersPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, lang.getComponent("menus.shulkers.title", player));

        inv.setItem(AdminSlots.SHULKERS_TOGGLE_1, createShulkerBtn(Material.ORANGE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-1", isShulkerOneEnabled(), player));
        inv.setItem(AdminSlots.SHULKERS_TOGGLE_2, createShulkerBtn(Material.LIGHT_BLUE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-2", isShulkerTwoEnabled(), player));

        ItemStack back = createBackButton(player);
        inv.setItem(AdminSlots.SHULKERS_BACK, back);

        player.openInventory(inv);
    }

    public void openTeamsEpisodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();
        boolean partidaEnCurso = plugin.getGameManager().getPhase() != GamePhase.LOBBY;
        int selected = tm.getTeamsFormedEpisode();

        Inventory inv = Bukkit.createInventory(null, 45, lang.getComponent("menus.teamsepisode.title", player));

        ItemStack info = new ItemStack(Material.BLUE_BANNER);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.teamsepisode.info.name", player));
        List<Component> infoLore = new ArrayList<>();
        for (String line : lang.getList("menus.teamsepisode.info.lore", player)) {
            infoLore.add(txt(line.replace("%episode%", String.valueOf(selected))));
        }
        iMeta.lore(infoLore);
        info.setItemMeta(iMeta);
        inv.setItem(AdminSlots.TEAMS_EPISODE_INFO, info);

        for (int i = 0; i < AdminSlots.TEAMS_EPISODE_BUTTONS.length; i++) {
            inv.setItem(AdminSlots.TEAMS_EPISODE_BUTTONS[i], createEpisodeButton(i + 1, selected, partidaEnCurso, player, lang, "menus.teamsepisode"));
        }

        ItemStack back = createBackButton(player);
        inv.setItem(AdminSlots.TEAMS_EPISODE_BACK, back);

        player.openInventory(inv);
    }

    public void openPvpEpisodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        GameManager gm = plugin.getGameManager();
        boolean partidaEnCurso = gm.getPhase() != GamePhase.LOBBY;
        int selected = gm.getPvpEnabledEpisode();

        Inventory inv = Bukkit.createInventory(null, 45, lang.getComponent("menus.pvpsepisode.title", player));

        ItemStack info = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.pvpsepisode.info.name", player));
        List<Component> infoLore = new ArrayList<>();
        for (String line : lang.getList("menus.pvpsepisode.info.lore", player)) {
            infoLore.add(txt(line.replace("%episode%", String.valueOf(selected))));
        }
        iMeta.lore(infoLore);
        info.setItemMeta(iMeta);
        inv.setItem(AdminSlots.PVP_EPISODE_INFO, info);

        for (int i = 0; i < AdminSlots.PVP_EPISODE_BUTTONS.length; i++) {
            inv.setItem(AdminSlots.PVP_EPISODE_BUTTONS[i], createEpisodeButton(i + 1, selected, partidaEnCurso, player, lang, "menus.pvpsepisode"));
        }

        ItemStack back = createBackButton(player);
        inv.setItem(AdminSlots.PVP_EPISODE_BACK, back);

        player.openInventory(inv);
    }

    public void openGameRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory rulesGui = Bukkit.createInventory(null, 36, lang.getComponent("menus.gamerules.title", player));

        World w = Bukkit.getWorlds().get(0);

        rulesGui.setItem(AdminSlots.RULES_NATURAL_REGENERATION, createRuleItem(Material.GOLDEN_APPLE, lang.get("menus.rules.nat-regen", player), w.getGameRuleValue(NATURAL_HEALTH_REGENERATION), player, lang));
        rulesGui.setItem(AdminSlots.RULES_PVP, createRuleItem(Material.NETHERITE_SWORD, lang.get("menus.rules.pvp", player), w.getGameRuleValue(PVP), player, lang));
        rulesGui.setItem(AdminSlots.RULES_DAY_NIGHT, createRuleItem(Material.PUFFERFISH, lang.get("menus.rules.day-night", player), w.getGameRuleValue(ADVANCE_TIME), player, lang));
        rulesGui.setItem(AdminSlots.RULES_MONSTERS, createRuleItem(Material.ZOMBIE_HEAD, lang.get("menus.rules.monsters", player), w.getGameRuleValue(SPAWN_MONSTERS), player, lang));
        rulesGui.setItem(AdminSlots.RULES_ADVANCEMENTS, createRuleItem(Material.CRAFTING_TABLE, lang.get("menus.rules.advancements", player), w.getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES), player, lang));
        rulesGui.setItem(AdminSlots.RULES_TRADER, createRuleItem(Material.VILLAGER_SPAWN_EGG, lang.get("menus.rules.trader", player), w.getGameRuleValue(SPAWN_WANDERING_TRADERS), player, lang));
        rulesGui.setItem(AdminSlots.RULES_LOCATOR, createRuleItem(Material.COMPASS, lang.get("menus.rules.locator", player), w.getGameRuleValue(LOCATOR_BAR), player, lang));

        ItemStack back = createBackButton(player);
        rulesGui.setItem(AdminSlots.RULES_BACK, back);

        player.openInventory(rulesGui);
    }

    public void openGamemodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        GameManager gm = plugin.getGameManager();

        Inventory rulesGui = Bukkit.createInventory(null, 9, lang.getComponent("menus.gamemode.title", player));

        boolean isClassic = gm.getCurrentMode() instanceof Classic;
        boolean isResourceRush = gm.getCurrentMode() instanceof ResourceRush;

        rulesGui.setItem(AdminSlots.GAMEMODE_CLASSIC, createGamemodeItem(Material.ENCHANTED_GOLDEN_APPLE,
                "classic", isClassic, player, lang));

        rulesGui.setItem(AdminSlots.GAMEMODE_RESOURCE_RUSH, createGamemodeItem(Material.HONEY_BLOCK,
                "resource-rush", isResourceRush, player, lang));

        ItemStack back = createBackButton(player);
        rulesGui.setItem(AdminSlots.GAMEMODE_BACK, back);

        player.openInventory(rulesGui);
    }

    public void openBarrierRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory barrierGui = Bukkit.createInventory(null, 36, lang.getComponent("menus.barrier.title", player));

        double currentSize = Bukkit.getWorlds().get(0).getWorldBorder().getSize();

        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.barrier.current-size.name", player));
        iMeta.lore(List.of(txt(lang.get("menus.barrier.current-size.lore", player).replace("%size%", String.valueOf((int)currentSize))), txt(lang.get("menus.barrier.min-size-lore", player))));
        info.setItemMeta(iMeta);
        barrierGui.setItem(AdminSlots.BORDER_INFO, info);

        barrierGui.setItem(AdminSlots.BORDER_MINUS_10, createBorderItem(Material.RED_STAINED_GLASS_PANE, "§c-10", -10, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_100, createBorderItem(Material.RED_WOOL, "§c-100", -100, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_500, createBorderItem(Material.RED_CONCRETE_POWDER, "§c-500", -500, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_1000, createBorderItem(Material.RED_CONCRETE, "§c-1000", -1000, player, lang));

        barrierGui.setItem(AdminSlots.BORDER_PLUS_10, createBorderItem(Material.GREEN_STAINED_GLASS_PANE, "§a+10", 10, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_100, createBorderItem(Material.GREEN_WOOL, "§a+100", 100, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_500, createBorderItem(Material.GREEN_CONCRETE_POWDER, "§a+500", 500, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_1000, createBorderItem(Material.GREEN_CONCRETE, "§a+1000", 1000, player, lang));

        ItemStack back = createBackButton(player);
        barrierGui.setItem(AdminSlots.BORDER_BACK, back);

        player.openInventory(barrierGui);
    }

    public void openTimePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory timeGui = Bukkit.createInventory(null, 27, lang.getComponent("menus.time.title", player));

        GameManager rpm = plugin.getGameManager();
        boolean estaPausado = rpm.isPaused();
        boolean partidaEnCurso = rpm.getPhase() != GamePhase.LOBBY;
        int totalSecs = rpm.getSecondsPerChapter();

        String tiempoVisual = TimeUtil.formatHms(totalSecs);

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.time.info-item.name", player));
        List<Component> lore = new ArrayList<>();
        for (String line : lang.getList("menus.time.info-item.lore", player)) {
            lore.add(txt(line.replace("%time%", tiempoVisual)));
        }
        iMeta.lore(lore);
        info.setItemMeta(iMeta);
        timeGui.setItem(AdminSlots.TIME_INFO, info);

        timeGui.setItem(AdminSlots.TIME_MINUS_1, createTimeBtn(Material.RED_STAINED_GLASS_PANE, "§c-1 m", -1, partidaEnCurso, player, lang));
        timeGui.setItem(AdminSlots.TIME_MINUS_5, createTimeBtn(Material.RED_WOOL, "§c-5 m", -5, partidaEnCurso, player, lang));
        timeGui.setItem(AdminSlots.TIME_MINUS_10, createTimeBtn(Material.RED_CONCRETE, "§c-10 m", -10, partidaEnCurso, player, lang));

        timeGui.setItem(AdminSlots.TIME_PLUS_1, createTimeBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+1 m", 1, partidaEnCurso, player, lang));
        timeGui.setItem(AdminSlots.TIME_PLUS_5, createTimeBtn(Material.GREEN_WOOL, "§a+5 m", 5, partidaEnCurso, player, lang));
        timeGui.setItem(AdminSlots.TIME_PLUS_10, createTimeBtn(Material.GREEN_CONCRETE, "§a+10 m", 10, partidaEnCurso, player, lang));

        ItemStack pauseBtn = new ItemStack(estaPausado ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta pMeta = pauseBtn.getItemMeta();
        pMeta.displayName(estaPausado ? lang.getComponent("menus.time.resume", player) : lang.getComponent("menus.time.pause", player));
        pauseBtn.setItemMeta(pMeta);
        timeGui.setItem(AdminSlots.TIME_PAUSE, pauseBtn);

        ItemStack back = createBackButton(player);
        timeGui.setItem(AdminSlots.TIME_BACK, back);

        player.openInventory(timeGui);
    }

    private ItemStack createSimpleItem(Material mat, String langKey, Player p) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(langKey + ".name", p));
        meta.lore(plugin.getLang().getComponentList(langKey + ".lore", p));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createShulkerBtn(Material mat, String key, boolean enabled, Player p) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLang().getComponent(key + ".name", p));
        List<Component> lore = new ArrayList<>();
        String status = enabled ? plugin.getLang().get("menus.common.enabled", p) : plugin.getLang().get("menus.common.disabled", p);
        for (String line : plugin.getLang().getList(key + ".lore", p)) {
            lore.add(txt(line.replace("%status%", status)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem(Material mat, String name, int amount, Player player, LanguageManager lang) {
        boolean bloqueado = !plugin.getGameManager().isMatchActive();
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();
        if (bloqueado) {
            meta.displayName(txt("§7§m" + name));
            meta.lore(List.of(lang.getComponent("menus.common.locked", player), lang.getComponent("game.border-not-started", player)));
        } else {
            meta.displayName(txt(name));
            meta.lore(List.of(txt(lang.get("menus.barrier.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount))))));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTimeBtn(Material mat, String name, int amount, boolean bloqueado, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();
        if (bloqueado) {
            meta.displayName(txt("§7§m" + name));
            meta.lore(List.of(lang.getComponent("menus.common.locked", player), lang.getComponent("menus.common.locked-lore", player)));
        } else {
            meta.displayName(txt(name));
            meta.lore(List.of(txt(lang.get("menus.time.change-lore", player).replace("%amount%", String.valueOf(Math.abs(amount))))));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRuleItem(Material mat, String name, Boolean state, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(txt(name));
        String status = state ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        meta.lore(Arrays.asList(txt(lang.get("menus.common.status", player) + status), Component.empty(), lang.getComponent("menus.common.click-to-toggle", player)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGamemodeItem(Material mat, String modeKey, boolean isSelected, Player player, LanguageManager lang) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String name = lang.get("menus.gamemode." + modeKey + ".name", player);
        List<String> descLore = lang.getList("menus.gamemode." + modeKey + ".lore", player);

        meta.displayName(txt((isSelected ? "§6§l" : "§f") + name));

        List<Component> finalLore = new ArrayList<>();

        if (isSelected) {
            finalLore.add(lang.getComponent("menus.gamemode.selected-status", player));
            finalLore.add(Component.empty());
            for (String line : descLore) finalLore.add(txt(line));

            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            finalLore.add(lang.getComponent("menus.gamemode.not-selected-status", player));
            finalLore.add(Component.empty());
            for (String line : descLore) finalLore.add(txt(line));
            finalLore.add(Component.empty());
            finalLore.add(lang.getComponent("menus.gamemode.click-to-select", player));
        }

        meta.lore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEpisodeButton(int episode, int selected, boolean locked, Player player, LanguageManager lang, String menuKey) {
        boolean isSelected = episode == selected;
        ItemStack item = new ItemStack(isSelected ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        String name = (isSelected ? "§6§l" : "§f") + lang.get(menuKey + ".button.name", player).replace("%episode%", String.valueOf(episode));
        meta.displayName(txt(name));
        List<Component> lore = new ArrayList<>();
        for (String line : lang.getList(menuKey + ".button.lore", player)) {
            lore.add(txt(line.replace("%episode%", String.valueOf(episode))));
        }
        if (isSelected) {
            lore.add(Component.empty());
            lore.add(lang.getComponent(menuKey + ".selected-status", player));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else if (locked) {
            lore.add(Component.empty());
            lore.add(lang.getComponent("menus.common.locked", player));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void toggleCombate18() {
        combate18 = !combate18;
        double speedValue = combate18 ? 1024.0 : 4.0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            var attackSpeed = p.getAttribute(Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(speedValue);
            }
        }
    }

    public void toggleOffhandLock() {
        bloquearManoSecundaria = !bloquearManoSecundaria;
    }

    public boolean isCombate18() { return combate18; }
    public boolean isOffhandLocked() { return bloquearManoSecundaria; }
    public boolean isShulkerOneEnabled() { return shulkerOneEnabled; }
    public void setShulkerOneEnabled(boolean e) { this.shulkerOneEnabled = e; }
    public boolean isShulkerTwoEnabled() { return shulkerTwoEnabled; }
    public void setShulkerTwoEnabled(boolean e) { this.shulkerTwoEnabled = e; }
}
