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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import static me.dalibex.UHC_DBasic.managers.AdminPanel.*;
import static org.bukkit.GameRules.*;
import static org.bukkit.GameRules.PVP;

public class UHC_EventManager implements Listener {

        private final UHC_DBasic plugin;

        public UHC_EventManager(UHC_DBasic plugin) {
            this.plugin = plugin;
        }

        // --- MANEJO DE MUERTES) ---
        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            Player muerto = event.getEntity();
            muerto.setGameMode(org.bukkit.GameMode.SPECTATOR);
            plugin.getRightPanelManager().getJugadoresEliminados().add(muerto.getName());
            muerto.getWorld().strikeLightningEffect(muerto.getLocation());

            // CABEZA DE JUGADOR
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(muerto);
                meta.setDisplayName("§eCabeza de §6" + muerto.getName());
                head.setItemMeta(meta);
            }
            org.bukkit.Location loc = muerto.getLocation();
            loc.getBlock().setType(Material.NETHER_BRICK_FENCE);
            loc.clone().add(0, 1, 0).getBlock().setType(Material.PLAYER_HEAD);
            org.bukkit.block.Block block = loc.clone().add(0, 1, 0).getBlock();
            if (block.getState() instanceof org.bukkit.block.Skull skull) {
                skull.setOwningPlayer(muerto);
                skull.update();
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getRightPanelManager().comprobarVictoria();
                }
            }.runTaskLater(plugin, 1L);
        }

        // --- BLOQUEOS DE MANO SECUNDARIA Y ESCUDOS ---
        @EventHandler
        public void onOffhandSwap(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
            if (AdminPanel.bloquearManoSecundaria) {
                event.setCancelled(true);
            }
        }
        @EventHandler
        public void onSweepAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
            if (!AdminPanel.combate18) return;

            if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
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

        // ---- INVENTARIO PANEL DE ADMINISTRACIÓN
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getView().getTitle().equals("§8⚙ Panel Administración UHC")) {
                event.setCancelled(true);
                Player p = (Player) event.getWhoClicked();
                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;

                int slot = event.getSlot();

                if (slot == 4) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openBarrierRulesPanel(p);
                }
                else if (slot == 2) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                }
                else if (slot == 6) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    openTimePanel(p);
                }
                else if (slot == 0) {
                    if (event.isLeftClick()) {
                        toggleCombate18(p);
                    } else if (event.isRightClick()) {
                        toggleManoSecundaria(p);
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    openMainAdminPanel(p);
                }
                else if (slot == 8) {
                    RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();
                    if (rpm.getTiempoTotalSegundos() > 0) {
                        p.sendMessage("§c§l⚠ §7Los equipos ya han sido sellados. No puedes cambiarlos ahora.");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }

                    TeamManager tm = UHC_DBasic.getPlugin(UHC_DBasic.class).getTeamManager();
                    int current = tm.getTeamSize();
                    int jugadoresOnline = Bukkit.getOnlinePlayers().size();

                    if (event.isLeftClick()) {
                        int proximoTamaño = current + 1;
                        if (proximoTamaño <= 4 && jugadoresOnline >= (proximoTamaño * 2)) {
                            tm.setTeamSize(proximoTamaño);
                            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                        } else if (proximoTamaño > 4) {
                            p.sendMessage("§cEl tamaño máximo de equipo es 4.");
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        } else {
                            p.sendMessage("§c¡No hay suficientes jugadores! Necesitas al menos " + (proximoTamaño * 2) + " para equipos de " + proximoTamaño);
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        }
                    } else if (event.isRightClick() && current > 1) {
                        tm.setTeamSize(current - 1);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    }
                    openMainAdminPanel(p);
                }
            }


            // LÓGICA GAMERULES
            else if (event.getView().getTitle().equals("§8⚖ GameRules del UHC")) {
                event.setCancelled(true);
                if (event.getCurrentItem() == null) return;

                Player p = (Player) event.getWhoClicked();
                Material mat = event.getCurrentItem().getType();

                // LOGICA CAMBIO DE LOS GAMERULES
                if (mat == Material.GOLDEN_APPLE) {
                    boolean current = p.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION);
                    p.getWorld().setGameRule(NATURAL_HEALTH_REGENERATION, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.PUFFERFISH) {
                    boolean current = p.getWorld().getGameRuleValue(ADVANCE_TIME);
                    p.getWorld().setGameRule(ADVANCE_TIME, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.ZOMBIE_HEAD) {
                    boolean current = p.getWorld().getGameRuleValue(SPAWN_MONSTERS);
                    p.getWorld().setGameRule(SPAWN_MONSTERS, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.CRAFTING_TABLE) {
                    boolean current = p.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES);
                    p.getWorld().setGameRule(SHOW_ADVANCEMENT_MESSAGES, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.VILLAGER_SPAWN_EGG) {
                    boolean current = p.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS);
                    p.getWorld().setGameRule(SPAWN_WANDERING_TRADERS, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.NETHERITE_SWORD) {
                    boolean current = p.getWorld().getGameRuleValue(PVP);
                    p.getWorld().setGameRule(PVP, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                } else if (mat == Material.COMPASS) {
                    boolean current = p.getWorld().getGameRuleValue(LOCATOR_BAR);
                    p.getWorld().setGameRule(LOCATOR_BAR, !current);
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    plugin.getAdminPanel().openGameRulesPanel(p);
                }
                else if (mat == Material.ARROW) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    openMainAdminPanel(p);
                }
            }

            // LÓGICA PANEL BARRERA
            else if (event.getView().getTitle().equals("§8⚖ Ajustar tamaño de barrera")) {
                event.setCancelled(true);
                Player p = (Player) event.getWhoClicked();

                String name = event.getCurrentItem().getItemMeta().getDisplayName();
                double currentSize = p.getWorld().getWorldBorder().getSize();
                double newSize = currentSize;

                if (event.getCurrentItem().getType() == Material.ARROW) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    openMainAdminPanel(p);
                    return;
                }
                if (p.getWorld().getWorldBorder().getSize() > 5999980) {
                    p.sendMessage("§c§l⚠ §cNo puedes ajustar el borde porque la partida no ha empezado.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                if (name.contains("+10")) newSize += 20;
                else if (name.contains("+100")) newSize += 200;
                else if (name.contains("+500")) newSize += 1000;
                else if (name.contains("+1000")) newSize += 2000;
                else if (name.contains("-10")) newSize -= 20;
                else if (name.contains("-100")) newSize -= 200;
                else if (name.contains("-500")) newSize -= 1000;
                else if (name.contains("-1000")) newSize -= 2000;

                if (newSize != currentSize && newSize > 0) {
                    p.getWorld().getWorldBorder().setSize(newSize);
                    p.sendMessage("§7[§bBorde§7] §fNuevo tamaño: §6" + (int)newSize + "x" + (int)newSize);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                    plugin.getAdminPanel().openBarrierRulesPanel(p);
                }
            }

            // LÓGICA TIMER / PAUSA
            else if (event.getView().getTitle().equals("§8⏲ Control de Tiempo")) {
                event.setCancelled(true);
                Player p = (Player) event.getWhoClicked();
                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;

                RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();

                if (item.getType() == Material.BARRIER) {
                    p.sendMessage("§c§l⚠ §7No puedes cambiar la duración con la partida en curso.");
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                if (item.getType().toString().contains("RED_") || item.getType().toString().contains("GREEN_")) {
                    String name = item.getItemMeta().getDisplayName();
                    int change = 0;
                    if (name.contains("10")) change = 10;
                    else if (name.contains("5")) change = 5;
                    else if (name.contains("1")) change = 1;

                    if (name.contains("-")) change *= -1;

                    int actualSegundos = rpm.getSegundosPorCapitulo();
                    int nuevoSegundos = actualSegundos + (change * 60);

                    // Mínimo 1 segundo por capítulo
                    if (nuevoSegundos < 1) {
                        p.sendMessage("§cEl tiempo mínimo es 1 segundo.");
                    } else {
                        rpm.setSegundosPorCapitulo(nuevoSegundos);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1.2f);
                        int nuevosMinutos = nuevoSegundos / 60;
                        Bukkit.broadcastMessage("§e§lUHC ELOUD > §fSe ha ajustado la duración de las partes a: §b" + nuevosMinutos + " minutos§f.");
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
                        }
                    }
                    openTimePanel(p);
                }
                else if (item.getType().toString().contains("DYE")) {
                    rpm.setPausado(!rpm.isPausado());
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    openTimePanel(p);
                }
                else if (item.getType() == Material.ARROW) {
                    p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                    openMainAdminPanel(p);
                }
            }
        }

        // ---- CONSUMO ITEMS ESPECIALES
        @EventHandler
        public void onConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
            ItemStack item = event.getItem();

            if (item.getType() == Material.GOLDEN_APPLE && item.hasItemMeta()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.equals("§6§l§k! §e§lGOLDEN HEAD §6§l§k!")) {
                    Player p = event.getPlayer();
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
                    // Regeneración II por 12 seg
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 12 * 20, 1));
                    // Absorción II por 5 min
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 300 * 20, 1));
                }
            }
        }

        // ---- TRACKING DE BRÚJULAS DE SEGUIMIENTO
        public void onCompassTrack() {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plugin.getRightPanelManager().getJugadoresEliminados().contains(p.getName())) continue;

                Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());

                if (team == null || team.getEntries().size() <= 1) {
                    p.setCompassTarget(new Location(p.getWorld(), 0, p.getLocation().getY(), 0));
                    continue;
                }

                Player cercano = null;
                double distanciaMinima = Double.MAX_VALUE;

                for (String entry : team.getEntries()) {
                    if (entry.equals(p.getName())) continue;

                    Player compañero = Bukkit.getPlayer(entry);
                    if (compañero != null && compañero.isOnline() &&
                            !plugin.getRightPanelManager().getJugadoresEliminados().contains(entry) &&
                            compañero.getWorld().equals(p.getWorld())) {

                        double dist = p.getLocation().distance(compañero.getLocation());
                        if (dist < distanciaMinima) {
                            distanciaMinima = dist;
                            cercano = compañero;
                        }
                    }
                }
                if (cercano != null) {
                    p.setCompassTarget(cercano.getLocation());
                    ItemStack itemMano = p.getInventory().getItemInMainHand();
                    if (itemMano.getType() == Material.COMPASS && itemMano.hasItemMeta() &&
                            itemMano.getItemMeta().getDisplayName().contains("Localizador")) {

                        p.sendActionBar("§bRastreando a: §f" + cercano.getName() + " §7(§a" + (int)distanciaMinima + "m§7)");
                    }
                } else {
                    p.setCompassTarget(new Location(p.getWorld(), 0, 100, 0));
                }
            }
        }
    }
