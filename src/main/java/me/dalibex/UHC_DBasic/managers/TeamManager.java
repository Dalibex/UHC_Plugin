package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TeamManager {

    private final Scoreboard board;
    private int teamSize = 1;
    private final ChatColor COLOR_UNICO = ChatColor.AQUA;

    public TeamManager() {
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void setTeamSize(int size) {
        this.teamSize = size;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public void shuffleTeams() {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        for (Team team : board.getTeams()) {
            team.unregister();
        }

        List<String> vivosNames = new ArrayList<>();
        List<String> muertosNames = new ArrayList<>();

        for (String name : plugin.getGameManager().getParticipantesIniciales()) {
            if (plugin.getGameManager().getJugadoresEliminados().contains(name)) {
                muertosNames.add(name);
            } else {
                vivosNames.add(name);
            }
        }

        int totalJugadores = vivosNames.size() + muertosNames.size();
        if (totalJugadores == 0) return;

        Collections.shuffle(vivosNames);
        Collections.shuffle(muertosNames);

        int numeroDeEquipos = (int) Math.ceil((double) totalJugadores / teamSize);
        List<Team> listaEquipos = new ArrayList<>();

        for (int i = 1; i <= numeroDeEquipos; i++) {
            String idEquipo = "team_" + i;
            Team team = board.registerNewTeam(idEquipo);
            team.setColor(COLOR_UNICO);
            team.setDisplayName(idEquipo);

            String prefix = lang.get("teams.prefix-format", null)
                    .replace("%color%", COLOR_UNICO.toString())
                    .replace("%name%", idEquipo);
            team.setPrefix(prefix);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            listaEquipos.add(team);
        }

        // REPARTO DE VIVOS
        for (int i = 0; i < vivosNames.size(); i++) {
            Team teamAsignado = listaEquipos.get(i % numeroDeEquipos);
            String name = vivosNames.get(i);
            asignarEquipoPorNombre(name, teamAsignado, lang);
        }

        // REPARTO DE MUERTOS
        if (!listaEquipos.isEmpty()) {
            for (String name : muertosNames) {
                Team equipoMasVacio = listaEquipos.stream()
                        .min(Comparator.comparingInt(t -> t.getEntries().size()))
                        .orElse(listaEquipos.get(0));

                asignarEquipoPorNombre(name, equipoMasVacio, lang);
            }
        }
    }

    /**
     * Método auxiliar corregido para aceptar Nombres (String)
     * Maneja automáticamente si el jugador está online u offline
     */
    private void asignarEquipoPorNombre(String name, Team team, LanguageManager lang) {
        team.addEntry(name);

        Player p = Bukkit.getPlayer(name);
        if (p != null && p.isOnline()) {
            String msg = lang.get("teams.assigned", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%color%", team.getColor().toString())
                    .replace("%name%", team.getDisplayName());
            p.sendMessage(msg);
        }
    }

    public boolean areInSameTeam(Player a, Player b) {
        if (a == null || b == null) return false;

        Team teamA = board.getEntryTeam(a.getName());
        Team teamB = board.getEntryTeam(b.getName());

        if (teamA == null || teamB == null) return false;

        return teamA.equals(teamB);
    }

    public boolean renombrarEquipo(Player player, String nuevoNombre) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Team team = board.getEntryTeam(player.getName());

        if (team == null) return false;

        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);

        String nombreAnterior = team.getDisplayName();
        team.setDisplayName(nuevoNombre);

        String prefix = lang.get("teams.prefix-format", null)
                .replace("%color%", team.getColor().toString())
                .replace("%name%", nuevoNombre);
        team.setPrefix(prefix);

        for (String entry : team.getEntries()) {
            Player member = Bukkit.getPlayer(entry);
            if (member != null) {
                member.playSound(member.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1, 1);
            }
        }

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (nombreAnterior.contains("team_")) {
                String foundedMsg = lang.get("teams.founded", all)
                        .replace("%prefix%", lang.get("general.prefix", all))
                        .replace("%color%", team.getColor().toString())
                        .replace("%name%", nuevoNombre);
                all.sendMessage(foundedMsg);
            } else {
                String renamedMsg = lang.get("teams.renamed", all)
                        .replace("%prefix%", lang.get("general.prefix", all))
                        .replace("%color%", team.getColor().toString())
                        .replace("%old%", nombreAnterior)
                        .replace("%new%", nuevoNombre);
                all.sendMessage(renamedMsg);
            }
            all.playSound(all.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
        return true;
    }

    public void borrarTodosLosEquipos() {
        for (Team team : board.getTeams()) {
            team.unregister();
        }
    }
}