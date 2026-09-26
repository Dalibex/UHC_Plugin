package me.dalibex.UHC_DBasic.managers.teams;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Authoritative plugin team membership cache, independent from TAB-owned scoreboard teams. */
class TeamMembershipStore {

    private final Scoreboard board;
    private final Map<String, String> memberToTeam = new LinkedHashMap<>();

    TeamMembershipStore(Scoreboard board) {
        this.board = board;
    }

    Team getPlayerTeam(String playerName) {
        if (playerName == null) return null;
        String canonical = canonicalTrackedName(playerName);
        Team cached = byName(memberToTeam.get(canonical));
        if (cached != null) return cached;
        Team entry = board.getEntryTeam(playerName);
        return TeamPalette.isPluginTeam(entry) ? entry : null;
    }

    List<String> getMemberNames(Team team) {
        if (!TeamPalette.isPluginTeam(team)) return List.of();
        List<String> members = new ArrayList<>();
        for (Map.Entry<String, String> entry : memberToTeam.entrySet()) {
            if (entry.getValue().equals(team.getName())) members.add(entry.getKey());
        }
        return members;
    }

    int getMemberCount(Team team) {
        if (!TeamPalette.isPluginTeam(team)) return 0;
        int count = 0;
        for (String teamName : memberToTeam.values()) {
            if (teamName.equals(team.getName())) count++;
        }
        return count;
    }

    void movePlayerToTeam(String playerName, Team target) {
        if (playerName == null || target == null) return;
        Team current = getPlayerTeam(playerName);
        if (current != null && !current.equals(target)) {
            current.removeEntry(playerName);
            untrack(playerName);
        }
        target.addEntry(playerName);
        track(playerName, target);
    }

    void track(String name, Team team) {
        if (name == null || team == null) return;
        memberToTeam.put(name, team.getName());
    }

    void untrack(String name) {
        if (name == null) return;
        memberToTeam.remove(canonicalTrackedName(name));
    }

    void clear() {
        memberToTeam.clear();
    }

    Map<String, String> snapshot() {
        return new LinkedHashMap<>(memberToTeam);
    }

    void restore(Map<String, String> snapshot) {
        if (snapshot == null) return;
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            Team team = byName(entry.getValue());
            if (team == null) continue;
            team.addEntry(entry.getKey());
            track(entry.getKey(), team);
        }
    }

    void resyncPlayerEntry(String playerName) {
        if (playerName == null) return;
        String canonical = canonicalTrackedName(playerName);
        Team team = byName(memberToTeam.get(canonical));
        if (team == null) return;
        if (!team.getEntries().contains(canonical)) team.addEntry(canonical);
    }

    private Team byName(String teamName) {
        if (teamName == null) return null;
        Team team = board.getTeam(teamName);
        return TeamPalette.isPluginTeam(team) ? team : null;
    }

    private String canonicalTrackedName(String playerName) {
        for (String known : memberToTeam.keySet()) {
            if (known.equalsIgnoreCase(playerName)) return known;
        }
        return playerName;
    }
}
