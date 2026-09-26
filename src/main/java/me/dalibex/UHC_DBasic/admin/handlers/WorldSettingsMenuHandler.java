package me.dalibex.UHC_DBasic.admin.handlers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.admin.AdminPanelManager;
import me.dalibex.UHC_DBasic.gamemodes.Classic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.utils.AdminSlots;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.GameRules.*;

/** Handles gamerule, gamemode, border, and time admin menus. */
public class WorldSettingsMenuHandler {

    private final UHC_DBasic plugin;

    public WorldSettingsMenuHandler(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public void handleGameRulesClick(Player player, Material material, AdminPanelManager admin) {
        if (material == Material.ARROW) {
            admin.openMainAdminPanel(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            return;
        }
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY) return;
        org.bukkit.GameRule<Boolean> rule = null;
        if (material == Material.GOLDEN_APPLE) rule = NATURAL_HEALTH_REGENERATION;
        else if (material == Material.PUFFERFISH) rule = ADVANCE_TIME;
        else if (material == Material.ZOMBIE_HEAD) rule = SPAWN_MONSTERS;
        else if (material == Material.CRAFTING_TABLE) rule = SHOW_ADVANCEMENT_MESSAGES;
        else if (material == Material.VILLAGER_SPAWN_EGG) rule = SPAWN_WANDERING_TRADERS;
        else if (material == Material.NETHERITE_SWORD) rule = PVP;
        else if (material == Material.COMPASS) rule = LOCATOR_BAR;

        if (rule != null) {
            boolean newVal = !plugin.getWorldManager().getMainWorld().getGameRuleValue(rule);
            plugin.getWorldManager().setGameRuleForAllWorlds(rule, newVal);
            player.playSound(player.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1f, newVal ? 1.5f : 0.8f);
        } else if (material != Material.ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
        admin.openGameRulesPanel(player);
    }

    public void handleGamemodeClick(Player player, int slot, AdminPanelManager admin) {
        if (slot == AdminSlots.GAMEMODE_BACK) {
            admin.openMainAdminPanel(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            return;
        }
        GameManager gm = plugin.getGameManager();
        if (gm.getPhase() != GamePhase.LOBBY) return;
        if (slot == 1) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
            return;
        }
        if (slot == AdminSlots.GAMEMODE_CLASSIC) {
            gm.changeMode(new Classic(plugin, gm));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        } else if (slot == AdminSlots.GAMEMODE_RESOURCE_RUSH) {
            gm.changeMode(new ResourceRush(plugin, gm));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        }
        admin.openGamemodePanel(player);
    }

    public void handleBarrierClick(Player player, int slot, Material material, AdminPanelManager admin) {
        if (material == Material.ARROW) {
            admin.openMainAdminPanel(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            return;
        }
        GameManager gm = plugin.getGameManager();
        if (!gm.isMatchActive()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        World world = plugin.getWorldManager().getMainWorld();
        int amount = AdminSlots.borderDeltaForSlot(slot);
        if (amount != 0) {
            double newSize = world.getWorldBorder().getSize() + amount;
            if (newSize < 20) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            world.getWorldBorder().setSize(newSize);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, amount > 0 ? 1.5f : 0.8f);
        }
        admin.openBarrierRulesPanel(player);
    }

    public void handleTimeClick(Player player, int slot, ItemStack item, AdminPanelManager admin) {
        GameManager gm = plugin.getGameManager();
        if (item.getType() == Material.ARROW) {
            admin.openMainAdminPanel(player);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            return;
        }
        if (item.getType().toString().contains("DYE")) {
            if (!gm.isMatchActive()) return;
            gm.setPaused(!gm.isPaused());
            player.playSound(player.getLocation(), gm.isPaused() ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        } else {
            if (gm.getPhase() != GamePhase.LOBBY) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                admin.openTimePanel(player);
                return;
            }
            int change = AdminSlots.timeDeltaMinutesForSlot(slot);
            if (change != 0) {
                int newTime = gm.getSecondsPerChapter() + (change * 60);
                if (newTime > 0) {
                    gm.setSecondsPerChapter(newTime);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, change > 0 ? 1.2f : 0.8f);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }
        admin.openTimePanel(player);
    }
}
