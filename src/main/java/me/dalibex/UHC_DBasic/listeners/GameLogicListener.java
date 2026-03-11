package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.*;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener especializado en la lógica visceral del juego.
 * Maneja muertes, combate (balanceo de hachas, 1.8), revelación de identidades y consumo de objetos.
 */
public class GameLogicListener implements Listener {

    private final UHC_DBasic plugin;

    public GameLogicListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player muerto = event.getEntity();
        GameManager gm = plugin.getGameManager();
        LanguageManager lang = plugin.getLang();

        muerto.setGameMode(GameMode.SPECTATOR);
        gm.getJugadoresEliminados().add(muerto.getName());
        muerto.getWorld().strikeLightningEffect(muerto.getLocation());

        spawnDeathHead(muerto, lang);

        // Verificar victoria tras un breve delay para permitir el procesamiento del estado
        new BukkitRunnable() {
            @Override
            public void run() { gm.getModoActual().checkVictory(); }
        }.runTaskLater(plugin, 1L);
    }

    private void spawnDeathHead(Player p, LanguageManager lang) {
        Location loc = p.getLocation();
        loc.getBlock().setType(Material.NETHER_BRICK_FENCE);
        loc.clone().add(0, 1, 0).getBlock().setType(Material.PLAYER_HEAD);
        if (loc.clone().add(0, 1, 0).getBlock().getState() instanceof Skull skull) {
            skull.setOwningPlayer(p);
            skull.update();
        }
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        // 1. Mecánicas de combate 1.8 alternativo
        if (AdminPanelManager.combate18) {
            handleCombat18(event, attacker);
        }

        // 2. Revelación de identidades (Skins)
        handleIdentityRevelation(attacker, victim);
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    private void handleCombat18(EntityDamageByEntityEvent event, Player attacker) {
        // Cancelar ataques de barrido
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
        }

        // Ajuste de daño de hachas
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        String type = hand.getType().toString();
        if (type.endsWith("_AXE")) {
            double reduction = type.contains("WOODEN") || type.contains("GOLDEN") ? 4.0 :
                               type.contains("STONE") ? 5.0 :
                               type.contains("IRON") ? 4.0 :
                               type.contains("DIAMOND") ? 3.0 :
                               type.contains("NETHERITE") ? 4.0 : 0.0;
            event.setDamage(Math.max(0.5, event.getDamage() - reduction));
        }
    }

    private void handleIdentityRevelation(Player attacker, Player victim) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isPartidaIniciada()) return;
        if (gm.getJugadoresRevelados().contains(victim.getUniqueId())) return;
        if (plugin.getTeamManager().areInSameTeam(attacker, victim)) return;

        gm.revelarIdentidad(victim);
        LanguageManager lang = plugin.getLang();

        victim.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("game-events.skins.revealed-victim", victim).replace("%player%", attacker.getName())));
        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("game-events.skins.revealed-attacker", attacker).replace("%player%", victim.getName())));
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.8f);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().equals(plugin.getLang().get("crafts.golden-head.name", null))) {
                Player p = event.getPlayer();
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 12 * 20, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300 * 20, 1));
            }
        }
    }
}
