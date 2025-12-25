package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.GameRules.*;

public class AdminPanel {

    // ------------------------------ DISEÑO PANELES ------------------------------
    public static void openMainAdminPanel(Player player) {
        Inventory mainGui = Bukkit.createInventory(null, 9, "§8⚙ Panel Administración UHC");
        TeamManager tm = UHC_DBasic.getPlugin(UHC_DBasic.class).getTeamManager();
        int teamSize = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();

        ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta ameta = pvpItem.getItemMeta();
        ameta.setDisplayName("§bAjustes de Combate Clásico");
        List<String> pvpLore = new ArrayList<>();
        pvpLore.add("§7Combate 1.8: " + (combate18 ? "§aActivado" : "§cDesactivado"));
        pvpLore.add("§7Escudos, Mano Secundaria: " + (bloquearManoSecundaria ? "§cBloqueados" : "§aPermitidos"));
        pvpLore.add("");
        pvpLore.add("§eClick Izquierdo: §7Toggle Cooldown 1.8");
        pvpLore.add("§bClick Derecho: §7Toggle Mano Secundaria/Escudos");
        ameta.setLore(pvpLore);
        pvpItem.setItemMeta(ameta);
        mainGui.setItem(0, pvpItem);

        ItemStack rulesItem = new ItemStack(Material.BOOK);
        ItemMeta meta = rulesItem.getItemMeta();
        meta.setDisplayName("§bAjustes de GameRules");
        meta.setLore(Arrays.asList("§7Haz click para ver y editar", "§7las reglas del servidor."));
        rulesItem.setItemMeta(meta);
        mainGui.setItem(2, rulesItem);
        player.openInventory(mainGui);

        ItemStack borderItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta bMeta = borderItem.getItemMeta();
        bMeta.setDisplayName("§5Ajustes del Borde");
        bMeta.setLore(Arrays.asList("§7Gestiona el tamaño ", "§7de la zona de juego."));
        borderItem.setItemMeta(bMeta);
        mainGui.setItem(4, borderItem);

        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta tMeta = timeItem.getItemMeta();
        tMeta.setDisplayName("§6Control de Partida");
        tMeta.setLore(Arrays.asList("§7Pausar, reanudar el cronómetro."));
        timeItem.setItemMeta(tMeta);
        mainGui.setItem(6, timeItem);

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
        mainGui.setItem(8, teamItem);
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
        rulesGui.setItem(16, createRuleItem(
                Material.COMPASS,
                "§bBarra de Localización",
                player.getWorld().getGameRuleValue(LOCATOR_BAR)
        ));
        // VOLVER
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cVolver");
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
    public static void openTimePanel(Player player) {
        Inventory timeGui = Bukkit.createInventory(null, 27, "§8⏲ Control de Tiempo");

        RightPanelManager rpm = UHC_DBasic.getPlugin(UHC_DBasic.class).getRightPanelManager();
        boolean estaPausado = rpm.isPausado();
        boolean partidaEnCurso = rpm.getTiempoTotalSegundos() > 0;
        int totalSecs = rpm.getSegundosPorCapitulo();

        int h = totalSecs / 3600;
        int m = (totalSecs % 3600) / 60;
        int s = totalSecs % 60;
        String tiempoVisual = String.format("%02dh %02dm %02ds", h, m, s);

        // INFO CENTRAL (Slot 13)
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName("§eTiempo por Capítulo");
        iMeta.setLore(Arrays.asList(
                "§fDuración actual: §b" + tiempoVisual,
                "",
                "§7Puedes usar §f/tpartes H M S",
                "§7para un ajuste preciso."
        ));
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
    // ----------------------------------------------------------------------------

    private static ItemStack createBorderItem(Material mat, String name, int amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Cambiar el borde", "§7en §f" + amount + " §7bloques."));
        item.setItemMeta(meta);
        return item;
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

    // ------------------------------ AJUSTES DE COMBATE CLASICO ------------------------------
    public static boolean combate18 = false;
    public static void toggleCombate18(Player admin) {
        combate18 = !combate18;
        double speedValue = combate18 ? 100.0 : 4.0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            org.bukkit.attribute.AttributeInstance attr = p.getAttribute(Attribute.ATTACK_SPEED);
            if (attr != null) {
                attr.setBaseValue(speedValue);
            }
        }

        String estado = combate18 ? "§aACTIVADO" : "§cDESACTIVADO";
        Bukkit.broadcastMessage("§e§lUHC ELOUD > §fEl combate 1.8 ha sido " + estado);
    }

    public static boolean bloquearManoSecundaria = false;
    public static void toggleManoSecundaria(Player admin) {
        bloquearManoSecundaria = !bloquearManoSecundaria;
        String estado = bloquearManoSecundaria ? "§cBLOQUEADOS" : "§aHABILITADOS";
        Bukkit.broadcastMessage("§e§lUHC ELOUD > §fLa mano secundaria y escudos han sido " + estado);
    }
    // ----------------------------------------------------------------------------------------

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
