package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class TeamManager {

    private static final String LEGACY_MIGRATION_MARKER = "migrations.legacy-bare-teams";

    private final UHC_DBasic plugin;
    private final Scoreboard board;
    private final NamespacedKey teamSelectorKey;
    private int teamSize = 1;
    private boolean customTeamsEnabled = false;
    private int teamsFormedEpisode = 3;

    /**
     * Authoritative plugin membership cache (canonical player name -> h_* team name).
     * TAB owns main scoreboard teams and may move entries, so gameplay must not rely on getEntryTeam.
     */
    private final Map<String, String> memberToTeam = new LinkedHashMap<>();

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
        this.teamSelectorKey = new NamespacedKey(plugin, "team_selector");
    }

    public boolean isCustomTeamsEnabled() { return customTeamsEnabled; }
    public void setCustomTeamsEnabled(boolean enabled) { this.customTeamsEnabled = enabled; }
    public void setTeamSize(int size) { this.teamSize = size; }
    public int getTeamSize() { return teamSize; }

    /** Episode where teams are formed and compasses are delivered. */
    public int getTeamsFormedEpisode() { return teamsFormedEpisode; }
    public void setTeamsFormedEpisode(int episode) { this.teamsFormedEpisode = episode; }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /** Returns the cached canonical player name for an entry. */
    private String canonicalTrackedName(String playerName) {
        if (playerName == null) return null;
        for (String known : memberToTeam.keySet()) {
            if (known.equalsIgnoreCase(playerName)) return known;
        }
        return playerName;
    }

    /** Returns the plugin team from the authoritative cache, never TAB-owned teams. */
    public Team getPlayerTeam(String playerName) {
        if (playerName == null) return null;
        String canonical = canonicalTrackedName(playerName);
        Team cached = byName(memberToTeam.get(canonical));
        if (cached != null) return cached;
        Team entry = board.getEntryTeam(playerName);
        return isPluginTeam(entry) ? entry : null;
    }

    /** Returns plugin team entries from the authoritative cache. */
    public List<String> getMemberNames(Team team) {
        if (team == null || !isPluginTeam(team)) return List.of();
        List<String> members = new ArrayList<>();
        for (Map.Entry<String, String> e : memberToTeam.entrySet()) {
            if (e.getValue().equals(team.getName())) members.add(e.getKey());
        }
        return members;
    }

    /** Returns the plugin team member count from the authoritative cache. */
    public int getMemberCount(Team team) {
        if (team == null || !isPluginTeam(team)) return 0;
        int count = 0;
        for (String teamName : memberToTeam.values()) {
            if (teamName.equals(team.getName())) count++;
        }
        return count;
    }

    public boolean hasPlayerTeam(String playerName) {
        return getPlayerTeam(playerName) != null;
    }

    private Team byName(String teamName) {
        if (teamName == null) return null;
        Team t = board.getTeam(teamName);
        return isPluginTeam(t) ? t : null;
    }

    private void track(String name, Team team) {
        if (name == null || team == null) return;
        memberToTeam.put(name, team.getName());
    }

    private void untrack(String name) {
        if (name == null) return;
        String canonical = canonicalTrackedName(name);
        memberToTeam.remove(canonical);
    }

    public void initializeCustomTeams() {
        deleteAllTeams();
        LanguageManager lang = plugin.getLang();

        for (int i = 0; i < TEAM_COLOR_KEYS.length; i++) {
            createTeam(i);
        }
    }

    /** Registers and configures a team with its color, display name, and prefix. */
    private Team createTeam(int index) {
        LanguageManager lang = plugin.getLang();
        String colorKey = TEAM_COLOR_KEYS[index % TEAM_COLOR_KEYS.length];
        Team team = board.registerNewTeam(ScoreboardHelper.TEAM_PREFIX + colorKey);
        team.color(TEAM_NAMED_COLORS[index % TEAM_NAMED_COLORS.length]);

        String localizedName = lang.get("teams.colors." + colorKey, null);
        team.displayName(Component.text(localizedName != null ? localizedName : colorKey));

        applyPrefix(team, legacySection().serialize(team.displayName()));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        return team;
    }

    private void applyPrefix(Team team, String nombre) {
        LanguageManager lang = plugin.getLang();
        String legacyCode = TEAM_LEGACY_CODES[getTeamIndexForGui(team) % TEAM_LEGACY_CODES.length];
        String prefix = lang.get("teams.prefix-format", null)
                .replace("%color%", legacyCode)
                .replace("%name%", nombre);
        team.prefix(TextUtil.deserialize(prefix));
    }

    public Team getTeamByColorSearch(String input) {
        String normalized = normalizeColorInput(input);
        for (Team team : getPluginTeams()) {
            String colorKey = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length());
            if (colorKey.equalsIgnoreCase(normalized)) return team;
            if (legacySection().serialize(team.displayName()).equalsIgnoreCase(normalized)) return team;
        }
        return null;
    }

    /** Normalizes visible, internal, and localized color inputs. */
    public static String normalizeColorInput(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.startsWith(ScoreboardHelper.TEAM_PREFIX)) {
            lower = lower.substring(ScoreboardHelper.TEAM_PREFIX.length());
        }
        return lower;
    }

    public void giveTeamSelectorItem(Player p) {
        LanguageManager lang = plugin.getLang();
        removeTeamSelectorItem(p);

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        meta.displayName(lang.getComponent("items.team-selector.name", p));
        meta.lore(lang.getComponentList("items.team-selector.lore", p));
        meta.getPersistentDataContainer().set(teamSelectorKey, PersistentDataType.BYTE, (byte) 1);
        selector.setItemMeta(meta);

        p.getInventory().setItem(8, selector);
    }

    public void removeTeamSelectorItem(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (isTeamSelector(item)) p.getInventory().setItem(i, null);
        }
    }

    public boolean isTeamSelector(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(teamSelectorKey, PersistentDataType.BYTE);
    }

    public void openTeamSelectorGUI(Player p) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !customTeamsEnabled) return;
        LanguageManager lang = plugin.getLang();
        List<Team> teams = getPluginTeams();
        int invSize = Math.max(9, (int) Math.ceil(teams.size() / 9.0) * 9);
        Inventory gui = Bukkit.createInventory(null, invSize, lang.getComponent("menus.team-selector.title", p));

        int slot = 0;
        for (Team team : teams) {
            int teamIndex = getTeamIndexForGui(team);
            Material dyeMat = TEAM_DYES[teamIndex % TEAM_DYES.length];
            ItemStack item = new ItemStack(dyeMat);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(TextUtil.item(
                    lang.get("menus.team-selector.team-item.name", p).replace("%name%", legacySection().serialize(team.displayName()))));
            List<Component> lore = new ArrayList<>();
            int current = getMemberCount(team);
            for (String l : lang.getList("menus.team-selector.team-item.lore", p)) {
                lore.add(TextUtil.item(l.replace("%current%", String.valueOf(current)).replace("%max%", String.valueOf(teamSize))));
            }
            for (String entry : getMemberNames(team)) {
                lore.add(TextUtil.item(lang.get("menus.team-selector.member-format", p).replace("%player%", entry)));
            }
            int emptySlots = teamSize - current;
            for (int h = 0; h < emptySlots; h++) {
                lore.add(TextUtil.item(lang.get("menus.team-selector.empty-slot", p)));
            }

            Team playerTeam = getPlayerTeam(p.getName());
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
        p.openInventory(gui);
    }

    /** Moves a player to the target team after removing their previous cached membership. */
    public void movePlayerToTeam(Player p, Team target) {
        if (p == null || target == null) return;
        Team current = getPlayerTeam(p.getName());
        if (current != null && !current.equals(target)) {
            current.removeEntry(p.getName());
            untrack(p.getName());
        }
        target.addEntry(p.getName());
        track(p.getName(), target);
    }

    public boolean tryJoinTeam(Player p, int slot) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !customTeamsEnabled) return false;
        LanguageManager lang = plugin.getLang();
        List<Team> teamList = getPluginTeams();
        if (slot < 0 || slot >= teamList.size()) return false;
        Team target = teamList.get(slot);
        Team current = getPlayerTeam(p.getName());
        if (current != null && !isPluginTeam(current)) return false;

        if (current != null && current.equals(target)) {
            p.sendMessage(lang.get("menus.team-selector.already-in-team", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        if (getMemberCount(target) >= teamSize) {
            p.sendMessage(lang.get("menus.team-selector.already-full", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        if (current != null) {
            current.removeEntry(p.getName());
            untrack(p.getName());
        }
        target.addEntry(p.getName());
        track(p.getName(), target);
        p.sendMessage(lang.get("menus.team-selector.joined", p).replace("%name%", legacySection().serialize(target.displayName().color(target.color()))));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }

    public boolean tryLeaveTeam(Player p) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !customTeamsEnabled) return false;
        Team current = getPlayerTeam(p.getName());
        if (current != null) {
            current.removeEntry(p.getName());
            untrack(p.getName());
            p.sendMessage(plugin.getLang().get("menus.team-selector.left", p));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return true;
        }
        return false;
    }

    public boolean allPlayersHaveTeam() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (!hasPlayerTeam(p.getName())) return false;
        }
        return true;
    }

    public int getTeamsWithPlayersCount() {
        Set<Team> used = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            Team t = getPlayerTeam(p.getName());
            if (t != null) used.add(t);
        }
        return used.size();
    }

    public void removeAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) removeTeamSelectorItem(p);
    }

    public void clearCustomTeams() {
        deleteAllTeams();
        removeAllSelectorItems();
    }


    public void giveAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR) giveTeamSelectorItem(p);
        }
    }

    private int getTeamIndexForGui(Team team) {
        String name = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length()).toLowerCase(Locale.ROOT);
        for (int i = 0; i < TEAM_COLOR_KEYS.length; i++) {
            if (TEAM_COLOR_KEYS[i].equalsIgnoreCase(name)) return i;
        }
        return 0;
    }

    public void shuffleTeams() {
        deleteAllTeams();
        LanguageManager lang = plugin.getLang();
        List<String> vivos = new ArrayList<>();
        List<String> muertos = new ArrayList<>();
        for (String name : plugin.getGameManager().getInitialParticipants()) {
            if (plugin.getGameManager().getEliminatedPlayers().contains(name)) muertos.add(name);
            else vivos.add(name);
        }
        if (vivos.isEmpty() && muertos.isEmpty()) return;
        Collections.shuffle(vivos);
        Collections.shuffle(muertos);

        int total = vivos.size() + muertos.size();
        int numeroDeEquipos = (int) Math.ceil((double) total / teamSize);
        List<Team> listaEquipos = new ArrayList<>();

        for (int i = 0; i < numeroDeEquipos; i++) {
            listaEquipos.add(createTeam(i));
        }

        for (int i = 0; i < vivos.size(); i++) {
            assignTeamByName(vivos.get(i), listaEquipos.get(i % numeroDeEquipos), lang);
        }
        if (!listaEquipos.isEmpty()) {
            for (String m : muertos) {
                Team emptiest = listaEquipos.stream().min(Comparator.comparingInt(this::getMemberCount)).get();
                assignTeamByName(m, emptiest, lang);
            }
        }
    }

    private void assignTeamByName(String name, Team team, LanguageManager lang) {
        team.addEntry(name);
        track(name, team);
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
        Team ta = getPlayerTeam(a.getName());
        Team tb = getPlayerTeam(b.getName());
        return ta != null && ta.equals(tb);
    }

    public boolean renameTeam(Player player, String nuevoNombre) {
        Team team = getPlayerTeam(player.getName());
        if (team == null) return false;
        LanguageManager lang = plugin.getLang();
        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);
        String nombreAnterior = legacySection().serialize(team.displayName());
        team.displayName(Component.text(nuevoNombre));
        applyPrefix(team, nuevoNombre);
        String legacyCode = TEAM_LEGACY_CODES[getTeamIndexForGui(team) % TEAM_LEGACY_CODES.length];

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
        if (!isPluginTeam(team)) return true;
        String name = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length()).toLowerCase(Locale.ROOT);
        LanguageManager lang = plugin.getLang();
        for (String key : TEAM_COLOR_KEYS) {
            if (key.equalsIgnoreCase(name)) {
                String localizedDefault = lang.get("teams.colors." + key, null);
                if (displayNameToCheck.equalsIgnoreCase(localizedDefault) || displayNameToCheck.equalsIgnoreCase(key)) return true;
            }
        }
        return name.startsWith("team_");
    }

    public void deleteAllTeams() {
        for (Team team : getPluginTeams()) team.unregister();
        memberToTeam.clear();
    }

    /** Returns the current membership snapshot for restoring after a team reset. */
    public Map<String, String> snapshotTeamMembers() {
        return new LinkedHashMap<>(memberToTeam);
    }

    /** Restores a membership snapshot, skipping entries whose teams no longer exist. */
    public void restoreTeamMembers(Map<String, String> snapshot) {
        if (snapshot == null) return;
        for (Map.Entry<String, String> e : snapshot.entrySet()) {
            Team team = byName(e.getValue());
            if (team == null) continue;
            team.addEntry(e.getKey());
            track(e.getKey(), team);
        }
    }

    /** Restores a player's h_* scoreboard entry if TAB moved it while the cache stayed valid. */
    public void resyncPlayerEntry(String playerName) {
        if (playerName == null) return;
        String canonical = canonicalTrackedName(playerName);
        if (canonical == null) return;
        Team team = byName(memberToTeam.get(canonical));
        if (team == null) return;
        if (!team.getEntries().contains(canonical)) team.addEntry(canonical);
    }

    public void migrateLegacyTeamsOnce() {
        if (plugin.getConfig().getBoolean(LEGACY_MIGRATION_MARKER, false)) return;
        for (String legacyName : TEAM_COLOR_KEYS) {
            Team legacy = board.getTeam(legacyName);
            if (legacy != null) legacy.unregister();
        }
        plugin.getConfig().set(LEGACY_MIGRATION_MARKER, true);
        plugin.saveConfig();
    }

    private boolean isPluginTeam(Team team) {
        return team != null && team.getName().startsWith(ScoreboardHelper.TEAM_PREFIX);
    }

    private List<Team> getPluginTeams() {
        return getTeams();
    }

    /** Returns plugin teams (internal h_* prefix), sorted by color index. */
    public List<Team> getTeams() {
        List<Team> teams = new ArrayList<>();
        for (Team team : board.getTeams()) {
            if (isPluginTeam(team)) teams.add(team);
        }
        teams.sort(Comparator.comparingInt(this::getTeamIndexForGui).thenComparing(Team::getName));
        return teams;
    }
}
