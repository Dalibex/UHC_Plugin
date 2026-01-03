package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.UHCGameMode;
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import static org.bukkit.GameRules.*;

public class UHC_EventManagerListener implements Listener {

    private final UHC_DBasic plugin;

    public UHC_EventManagerListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    // --- MANEJO DE MUERTES ---
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player muerto = event.getEntity();
        LanguageManager lang = plugin.getLang();

        muerto.setGameMode(org.bukkit.GameMode.SPECTATOR);
        plugin.getGameManager().getJugadoresEliminados().add(muerto.getName());
        muerto.getWorld().strikeLightningEffect(muerto.getLocation());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(muerto);
            meta.setDisplayName(lang.get("game.player-head-name", null).replace("%player%", muerto.getName()));
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
            @Override public void run() { plugin.getGameManager().getModoActual().checkVictory(); }
        }.runTaskLater(plugin, 1L);
    }

    // --- BLOQUEOS DE MANO SECUNDARIA Y ESCUDOS ---
    @EventHandler
    public void onOffhandSwap(PlayerSwapHandItemsEvent event) {
        if (AdminPanelManager.bloquearManoSecundaria) event.setCancelled(true);
    }
    @EventHandler
    public void onSweepAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (AdminPanelManager.combate18 && event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if (AdminPanelManager.bloquearManoSecundaria) {
            Player p = event.getPlayer();
            // Si el jugador intenta usar el escudo (Click derecho) teniendo uno en cualquier mano, se cancela
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (p.getInventory().getItemInMainHand().getType() == Material.SHIELD ||
                        p.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                    event.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onOffhandBlock(InventoryClickEvent event) {
        if (!AdminPanelManager.bloquearManoSecundaria) return;
        if (event.getWhoClicked().getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (event.getView().getTitle().contains(plugin.getLang().get("menus.main-admin.title", null))) return;

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
    public void onAxeDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!AdminPanelManager.combate18) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        String name = item.getType().toString();

        if (name.endsWith("_AXE")) {
            double reduction = 0.0;
            if (name.contains("WOODEN") || name.contains("GOLDEN")) reduction = 4.0;
            else if (name.contains("STONE")) reduction = 5.0;
            else if (name.contains("IRON")) reduction = 4.0;
            else if (name.contains("DIAMOND")) reduction = 3.0;
            else if (name.contains("NETHERITE")) reduction = 4.0;

            event.setDamage(Math.max(0, event.getDamage() - reduction));
        }
    }
    // ---------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        LanguageManager lang = plugin.getLang();
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();
        AdminPanelManager admin = plugin.getAdminPanel();

        if (item == null || item.getType() == Material.AIR) return;

        // PANEL PRINCIPAL
        if (title.equals(lang.get("menus.main-admin.title", p))) {
            event.setCancelled(true);
            int slot = event.getSlot();

            if (slot == 0) {
                if (event.isLeftClick()) admin.toggleCombate18();
                else if (event.isRightClick()) admin.toggleManoSecundaria();
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                admin.openMainAdminPanel(p);
            }
            else if (slot == 1) admin.openGeneralRulesPanel(p);
            else if (slot == 2) admin.openGameRulesPanel(p);
            else if (slot == 3) {
                if (plugin.getGameManager().getTiempoTotalSegundos() > 0) {
                    p.sendMessage(lang.get("menus.common.locked", p));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }
                admin.openGamemodePanel(p);
            }
            else if (slot == 4) admin.openBarrierRulesPanel(p);
            else if (slot == 6) admin.openTimePanel(p);
            else if (slot == 8) {
                GameManager rpm = plugin.getGameManager();
                if (rpm.getTiempoTotalSegundos() > 0) {
                    p.sendMessage(lang.get("menus.common.locked", p));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                TeamManager tm = plugin.getTeamManager();
                int current = tm.getTeamSize();
                int online = Bukkit.getOnlinePlayers().size();

                if (event.isLeftClick()) {
                    int next = current + 1;
                    if (next <= 4 && online >= (next * 2)) {
                        tm.setTeamSize(next);
                        p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    } else if (next > 4) {
                        p.sendMessage(lang.get("game.team-size-max", p));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    } else {
                        p.sendMessage(lang.get("game.team-size-error", p)
                                .replace("%min%", String.valueOf(next * 2))
                                .replace("%n%", String.valueOf(next)));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }
                } else if (event.isRightClick() && current > 1) {
                    tm.setTeamSize(current - 1);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                }
                admin.openMainAdminPanel(p);
            }
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
        }

        // PANEL AJUSTES GENERALES
        else if (title.equals(lang.get("menus.generalrules.title", p))) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (slot == 11) admin.setShulkerOneEnabled(!admin.isShulkerOneEnabled());
            else if (slot == 15) admin.setShulkerTwoEnabled(!admin.isShulkerTwoEnabled());
            else if (slot == 18) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); return; }

            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            admin.openGeneralRulesPanel(p);
        }

        // PANEL GAMERULES (APLICAR A TODAS LAS DIMENSIONES)
        else if (title.equals(lang.get("menus.gamerules.title", p))) {
            event.setCancelled(true);
            Material mat = item.getType();
            if (mat == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); return; }

            org.bukkit.GameRule<Boolean> ruleToToggle = null;
            if (mat == Material.GOLDEN_APPLE) ruleToToggle = NATURAL_HEALTH_REGENERATION;
            else if (mat == Material.PUFFERFISH) ruleToToggle = ADVANCE_TIME;
            else if (mat == Material.ZOMBIE_HEAD) ruleToToggle = SPAWN_MONSTERS;
            else if (mat == Material.CRAFTING_TABLE) ruleToToggle = SHOW_ADVANCEMENT_MESSAGES;
            else if (mat == Material.VILLAGER_SPAWN_EGG) ruleToToggle = SPAWN_WANDERING_TRADERS;
            else if (mat == Material.NETHERITE_SWORD) ruleToToggle = PVP;
            else if (mat == Material.COMPASS) ruleToToggle = LOCATOR_BAR;

            if (ruleToToggle != null) {
                boolean newVal = !Bukkit.getWorlds().get(0).getGameRuleValue(ruleToToggle);
                for (World w : Bukkit.getWorlds()) w.setGameRule(ruleToToggle, newVal);
            }
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
            admin.openGameRulesPanel(p);
        }

        // PANEL DE SELECCION DE GAMEMODE
        else if (title.equals(lang.get("menus.gamemode.title", p))) {
            event.setCancelled(true);
            GameManager gm = plugin.getGameManager();
            int slot = event.getSlot();

            if (slot == 0) { // BOTÓN ATRÁS
                admin.openMainAdminPanel(p);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                return;
            }

            if (slot == 2) { // SELECCIÓN CLASSIC
                if (!(gm.getModoActual() instanceof me.dalibex.UHC_DBasic.gamemodes.Classic)) {
                    gm.cambiarModo(new me.dalibex.UHC_DBasic.gamemodes.Classic(plugin, gm));
                    p.sendMessage(lang.get("general.prefix", p) + lang.get("menus.gamemode.classic-activated", p));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    admin.openGamemodePanel(p);
                }
            }
            else if (slot == 4) { // SELECCIÓN RESOURCE RUSH
                if (!(gm.getModoActual() instanceof me.dalibex.UHC_DBasic.gamemodes.ResourceRush)) {
                    gm.cambiarModo(new me.dalibex.UHC_DBasic.gamemodes.ResourceRush(plugin, gm));
                    p.sendMessage(lang.get("general.prefix", p) + lang.get("menus.gamemode.resource-rush-activated", p));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    admin.openGamemodePanel(p);
                }
            }
        }

        // PANEL BARRERA (AFECTA AL MUNDO PRINCIPAL)
        else if (title.equals(lang.get("menus.barrier.title", p))) {
            event.setCancelled(true);
            if (item.getType() == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1); return; }

            World overworld = Bukkit.getWorlds().get(0);
            if (overworld.getWorldBorder().getSize() > 5999980 && event.getSlot() != 13) {
                p.sendMessage(lang.get("game.border-not-started", p));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            int amount = 0, slot = event.getSlot();
            if (slot == 10) amount = -20; else if (slot == 11) amount = -200; else if (slot == 19) amount = -1000; else if (slot == 20) amount = -2000;
            else if (slot == 15) amount = 20; else if (slot == 16) amount = 200; else if (slot == 24) amount = 1000; else if (slot == 25) amount = 2000;

            if (amount != 0) {
                overworld.getWorldBorder().setSize(overworld.getWorldBorder().getSize() + amount);
                p.sendMessage(lang.get("game.border-update", p).replace("%size%", String.valueOf((int) overworld.getWorldBorder().getSize())));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                admin.openBarrierRulesPanel(p);
            }
        }

        // PANEL TIMER
        else if (title.equals(lang.get("menus.time.title", p))) {
            event.setCancelled(true);
            GameManager rpm = plugin.getGameManager();
            if (item.getType() == Material.BARRIER) { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1); return; }

            int change = 0, slot = event.getSlot();
            if (slot == 10) change = -1; else if (slot == 11) change = -5; else if (slot == 12) change = -10;
            else if (slot == 14) change = 1; else if (slot == 15) change = 5; else if (slot == 16) change = 10;

            if (change != 0) {
                int nuevoS = rpm.getSegundosPorCapitulo() + (change * 60);
                if (nuevoS < 60) {
                    p.sendMessage(lang.get("game.time-min-error", p));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                } else {
                    rpm.setSegundosPorCapitulo(nuevoS);

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        String mensaje = lang.get("game.time-update", online)
                                .replace("%prefix%", lang.get("general.prefix", online))
                                .replace("%time%", String.valueOf(nuevoS / 60));
                        online.sendMessage(mensaje);
                    }

                    admin.openTimePanel(p);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                }
            } else if (item.getType().toString().contains("DYE")) {
                rpm.setPausado(!rpm.isPausado());
                admin.openTimePanel(p);
            } else if (item.getType() == Material.ARROW) admin.openMainAdminPanel(p);

            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
        }
    }

    @EventHandler
    public void onConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().equals(plugin.getLang().get("crafts.golden-head.name", null))) {
                Player p = event.getPlayer();
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.REGENERATION, 12 * 20, 1));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.ABSORPTION, 300 * 20, 1));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player p = event.getPlayer();

        GameManager rpm = plugin.getGameManager();
        UHCGameMode modo = rpm.getModoActual();

        if (p.getGameMode() != GameMode.SPECTATOR) {
            p.setGameMode(GameMode.ADVENTURE);
        }

        if (rpm.getTiempoTotalSegundos() == 0) {
            World world = p.getWorld();
            world.getChunkAt(0, 0).load(true);
            int y = world.getHighestBlockYAt(0, 0);
            if (y < 60) y = 100;
            Location spawnLoc = new Location(world, 0.5, y + 1, 0.5);
            p.teleport(spawnLoc);
        }

        double attackSpeedValue = AdminPanelManager.combate18 ? 1024.0 : 4.0;
        p.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(attackSpeedValue);

        if (rpm.getTiempoTotalSegundos() > 0) {
            int crono = rpm.getTiempoTotalSegundos();
            int segundosCap = rpm.getSegundosPorCapitulo();
            int restante = segundosCap - (crono % segundosCap);

            modo.updateScoreboard(p, formatTime(restante), formatTime(crono), true);
        } else {
            modo.updateScoreboard(p, "00:00", "00:00", false);
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            modo.updateScoreboard(online, "...", "...", rpm.getTiempoTotalSegundos() > 0);
        }
    }

    @EventHandler
    public void onIdentityHit(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isPartidaIniciada()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;

        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null || attacker.equals(victim)) return;

        GameManager gm = plugin.getGameManager();

        if (gm.getJugadoresRevelados().contains(victim.getUniqueId())) return;

        if (plugin.getTeamManager().areInSameTeam(attacker, victim)) return;

        // --- PROCESO DE REVELACIÓN ---
        gm.revelarIdentidad(victim);

        String msgVictim = plugin.getLang()
                .get("game-events.skins.revealed-victim", victim)
                .replace("%player%", attacker.getName());

        String msgAttacker = plugin.getLang()
                .get("game-events.skins.revealed-attacker", attacker)
                .replace("%player%", victim.getName());

        victim.sendMessage(ChatColor.translateAlternateColorCodes('&', msgVictim));
        attacker.sendMessage(ChatColor.translateAlternateColorCodes('&', msgAttacker));

        victim.getWorld().playSound(
                victim.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BIT,
                1.0f,
                0.8f
        );
    }

    // AUXILIAR
    private String formatTime(int s) {
        int h = s / 3600; int m = (s % 3600) / 60; int sec = s % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }
    public void onCompassTrack() {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().getJugadoresEliminados().contains(p.getName())) continue;
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (team == null || team.getEntries().size() <= 1) {
                p.setCompassTarget(new Location(p.getWorld(), 0, 100, 0));
                continue;
            }
            Player cercano = null; double distMin = Double.MAX_VALUE;
            for (String entry : team.getEntries()) {
                if (entry.equals(p.getName())) continue;
                Player comp = Bukkit.getPlayer(entry);
                if (comp != null && comp.isOnline() && !plugin.getGameManager().getJugadoresEliminados().contains(entry) && comp.getWorld().equals(p.getWorld())) {
                    double d = p.getLocation().distance(comp.getLocation());
                    if (d < distMin) { distMin = d; cercano = comp; }
                }
            }
            if (cercano != null) {
                p.setCompassTarget(cercano.getLocation());
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.COMPASS && hand.hasItemMeta() && hand.getItemMeta().getDisplayName().equals(lang.get("tracking-compass.name", p))) {
                    p.sendActionBar(lang.get("compass.tracking-actionbar", p).replace("%player%", cercano.getName()).replace("%dist%", String.valueOf((int)distMin)));
                }
            }
        }
    }
}