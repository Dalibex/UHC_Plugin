package me.dalibex.UHC_DBasic.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Handles special game items: team selector and tracking compass. */
public class ItemsListener implements Listener {

    private final UHC_DBasic plugin;
    private final NamespacedKey trackingCompassKey;

    /** Last compass target per player, used to avoid resending duplicate target packets. */
    private final Map<UUID, CompassTargetKey> lastCompassTarget = new HashMap<>();

    private record CompassTargetKey(UUID worldId, int x, int y, int z) { }

    public ItemsListener(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.trackingCompassKey = new NamespacedKey(plugin, "tracking_compass");
    }

    @EventHandler
    public void onSpecialItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        if (plugin.getTeamManager().isTeamSelector(item)) {
            event.setCancelled(true);
            plugin.getTeamManager().openTeamSelectorGUI(p);
        }
    }

    /** Updates tracking compasses periodically from GameManager. */
    public void updateTrackingCompasses() {
        LanguageManager lang = plugin.getLang();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().getEliminatedPlayers().contains(p.getName())) continue;
            if (!hasTrackingCompass(p)) {
                lastCompassTarget.remove(p.getUniqueId());
                continue;
            }

            Location playerLocation = p.getLocation();

            Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
            boolean compassHeld = isTrackingCompass(p.getInventory().getItemInMainHand())
                    || isTrackingCompass(p.getInventory().getItemInOffHand());

            if (team == null || plugin.getTeamManager().getMemberCount(team) <= 1) {
                updateCompassTarget(p, p.getWorld().getSpawnLocation(), false);
                continue;
            }

            Player nearest = null;
            Location nearestLocation = null;
            double minDistanceSquared = Double.MAX_VALUE;

            for (String entry : plugin.getTeamManager().getMemberNames(team)) {
                if (entry.equalsIgnoreCase(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);

                if (comp != null && comp.isOnline() && 
                    !plugin.getGameManager().getEliminatedPlayers().contains(entry) && 
                    comp.getWorld().equals(p.getWorld())) {
                    
                    Location teammateLocation = comp.getLocation();
                    double distanceSquared = playerLocation.distanceSquared(teammateLocation);
                    if (distanceSquared < minDistanceSquared) {
                        minDistanceSquared = distanceSquared;
                        nearest = comp;
                        nearestLocation = teammateLocation;
                    }
                }
            }

            if (nearest != null) {
                updateCompassTarget(p, nearestLocation, compassHeld);

                // Show ActionBar while the compass is held, because this loop runs at 1 Hz.
                if (compassHeld) {
                    p.sendActionBar(legacySection().deserialize(
                        lang.get("compass.tracking-actionbar", p)
                            .replace("%player%", nearest.getName())
                            .replace("%dist%", String.valueOf((int) Math.sqrt(minDistanceSquared)))));
                }
            } else {
                updateCompassTarget(p, p.getWorld().getSpawnLocation(), false);
            }
        }
    }

    /** Updates the compass target only when it changed, unless forced. */
    private boolean updateCompassTarget(Player p, Location target, boolean force) {
        CompassTargetKey key = new CompassTargetKey(target.getWorld().getUID(), target.getBlockX(), target.getBlockY(), target.getBlockZ());
        CompassTargetKey prev = lastCompassTarget.get(p.getUniqueId());
        if (!force && key.equals(prev)) return false;
        p.setCompassTarget(target);
        lastCompassTarget.put(p.getUniqueId(), key);
        return true;
    }

    private boolean hasTrackingCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrackingCompass(item)) return true;
        }
        return false;
    }

    private boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(trackingCompassKey, PersistentDataType.BYTE);
    }

    public void clearCompassTargetCache() {
        lastCompassTarget.clear();
    }

    public void clearCompassTarget(Player player) {
        if (player != null) lastCompassTarget.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastCompassTarget.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        lastCompassTarget.remove(event.getPlayer().getUniqueId());
    }
}
