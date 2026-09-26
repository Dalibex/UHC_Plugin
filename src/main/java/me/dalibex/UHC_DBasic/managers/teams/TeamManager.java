package me.dalibex.UHC_DBasic.managers.teams;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/** Coordinates team creation, assignment, custom-team selection, and team formation. */
public class TeamManager {

    private static final String LEGACY_MIGRATION_MARKER = "migrations.legacy-bare-teams";

    private final UHC_DBasic plugin;
    private final Scoreboard board;
    private final TeamMembershipStore membership;
    private final TeamSelectorMenu selectorMenu;
    private int teamSize = 1;
    private boolean customTeamsEnabled = false;
    private int teamsFormedEpisode = 3;

    public TeamManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
        this.membership = new TeamMembershipStore(board);
        this.selectorMenu = new TeamSelectorMenu(plugin);
    }

    public boolean isCustomTeamsEnabled() { return customTeamsEnabled; }
    public void setCustomTeamsEnabled(boolean enabled) { this.customTeamsEnabled = enabled; }
    public void setTeamSize(int size) { this.teamSize = size; }
    public int getTeamSize() { return teamSize; }
    public int getTeamsFormedEpisode() { return teamsFormedEpisode; }
    public void setTeamsFormedEpisode(int episode) { this.teamsFormedEpisode = episode; }

    public static String normalizeColorInput(String input) {
        return TeamPalette.normalizeColorInput(input);
    }

    public Team getPlayerTeam(String playerName) { return membership.getPlayerTeam(playerName); }
    public List<String> getMemberNames(Team team) { return membership.getMemberNames(team); }
    public int getMemberCount(Team team) { return membership.getMemberCount(team); }
    public boolean hasPlayerTeam(String playerName) { return getPlayerTeam(playerName) != null; }
    public void giveTeamSelectorItem(Player player) { selectorMenu.giveSelectorItem(player); }
    public void removeTeamSelectorItem(Player player) { selectorMenu.removeSelectorItem(player); }
    public boolean isTeamSelector(ItemStack item) { return selectorMenu.isSelector(item); }

    public void initializeCustomTeams() {
        deleteAllTeams();
        for (int i = 0; i < TeamPalette.size(); i++) createTeam(i);
    }

    private Team createTeam(int index) {
        return TeamPalette.createTeam(board, plugin.getLang(), index);
    }

    public Team getTeamByColorSearch(String input) {
        String normalized = TeamPalette.normalizeColorInput(input);
        for (Team team : getPluginTeams()) {
            String colorKey = team.getName().substring(ScoreboardHelper.TEAM_PREFIX.length());
            if (colorKey.equalsIgnoreCase(normalized)) return team;
            if (legacySection().serialize(team.displayName()).equalsIgnoreCase(normalized)) return team;
        }
        return null;
    }

    public void openTeamSelectorGUI(Player player) {
        selectorMenu.open(player, getPluginTeams(), membership, teamSize);
    }

    public void movePlayerToTeam(Player player, Team target) {
        if (player != null) membership.movePlayerToTeam(player.getName(), target);
    }

    public boolean tryJoinTeam(Player player, int slot) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !customTeamsEnabled) return false;
        LanguageManager lang = plugin.getLang();
        List<Team> teamList = getPluginTeams();
        if (slot < 0 || slot >= teamList.size()) return false;
        Team target = teamList.get(slot);
        Team current = getPlayerTeam(player.getName());
        if (current != null && !TeamPalette.isPluginTeam(current)) return false;

        if (current != null && current.equals(target)) {
            player.sendMessage(lang.get("menus.team-selector.already-in-team", player));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        if (getMemberCount(target) >= teamSize) {
            player.sendMessage(lang.get("menus.team-selector.already-full", player));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
        membership.movePlayerToTeam(player.getName(), target);
        player.sendMessage(lang.get("menus.team-selector.joined", player)
                .replace("%name%", legacySection().serialize(target.displayName().color(target.color()))));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }

    public boolean tryLeaveTeam(Player player) {
        if (plugin.getGameManager().getPhase() != GamePhase.LOBBY || !customTeamsEnabled) return false;
        Team current = getPlayerTeam(player.getName());
        if (current == null) return false;
        current.removeEntry(player.getName());
        membership.untrack(player.getName());
        player.sendMessage(plugin.getLang().get("menus.team-selector.left", player));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        return true;
    }

    public boolean allPlayersHaveTeam() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (!hasPlayerTeam(player.getName())) return false;
        }
        return true;
    }

    public int getTeamsWithPlayersCount() {
        Set<Team> used = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            Team team = getPlayerTeam(player.getName());
            if (team != null) used.add(team);
        }
        return used.size();
    }

    public void removeAllSelectorItems() {
        for (Player player : Bukkit.getOnlinePlayers()) removeTeamSelectorItem(player);
    }

    public void clearCustomTeams() {
        deleteAllTeams();
        removeAllSelectorItems();
    }

    public void giveAllSelectorItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != org.bukkit.GameMode.SPECTATOR) giveTeamSelectorItem(player);
        }
    }

    public void shuffleTeams() {
        deleteAllTeams();
        LanguageManager lang = plugin.getLang();
        List<String> alive = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        for (String name : plugin.getGameManager().getInitialParticipants()) {
            if (plugin.getGameManager().getEliminatedPlayers().contains(name)) dead.add(name);
            else alive.add(name);
        }
        if (alive.isEmpty() && dead.isEmpty()) return;
        Collections.shuffle(alive);
        Collections.shuffle(dead);

        int total = alive.size() + dead.size();
        int teamCount = (int) Math.ceil((double) total / teamSize);
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) teams.add(createTeam(i));

        for (int i = 0; i < alive.size(); i++) assignTeamByName(alive.get(i), teams.get(i % teamCount), lang);
        for (String name : dead) {
            Team emptiest = teams.stream().min(Comparator.comparingInt(this::getMemberCount)).get();
            assignTeamByName(name, emptiest, lang);
        }
    }

    private void assignTeamByName(String name, Team team, LanguageManager lang) {
        team.addEntry(name);
        membership.track(name, team);
        Player player = Bukkit.getPlayer(name);
        if (player != null && player.isOnline()) {
            player.sendMessage(lang.get("teams.assigned", player)
                    .replace("%prefix%", lang.get("general.prefix", player))
                    .replace("%color%", TeamPalette.legacyCode(TeamPalette.indexFor(team)))
                    .replace("%name%", legacySection().serialize(team.displayName())));
        }
    }

    public boolean areInSameTeam(Player a, Player b) {
        if (a == null || b == null) return false;
        Team ta = getPlayerTeam(a.getName());
        Team tb = getPlayerTeam(b.getName());
        return ta != null && ta.equals(tb);
    }

    public boolean renameTeam(Player player, String newName) {
        Team team = getPlayerTeam(player.getName());
        if (team == null) return false;
        LanguageManager lang = plugin.getLang();
        if (newName.length() > 16) newName = newName.substring(0, 16);
        String oldName = legacySection().serialize(team.displayName());
        team.displayName(net.kyori.adventure.text.Component.text(newName));
        TeamPalette.applyPrefix(team, lang);
        String legacyCode = TeamPalette.legacyCode(TeamPalette.indexFor(team));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isDefaultName(team, oldName)) {
                online.sendMessage(lang.get("teams.founded", online).replace("%prefix%", lang.get("general.prefix", online)).replace("%color%", legacyCode).replace("%name%", newName));
            } else {
                online.sendMessage(lang.get("teams.renamed", online).replace("%prefix%", lang.get("general.prefix", online)).replace("%color%", legacyCode).replace("%old%", oldName).replace("%new%", newName));
            }
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
        return true;
    }

    public boolean isDefaultName(Team team) {
        return isDefaultName(team, legacySection().serialize(team.displayName()));
    }

    private boolean isDefaultName(Team team, String displayNameToCheck) {
        return TeamPalette.isDefaultName(team, plugin.getLang(), displayNameToCheck);
    }

    public void deleteAllTeams() {
        for (Team team : getPluginTeams()) team.unregister();
        membership.clear();
    }

    public Map<String, String> snapshotTeamMembers() {
        return new LinkedHashMap<>(membership.snapshot());
    }

    public void restoreTeamMembers(Map<String, String> snapshot) {
        membership.restore(snapshot);
    }

    public void resyncPlayerEntry(String playerName) {
        membership.resyncPlayerEntry(playerName);
    }

    public void migrateLegacyTeamsOnce() {
        if (plugin.getConfig().getBoolean(LEGACY_MIGRATION_MARKER, false)) return;
        for (int i = 0; i < TeamPalette.size(); i++) {
            Team legacy = board.getTeam(TeamPalette.colorKey(i));
            if (legacy != null) legacy.unregister();
        }
        plugin.getConfig().set(LEGACY_MIGRATION_MARKER, true);
        plugin.saveConfig();
    }

    private List<Team> getPluginTeams() {
        return getTeams();
    }

    public List<Team> getTeams() {
        List<Team> teams = new ArrayList<>();
        for (Team team : board.getTeams()) {
            if (TeamPalette.isPluginTeam(team)) teams.add(team);
        }
        teams.sort(Comparator.comparingInt(TeamPalette::indexFor).thenComparing(Team::getName));
        return teams;
    }
}
