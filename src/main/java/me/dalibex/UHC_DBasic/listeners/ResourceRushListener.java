package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ResourceRushListener implements Listener {

    private final UHC_DBasic plugin;

    public ResourceRushListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItem(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getCurrentItem();
            if (result != null) checkItem(player, result.getType());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                checkItem(player, clicked.getType());
            }

            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                checkItem(player, cursor.getType());
            }
        }
    }

    private void checkItem(Player player, Material material) {
        if (plugin.getRightPanelManager().getTiempoTotalSegundos() > 0 &&
                plugin.getRightPanelManager().getModoActual() instanceof ResourceRush rr) {

            if (rr.getObjetivosActivos().contains(material)) {
                rr.completarObjetivo(player, material);
            }
        }
    }
}
