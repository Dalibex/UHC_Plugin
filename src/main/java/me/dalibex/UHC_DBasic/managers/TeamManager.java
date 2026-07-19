package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class TeamManager {

    private final UHC_DBasic plugin;
    private final Scoreboard board;
    private int teamSize = 1;
    private boolean customTeamsEnabled = false;

    private static final Material[] TEAM_DYES = {
            Material.RED_DYE, Material.BLUE_DYE, Material.GREEN_DYE,
            Material.YELLOW_DYE, Material.ORANGE_DYE, Material.PURPLE_DYE,
            Material.CYAN_DYE, Material.PINK_DYE, Material.LIME_DYE,
            Material.LIGHT_BLUE_DYE, Material.MAGENTA_DYE, Material.WHITE_DYE
    };

    private static final String[] TEAM_COLOR_KEYS = {
            "red", "blue", "green", "yellow", "orange", "purple",
            "cyan", "pink", "lime", "light_blue", "magenta", "white"
    };

    private static final NamedTextColor[] TEAM_NAMED_COLORS = {
            NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.DARK_GREEN,
            NamedTextColor.YELLOW, NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE,
            NamedTextColor.DARK_AQUA, NamedTextColor.LIGHT_PURPLE, NamedTextColor.GREEN,
            NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE, NamedTextColor.WHITE
    };

    private static final String[] TEAM_LEGACY_CODES = {
            "§c", "§9", "§2", "§e", "§6", "§5", "§3", "§d", "§a", "§b", "§d", "§f"
    };

    public TeamManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public boolean isCustomTeamsEnabled() { return customTeamsEnabled; }
    public void setCustomTeamsEnabled(boolean enabled) { this.customTeamsEnabled = enabled; }
    public void setTeamSize(int size) { this.teamSize = size; }
    public int getTeamSize() { return teamSize; }

    public void initializeCustomTeams() {
        borrarTodosLosEquipos();
        LanguageManager lang = plugin.getLang();

        int numeroDeEquipos = TEAM_COLOR_KEYS.length;

        for (int i = 0; i < numeroDeEquipos; i++) {
            String colorKey = TEAM_COLOR_KEYS[i % TEAM_COLOR_KEYS.length];
            Team team = board.registerNewTeam(colorKey);
            NamedTextColor color = TEAM_NAMED_COLORS[i % TEAM_NAMED_COLORS.length];
            team.color(color);

            String localizedName = lang.get("teams.colors." + colorKey, null);
            team.displayName(Component.text(localizedName != null ? localizedName : colorKey));

            String legacyCode = TEAM_LEGACY_CODES[i % TEAM_LEGACY_CODES.length];
            String prefix = lang.get("teams.prefix-format", null)
                    .replace("%color%", legacyCode)
                    .replace("%name%", legacySection().serialize(team.displayName()));
            team.prefix(legacySection().deserialize(prefix));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    public Team getTeamByColorSearch(String input) {
        String lower = input.toLowerCase();
        for (Team team : board.getTeams()) {
            if (team.getName().equalsIgnoreCase(lower)) return team;
            if (legacySection().serialize(team.displayName()).equalsIgnoreCase(input)) return team;
        }
        return null;
    }

    public void giveTeamSelectorItem(Player p) {
        LanguageManager lang = plugin.getLang();
        removeTeamSelectorItem(p);

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        meta.displayName(lang.getComponent("items.team-selector.name", p));
        meta.lore(lang.getComponentList("items.team-selector.lore", p));
        selector.setItemMeta(meta);

        p.getInventory().setItem(8, selector);
    }

    public void removeTeamSelectorItem(Player p) {
        LanguageManager lang = plugin.getLang();
        Component selectorName = lang.getComponent("items.team-selector.name", p);

        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().displayName().equals(selectorName)) {
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    public void openTeamSelectorGUI(Player p) {
        LanguageManager lang = plugin.getLang();
        Set<Team> teams = board.getTeams();
        int invSize = Math.max(9, (int) Math.ceil(teams.size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(null, invSize, lang.getComponent("menus.team-selector.title", p));

        int slot = 0;
        for (Team team : teams) {
            int teamIndex = getTeamIndexForGui(team);
            Material dyeMat = TEAM_DYES[teamIndex % TEAM_DYES.length];
            ItemStack item = new ItemStack(dyeMat);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(legacySection().deserialize(
                    lang.get("menus.team-selector.team-item.name", p).replace("%name%", legacySection().serialize(team.displayName()))).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            int current = team.getEntries().size();
            for (String l : lang.getList("menus.team-selector.team-item.lore", p)) {
                lore.add(legacySection().deserialize(l.replace("%current%", String.valueOf(current)).replace("%max%", String.valueOf(teamSize))).decoration(TextDecoration.ITALIC, false));
            }
            for (String entry : team.getEntries()) {
                lore.add(legacySection().deserialize(lang.get("menus.team-selector.member-format", p).replace("%player%", entry)).decoration(TextDecoration.ITALIC, false));
            }
            int huecos = teamSize - current;
            for (int h = 0; h < huecos; h++) lore.add(legacySection().deserialize(lang.get("menus.team-selector.empty-slot", p)).decoration(TextDecoration.ITALIC, false));

            Team playerTeam = board.getEntryTeam(p.getName());
            if (playerTeam != null && playerTeam.equals(team)) {
                lore.add(Component.empty());
                lore.add(legacySection().deserialize("§a✔ Tu equipo actual").decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }
        p.openInventory(gui);
    }

    public boolean tryJoinTeam(Player p, int slot) {
        LanguageManager lang = plugin.getLang();
        List<Team> teamList = new ArrayList<>(board.getTeams());
        if (slot < 0 || slot >= teamList.size()) return false;
        Team target = teamList.get(slot);
        Team current = board.getEntryTeam(p.getName());

        if (current != null && current.equals(target)) {
            p.sendMessage(lang.get("menus.team-selector.already-in-team", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        if (target.getEntries().size() >= teamSize) {
            p.sendMessage(lang.get("menus.team-selector.already-full", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        if (current != null) current.removeEntry(p.getName());
        target.addEntry(p.getName());
        p.sendMessage(lang.get("menus.team-selector.joined", p).replace("%name%", legacySection().serialize(target.displayName().color(target.color()))));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }

    public boolean tryLeaveTeam(Player p) {
        Team current = board.getEntryTeam(p.getName());
        if (current != null) {
            current.removeEntry(p.getName());
            p.sendMessage(plugin.getLang().get("menus.team-selector.left", p));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return true;
        }
        return false;
    }

    public boolean allPlayersHaveTeam() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (board.getEntryTeam(p.getName()) == null) return false;
        }
        return true;
    }

    public int getTeamsWithPlayersCount() {
        Set<Team> used = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            Team t = board.getEntryTeam(p.getName());
            if (t != null) used.add(t);
        }
        return used.size();
    }

    public void removeAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) removeTeamSelectorItem(p);
    }

    public void clearCustomTeams() {
        borrarTodosLosEquipos();
        removeAllSelectorItems();
    }


    public void giveAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR) giveTeamSelectorItem(p);
        }
    }

    private int getTeamIndexForGui(Team team) {
        String name = team.getName().toLowerCase();
        for (int i = 0; i < TEAM_COLOR_KEYS.length; i++) {
            if (TEAM_COLOR_KEYS[i].equalsIgnoreCase(name)) return i;
        }
        return 0;
    }

    public void shuffleTeams() {
        borrarTodosLosEquipos();
        LanguageManager lang = plugin.getLang();
        List<String> vivos = new ArrayList<>();
        List<String> muertos = new ArrayList<>();
        for (String name : plugin.getGameManager().getParticipantesIniciales()) {
            if (plugin.getGameManager().getJugadoresEliminados().contains(name)) muertos.add(name);
            else vivos.add(name);
        }
        if (vivos.isEmpty() && muertos.isEmpty()) return;
        Collections.shuffle(vivos);
        Collections.shuffle(muertos);

        int total = vivos.size() + muertos.size();
        int numeroDeEquipos = (int) Math.ceil((double) total / teamSize);
        List<Team> listaEquipos = new ArrayList<>();

        for (int i = 0; i < numeroDeEquipos; i++) {
            String colorKey = TEAM_COLOR_KEYS[i % TEAM_COLOR_KEYS.length];
            Team team = board.registerNewTeam(colorKey);
            NamedTextColor color = TEAM_NAMED_COLORS[i % TEAM_NAMED_COLORS.length];
            team.color(color);
            String localizedName = lang.get("teams.colors." + colorKey, null);
            team.displayName(Component.text(localizedName != null ? localizedName : colorKey));
            String legacyCode = TEAM_LEGACY_CODES[i % TEAM_LEGACY_CODES.length];
            String prefix = lang.get("teams.prefix-format", null).replace("%color%", legacyCode).replace("%name%", legacySection().serialize(team.displayName()));
            team.prefix(legacySection().deserialize(prefix));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            listaEquipos.add(team);
        }

        for (int i = 0; i < vivos.size(); i++) asignarEquipoPorNombre(vivos.get(i), listaEquipos.get(i % numeroDeEquipos), lang);
        if (!listaEquipos.isEmpty()) {
            for (String m : muertos) {
                Team MasVacio = listaEquipos.stream().min(Comparator.comparingInt(t -> t.getEntries().size())).get();
                asignarEquipoPorNombre(m, MasVacio, lang);
            }
        }
    }

    private void asignarEquipoPorNombre(String name, Team team, LanguageManager lang) {
        team.addEntry(name);
        Player p = Bukkit.getPlayer(name);
        if (p != null && p.isOnline()) {
            String legacyCode = TEAM_LEGACY_CODES[getTeamIndexForGui(team) % TEAM_LEGACY_CODES.length];
            p.sendMessage(lang.get("teams.assigned", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%color%", legacyCode)
                    .replace("%name%", legacySection().serialize(team.displayName())));
        }
    }

    public boolean areInSameTeam(Player a, Player b) {
        if (a == null || b == null) return false;
        Team ta = board.getEntryTeam(a.getName());
        Team tb = board.getEntryTeam(b.getName());
        return ta != null && ta.equals(tb);
    }

    public boolean renombrarEquipo(Player player, String nuevoNombre) {
        Team team = board.getEntryTeam(player.getName());
        if (team == null) return false;
        LanguageManager lang = plugin.getLang();
        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);
        String nombreAnterior = legacySection().serialize(team.displayName());
        team.displayName(Component.text(nuevoNombre));
        String legacyCode = TEAM_LEGACY_CODES[getTeamIndexForGui(team) % TEAM_LEGACY_CODES.length];
        String prefix = lang.get("teams.prefix-format", null).replace("%color%", legacyCode).replace("%name%", nuevoNombre);
        team.prefix(legacySection().deserialize(prefix));

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (isDefaultName(team, nombreAnterior)) {
                all.sendMessage(lang.get("teams.founded", all).replace("%prefix%", lang.get("general.prefix", all)).replace("%color%", legacyCode).replace("%name%", nuevoNombre));
            } else {
                all.sendMessage(lang.get("teams.renamed", all).replace("%prefix%", lang.get("general.prefix", all)).replace("%color%", legacyCode).replace("%old%", nombreAnterior).replace("%new%", nuevoNombre));
            }
            all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
        return true;
    }

    public boolean isDefaultName(Team team) { return isDefaultName(team, legacySection().serialize(team.displayName())); }

    private boolean isDefaultName(Team team, String displayNameToCheck) {
        if (team == null) return true;
        String name = team.getName().toLowerCase();
        LanguageManager lang = plugin.getLang();
        for (String key : TEAM_COLOR_KEYS) {
            if (key.equalsIgnoreCase(name)) {
                String localizedDefault = lang.get("teams.colors." + key, null);
                if (displayNameToCheck.equalsIgnoreCase(localizedDefault) || displayNameToCheck.equalsIgnoreCase(key)) return true;
            }
        }
        return name.startsWith("team_");
    }

    public int getTeamIndex(String colorKey) {
        for (int i = 0; i < TEAM_COLOR_KEYS.length; i++) {
            if (TEAM_COLOR_KEYS[i].equalsIgnoreCase(colorKey)) return i;
        }
        return -1;
    }

    public void borrarTodosLosEquipos() {
        for (Team team : board.getTeams()) team.unregister();
    }
}