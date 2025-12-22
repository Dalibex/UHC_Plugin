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

        ItemStack borderItem = new ItemStack(Material.EMERALD_BLOCK);
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

        RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;

        ItemStack teamItem = new ItemStack(partidaEnCurso ? Material.BARRIER : Material.WHITE_BANNER);
        ItemMeta cMeta = teamItem.getItemMeta();
        cMeta.setDisplayName(partidaEnCurso ? "§7§mConfigurar Equipos" : "§aConfigurar Equipos");

        int numEquipos = (jugadoresOnline == 0) ? 0 : (int) Math.ceil((double) jugadoresOnline / teamSize);
        List<String> lore = new ArrayList<>();

        if (partidaEnCurso) {
            lore.add("§c⚠ BLOQUEADO");
            lore.add("§7No puedes cambiar los equipos");
            lore.add("§7una vez iniciada la partida.");
            cMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            cMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add("§7Tamaño actual: §e" + (teamSize == 1 ? "Solos" : "Equipos de " + teamSize));
            lore.add("");
            lore.add("§fJugadores online: §b" + jugadoresOnline);
            lore.add("§fEquipos resultantes: §b" + numEquipos);
            lore.add("");
            lore.add("§bClick Izquierdo: §7Aumentar (+1)");
            lore.add("§bClick Derecho: §7Reducir (-1)");
        }
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
        Inventory timeGui = Bukkit.createInventory(null, 27, "§8⏲ Control de Tiempo");

        RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();
        boolean estaPausado = rpm.isPausado();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;
        int minPorCapitulo = rpm.getSegundosPorCapitulo() / 60;

        // INFO CENTRAL (Slot 13)
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName("§eTiempo por Capítulo");
        iMeta.setLore(Arrays.asList("§fCada parte dura: §b" + minPorCapitulo + " min", "", "§7Ajusta esto antes de empezar."));
        info.setItemMeta(iMeta);
        timeGui.setItem(13, info);

        /* BOTONES PARA QUITAR */
        timeGui.setItem(10, createTimeBtn(Material.RED_STAINED_GLASS_PANE, "§c-1 Minuto", -1, partidaEnCurso));
        timeGui.setItem(11, createTimeBtn(Material.RED_WOOL, "§c-5 Minutos", -5, partidaEnCurso));
        timeGui.setItem(12, createTimeBtn(Material.RED_CONCRETE, "§c-10 Minutos", -10, partidaEnCurso));

        // BOTONES PARA AÑADIR
        timeGui.setItem(14, createTimeBtn(Material.GREEN_STAINED_GLASS_PANE, "§a+1 Minuto", 1, partidaEnCurso));
        timeGui.setItem(15, createTimeBtn(Material.GREEN_WOOL, "§a+5 Minutos", 5, partidaEnCurso));
        timeGui.setItem(16, createTimeBtn(Material.GREEN_CONCRETE, "§a+10 Minutos", 10, partidaEnCurso));

        // PAUSA / REANUDAR
        ItemStack pauseBtn = new ItemStack(estaPausado ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta pMeta = pauseBtn.getItemMeta();
        pMeta.setDisplayName(estaPausado ? "§a▶ Reanudar Partida" : "§7⏸ Pausar Partida");
        pauseBtn.setItemMeta(pMeta);
        timeGui.setItem(21, pauseBtn);

        // VOLVER
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName("§cVolver");
        back.setItemMeta(bMeta);
        timeGui.setItem(18, back);

        player.openInventory(timeGui);
    }

    private static ItemStack createTimeBtn(Material mat, String name, int amount, boolean bloqueado) {
        ItemStack item = new ItemStack(bloqueado ? Material.BARRIER : mat);
        ItemMeta meta = item.getItemMeta();

        if (bloqueado) {
            meta.setDisplayName("§7§m" + name);
            meta.setLore(Arrays.asList("§c⚠ BLOQUEADO", "§7La partida ya ha comenzado."));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("§7Cambia la duración en", "§f" + amount + " §7minutos por parte."));
        }

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8⚙ Panel Administración UHC")) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            int slot = event.getSlot();

            if (slot == 1) {
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openBarrierRulesPanel(p);
            }
            else if (slot == 3) {
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
            }
            else if (slot == 5) {
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openTimePanel(p);
            }
            else if (slot == 7) {
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
                openGameRulesPanel(p);
            } else if (mat == Material.PUFFERFISH) {
                boolean current = p.getWorld().getGameRuleValue(ADVANCE_TIME);
                p.getWorld().setGameRule(ADVANCE_TIME, !current);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
            } else if (mat == Material.ZOMBIE_HEAD) {
                boolean current = p.getWorld().getGameRuleValue(SPAWN_MONSTERS);
                p.getWorld().setGameRule(SPAWN_MONSTERS, !current);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
            } else if (mat == Material.CRAFTING_TABLE) {
                boolean current = p.getWorld().getGameRuleValue(SHOW_ADVANCEMENT_MESSAGES);
                p.getWorld().setGameRule(SHOW_ADVANCEMENT_MESSAGES, !current);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
            } else if (mat == Material.VILLAGER_SPAWN_EGG) {
                boolean current = p.getWorld().getGameRuleValue(SPAWN_WANDERING_TRADERS);
                p.getWorld().setGameRule(SPAWN_WANDERING_TRADERS, !current);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
            } else if (mat == Material.NETHERITE_SWORD) {
                boolean current = p.getWorld().getGameRuleValue(PVP);
                p.getWorld().setGameRule(PVP, !current);
                p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1, 1);
                openGameRulesPanel(p);
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
                openBarrierRulesPanel(p);
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

                // Mínimo 1 minuto por capítulo
                if (nuevoSegundos < 60) {
                    p.sendMessage("§cEl tiempo mínimo es 1 minuto.");
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
