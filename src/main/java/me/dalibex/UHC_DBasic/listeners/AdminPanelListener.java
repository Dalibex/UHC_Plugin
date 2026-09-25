package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.AdminSlots;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import static org.bukkit.GameRules.*;

/**
 * Listener especializado en el manejo de interfaces administrativas y restricciones de inventario.
 */
public class AdminPanelListener implements Listener {

    private final UHC_DBasic plugin;

    public AdminPanelListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        String title = legacySection().serialize(event.getView().title());
        LanguageManager lang = plugin.getLang();
        AdminPanelManager admin = plugin.getAdminPanel();
        boolean teamSelector = title.equals(lang.get("menus.team-selector.title", p));
        boolean adminMenu = isAdminMenu(title, p, lang);

        if (teamSelector || adminMenu) event.setCancelled(true);
        if (!teamSelector && !adminMenu) {
            handleOffhandInventoryRestrictions(event, p, title, lang);
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (teamSelector) {
            if (plugin.getGameManager().getPhase() == me.dalibex.UHC_DBasic.managers.GamePhase.LOBBY
                    && plugin.getTeamManager().isCustomTeamsEnabled()) {
                handleTeamSelectorClick(p, event.getRawSlot(), event.isRightClick());
            }
            return;
        }
        if (!plugin.isAdmin(p)) {
            p.sendMessage(lang.get("general.no-permission", p));
            return;
        }

        if (title.equals(lang.get("menus.main-admin.title", p))) {
            handleMainAdminClick(p, event.getSlot(), event.isLeftClick(), event.isRightClick(), admin, lang);
        } else if (title.equals(lang.get("menus.generalrules.title", p))) {
            handleGeneralRulesClick(p, event.getSlot(), admin);
        } else if (title.equals(lang.get("menus.shulkers.title", p))) {
            handleShulkersClick(p, event.getSlot(), admin);
        } else if (title.equals(lang.get("menus.shulkerepisode.one-title", p))) {
            handleShulkerEpisodeClick(p, event.getSlot(), admin, 1);
        } else if (title.equals(lang.get("menus.shulkerepisode.two-title", p))) {
            handleShulkerEpisodeClick(p, event.getSlot(), admin, 2);
        } else if (title.equals(lang.get("menus.teamsepisode.title", p))) {
            handleTeamsEpisodeClick(p, event.getSlot(), admin);
        } else if (title.equals(lang.get("menus.pvpsepisode.title", p))) {
            handlePvpEpisodeClick(p, event.getSlot(), admin);
        } else if (title.equals(lang.get("menus.gamerules.title", p))) {
            handleGameRulesClick(p, item.getType(), admin);
        } else if (title.equals(lang.get("menus.gamemode.title", p))) {
            handleGamemodeClick(p, event.getSlot(), admin, lang);
        } else if (title.equals(lang.get("menus.barrier.title", p))) {
            handleBarrierClick(p, event.getSlot(), item.getType(), admin, lang);
        } else if (title.equals(lang.get("menus.time.title", p))) {
            handleTimeClick(p, event.getSlot(), item, admin, lang);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = legacySection().serialize(event.getView().title());
        LanguageManager lang = plugin.getLang();
        if (!isAdminMenu(title, p, lang) && !title.equals(lang.get("menus.team-selector.title", p))) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    private boolean isAdminMenu(String title, Player p, LanguageManager lang) {
        return title.equals(lang.get("menus.main-admin.title", p))
                || title.equals(lang.get("menus.generalrules.title", p))
                || title.equals(lang.get("menus.shulkers.title", p))
                || title.equals(lang.get("menus.shulkerepisode.one-title", p))
                || title.equals(lang.get("menus.shulkerepisode.two-title", p))
                || title.equals(lang.get("menus.teamsepisode.title", p))
                || title.equals(lang.get("menus.pvpsepisode.title", p))
                || title.equals(lang.get("menus.gamerules.title", p))
                || title.equals(lang.get("menus.gamemode.title", p))
                || title.equals(lang.get("menus.barrier.title", p))
                || title.equals(lang.get("menus.time.title", p));
    }

    // --- Menu handlers ---

    private void handleMainAdminClick(Player p, int slot, boolean left, boolean right, AdminPanelManager admin, LanguageManager lang) {
        if (slot == AdminSlots.MAIN_COMBAT) {
            if (left) admin.toggleCombate18(); else if (right) admin.toggleOffhandLock();
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
            admin.openMainAdminPanel(p);
        } else if (slot == AdminSlots.MAIN_GENERAL_RULES) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f); admin.openGeneralRulesPanel(p); }
        else if (slot == AdminSlots.MAIN_GAME_RULES) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f); admin.openGameRulesPanel(p); }
        else if (slot == AdminSlots.MAIN_GAMEMODE) {
            if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            admin.openGamemodePanel(p);
        }
        else if (slot == AdminSlots.MAIN_BORDER) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f); admin.openBarrierRulesPanel(p); }
        else if (slot == AdminSlots.MAIN_TIME) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f); admin.openTimePanel(p); }
        else if (slot == AdminSlots.MAIN_CUSTOM_TEAMS) handleCustomTeamsToggle(p, admin, lang);
        else if (slot == AdminSlots.MAIN_TEAMS_SIZE) handleTeamSizeChange(p, slot, left, right, admin, lang);
    }

    private void handleCustomTeamsToggle(Player p, AdminPanelManager admin, LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;

        boolean newState = !tm.isCustomTeamsEnabled();
        if (newState) {
            if (tm.getTeamSize() <= 1) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            tm.setCustomTeamsEnabled(true);
            tm.initializeCustomTeams();
            tm.giveAllSelectorItems();
        } else {
            tm.setCustomTeamsEnabled(false);
            tm.clearCustomTeams();
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
        }
        admin.openMainAdminPanel(p);
    }

    private void handleTeamSizeChange(Player p, int slot, boolean left, boolean right, AdminPanelManager admin, LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int current = tm.getTeamSize();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        
        if (left) {
            if (current < 4) {
                int next = current + 1;
                if (next > 1 && onlinePlayers < next * 2) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                tm.setTeamSize(next);
            }
        } else if (right && current > 1) {
            int next = current - 1;
            tm.setTeamSize(next);
            if (next == 1 && tm.isCustomTeamsEnabled()) {
                tm.setCustomTeamsEnabled(false);
                tm.clearCustomTeams();
            }
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
        if (tm.isCustomTeamsEnabled()) {
            tm.initializeCustomTeams();
            tm.giveAllSelectorItems();
        }
        admin.openMainAdminPanel(p);
    }

    private void handleTeamSelectorClick(Player p, int slot, boolean right) {
        TeamManager tm = plugin.getTeamManager();
        if (right) {
            if (tm.tryLeaveTeam(p)) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                tm.openTeamSelectorGUI(p);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } else {
            if (tm.tryJoinTeam(p, slot)) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                tm.openTeamSelectorGUI(p);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void handleGeneralRulesClick(Player p, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.GENERAL_SHULKERS_MENU) {
            if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            admin.openShulkersPanel(p);
        } else if (slot == AdminSlots.GENERAL_PVP_EPISODE_MENU) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            admin.openPvpEpisodePanel(p);
        } else if (slot == AdminSlots.GENERAL_TEAMS_EPISODE_MENU) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            admin.openTeamsEpisodePanel(p);
        } else if (slot == AdminSlots.GENERAL_BACK) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openMainAdminPanel(p);
        }
    }

    private void handleShulkersClick(Player p, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.SHULKERS_BACK) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(p);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;
        if (slot == AdminSlots.SHULKERS_TOGGLE_1) {
            admin.setShulkerOneEnabled(!admin.isShulkerOneEnabled());
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        } else if (slot == AdminSlots.SHULKERS_EPISODE_1) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            admin.openShulkerEpisodePanel(p, 1);
            return;
        } else if (slot == AdminSlots.SHULKERS_EPISODE_2) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            admin.openShulkerEpisodePanel(p, 2);
            return;
        } else if (slot == AdminSlots.SHULKERS_TOGGLE_2) {
            admin.setShulkerTwoEnabled(!admin.isShulkerTwoEnabled());
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        }
        admin.openShulkersPanel(p);
    }

    private void handleShulkerEpisodeClick(Player p, int slot, AdminPanelManager admin, int shulkerNumber) {
        if (slot == AdminSlots.SHULKER_EPISODE_BACK) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openShulkersPanel(p);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            if (shulkerNumber == 1) admin.setShulkerOneEpisode(episode);
            else admin.setShulkerTwoEpisode(episode);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openShulkerEpisodePanel(p, shulkerNumber);
    }

    private void handleTeamsEpisodeClick(Player p, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.TEAMS_EPISODE_BACK) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(p);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            plugin.getTeamManager().setTeamsFormedEpisode(episode);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openTeamsEpisodePanel(p);
    }

    private void handlePvpEpisodeClick(Player p, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.PVP_EPISODE_BACK) {
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            admin.openGeneralRulesPanel(p);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int episode = AdminSlots.episodeForSlot(slot);
        if (episode != -1) {
            plugin.getGameManager().setPvpEnabledEpisode(episode);
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
        }
        admin.openPvpEpisodePanel(p);
    }

    private void handleGameRulesClick(Player p, Material mat, AdminPanelManager admin) {
        if (mat == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;
        org.bukkit.GameRule<Boolean> rule = null;
        if (mat == Material.GOLDEN_APPLE) rule = NATURAL_HEALTH_REGENERATION;
        else if (mat == Material.PUFFERFISH) rule = ADVANCE_TIME;
        else if (mat == Material.ZOMBIE_HEAD) rule = SPAWN_MONSTERS;
        else if (mat == Material.CRAFTING_TABLE) rule = SHOW_ADVANCEMENT_MESSAGES;
        else if (mat == Material.VILLAGER_SPAWN_EGG) rule = SPAWN_WANDERING_TRADERS;
        else if (mat == Material.NETHERITE_SWORD) rule = PVP;
        else if (mat == Material.COMPASS) rule = LOCATOR_BAR;

        if (rule != null) {
            boolean newVal = !Bukkit.getWorlds().get(0).getGameRuleValue(rule);
            for (World w : Bukkit.getWorlds()) w.setGameRule(rule, newVal);
            p.playSound(p.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1f, newVal ? 1.5f : 0.8f);
        } else if (mat != Material.ARROW) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        admin.openGameRulesPanel(p);
    }

    private void handleGamemodeClick(Player p, int slot, AdminPanelManager admin, LanguageManager lang) {
        if (slot == AdminSlots.GAMEMODE_BACK) {
            admin.openMainAdminPanel(p);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            return;
        }
        GameManager gm = plugin.getGameManager();
        if (gm.getPhase() != GamePhase.LOBBY) return;
        if (slot == 1) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            return;
        }
        if (slot == AdminSlots.GAMEMODE_CLASSIC) {
            gm.changeMode(new me.dalibex.UHC_DBasic.gamemodes.Classic(plugin, gm));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else if (slot == AdminSlots.GAMEMODE_RESOURCE_RUSH) {
            gm.changeMode(new me.dalibex.UHC_DBasic.gamemodes.ResourceRush(plugin, gm));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        }
        admin.openGamemodePanel(p);
    }

    private void handleBarrierClick(Player p, int slot, Material mat, AdminPanelManager admin, LanguageManager lang) {
        if (mat == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        
        GameManager gm = plugin.getGameManager();
        if (!gm.isMatchActive()) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        World w = Bukkit.getWorlds().get(0);
        int amount = AdminSlots.borderDeltaForSlot(slot);
        if (amount != 0) {
            double newSize = w.getWorldBorder().getSize() + amount;
            if (newSize < 20) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            w.getWorldBorder().setSize(newSize);
            float pitch = (amount > 0) ? 1.5f : 0.8f;
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, pitch);
        }
        admin.openBarrierRulesPanel(p);
    }

    private void handleTimeClick(Player p, int slot, ItemStack item, AdminPanelManager admin, LanguageManager lang) {
        GameManager gm = plugin.getGameManager();
        if (item.getType() == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        if (item.getType().toString().contains("DYE")) { 
            if (!gm.isMatchActive()) return;
            gm.setPaused(!gm.isPaused()); 
            p.playSound(p.getLocation(), gm.isPaused() ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
        else {
            if (gm.getPhase() != GamePhase.LOBBY) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                admin.openTimePanel(p);
                return;
            }
            int change = AdminSlots.timeDeltaMinutesForSlot(slot);
            if (change != 0) {
                int newTime = gm.getSecondsPerChapter() + (change * 60);
                if (newTime > 0) {
                    gm.setSecondsPerChapter(newTime);
                    float pitch = (change > 0) ? 1.2f : 0.8f;
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, pitch);
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
        admin.openTimePanel(p);
    }

    // --- Inventory and offhand restrictions ---

    @EventHandler
    public void onOffhandSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.getAdminPanel().isOffhandLocked()) event.setCancelled(true);
    }

    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if (plugin.getAdminPanel().isOffhandLocked() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Player p = event.getPlayer();
            if (p.getInventory().getItemInMainHand().getType() == Material.SHIELD || p.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                event.setCancelled(true);
            }
        }
    }

    private void handleOffhandInventoryRestrictions(InventoryClickEvent event, Player p, String title, LanguageManager lang) {
        if (!plugin.getAdminPanel().isOffhandLocked() || p.getGameMode() == GameMode.CREATIVE) return;
        if (title.contains(lang.get("menus.main-admin.title", null))) return;

        if (event.getSlot() == 40 || event.getRawSlot() == 45 || event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }
    }
}
