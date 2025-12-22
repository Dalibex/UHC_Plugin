package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.GameRules.*;

public class AdminPanel implements Listener {

    public static void openMainAdminPanel(Player player) {
        Inventory mainGui = Bukkit.createInventory(null, 9, "§8⚙ Panel Administración UHC");
        TeamManager tm = UHC_DBasic.getPlugin(UHC_DBasic.class).getTeamManager();
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        ItemStack rulesItem = new ItemStack(Material.BOOK);
        ItemMeta meta = rulesItem.getItemMeta();
        meta.setDisplayName("§bAjustes de GameRules");
        meta.setLore(Arrays.asList("§7Haz click para ver y editar", "§7las reglas del servidor."));
        rulesItem.setItemMeta(meta);

        mainGui.setItem(3, rulesItem);
        player.openInventory(mainGui);

        ItemStack borderItem = new ItemStack(Material.BARRIER);
        ItemMeta bMeta = borderItem.getItemMeta();
        bMeta.setDisplayName("§5Ajustes del Borde");
        bMeta.setLore(Arrays.asList("§7Gestiona el tamaño ", "§7de la zona de juego."));
        borderItem.setItemMeta(bMeta);
        mainGui.setItem(1, borderItem);

        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta tMeta = timeItem.getItemMeta();
        tMeta.setDisplayName("§6Control de Partida");
        tMeta.setLore(Arrays.asList("§7Pausar, reanudar el cronómetro."));
        timeItem.setItemMeta(tMeta);
        mainGui.setItem(5, timeItem);

        ItemStack teamItem = new ItemStack(Material.WHITE_BANNER);
        ItemMeta cMeta = teamItem.getItemMeta();
        cMeta.setDisplayName("§aConfigurar Equipos");
        int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);
        List<String> lore = new ArrayList<>();
        lore.add("§7Tamaño actual: §e" + (teamSize == 1 ? "Solos" : "Equipos de " + teamSize));
        lore.add("");
        lore.add("§fJugadores online: §b" + jugadoresOnline);
        lore.add("§fEquipos resultantes: §b" + numEquipos);
        lore.add("");
        lore.add("§bClick Izquierdo: §7Aumentar (+1)");
        lore.add("§bClick Derecho: §7Reducir (-1)");

        if (teamSize > 1 && jugadoresOnline % teamSize != 0 && jugadoresOnline > 0) {
            lore.add("");
            lore.add("§e⚠ §iEl reparto será equilibrado (ej: 3, 2, 2)");
        }

        cMeta.setLore(lore);
        teamItem.setItemMeta(cMeta);
        mainGui.setItem(7, teamItem);
    }

    public void openGameRulesPanel(Player player) {
        Inventory rulesGui = Bukkit.createInventory(null, 36, "§8⚖ GameRules del UHC");

        // REGENERACION NATURAL
        rulesGui.setItem(10, createRuleItem(
                Material.GOLDEN_APPLE,
                "§eRegeneración Natural",
                player.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION)
        ));
        // PVP
        rulesGui.setItem(11, createRuleItem(
                Material.NETHERITE_SWORD,
                "§aPVP",
                player.getWorld().getGameRuleValue(PVP)
        ));
        // CICLO DIA NOCHE
        rulesGui.setItem(12, createRuleItem(
                Material.PUFFERFISH,
                "§aCiclo día/noche",
                player.getWorld().getGameRuleValue(ADVANCE_TIME)
        ));
        // SPAWN DE MOBS HOSTILES
        rulesGui.setItem(13, createRuleItem(
                Material.ZOMBIE_HEAD,
                "§aSpawn de Enemigos",
                player.getWorld().getGameRuleValue(SPAWN_MONSTERS)
        ));
        // ANUNCIAR LOGROS POR CHAT
        rulesGui.setItem(14, createRuleItem(
                Material.CRAFTING_TABLE,
                "§aAnuncio de Logros",
                player.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES)
        ));
        // SPAWNEAR WANDERING TRADER
        rulesGui.setItem(15, createRuleItem(
                Material.VILLAGER_SPAWN_EGG,
                "§aSpawn del Wandering Trader",
                player.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS)
        ));
        // VOLVER
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cVolver al menú principal");
        back.setItemMeta(backMeta);
        rulesGui.setItem(27, back);

        player.openInventory(rulesGui);
    }

    public static void openBarrierRulesPanel(Player player) {
        Inventory barrierGui = Bukkit.createInventory(null, 36, "§8⚖ Ajustar tamaño de barrera");

        double currentSize = player.getWorld().getWorldBorder().getSize();

        // INFO
        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName("§bTamaño Actual");
        iMeta.setLore(Arrays.asList("§fEl borde mide: §6" + (int)currentSize + "x" + (int)currentSize));
        info.setItemMeta(iMeta);
        barrierGui.setItem(13, info);

        // REDUCIR
        barrierGui.setItem(10, createBorderItem(Material.RED_STAINED_GLASS_PANE, "§c-10 Bloques", -10));
        barrierGui.setItem(11, createBorderItem(Material.RED_WOOL, "§c-100 Bloques", -100));
        barrierGui.setItem(19, createBorderItem(Material.RED_CONCRETE_POWDER, "§c-500 Bloques", -500));
        barrierGui.setItem(20, createBorderItem(Material.RED_CONCRETE, "§c-1000 Bloques", -1000));

        // AUMENTAR
        barrierGui.setItem(15, createBorderItem(Material.GREEN_STAINED_GLASS_PANE, "§a+10 Bloques", 10));
        barrierGui.setItem(16, createBorderItem(Material.GREEN_WOOL, "§a+100 Bloques", 100));
        barrierGui.setItem(24, createBorderItem(Material.GREEN_CONCRETE_POWDER, "§a+500 Bloques", 500));
        barrierGui.setItem(25, createBorderItem(Material.GREEN_CONCRETE, "§a+1000 Bloques", 1000));

        // Volver
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cVolver");
        back.setItemMeta(backMeta);
        barrierGui.setItem(31, back);

        player.openInventory(barrierGui);
    }

    private static ItemStack createBorderItem(Material mat, String name, int amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Cambiar el borde", "§7en §f" + amount + " §7bloques."));
        item.setItemMeta(meta);
        return item;
    }

    public static void openTimePanel(Player player) {
        Inventory timeGui = Bukkit.createInventory(null, 9, "§8⏲ Control de Tiempo");

        RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();
        boolean estaPausado = rpm.isPausado();

        // BOTÓN REANUDAR
        ItemStack resume = new ItemStack(Material.GREEN_WOOL);
        ItemMeta rMeta = resume.getItemMeta();
        rMeta.setDisplayName(estaPausado ? "§a▶ Reanudar Cronómetro" : "§7El cronómetro ya está corriendo");
        resume.setItemMeta(rMeta);
        timeGui.setItem(3, resume);

        // BOTÓN PAUSAR
        ItemStack pause = new ItemStack(Material.RED_WOOL);
        ItemMeta pMeta = pause.getItemMeta();
        pMeta.setDisplayName(estaPausado ? "§7El cronómetro ya está pausado" : "§c⏸ Pausar Cronómetro");
        pause.setItemMeta(pMeta);
        timeGui.setItem(5, pause);

        // VOLVER
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§cVolver");
        back.setItemMeta(bMeta);
        timeGui.setItem(0, back);

        player.openInventory(timeGui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8⚙ Panel Administración UHC")) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();

            if (event.getCurrentItem() == null) return;
            if (event.getCurrentItem().getType() == Material.BOOK) {
                openGameRulesPanel((Player) event.getWhoClicked());
            }
            else if (event.getCurrentItem().getType() == Material.BARRIER) {
                openBarrierRulesPanel((Player) event.getWhoClicked());
            }
            else if (event.getCurrentItem().getType() == Material.CLOCK) {
                openTimePanel((Player) event.getWhoClicked());
            }
            else if (event.getCurrentItem().getType() == Material.WHITE_BANNER) {
                TeamManager tm = UHC_DBasic.getPlugin(UHC_DBasic.class).getTeamManager();
                int current = tm.getTeamSize();
                int jugadoresOnline = Bukkit.getOnlinePlayers().size();

                if (event.isLeftClick()) {
                    int proximoTamaño = current + 1;
                    if (proximoTamaño <= 4 && jugadoresOnline >= (proximoTamaño * 2)) {
                        tm.setTeamSize(proximoTamaño);
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                    } else if (proximoTamaño > 4) {
                        p.sendMessage("§cEl tamaño máximo de equipo es 4.");
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1, 1);
                    } else {
                        p.sendMessage("§c¡No hay suficientes jugadores! Necesitas al menos " + (proximoTamaño * 2) + " para equipos de " + proximoTamaño);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }
                }
                else if (event.isRightClick() && current > 1) {
                    tm.setTeamSize(current - 1);
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
                }

                openMainAdminPanel(p);
            }
        }
        else if (event.getView().getTitle().equals("§8⚖ GameRules del UHC")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player p = (Player) event.getWhoClicked();
            Material mat = event.getCurrentItem().getType();

            // LOGICA CAMBIO DE LOS GAMERULES
            if (mat == Material.GOLDEN_APPLE) {
                boolean current = p.getWorld().getGameRuleValue(NATURAL_HEALTH_REGENERATION);
                p.getWorld().setGameRule(NATURAL_HEALTH_REGENERATION, !current);
                openGameRulesPanel(p);
            } else if (mat == Material.PUFFERFISH) {
                boolean current = p.getWorld().getGameRuleValue(ADVANCE_TIME);
                p.getWorld().setGameRule(ADVANCE_TIME, !current);
                openGameRulesPanel(p);
            } else if (mat == Material.ZOMBIE_HEAD) {
                boolean current = p.getWorld().getGameRuleValue(SPAWN_MONSTERS);
                p.getWorld().setGameRule(SPAWN_MONSTERS, !current);
                openGameRulesPanel(p);
            } else if (mat == Material.CRAFTING_TABLE) {
                boolean current = p.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES);
                p.getWorld().setGameRule(SHOW_ADVANCEMENT_MESSAGES, !current);
                openGameRulesPanel(p);
            } else if (mat == Material.VILLAGER_SPAWN_EGG) {
                boolean current = p.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS);
                p.getWorld().setGameRule(SPAWN_WANDERING_TRADERS, !current);
                openGameRulesPanel(p);
            } else if (mat == Material.NETHERITE_SWORD) {
                boolean current = p.getWorld().getGameRuleValue(PVP);
                p.getWorld().setGameRule(PVP, !current);
                openGameRulesPanel(p);
            }
            else if (mat == Material.ARROW) {
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
                openMainAdminPanel(p);
                return;
            }
            if (p.getWorld().getWorldBorder().getSize() > 5999980) {
                p.sendMessage("§c§l⚠ §cNo puedes ajustar el borde porque la partida no ha empezado.");
                p.closeInventory();
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
                openBarrierRulesPanel(p);
            }
        }

        // LÓGICA TIMER / PAUSA
        else if (event.getView().getTitle().equals("§8⏲ Control de Tiempo")) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            Material mat = event.getCurrentItem().getType();

            RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();

            if (mat == Material.GREEN_WOOL) {
                // REANUDAR
                rpm.setPausado(false);
                p.sendMessage("§a§l▶ §fEl cronómetro del UHC ha sido §aREANUDADO§f.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.2f);
                openTimePanel(p);
            }
            else if (mat == Material.RED_WOOL) {
                // PAUSAR
                rpm.setPausado(true);
                p.sendMessage("§c§l⏸ §fEl cronómetro del UHC ha sido §cPAUSADO§f.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0.5f);
                openTimePanel(p);
            }
            else if (mat == Material.ARROW) {
                openMainAdminPanel(p);
            }
        }
    }

    @EventHandler // MUERTE DE JUGADOR, PARA NO CREAR OTRA CLASE LISTENER LO PONGO AQUI
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player p = event.getEntity();
        p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        p.getWorld().strikeLightningEffect(p.getLocation());
    }

    private ItemStack createRuleItem(Material mat, String name, Boolean state) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(
                "§7Estado: " + (state ? "§aActivado" : "§cDesactivado"),
                "",
                "§eClick para cambiar"
        ));
        item.setItemMeta(meta);
        return item;
    }
}
