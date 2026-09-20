package me.dalibex.UHC_DBasic.listeners;

import java.util.Locale;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Listener especializado en la lógica visceral del juego.
 * Maneja muertes, combate (balanceo de hachas, 1.8), revelación de identidades y consumo de objetos.
 */
public class GameLogicListener implements Listener {

    private final UHC_DBasic plugin;

    public GameLogicListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player muerto = event.getEntity();
        GameManager gm = plugin.getGameManager();
        if (!gm.isMatchActive() || !gm.getInitialParticipants().contains(muerto.getName())
                || gm.getEliminatedPlayers().contains(muerto.getName())) return;

        muerto.setGameMode(GameMode.SPECTATOR);
        gm.eliminatePlayer(muerto.getName());
        muerto.getWorld().strikeLightningEffect(muerto.getLocation());

        spawnDeathHead(muerto);

        // Verificar victoria tras un breve delay para permitir el procesamiento del estado
        new BukkitRunnable() {
            @Override
            public void run() { gm.getCurrentMode().checkVictory(); }
        }.runTaskLater(plugin, 1L);
    }

    private void spawnDeathHead(Player p) {
        Location loc = p.getLocation();
        loc.getBlock().setType(Material.NETHER_BRICK_FENCE);
        loc.clone().add(0, 1, 0).getBlock().setType(Material.PLAYER_HEAD);
        if (loc.clone().add(0, 1, 0).getBlock().getState() instanceof Skull skull) {
            // La cabeza muestra la skin REAL del muerto (no la falsa que llevaba)
            plugin.getSkinsManager().applyOwnHead(skull, p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isMatchActive() || !gm.getInitialParticipants().contains(attacker.getName())
                || !gm.getInitialParticipants().contains(victim.getName())
                || gm.getEliminatedPlayers().contains(attacker.getName())
                || gm.getEliminatedPlayers().contains(victim.getName())) return;

        // 1. Mecánicas de combate 1.8 alternativo
        if (plugin.getAdminPanel().isCombate18()) {
            handleCombat18(event, attacker);
            if (event.isCancelled()) return;
        }

        // 2. Marcar combate para el retardo del cambio de skin (30s)
        plugin.getSkinsManager().markInCombat(attacker);
        plugin.getSkinsManager().markInCombat(victim);

        // 3. Revelación de identidades (Skins)
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
        if (!gm.isMatchActive()) return;
        if (plugin.getSkinsManager().getRevealedPlayers().contains(victim.getName().toLowerCase(Locale.ROOT))) return;
        if (plugin.getTeamManager().areInSameTeam(attacker, victim)) return;

        plugin.getSkinsManager().revealIdentity(victim);
        LanguageManager lang = plugin.getLang();

        victim.sendMessage(legacySection().deserialize(lang.get("game-events.skins.revealed-victim", victim).replace("%player%", attacker.getName())));
        attacker.sendMessage(legacySection().deserialize(lang.get("game-events.skins.revealed-attacker", attacker).replace("%player%", victim.getName())));
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 0.8f);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (plugin.getSpecialCraftsManager().isGoldenHead(item)) {
            Player p = event.getPlayer();
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 12 * 20, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300 * 20, 1));
        }
    }
}
