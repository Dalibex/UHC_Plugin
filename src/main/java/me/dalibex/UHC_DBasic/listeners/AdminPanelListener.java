package me.dalibex.UHC_DBasic.listeners;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import static org.bukkit.GameRules.*;

/**
 * Listener especializado en el manejo de interfaces administrativas y restricciones de inventario.
 */
public class AdminPanelListener implements Listener {

    private final UHC_DBasic plugin;

    public AdminPanelListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        String title = legacySection().serialize(event.getView().title());
        LanguageManager lang = plugin.getLang();
        AdminPanelManager admin = plugin.getAdminPanel();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        // 1. Delegación según el título del menú
        if (title.equals(lang.get("menus.main-admin.title", p))) {
            event.setCancelled(true);
            handleMainAdminClick(p, event.getSlot(), event.isLeftClick(), event.isRightClick(), admin, lang);
        } else if (title.equals(lang.get("menus.team-selector.title", p))) {
            event.setCancelled(true);
            handleTeamSelectorClick(p, event.getSlot(), event.isRightClick());
        } else if (title.equals(lang.get("menus.generalrules.title", p))) {
            event.setCancelled(true);
            handleGeneralRulesClick(p, event.getSlot(), admin);
        } else if (title.equals(lang.get("menus.gamerules.title", p))) {
            event.setCancelled(true);
            handleGameRulesClick(p, item.getType(), admin);
        } else if (title.equals(lang.get("menus.gamemode.title", p))) {
            event.setCancelled(true);
            handleGamemodeClick(p, event.getSlot(), admin, lang);
        } else if (title.equals(lang.get("menus.barrier.title", p))) {
            event.setCancelled(true);
            handleBarrierClick(p, event.getSlot(), item.getType(), admin, lang);
        } else if (title.equals(lang.get("menus.time.title", p))) {
            event.setCancelled(true);
            handleTimeClick(p, event.getSlot(), item, admin, lang);
        }
        
        if (event.isCancelled()) {
            // Sonido base silencioso para evitar el doble clic molesto del cliente
        }

        // 2. Bloqueo de mano secundaria si está habilitado
        handleOffhandInventoryRestrictions(event, p, title, lang);
    }

    // --- MANEJO DE MENÚS (EXTRACTOS) ---

    private void handleMainAdminClick(Player p, int slot, boolean left, boolean right, AdminPanelManager admin, LanguageManager lang) {
        if (slot == 0) {
            if (left) admin.toggleCombate18(); else if (right) admin.toggleManoSecundaria();
            p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
            admin.openMainAdminPanel(p);
        } else if (slot == 1) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f); admin.openGeneralRulesPanel(p); }
        else if (slot == 2) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f); admin.openGameRulesPanel(p); }
        else if (slot == 3) {
            if (plugin.getGameManager().getTiempoTotalSegundos() > 0) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            admin.openGamemodePanel(p);
        }
        else if (slot == 4) { p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f); admin.openBarrierRulesPanel(p); }
        else if (slot == 6) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f); admin.openTimePanel(p); }
        else if (slot == 7) handleCustomTeamsToggle(p, admin, lang);
        else if (slot == 8) handleTeamSizeChange(p, slot, left, right, admin, lang);
    }

    private void handleCustomTeamsToggle(Player p, AdminPanelManager admin, LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getTiempoTotalSegundos() > 0) return;

        boolean newState = !tm.isCustomTeamsEnabled();
        if (newState) {
            if (tm.getTeamSize() <= 1) {
                p.sendMessage(lang.get("game.custom-teams-solos-error", p));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            tm.setCustomTeamsEnabled(true);
            tm.initializeCustomTeams();
            tm.giveAllSelectorItems();
        } else {
            tm.setCustomTeamsEnabled(false);
            tm.clearCustomTeams();
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
        }
        admin.openMainAdminPanel(p);
    }

    private void handleTeamSizeChange(Player p, int slot, boolean left, boolean right, AdminPanelManager admin, LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (plugin.getGameManager().getTiempoTotalSegundos() > 0) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int current = tm.getTeamSize();
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        
        if (left) {
            if (current < 4) {
                int next = current + 1;
                // Verificar que haya suficientes jugadores para al menos 2 equipos (o es modo solos)
                if (next > 1 && jugadoresOnline < next * 2) {
                    p.sendMessage(lang.get("game.team-size-error", p).replace("%min%", String.valueOf(next * 2)).replace("%n%", String.valueOf(next)));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                tm.setTeamSize(next);
            }
        } else if (right && current > 1) {
            int next = current - 1;
            tm.setTeamSize(next);
            if (next == 1 && tm.isCustomTeamsEnabled()) {
                tm.setCustomTeamsEnabled(false);
                tm.clearCustomTeams();
            }
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
        if (tm.isCustomTeamsEnabled()) { tm.initializeCustomTeams(); tm.giveAllSelectorItems(); }
        admin.openMainAdminPanel(p);
    }

    private void handleTeamSelectorClick(Player p, int slot, boolean right) {
        TeamManager tm = plugin.getTeamManager();
        if (right) { 
            if (tm.tryLeaveTeam(p)) { 
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f); 
                tm.openTeamSelectorGUI(p); 
            } else { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); }
        } else { 
            if (tm.tryJoinTeam(p, slot)) { 
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f); 
                tm.openTeamSelectorGUI(p); 
            } else { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); }
        }
    }

    private void handleGeneralRulesClick(Player p, int slot, AdminPanelManager admin) {
        if (slot == 11) { admin.setShulkerOneEnabled(!admin.isShulkerOneEnabled()); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f); }
        else if (slot == 15) { admin.setShulkerTwoEnabled(!admin.isShulkerTwoEnabled()); p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f); }
        else if (slot == 18) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        admin.openGeneralRulesPanel(p);
    }

    private void handleGameRulesClick(Player p, Material mat, AdminPanelManager admin) {
        if (mat == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        org.bukkit.GameRule<Boolean> rule = null;
        if (mat == Material.GOLDEN_APPLE) rule = NATURAL_HEALTH_REGENERATION;
        else if (mat == Material.PUFFERFISH) rule = ADVANCE_TIME;
        else if (mat == Material.ZOMBIE_HEAD) rule = SPAWN_MONSTERS;
        else if (mat == Material.CRAFTING_TABLE) rule = SHOW_ADVANCEMENT_MESSAGES;
        else if (mat == Material.VILLAGER_SPAWN_EGG) rule = SPAWN_WANDERING_TRADERS;
        else if (mat == Material.NETHERITE_SWORD) rule = PVP;
        else if (mat == Material.COMPASS) rule = LOCATOR_BAR;

        if (rule != null) {
            boolean newVal = !Bukkit.getWorlds().get(0).getGameRuleValue(rule);
            for (World w : Bukkit.getWorlds()) w.setGameRule(rule, newVal);
            p.playSound(p.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1f, newVal ? 1.5f : 0.8f);
        } else if (mat != Material.ARROW) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        admin.openGameRulesPanel(p);
    }

    private void handleGamemodeClick(Player p, int slot, AdminPanelManager admin, LanguageManager lang) {
        if (slot == 0) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        GameManager gm = plugin.getGameManager();
        if (slot == 1) { // Information Slot
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            return;
        }
        if (slot == 2) { 
            gm.cambiarModo(new me.dalibex.UHC_DBasic.gamemodes.Classic(plugin, gm)); 
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        }
        else if (slot == 4) { 
            gm.cambiarModo(new me.dalibex.UHC_DBasic.gamemodes.ResourceRush(plugin, gm)); 
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        }
        admin.openGamemodePanel(p);
    }

    private void handleBarrierClick(Player p, int slot, Material mat, AdminPanelManager admin, LanguageManager lang) {
        if (mat == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        
        GameManager gm = plugin.getGameManager();
        if (gm.getTiempoTotalSegundos() <= 0) {
            p.sendMessage(lang.get("game.border-not-started", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        World w = Bukkit.getWorlds().get(0);
        int amount = (slot == 10) ? -20 : (slot == 11) ? -200 : (slot == 19) ? -1000 : (slot == 20) ? -2000 :
                     (slot == 15) ? 20 : (slot == 16) ? 200 : (slot == 24) ? 1000 : (slot == 25) ? 2000 : 0;
        if (amount != 0) {
            double newSize = w.getWorldBorder().getSize() + amount;
            if (newSize < 20) {
                p.sendMessage(lang.get("menus.barrier.min-size-error", p));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            w.getWorldBorder().setSize(newSize);
            float pitch = (amount > 0) ? 1.5f : 0.8f;
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, pitch);
            p.sendMessage(lang.get("game.border-update", p).replace("%size%", String.valueOf((int)w.getWorldBorder().getSize())));
        }
        admin.openBarrierRulesPanel(p);
    }

    private void handleTimeClick(Player p, int slot, ItemStack item, AdminPanelManager admin, LanguageManager lang) {
        GameManager gm = plugin.getGameManager();
        if (item.getType() == Material.ARROW) { admin.openMainAdminPanel(p); p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f); return; }
        if (item.getType().toString().contains("DYE")) { 
            gm.setPausado(!gm.isPausado()); 
            p.playSound(p.getLocation(), gm.isPausado() ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
        else {
            int change = (slot == 10) ? -1 : (slot == 11) ? -5 : (slot == 12) ? -10 :
                         (slot == 14) ? 1 : (slot == 15) ? 5 : (slot == 16) ? 10 : 0;
            if (change != 0) {
                int nuevoTime = gm.getSegundosPorCapitulo() + (change * 60);
                if (nuevoTime > 0) {
                    gm.setSegundosPorCapitulo(nuevoTime);
                    float pitch = (change > 0) ? 1.2f : 0.8f;
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, pitch);
                    
                    String tiempoVisual = String.format("%02dh %02dm %02ds", nuevoTime / 3600, (nuevoTime % 3600) / 60, nuevoTime % 60);
                    p.sendMessage(lang.get("timer.success", p).replace("%prefix%", lang.get("general.prefix", p)).replace("%time%", tiempoVisual));
                } else {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
        admin.openTimePanel(p);
    }

    // --- RESTRICCIONES DE INVENTARIO Y MANO SECUNDARIA ---

    @EventHandler
    public void onOffhandSwap(PlayerSwapHandItemsEvent event) {
        if (AdminPanelManager.bloquearManoSecundaria) event.setCancelled(true);
    }

    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if (AdminPanelManager.bloquearManoSecundaria && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            Player p = event.getPlayer();
            if (p.getInventory().getItemInMainHand().getType() == Material.SHIELD || p.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                event.setCancelled(true);
            }
        }
    }

    private void handleOffhandInventoryRestrictions(InventoryClickEvent event, Player p, String title, LanguageManager lang) {
        if (!AdminPanelManager.bloquearManoSecundaria || p.getGameMode() == GameMode.CREATIVE) return;
        if (title.contains(lang.get("menus.main-admin.title", null))) return;

        if (event.getSlot() == 40 || event.getRawSlot() == 45 || event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
        }
    }
}
