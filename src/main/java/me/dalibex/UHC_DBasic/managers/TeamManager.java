package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

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

    private static final ChatColor[] TEAM_CHAT_COLORS = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.DARK_GREEN,
            ChatColor.YELLOW, ChatColor.GOLD, ChatColor.DARK_PURPLE,
            ChatColor.DARK_AQUA, ChatColor.LIGHT_PURPLE, ChatColor.GREEN,
            ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.WHITE
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

        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        if (jugadoresOnline == 0 || teamSize <= 1) return;

        int numeroDeEquipos = (int) Math.ceil((double) jugadoresOnline / teamSize);

        for (int i = 0; i < numeroDeEquipos; i++) {
            String colorKey = TEAM_COLOR_KEYS[i % TEAM_COLOR_KEYS.length];
            Team team = board.registerNewTeam(colorKey);
            ChatColor color = TEAM_CHAT_COLORS[i % TEAM_CHAT_COLORS.length];
            team.setColor(color);
            
            String localizedName = lang.get("teams.colors." + colorKey, null);
            team.setDisplayName(localizedName != null ? localizedName : colorKey);

            String prefix = lang.get("teams.prefix-format", null)
                    .replace("%color%", color.toString())
                    .replace("%name%", team.getDisplayName());
            team.setPrefix(prefix);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    public Team getTeamByColorSearch(String input) {
        String lower = input.toLowerCase();
        for (Team team : board.getTeams()) {
            if (team.getName().equalsIgnoreCase(lower)) return team;
            if (team.getDisplayName().equalsIgnoreCase(input)) return team;
        }
        return null;
    }

    public void giveTeamSelectorItem(Player p) {
        LanguageManager lang = plugin.getLang();
        removeTeamSelectorItem(p);

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName(lang.get("items.team-selector.name", p));
        meta.setLore(lang.getList("items.team-selector.lore", p));
        selector.setItemMeta(meta);

        p.getInventory().setItem(8, selector);
    }

    public void removeTeamSelectorItem(Player p) {
        LanguageManager lang = plugin.getLang();
        String selectorName = lang.get("items.team-selector.name", p);

        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(selectorName)) {
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    public void openTeamSelectorGUI(Player p) {
        LanguageManager lang = plugin.getLang();
        Set<Team> teams = board.getTeams();
        int invSize = Math.max(9, (int) Math.ceil(teams.size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(null, invSize, lang.get("menus.team-selector.title", p));

        int slot = 0;
        for (Team team : teams) {
            int teamIndex = getTeamIndexForGui(team);
            Material dyeMat = TEAM_DYES[teamIndex % TEAM_DYES.length];
            ItemStack item = new ItemStack(dyeMat);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(lang.get("menus.team-selector.team-item.name", p).replace("%name%", team.getDisplayName()));
            List<String> lore = new ArrayList<>();
            int current = team.getEntries().size();
            for (String l : lang.getList("menus.team-selector.team-item.lore", p)) {
                lore.add(l.replace("%current%", String.valueOf(current)).replace("%max%", String.valueOf(teamSize)));
            }
            for (String entry : team.getEntries()) {
                lore.add(lang.get("menus.team-selector.member-format", p).replace("%player%", entry));
            }
            int huecos = teamSize - current;
            for (int h = 0; h < huecos; h++) lore.add(lang.get("menus.team-selector.empty-slot", p));
            
            Team playerTeam = board.getEntryTeam(p.getName());
            if (playerTeam != null && playerTeam.equals(team)) {
                lore.add("");
                lore.add("§a✔ Tu equipo actual");
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            meta.setLore(lore);
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
        p.sendMessage(lang.get("menus.team-selector.joined", p).replace("%name%", target.getColor() + target.getDisplayName()));
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
            ChatColor color = TEAM_CHAT_COLORS[i % TEAM_CHAT_COLORS.length];
            team.setColor(color);
            String localizedName = lang.get("teams.colors." + colorKey, null);
            team.setDisplayName(localizedName != null ? localizedName : colorKey);
            String prefix = lang.get("teams.prefix-format", null).replace("%color%", color.toString()).replace("%name%", team.getDisplayName());
            team.setPrefix(prefix);
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
            p.sendMessage(lang.get("teams.assigned", p).replace("%prefix%", lang.get("general.prefix", p)).replace("%color%", team.getColor().toString()).replace("%name%", team.getDisplayName()));
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
        String nombreAnterior = team.getDisplayName();
        team.setDisplayName(nuevoNombre);
        String prefix = lang.get("teams.prefix-format", null).replace("%color%", team.getColor().toString()).replace("%name%", nuevoNombre);
        team.setPrefix(prefix);

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (isDefaultName(team, nombreAnterior)) {
                all.sendMessage(lang.get("teams.founded", all).replace("%prefix%", lang.get("general.prefix", all)).replace("%color%", team.getColor().toString()).replace("%name%", nuevoNombre));
            } else {
                all.sendMessage(lang.get("teams.renamed", all).replace("%prefix%", lang.get("general.prefix", all)).replace("%color%", team.getColor().toString()).replace("%old%", nombreAnterior).replace("%new%", nuevoNombre));
            }
            all.playSound(all.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
        return true;
    }

    public boolean isDefaultName(Team team) { return isDefaultName(team, team.getDisplayName()); }

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