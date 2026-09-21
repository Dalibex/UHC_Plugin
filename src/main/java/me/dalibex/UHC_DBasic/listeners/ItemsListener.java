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

/**
 * Listener especializado en el uso de objetos especiales del juego.
 * Maneja el selector de equipos y la brújula de seguimiento.
 */
public class ItemsListener implements Listener {

    private final UHC_DBasic plugin;
    private final NamespacedKey trackingCompassKey;

    /**
     * Clave del último objetivo de brújula por jugador (world:x:y:z) para
     * no reenviar paquetes duplicados cada segundo en updateTrackingCompasses.
     */
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

    /**
     * Lógica de actualización de brújulas de seguimiento.
     * Este método se llama periódicamente desde el GameManager.
     */
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

            Player cercano = null;
            Location ubicacionCercana = null;
            double distMin = Double.MAX_VALUE;

            for (String entry : plugin.getTeamManager().getMemberNames(team)) {
                if (entry.equalsIgnoreCase(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);

                if (comp != null && comp.isOnline() && 
                    !plugin.getGameManager().getEliminatedPlayers().contains(entry) && 
                    comp.getWorld().equals(p.getWorld())) {
                    
                    Location teammateLocation = comp.getLocation();
                    double d = playerLocation.distanceSquared(teammateLocation);
                    if (d < distMin) {
                        distMin = d;
                        cercano = comp;
                        ubicacionCercana = teammateLocation;
                    }
                }
            }

            if (cercano != null) {
                updateCompassTarget(p, ubicacionCercana, compassHeld);

                // Mostrar ActionBar cada segundo (el bucle corre a 1 Hz) siempre
                // que el jugador sostenga la brújula en mano u off-hand, para que
                // la distancia se vea de forma continua sobre la barra de vida.
                if (compassHeld) {

                    p.sendActionBar(legacySection().deserialize(
                        lang.get("compass.tracking-actionbar", p)
                            .replace("%player%", cercano.getName())
                            .replace("%dist%", String.valueOf((int) Math.sqrt(distMin)))));
                }
            } else {
                updateCompassTarget(p, p.getWorld().getSpawnLocation(), false);
            }
        }
    }

    /**
     * Establece el objetivo de la brújula solo si cambió con respecto al
     * último tick, evitando el reenvío de paquetes cada segundo.
     *
     * @return true si el objetivo cambió (o es la primera vez).
     */
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
