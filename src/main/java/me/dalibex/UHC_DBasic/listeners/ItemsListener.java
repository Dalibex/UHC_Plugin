package me.dalibex.UHC_DBasic.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    /**
     * Clave del último objetivo de brújula por jugador (world:x:y:z) para
     * no reenviar paquetes duplicados cada segundo en updateTrackingCompasses.
     */
    private final Map<UUID, String> lastCompassTarget = new HashMap<>();

    public ItemsListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpecialItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        LanguageManager lang = plugin.getLang();

        if (item.getType() == Material.NETHER_STAR) {
            String selectorName = lang.get("items.team-selector.name", p);
            ItemMeta meta = item.getItemMeta();
            if (meta != null && selectorName.equals(legacySection().serialize(meta.displayName()))) {
                event.setCancelled(true);
                plugin.getTeamManager().openTeamSelectorGUI(p);
            }
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

            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (team == null || team.getEntries().size() <= 1) {
                // Si no tiene equipo, apuntar al centro
                updateCompassTarget(p, "world:0:100:0", new Location(p.getWorld(), 0, 100, 0));
                continue;
            }

            Player cercano = null;
            double distMin = Double.MAX_VALUE;

            for (String entry : team.getEntries()) {
                if (entry.equals(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);

                if (comp != null && comp.isOnline() && 
                    !plugin.getGameManager().getEliminatedPlayers().contains(entry) && 
                    comp.getWorld().equals(p.getWorld())) {
                    
                    double d = p.getLocation().distance(comp.getLocation());
                    if (d < distMin) {
                        distMin = d;
                        cercano = comp;
                    }
                }
            }

            if (cercano != null) {
                Location loc = cercano.getLocation();
                String key = loc.getWorld().getName() + ":" + (int)loc.getX() + ":" + (int)loc.getY() + ":" + (int)loc.getZ();
                boolean changed = updateCompassTarget(p, key, loc);
                
                // Mostrar ActionBar solo si el objetivo cambió
                if (changed) {
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    ItemMeta hMeta = hand.getItemMeta();
                    if (hand.getType() == Material.COMPASS && hMeta != null && 
                        lang.get("tracking-compass.name", p).equals(legacySection().serialize(hMeta.displayName()))) {

                        p.sendActionBar(legacySection().deserialize(
                            lang.get("compass.tracking-actionbar", p)
                                .replace("%player%", cercano.getName())
                                .replace("%dist%", String.valueOf((int)distMin))));
                    }
                }
            }
        }
    }

    /**
     * Establece el objetivo de la brújula solo si cambió con respecto al
     * último tick, evitando el reenvío de paquetes cada segundo.
     *
     * @return true si el objetivo cambió (o es la primera vez).
     */
    private boolean updateCompassTarget(Player p, String key, Location target) {
        String prev = lastCompassTarget.get(p.getUniqueId());
        if (key.equals(prev)) return false;
        p.setCompassTarget(target);
        lastCompassTarget.put(p.getUniqueId(), key);
        return true;
    }
}
