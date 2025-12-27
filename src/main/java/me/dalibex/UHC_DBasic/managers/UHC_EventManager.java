package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import static me.dalibex.UHC_DBasic.managers.AdminPanel.*;
import static org.bukkit.GameRules.*;

public class UHC_EventManager implements Listener {

    private final UHC_DBasic plugin;

    public UHC_EventManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    // --- MANEJO DE MUERTES ---
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player muerto = event.getEntity();
        LanguageManager lang = plugin.getLang();

        muerto.setGameMode(org.bukkit.GameMode.SPECTATOR);
        plugin.getRightPanelManager().getJugadoresEliminados().add(muerto.getName());
        muerto.getWorld().strikeLightningEffect(muerto.getLocation());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(muerto);
            meta.setDisplayName(lang.get("game.player-head-name").replace("%player%", muerto.getName()));
            head.setItemMeta(meta);
        }

        Location loc = muerto.getLocation();
        loc.getBlock().setType(Material.NETHER_BRICK_FENCE);
        loc.clone().add(0, 1, 0).getBlock().setType(Material.PLAYER_HEAD);
        if (loc.clone().add(0, 1, 0).getBlock().getState() instanceof org.bukkit.block.Skull skull) {
            skull.setOwningPlayer(muerto);
            skull.update();
        }

        new BukkitRunnable() {
            @Override public void run() { plugin.getRightPanelManager().comprobarVictoria(); }
        }.runTaskLater(plugin, 1L);
    }

    // --- BLOQUEOS DE MANO SECUNDARIA Y ESCUDOS ---
    @EventHandler
    public void onOffhandSwap(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        if (AdminPanel.bloquearManoSecundaria) event.setCancelled(true);
    }
    @EventHandler
    public void onSweepAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (AdminPanel.combate18 && event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onOffhandBlock(InventoryClickEvent event) {
        if (!AdminPanel.bloquearManoSecundaria) return;
        if (event.getWhoClicked().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (event.getView().getTitle().contains("Panel Administración UHC")) return;

        if (event.getSlot() == 40 || event.getRawSlot() == 45) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack affected = event.getCurrentItem();
            if (affected != null && affected.getType() == Material.SHIELD) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onShieldUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (AdminPanel.bloquearManoSecundaria) {
            Player p = event.getPlayer();
            boolean tieneEscudoManoPrincipal = p.getInventory().getItemInMainHand().getType() == Material.SHIELD;
            boolean tieneEscudoManoSecundaria = p.getInventory().getItemInOffHand().getType() == Material.SHIELD;
            if (tieneEscudoManoPrincipal || tieneEscudoManoSecundaria) {
                if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
                        event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!AdminPanel.bloquearManoSecundaria) return;
        if (event.getWhoClicked().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (event.getInventorySlots().contains(40) || event.getRawSlots().contains(45)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onAxeDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!AdminPanel.combate18) return;

        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        org.bukkit.inventory.ItemStack item = attacker.getInventory().getItemInMainHand();
        Material weapon = item.getType();
        String name = weapon.toString();

        if (name.endsWith("_AXE")) {
            double damageReduction = 0.0;

            if (name.contains("WOODEN") || name.contains("GOLDEN")) {
                damageReduction = 4.0;
            } else if (name.contains("STONE")) {
                damageReduction = 5.0;
            } else if (name.contains("IRON")) {
                damageReduction = 4.0;
            } else if (name.contains("DIAMOND")) {
                damageReduction = 3.0;
            } else if (name.contains("NETHERITE")) {
                damageReduction = 4.0;
            } // Para conseguir valores originales de la 1.8 en hachas

            // Daño total actual (Base + Fuerza + Sharpness + Crítico)
            double currentDamage = event.getDamage();
            double finalDamage = Math.max(0, currentDamage - damageReduction);
            event.setDamage(finalDamage);
        }
    }

    // --- INVENTARIO PANEL DE ADMINISTRACIÓN ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        LanguageManager lang = plugin.getLang();
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        // PANEL PRINCIPAL
        if (title.equals(lang.get("menus.main-admin.title"))) {
            event.setCancelled(true);
            int slot = event.getSlot();

            if (slot == 4) { p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); plugin.getAdminPanel().openBarrierRulesPanel(p); }
            else if (slot == 2) { p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); plugin.getAdminPanel().openGameRulesPanel(p); }
            else if (slot == 6) { p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); openTimePanel(p); }
            else if (slot == 0) {
                if (event.isLeftClick()) toggleCombate18(p);
                else if (event.isRightClick()) toggleManoSecundaria(p);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                openMainAdminPanel(p);
            }
            else if (slot == 8) {
                RightPanelManager rpm = plugin.getRightPanelManager();
                if (rpm.getTiempoTotalSegundos() > 0) {
                    p.sendMessage(lang.get("menus.common.locked") + " " + lang.get("menus.common.locked-lore"));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }
                TeamManager tm = plugin.getTeamManager();
                int current = tm.getTeamSize();
                int online = Bukkit.getOnlinePlayers().size();

                if (event.isLeftClick()) {
                    int next = current + 1;
                    if (next <= 4 && online >= (next * 2)) {
                        tm.setTeamSize(next); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    } else if (next > 4) {
                        p.sendMessage(lang.get("game.team-size-max"));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    } else {
                        p.sendMessage(lang.get("game.team-size-error").replace("%min%", String.valueOf(next * 2)).replace("%n%", String.valueOf(next)));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }
                } else if (event.isRightClick() && current > 1) {
                    tm.setTeamSize(current - 1); p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                }
                openMainAdminPanel(p);
            }
        }

        // PANEL GAMERULES
        else if (title.equals(lang.get("menus.gamerules.title"))) {
            event.setCancelled(true);
            Material mat = item.getType();
            if (mat == Material.GOLDEN_APPLE) p.getWorld().setGameRule(NATURAL_HEALTH_REGENERATION, !p.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION));
            else if (mat == Material.PUFFERFISH) p.getWorld().setGameRule(ADVANCE_TIME, !p.getWorld().getGameRuleValue(ADVANCE_TIME));
            else if (mat == Material.ZOMBIE_HEAD) p.getWorld().setGameRule(SPAWN_MONSTERS, !p.getWorld().getGameRuleValue(SPAWN_MONSTERS));
            else if (mat == Material.CRAFTING_TABLE) p.getWorld().setGameRule(SHOW_ADVANCEMENT_MESSAGES, !p.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES));
            else if (mat == Material.VILLAGER_SPAWN_EGG) p.getWorld().setGameRule(SPAWN_WANDERING_TRADERS, !p.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS));
            else if (mat == Material.NETHERITE_SWORD) p.getWorld().setGameRule(PVP, !p.getWorld().getGameRuleValue(PVP));
            else if (mat == Material.COMPASS) p.getWorld().setGameRule(LOCATOR_BAR, !p.getWorld().getGameRuleValue(LOCATOR_BAR));
            else if (mat == Material.ARROW) { p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); openMainAdminPanel(p); return; }

            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
            plugin.getAdminPanel().openGameRulesPanel(p);
        }

        // PANEL BARRERA
        else if (title.equals(lang.get("menus.barrier.title"))) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) {
                openMainAdminPanel(p);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                return;
            }

            if (p.getWorld().getWorldBorder().getSize() > 5999980) {
                p.sendMessage(lang.get("game.border-not-started"));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            int amount = 0, slot = event.getSlot();
            if (slot == 10) amount = -20; else if (slot == 11) amount = -200; else if (slot == 19) amount = -1000; else if (slot == 20) amount = -2000;
            else if (slot == 15) amount = 20; else if (slot == 16) amount = 200; else if (slot == 24) amount = 1000; else if (slot == 25) amount = 2000;

            if (amount != 0) {
                p.getWorld().getWorldBorder().setSize(p.getWorld().getWorldBorder().getSize() + amount);
                p.sendMessage(lang.get("game.border-update").replace("%size%", String.valueOf((int) p.getWorld().getWorldBorder().getSize())));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                plugin.getAdminPanel().openBarrierRulesPanel(p);
            }
        }

        // PANEL TIMER
        else if (title.equals(lang.get("menus.time.title"))) {
            event.setCancelled(true);
            if (item.getType() == Material.BARRIER) {
                p.sendMessage(lang.get("menus.common.locked-lore"));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                return;
            }

            RightPanelManager rpm = plugin.getRightPanelManager();
            int change = 0, slot = event.getSlot();
            if (slot == 10) change = -1; else if (slot == 11) change = -5; else if (slot == 12) change = -10;
            else if (slot == 14) change = 1; else if (slot == 15) change = 5; else if (slot == 16) change = 10;

            if (change != 0) {
                int nuevoS = rpm.getSegundosPorCapitulo() + (change * 60);
                if (nuevoS < 60) {
                    p.sendMessage(lang.get("game.time-min-error"));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                }

                else {
                    rpm.setSegundosPorCapitulo(nuevoS);
                    Bukkit.broadcastMessage(lang.get("game.time-update").replace("%prefix%", lang.get("general.prefix")).replace("%time%", String.valueOf(nuevoS / 60)));
                    openTimePanel(p);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                }
            } else if (item.getType().toString().contains("DYE")) {
                rpm.setPausado(!rpm.isPausado());
                openTimePanel(p);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
            } else if (item.getType() == Material.ARROW) {
                openMainAdminPanel(p);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
            }
        }
    }

    @EventHandler
    public void onConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().equals(plugin.getLang().get("crafts.golden-head.name"))) {
                Player p = event.getPlayer();
                p.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 12 * 20, 1));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 300 * 20, 1));
            }
        }
    }

    public void onCompassTrack() {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getRightPanelManager().getJugadoresEliminados().contains(p.getName())) continue;
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (team == null || team.getEntries().size() <= 1) {
                p.setCompassTarget(new Location(p.getWorld(), 0, p.getLocation().getY(), 0)); continue;
            }
            Player cercano = null; double distMin = Double.MAX_VALUE;
            for (String entry : team.getEntries()) {
                if (entry.equals(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);
                if (comp != null && comp.isOnline() && !plugin.getRightPanelManager().getJugadoresEliminados().contains(entry) && comp.getWorld().equals(p.getWorld())) {
                    double d = p.getLocation().distance(comp.getLocation());
                    if (d < distMin) { distMin = d; cercano = comp; }
                }
            }
            if (cercano != null) {
                p.setCompassTarget(cercano.getLocation());
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.COMPASS && hand.hasItemMeta() && hand.getItemMeta().getDisplayName().equals(lang.get("tracking-compass.name"))) {
                    p.sendActionBar(lang.get("compass.tracking-actionbar").replace("%player%", cercano.getName()).replace("%dist%", String.valueOf((int)distMin)));
                }
            } else p.setCompassTarget(new Location(p.getWorld(), 0, 100, 0));
        }
    }
}