package me.dalibex.UHC_DBasic.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.ResourceRush;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class ResourceRushListener implements Listener {

    private final UHC_DBasic plugin;

    private final Map<UUID, Long> ultimoShift = new HashMap<>();
    private final Map<UUID, Integer> contadorShift = new HashMap<>();

    public ResourceRushListener(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItem(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getCurrentItem();
            if (result == null || result.getType() == Material.AIR) return;
            Material material = result.getType();
            int before = countMaterial(player, material);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (countMaterial(player, material) > before) {
                    checkItem(player, material);
                }
            });
        }
    }

    private int countMaterial(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) total += item.getAmount();
        }
        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor.getType() == material) total += cursor.getAmount();
        return total;
    }

    private void checkItem(Player player, Material material) {
        if (plugin.getGameManager().isMatchActive()
                && player.getGameMode() == GameMode.SURVIVAL
                && plugin.getGameManager().getInitialParticipants().contains(player.getName())
                && !plugin.getGameManager().getEliminatedPlayers().contains(player.getName()) &&
                plugin.getGameManager().getCurrentMode() instanceof ResourceRush rr) {

            if (rr.getActiveObjectives().contains(material)) {
                rr.completeObjective(player, material);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (!plugin.getGameManager().isMatchActive()) return;

        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        long ahora = System.currentTimeMillis();

        if (!ultimoShift.containsKey(uuid) || (ahora - ultimoShift.get(uuid) > 3000)) {
            contadorShift.put(uuid, 1);
        } else {
            int cuenta = contadorShift.get(uuid) + 1;
            if (cuenta >= 3) {
                showObjectivesSummary(p);
                contadorShift.put(uuid, 0);
            } else {
                contadorShift.put(uuid, cuenta);
            }
        }
        ultimoShift.put(uuid, ahora);
    }

    private void showObjectivesSummary(Player p) {
        LanguageManager lang = plugin.getLang();
        if (!(plugin.getGameManager().getCurrentMode() instanceof ResourceRush)) return;
        ResourceRush rr = (ResourceRush) plugin.getGameManager().getCurrentMode();

        List<Material> activos = rr.getActiveObjectives();
        List<Material> conseguidos = rr.getPlayerAchievements(p);

        p.sendMessage("");
        p.sendMessage(legacySection().deserialize(lang.get("resource-rush.summary.header", p)));

        if (activos.isEmpty()) {
            p.sendMessage(legacySection().deserialize(lang.get("resource-rush.summary.empty", p)));
        } else {
            String formatDone = lang.get("resource-rush.summary.item-done", p);
            String formatPending = lang.get("resource-rush.summary.item-pending", p);

            for (Material mat : activos) {
                String nombreMat = mat.name().replace("_", " ").toLowerCase(Locale.ROOT);
                String line = conseguidos.contains(mat) ? formatDone : formatPending;
                p.sendMessage(legacySection().deserialize(line.replace("%item%", nombreMat)));
            }
        }

        p.sendMessage(legacySection().deserialize(lang.get("resource-rush.summary.footer", p)
                .replace("%done%", String.valueOf(conseguidos.size()))
                .replace("%total%", String.valueOf(activos.size()))));

        p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);
    }
}
