package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

/**
 * Listener especializado en el uso de objetos especiales del juego.
 * Maneja el selector de equipos y la brújula de seguimiento.
 */
public class ItemsListener implements Listener {

    private final UHC_DBasic plugin;

    public ItemsListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpecialItemUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        LanguageManager lang = plugin.getLang();

        // 1. Selector de Equipos (Estrella del Nether)
        if (item.getType() == Material.NETHER_STAR) {
            String selectorName = lang.get("items.team-selector.name", p);
            if (item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(selectorName)) {
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
            if (plugin.getGameManager().getJugadoresEliminados().contains(p.getName())) continue;

            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (team == null || team.getEntries().size() <= 1) {
                // Si no tiene equipo, apuntar al centro
                p.setCompassTarget(new Location(p.getWorld(), 0, 100, 0));
                continue;
            }

            // Buscar al compañero vivo más cercano en la misma dimensión
            Player cercano = null;
            double distMin = Double.MAX_VALUE;

            for (String entry : team.getEntries()) {
                if (entry.equals(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);

                if (comp != null && comp.isOnline() && 
                    !plugin.getGameManager().getJugadoresEliminados().contains(entry) && 
                    comp.getWorld().equals(p.getWorld())) {
                    
                    double d = p.getLocation().distance(comp.getLocation());
                    if (d < distMin) {
                        distMin = d;
                        cercano = comp;
                    }
                }
            }

            if (cercano != null) {
                p.setCompassTarget(cercano.getLocation());
                
                // Mostrar ActionBar si tiene la brújula en la mano
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.COMPASS && hand.hasItemMeta() && 
                    hand.getItemMeta().getDisplayName().equals(lang.get("tracking-compass.name", p))) {
                    
                    String msg = lang.get("compass.tracking-actionbar", p)
                            .replace("%player%", cercano.getName())
                            .replace("%dist%", String.valueOf((int)distMin));
                    p.sendActionBar(msg);
                }
            }
        }
    }
}
