package me.dalibex.UHC_DBasic.admin.handlers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.admin.AdminPanelManager;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.MatchSettingsManager;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import me.dalibex.UHC_DBasic.utils.AdminSlots;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Handles clicks in the main admin menu and team selector. */
public class MainAdminMenuHandler {

    private final UHC_DBasic plugin;

    public MainAdminMenuHandler(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public void handleMainAdminClick(Player player, int slot, boolean left, boolean right, AdminPanelManager admin, LanguageManager lang) {
        if (slot == AdminSlots.MAIN_COMBAT) {
            MatchSettingsManager settings = plugin.getMatchSettings();
            if (left) settings.toggleCombat18(); else if (right) settings.toggleOffhandLock();
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
            admin.openMainAdminPanel(player);
        } else if (slot == AdminSlots.MAIN_GENERAL_RULES) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            admin.openGeneralRulesPanel(player);
        } else if (slot == AdminSlots.MAIN_GAME_RULES) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
            admin.openGameRulesPanel(player);
        } else if (slot == AdminSlots.MAIN_GAMEMODE) {
            if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            admin.openGamemodePanel(player);
        } else if (slot == AdminSlots.MAIN_BORDER) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            admin.openBarrierRulesPanel(player);
        } else if (slot == AdminSlots.MAIN_TIME) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            admin.openTimePanel(player);
        } else if (slot == AdminSlots.MAIN_CUSTOM_TEAMS) {
            handleCustomTeamsToggle(player, admin, lang);
        } else if (slot == AdminSlots.MAIN_TEAMS_SIZE) {
            handleTeamSizeChange(player, left, right, admin);
        }
    }

    private void handleCustomTeamsToggle(Player player, AdminPanelManager admin, LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;

        boolean newState = !tm.isCustomTeamsEnabled();
        if (newState) {
            if (tm.getTeamSize() <= 1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            tm.setCustomTeamsEnabled(true);
            tm.initializeCustomTeams();
            tm.giveAllSelectorItems();
        } else {
            tm.setCustomTeamsEnabled(false);
            tm.clearCustomTeams();
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
        }
        admin.openMainAdminPanel(player);
    }

    private void handleTeamSizeChange(Player player, boolean left, boolean right, AdminPanelManager admin) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int current = tm.getTeamSize();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (left && current < 4) {
            int next = current + 1;
            if (next > 1 && onlinePlayers < next * 2) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            tm.setTeamSize(next);
        } else if (right && current > 1) {
            int next = current - 1;
            tm.setTeamSize(next);
            if (next == 1 && tm.isCustomTeamsEnabled()) {
                tm.setCustomTeamsEnabled(false);
                tm.clearCustomTeams();
            }
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
        if (tm.isCustomTeamsEnabled()) {
            tm.initializeCustomTeams();
            tm.giveAllSelectorItems();
        }
        admin.openMainAdminPanel(player);
    }

    public void handleTeamSelectorClick(Player player, int slot, boolean right) {
        TeamManager tm = plugin.getTeamManager();
        if (right) {
            if (tm.tryLeaveTeam(player)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                tm.openTeamSelectorGUI(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } else {
            if (tm.tryJoinTeam(player, slot)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                tm.openTeamSelectorGUI(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}
