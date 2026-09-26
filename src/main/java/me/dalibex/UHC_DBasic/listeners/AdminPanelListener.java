package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.admin.AdminPanelManager;
import me.dalibex.UHC_DBasic.admin.handlers.GeneralRulesMenuHandler;
import me.dalibex.UHC_DBasic.admin.handlers.MainAdminMenuHandler;
import me.dalibex.UHC_DBasic.admin.handlers.WorldSettingsMenuHandler;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Routes protected inventory clicks to the correct admin/team menu handler. */
public class AdminPanelListener implements Listener {

    private final UHC_DBasic plugin;
    private final MainAdminMenuHandler mainHandler;
    private final GeneralRulesMenuHandler generalHandler;
    private final WorldSettingsMenuHandler worldHandler;

    public AdminPanelListener(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.mainHandler = new MainAdminMenuHandler(plugin);
        this.generalHandler = new GeneralRulesMenuHandler(plugin);
        this.worldHandler = new WorldSettingsMenuHandler(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = legacySection().serialize(event.getView().title());
        LanguageManager lang = plugin.getLang();
        AdminPanelManager admin = plugin.getAdminPanel();
        boolean teamSelector = title.equals(lang.get("menus.team-selector.title", player));
        boolean adminMenu = isAdminMenu(title, player, lang);

        if (teamSelector || adminMenu) event.setCancelled(true);
        if (!teamSelector && !adminMenu) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (teamSelector) {
            if (plugin.getGameManager().getPhase() == GamePhase.LOBBY && plugin.getTeamManager().isCustomTeamsEnabled()) {
                mainHandler.handleTeamSelectorClick(player, event.getRawSlot(), event.isRightClick());
            }
            return;
        }
        if (!plugin.isAdmin(player)) {
            player.sendMessage(lang.get("general.no-permission", player));
            return;
        }

        routeAdminClick(title, player, event, item, admin, lang);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = legacySection().serialize(event.getView().title());
        LanguageManager lang = plugin.getLang();
        if (!isAdminMenu(title, player, lang) && !title.equals(lang.get("menus.team-selector.title", player))) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) event.setCancelled(true);
    }

    private void routeAdminClick(String title, Player player, InventoryClickEvent event, ItemStack item,
                                 AdminPanelManager admin, LanguageManager lang) {
        int slot = event.getSlot();
        if (title.equals(lang.get("menus.main-admin.title", player))) {
            mainHandler.handleMainAdminClick(player, slot, event.isLeftClick(), event.isRightClick(), admin, lang);
        } else if (title.equals(lang.get("menus.generalrules.title", player))) {
            generalHandler.handleGeneralRulesClick(player, slot, admin);
        } else if (title.equals(lang.get("menus.shulkers.title", player))) {
            generalHandler.handleShulkersClick(player, slot, admin);
        } else if (title.equals(lang.get("menus.shulkerepisode.one-title", player))) {
            generalHandler.handleShulkerEpisodeClick(player, slot, admin, 1);
        } else if (title.equals(lang.get("menus.shulkerepisode.two-title", player))) {
            generalHandler.handleShulkerEpisodeClick(player, slot, admin, 2);
        } else if (title.equals(lang.get("menus.teamsepisode.title", player))) {
            generalHandler.handleTeamsEpisodeClick(player, slot, admin);
        } else if (title.equals(lang.get("menus.pvpsepisode.title", player))) {
            generalHandler.handlePvpEpisodeClick(player, slot, admin);
        } else if (title.equals(lang.get("menus.gamerules.title", player))) {
            worldHandler.handleGameRulesClick(player, item.getType(), admin);
        } else if (title.equals(lang.get("menus.gamemode.title", player))) {
            worldHandler.handleGamemodeClick(player, slot, admin);
        } else if (title.equals(lang.get("menus.barrier.title", player))) {
            worldHandler.handleBarrierClick(player, slot, item.getType(), admin);
        } else if (title.equals(lang.get("menus.time.title", player))) {
            worldHandler.handleTimeClick(player, slot, item, admin);
        }
    }

    private boolean isAdminMenu(String title, Player player, LanguageManager lang) {
        return title.equals(lang.get("menus.main-admin.title", player))
                || title.equals(lang.get("menus.generalrules.title", player))
                || title.equals(lang.get("menus.shulkers.title", player))
                || title.equals(lang.get("menus.shulkerepisode.one-title", player))
                || title.equals(lang.get("menus.shulkerepisode.two-title", player))
                || title.equals(lang.get("menus.teamsepisode.title", player))
                || title.equals(lang.get("menus.pvpsepisode.title", player))
                || title.equals(lang.get("menus.gamerules.title", player))
                || title.equals(lang.get("menus.gamemode.title", player))
                || title.equals(lang.get("menus.barrier.title", player))
                || title.equals(lang.get("menus.time.title", player));
    }
}
