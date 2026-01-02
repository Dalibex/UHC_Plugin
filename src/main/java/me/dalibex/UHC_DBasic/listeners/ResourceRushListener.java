package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ResourceRushListener implements Listener {

    private final UHC_DBasic plugin;

    private final Map<UUID, Long> ultimoShift = new HashMap<>();
    private final Map<UUID, Integer> contadorShift = new HashMap<>();

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
        if (plugin.getGameManager().getTiempoTotalSegundos() > 0 &&
                plugin.getGameManager().getModoActual() instanceof ResourceRush rr) {

            if (rr.getObjetivosActivos().contains(material)) {
                rr.completarObjetivo(player, material);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (!plugin.getGameManager().isPartidaIniciada()) return;

        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        long ahora = System.currentTimeMillis();

        if (!ultimoShift.containsKey(uuid) || (ahora - ultimoShift.get(uuid) > 3000)) {
            contadorShift.put(uuid, 1);
        } else {
            int cuenta = contadorShift.get(uuid) + 1;
            if (cuenta >= 3) {
                mostrarResumenObjetivos(p);
                contadorShift.put(uuid, 0);
            } else {
                contadorShift.put(uuid, cuenta);
            }
        }
        ultimoShift.put(uuid, ahora);
    }

    private void mostrarResumenObjetivos(Player p) {
        LanguageManager lang = plugin.getLang();
        if (!(plugin.getGameManager().getModoActual() instanceof ResourceRush)) return;
        ResourceRush rr = (ResourceRush) plugin.getGameManager().getModoActual();

        List<Material> activos = rr.getObjetivosActivos();
        List<Material> conseguidos = rr.getLogrosJugador(p);

        p.sendMessage("");
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.summary.header", p)));

        if (activos.isEmpty()) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.summary.empty", p)));
        } else {
            String formatDone = lang.get("resource-rush.summary.item-done", p);
            String formatPending = lang.get("resource-rush.summary.item-pending", p);

            for (Material mat : activos) {
                String nombreMat = mat.name().replace("_", " ").toLowerCase();
                String line = conseguidos.contains(mat) ? formatDone : formatPending;
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', line.replace("%item%", nombreMat)));
            }
        }

        p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.summary.footer", p)
                .replace("%done%", String.valueOf(conseguidos.size()))
                .replace("%total%", String.valueOf(activos.size()))));

        p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }
}
