package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Applies match restrictions that are not specific to Admin Panel menus. */
public class GameplayRestrictionListener implements Listener {

    private final UHC_DBasic plugin;

    public GameplayRestrictionListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOffhandSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.getMatchSettings().isOffhandLocked()) event.setCancelled(true);
    }

    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if (!plugin.getMatchSettings().isOffhandLocked()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.SHIELD
                || player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getMatchSettings().isOffhandLocked() || player.getGameMode() == GameMode.CREATIVE) return;
        if (isAdminOrTeamMenu(legacySection().serialize(event.getView().title()), player)) return;

        if (event.getSlot() == 40 || event.getRawSlot() == 45 || event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }
    }

    private boolean isAdminOrTeamMenu(String title, Player player) {
        LanguageManager lang = plugin.getLang();
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
                || title.equals(lang.get("menus.time.title", player))
                || title.equals(lang.get("menus.team-selector.title", player));
    }
}
