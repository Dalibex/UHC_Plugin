package me.dalibex.UHC_DBasic.managers.teams;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Builds and owns the custom team selector item and GUI. */
class TeamSelectorMenu {

    private final UHC_DBasic plugin;
    private final NamespacedKey teamSelectorKey;

    TeamSelectorMenu(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.teamSelectorKey = new NamespacedKey(plugin, "team_selector");
    }

    void giveSelectorItem(Player player) {
        LanguageManager lang = plugin.getLang();
        removeSelectorItem(player);

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        meta.displayName(lang.getComponent("items.team-selector.name", player));
        meta.lore(lang.getComponentList("items.team-selector.lore", player));
        meta.getPersistentDataContainer().set(teamSelectorKey, PersistentDataType.BYTE, (byte) 1);
        selector.setItemMeta(meta);

        player.getInventory().setItem(8, selector);
    }

    void removeSelectorItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isSelector(item)) player.getInventory().setItem(i, null);
        }
    }

    boolean isSelector(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(teamSelectorKey, PersistentDataType.BYTE);
    }

    void open(Player player, List<Team> teams, TeamMembershipStore membership, int teamSize) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !plugin.getTeamManager().isCustomTeamsEnabled()) return;
        LanguageManager lang = plugin.getLang();
        int invSize = Math.max(9, (int) Math.ceil(teams.size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(null, invSize, lang.getComponent("menus.team-selector.title", player));

        int slot = 0;
        for (Team team : teams) {
            int teamIndex = TeamPalette.indexFor(team);
            ItemStack item = new ItemStack(TeamPalette.dye(teamIndex));
            ItemMeta meta = item.getItemMeta();

            meta.displayName(TextUtil.item(
                    lang.get("menus.team-selector.team-item.name", player)
                            .replace("%name%", legacySection().serialize(team.displayName()))));
            List<Component> lore = new ArrayList<>();
            int current = membership.getMemberCount(team);
            for (String line : lang.getList("menus.team-selector.team-item.lore", player)) {
                lore.add(TextUtil.item(line.replace("%current%", String.valueOf(current)).replace("%max%", String.valueOf(teamSize))));
            }
            for (String entry : membership.getMemberNames(team)) {
                lore.add(TextUtil.item(lang.get("menus.team-selector.member-format", player).replace("%player%", entry)));
            }
            for (int i = 0; i < teamSize - current; i++) lore.add(TextUtil.item(lang.get("menus.team-selector.empty-slot", player)));

            Team playerTeam = membership.getPlayerTeam(player.getName());
            if (playerTeam != null && playerTeam.equals(team)) {
                lore.add(Component.empty());
                lore.add(TextUtil.item("§a✔ Tu equipo actual"));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }
        player.openInventory(gui);
    }
}
