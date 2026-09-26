package me.dalibex.UHC_DBasic.admin;

import java.util.ArrayList;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.MatchSettingsManager;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import me.dalibex.UHC_DBasic.utils.AdminSlots;
import me.dalibex.UHC_DBasic.utils.TimeUtil;
import net.kyori.adventure.text.Component;

public class AdminPanelManager {

    private final UHC_DBasic plugin;
    private final AdminItemFactory itemFactory;

    public AdminPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.itemFactory = new AdminItemFactory(plugin);
    }

    public void openMainAdminPanel(Player player) {
        if (!plugin.isAdmin(player)) return;

        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        Inventory mainGui = Bukkit.createInventory(null, 9, lang.getComponent("menus.main-admin.title", player));
        int teamSize = tm.getTeamSize();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();

        ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta ameta = pvpItem.getItemMeta();
        ameta.displayName(lang.getComponent("menus.main-admin.combat-item.name", player));
        MatchSettingsManager settings = plugin.getMatchSettings();
        String s18 = settings.isCombat18() ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
        String sOff = settings.isOffhandLocked() ? lang.get("admin-messages.state-blocked", player) : lang.get("admin-messages.state-allowed", player);

        List<Component> pvpLore = new ArrayList<>();
        for (String line : lang.getList("menus.main-admin.combat-item.lore", player)) {
            pvpLore.add(itemFactory.text(line.replace("%status18%", s18).replace("%statusOffhand%", sOff)));
        }
        ameta.lore(pvpLore);
        pvpItem.setItemMeta(ameta);
        mainGui.setItem(AdminSlots.MAIN_COMBAT, pvpItem);

        mainGui.setItem(AdminSlots.MAIN_GENERAL_RULES, itemFactory.simpleItem(Material.PAPER, "menus.main-admin.general-rules-item", player));
        mainGui.setItem(AdminSlots.MAIN_GAME_RULES, itemFactory.simpleItem(Material.BOOK, "menus.main-admin.rules-item", player));
        mainGui.setItem(AdminSlots.MAIN_BORDER, itemFactory.simpleItem(Material.EMERALD_BLOCK, "menus.main-admin.border-item", player));
        mainGui.setItem(AdminSlots.MAIN_TIME, itemFactory.simpleItem(Material.CLOCK, "menus.main-admin.time-item", player));

        GameManager gm = plugin.getGameManager();
        boolean matchStarted = gm.getPhase() != GamePhase.LOBBY;

        ItemStack gmItem = new ItemStack(matchStarted ? Material.BARRIER : Material.NETHER_STAR);
        ItemMeta gmMeta = gmItem.getItemMeta();
        String gmNameKey = matchStarted ? "menus.main-admin.gamemode-item.name-locked" : "menus.main-admin.gamemode-item.name";
        gmMeta.displayName(lang.getComponent(gmNameKey, player));

        List<Component> gmLore = new ArrayList<>();
        for (String line : lang.getList("menus.main-admin.gamemode-item.lore", player)) {
            gmLore.add(itemFactory.text(line.replace("%mode%", gm.getCurrentMode().getName())));
        }

        if (matchStarted) {
            gmLore.add(Component.empty());
            gmLore.add(lang.getComponent("menus.common.locked", player));
        }
        gmMeta.lore(gmLore);
        gmItem.setItemMeta(gmMeta);
        mainGui.setItem(AdminSlots.MAIN_GAMEMODE, gmItem);

        ItemStack customTeamItem;
        if (matchStarted) {
            customTeamItem = itemFactory.lockedItem(player, "menus.main-admin.custom-teams-item.name-locked");
        } else {
            customTeamItem = new ItemStack(Material.PAINTING);
            ItemMeta ctMeta = customTeamItem.getItemMeta();
            ctMeta.displayName(lang.getComponent("menus.main-admin.custom-teams-item.name", player));
            String status = tm.isCustomTeamsEnabled() ? lang.get("menus.common.enabled", player) : lang.get("menus.common.disabled", player);
            List<Component> ctLore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.custom-teams-item.lore", player)) {
                ctLore.add(itemFactory.text(line.replace("%status%", status)));
            }
            if (teamSize <= 1) {
                ctLore.add(Component.empty());
                ctLore.add(itemFactory.text(lang.get("game.custom-teams-solos-error", player)));
            }
            ctMeta.lore(ctLore);
            customTeamItem.setItemMeta(ctMeta);
        }
        mainGui.setItem(AdminSlots.MAIN_CUSTOM_TEAMS, customTeamItem);

        ItemStack teamItem;
        if (matchStarted) {
            teamItem = itemFactory.lockedItem(player, "menus.main-admin.teams-item.name-locked");
        } else {
            teamItem = new ItemStack(Material.WHITE_BANNER);
            ItemMeta cMeta = teamItem.getItemMeta();
            cMeta.displayName(lang.getComponent("menus.main-admin.teams-item.name", player));
            String sizeStr = (teamSize == 1) ? lang.get("menus.main-admin.teams-item.size-solos", player) :
                    lang.get("menus.main-admin.teams-item.size-teams", player).replace("%n%", String.valueOf(teamSize));
            int teamCount = (onlinePlayers == 0) ? 0 : (int) Math.ceil((double) onlinePlayers / teamSize);

            List<Component> lore = new ArrayList<>();
            for (String line : lang.getList("menus.main-admin.teams-item.lore", player)) {
                lore.add(itemFactory.text(line.replace("%size%", sizeStr).replace("%online%", String.valueOf(onlinePlayers)).replace("%total%", String.valueOf(teamCount))));
            }
            if (teamSize < 4 && onlinePlayers < (teamSize + 1) * 2) {
                lore.add(Component.empty());
                lore.add(itemFactory.text(lang.get("game.team-size-error", player).replace("%min%", String.valueOf((teamSize + 1) * 2)).replace("%n%", String.valueOf(teamSize + 1))));
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
        boolean matchStarted = plugin.getGameManager().getPhase() != GamePhase.LOBBY;

        ItemStack shulkersBtn;
        if (matchStarted) {
            shulkersBtn = itemFactory.lockedItem(player, "menus.generalrules.settings.shulkers-menu.name");
        } else {
            shulkersBtn = new ItemStack(Material.PURPLE_SHULKER_BOX);
            ItemMeta sMeta = shulkersBtn.getItemMeta();
            sMeta.displayName(lang.getComponent("menus.generalrules.settings.shulkers-menu.name", player));
            sMeta.lore(lang.getComponentList("menus.generalrules.settings.shulkers-menu.lore", player));
            shulkersBtn.setItemMeta(sMeta);
        }
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

        ItemStack back = itemFactory.backButton(player);
        inv.setItem(AdminSlots.GENERAL_BACK, back);

        player.openInventory(inv);
    }

    public void openShulkersPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory inv = Bukkit.createInventory(null, 27, lang.getComponent("menus.shulkers.title", player));

        MatchSettingsManager settings = plugin.getMatchSettings();
        inv.setItem(AdminSlots.SHULKERS_TOGGLE_1, itemFactory.shulkerButton(Material.ORANGE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-1", settings.isShulkerOneEnabled(), settings.getShulkerOneEpisode(), player));
        inv.setItem(AdminSlots.SHULKERS_EPISODE_1, itemFactory.simpleItem(Material.CLOCK, "menus.generalrules.settings.shulker-episode-1", player));
        inv.setItem(AdminSlots.SHULKERS_EPISODE_2, itemFactory.simpleItem(Material.CLOCK, "menus.generalrules.settings.shulker-episode-2", player));
        inv.setItem(AdminSlots.SHULKERS_TOGGLE_2, itemFactory.shulkerButton(Material.LIGHT_BLUE_SHULKER_BOX, "menus.generalrules.settings.shulker-item-2", settings.isShulkerTwoEnabled(), settings.getShulkerTwoEpisode(), player));

        ItemStack back = itemFactory.backButton(player);
        inv.setItem(AdminSlots.SHULKERS_BACK, back);

        player.openInventory(inv);
    }

    public void openTeamsEpisodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();
        boolean matchStarted = plugin.getGameManager().getPhase() != GamePhase.LOBBY;
        int selected = tm.getTeamsFormedEpisode();

        Inventory inv = Bukkit.createInventory(null, 45, lang.getComponent("menus.teamsepisode.title", player));
        itemFactory.populateEpisodeSelector(inv, player, lang, Material.BLUE_BANNER, "menus.teamsepisode",
                selected, matchStarted, AdminSlots.TEAMS_EPISODE_INFO,
                AdminSlots.TEAMS_EPISODE_BUTTONS, AdminSlots.TEAMS_EPISODE_BACK, 0);

        player.openInventory(inv);
    }

    public void openPvpEpisodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        GameManager gm = plugin.getGameManager();
        boolean matchStarted = gm.getPhase() != GamePhase.LOBBY;
        int selected = gm.getPvpEnabledEpisode();

        Inventory inv = Bukkit.createInventory(null, 45, lang.getComponent("menus.pvpsepisode.title", player));
        itemFactory.populateEpisodeSelector(inv, player, lang, Material.NETHERITE_SWORD, "menus.pvpsepisode",
                selected, matchStarted, AdminSlots.PVP_EPISODE_INFO,
                AdminSlots.PVP_EPISODE_BUTTONS, AdminSlots.PVP_EPISODE_BACK, 0);

        player.openInventory(inv);
    }

    public void openShulkerEpisodePanel(Player player, int shulkerNumber) {
        LanguageManager lang = plugin.getLang();
        boolean matchStarted = plugin.getGameManager().getPhase() != GamePhase.LOBBY;
        MatchSettingsManager settings = plugin.getMatchSettings();
        int selected = shulkerNumber == 1 ? settings.getShulkerOneEpisode() : settings.getShulkerTwoEpisode();
        String titleKey = shulkerNumber == 1 ? "menus.shulkerepisode.one-title" : "menus.shulkerepisode.two-title";

        Inventory inv = Bukkit.createInventory(null, 45, lang.getComponent(titleKey, player));
        Material infoMaterial = shulkerNumber == 1 ? Material.ORANGE_SHULKER_BOX : Material.LIGHT_BLUE_SHULKER_BOX;
        itemFactory.populateEpisodeSelector(inv, player, lang, infoMaterial, "menus.shulkerepisode",
                selected, matchStarted, AdminSlots.SHULKER_EPISODE_INFO,
                AdminSlots.SHULKER_EPISODE_BUTTONS, AdminSlots.SHULKER_EPISODE_BACK, shulkerNumber);

        player.openInventory(inv);
    }

    public void openGameRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory rulesGui = Bukkit.createInventory(null, 36, lang.getComponent("menus.gamerules.title", player));

        World w = plugin.getWorldManager().getMainWorld();

        rulesGui.setItem(AdminSlots.RULES_NATURAL_REGENERATION, itemFactory.ruleItem(Material.GOLDEN_APPLE, lang.get("menus.rules.nat-regen", player), w.getGameRuleValue(NATURAL_HEALTH_REGENERATION), player, lang));
        rulesGui.setItem(AdminSlots.RULES_PVP, itemFactory.ruleItem(Material.NETHERITE_SWORD, lang.get("menus.rules.pvp", player), w.getGameRuleValue(PVP), player, lang));
        rulesGui.setItem(AdminSlots.RULES_DAY_NIGHT, itemFactory.ruleItem(Material.PUFFERFISH, lang.get("menus.rules.day-night", player), w.getGameRuleValue(ADVANCE_TIME), player, lang));
        rulesGui.setItem(AdminSlots.RULES_MONSTERS, itemFactory.ruleItem(Material.ZOMBIE_HEAD, lang.get("menus.rules.monsters", player), w.getGameRuleValue(SPAWN_MONSTERS), player, lang));
        rulesGui.setItem(AdminSlots.RULES_ADVANCEMENTS, itemFactory.ruleItem(Material.CRAFTING_TABLE, lang.get("menus.rules.advancements", player), w.getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES), player, lang));
        rulesGui.setItem(AdminSlots.RULES_TRADER, itemFactory.ruleItem(Material.VILLAGER_SPAWN_EGG, lang.get("menus.rules.trader", player), w.getGameRuleValue(SPAWN_WANDERING_TRADERS), player, lang));
        rulesGui.setItem(AdminSlots.RULES_LOCATOR, itemFactory.ruleItem(Material.COMPASS, lang.get("menus.rules.locator", player), w.getGameRuleValue(LOCATOR_BAR), player, lang));

        ItemStack back = itemFactory.backButton(player);
        rulesGui.setItem(AdminSlots.RULES_BACK, back);

        player.openInventory(rulesGui);
    }

    public void openGamemodePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        GameManager gm = plugin.getGameManager();

        Inventory rulesGui = Bukkit.createInventory(null, 9, lang.getComponent("menus.gamemode.title", player));

        boolean isClassic = gm.getCurrentMode() instanceof Classic;
        boolean isResourceRush = gm.getCurrentMode() instanceof ResourceRush;

        rulesGui.setItem(AdminSlots.GAMEMODE_CLASSIC, itemFactory.gamemodeItem(Material.ENCHANTED_GOLDEN_APPLE,
                "classic", isClassic, player, lang));

        rulesGui.setItem(AdminSlots.GAMEMODE_RESOURCE_RUSH, itemFactory.gamemodeItem(Material.HONEY_BLOCK,
                "resource-rush", isResourceRush, player, lang));

        ItemStack back = itemFactory.backButton(player);
        rulesGui.setItem(AdminSlots.GAMEMODE_BACK, back);

        player.openInventory(rulesGui);
    }

    public void openBarrierRulesPanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory barrierGui = Bukkit.createInventory(null, 36, lang.getComponent("menus.barrier.title", player));

        double currentSize = plugin.getWorldManager().getMainWorld().getWorldBorder().getSize();

        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.barrier.current-size.name", player));
        iMeta.lore(List.of(itemFactory.text(lang.get("menus.barrier.current-size.lore", player).replace("%size%", String.valueOf((int)currentSize))), itemFactory.text(lang.get("menus.barrier.min-size-lore", player))));
        info.setItemMeta(iMeta);
        barrierGui.setItem(AdminSlots.BORDER_INFO, info);

        boolean locked = !plugin.getGameManager().isMatchActive();
        barrierGui.setItem(AdminSlots.BORDER_MINUS_10, itemFactory.borderButton(Material.RED_STAINED_GLASS_PANE, "§c-10", -10, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_100, itemFactory.borderButton(Material.RED_WOOL, "§c-100", -100, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_500, itemFactory.borderButton(Material.RED_CONCRETE_POWDER, "§c-500", -500, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_MINUS_1000, itemFactory.borderButton(Material.RED_CONCRETE, "§c-1000", -1000, locked, player, lang));

        barrierGui.setItem(AdminSlots.BORDER_PLUS_10, itemFactory.borderButton(Material.GREEN_STAINED_GLASS_PANE, "§a+10", 10, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_100, itemFactory.borderButton(Material.GREEN_WOOL, "§a+100", 100, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_500, itemFactory.borderButton(Material.GREEN_CONCRETE_POWDER, "§a+500", 500, locked, player, lang));
        barrierGui.setItem(AdminSlots.BORDER_PLUS_1000, itemFactory.borderButton(Material.GREEN_CONCRETE, "§a+1000", 1000, locked, player, lang));

        ItemStack back = itemFactory.backButton(player);
        barrierGui.setItem(AdminSlots.BORDER_BACK, back);

        player.openInventory(barrierGui);
    }

    public void openTimePanel(Player player) {
        LanguageManager lang = plugin.getLang();
        Inventory timeGui = Bukkit.createInventory(null, 27, lang.getComponent("menus.time.title", player));

        GameManager gm = plugin.getGameManager();
        boolean paused = gm.isPaused();
        boolean matchStarted = gm.getPhase() != GamePhase.LOBBY;
        int totalSecs = gm.getSecondsPerChapter();

        String displayTime = TimeUtil.formatHms(totalSecs);

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(lang.getComponent("menus.time.info-item.name", player));
        List<Component> lore = new ArrayList<>();
        for (String line : lang.getList("menus.time.info-item.lore", player)) {
            lore.add(itemFactory.text(line.replace("%time%", displayTime)));
        }
        iMeta.lore(lore);
        info.setItemMeta(iMeta);
        timeGui.setItem(AdminSlots.TIME_INFO, info);

        timeGui.setItem(AdminSlots.TIME_MINUS_1, itemFactory.timeButton(Material.RED_STAINED_GLASS_PANE, "§c-1 m", -1, matchStarted, player, lang));
        timeGui.setItem(AdminSlots.TIME_MINUS_5, itemFactory.timeButton(Material.RED_WOOL, "§c-5 m", -5, matchStarted, player, lang));
        timeGui.setItem(AdminSlots.TIME_MINUS_10, itemFactory.timeButton(Material.RED_CONCRETE, "§c-10 m", -10, matchStarted, player, lang));

        timeGui.setItem(AdminSlots.TIME_PLUS_1, itemFactory.timeButton(Material.GREEN_STAINED_GLASS_PANE, "§a+1 m", 1, matchStarted, player, lang));
        timeGui.setItem(AdminSlots.TIME_PLUS_5, itemFactory.timeButton(Material.GREEN_WOOL, "§a+5 m", 5, matchStarted, player, lang));
        timeGui.setItem(AdminSlots.TIME_PLUS_10, itemFactory.timeButton(Material.GREEN_CONCRETE, "§a+10 m", 10, matchStarted, player, lang));

        ItemStack pauseBtn = new ItemStack(paused ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta pMeta = pauseBtn.getItemMeta();
        pMeta.displayName(paused ? lang.getComponent("menus.time.resume", player) : lang.getComponent("menus.time.pause", player));
        pauseBtn.setItemMeta(pMeta);
        timeGui.setItem(AdminSlots.TIME_PAUSE, pauseBtn);

        ItemStack back = itemFactory.backButton(player);
        timeGui.setItem(AdminSlots.TIME_BACK, back);

        player.openInventory(timeGui);
    }

}
