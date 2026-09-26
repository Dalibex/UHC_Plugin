package me.dalibex.UHC_DBasic.admin.handlers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.admin.AdminPanelManager;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.MatchSettingsManager;
import me.dalibex.UHC_DBasic.utils.AdminSlots;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Handles general-rules, shulker, and episode selector menus. */
public class GeneralRulesMenuHandler {

    private final UHC_DBasic plugin;

    public GeneralRulesMenuHandler(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public void handleGeneralRulesClick(Player player, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.GENERAL_SHULKERS_MENU) {
            if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            admin.openShulkersPanel(player);
        } else if (slot == AdminSlots.GENERAL_PVP_EPISODE_MENU) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            admin.openPvpEpisodePanel(player);
        } else if (slot == AdminSlots.GENERAL_TEAMS_EPISODE_MENU) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            admin.openTeamsEpisodePanel(player);
        } else if (slot == AdminSlots.GENERAL_BACK) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openMainAdminPanel(player);
        }
    }

    public void handleShulkersClick(Player player, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.SHULKERS_BACK) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(player);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;
        MatchSettingsManager settings = plugin.getMatchSettings();
        if (slot == AdminSlots.SHULKERS_TOGGLE_1) {
            settings.setShulkerOneEnabled(!settings.isShulkerOneEnabled());
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        } else if (slot == AdminSlots.SHULKERS_EPISODE_1) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            admin.openShulkerEpisodePanel(player, 1);
            return;
        } else if (slot == AdminSlots.SHULKERS_EPISODE_2) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            admin.openShulkerEpisodePanel(player, 2);
            return;
        } else if (slot == AdminSlots.SHULKERS_TOGGLE_2) {
            settings.setShulkerTwoEnabled(!settings.isShulkerTwoEnabled());
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        }
        admin.openShulkersPanel(player);
    }

    public void handleShulkerEpisodeClick(Player player, int slot, AdminPanelManager admin, int shulkerNumber) {
        if (slot == AdminSlots.SHULKER_EPISODE_BACK) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openShulkersPanel(player);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            MatchSettingsManager settings = plugin.getMatchSettings();
            if (shulkerNumber == 1) settings.setShulkerOneEpisode(episode);
            else settings.setShulkerTwoEpisode(episode);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openShulkerEpisodePanel(player, shulkerNumber);
    }

    public void handleTeamsEpisodeClick(Player player, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.TEAMS_EPISODE_BACK) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(player);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            plugin.getTeamManager().setTeamsFormedEpisode(episode);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openTeamsEpisodePanel(player);
    }

    public void handlePvpEpisodeClick(Player player, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.PVP_EPISODE_BACK) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(player);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            plugin.getGameManager().setPvpEnabledEpisode(episode);
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openPvpEpisodePanel(player);
    }
}
