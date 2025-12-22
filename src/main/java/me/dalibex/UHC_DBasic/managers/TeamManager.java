package me.dalibex.UHC_DBasic.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamManager {

    private final Scoreboard board;
    private int teamSize = 1;

    private final ChatColor[] colores = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
            ChatColor.AQUA, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.LIGHT_PURPLE,
            ChatColor.DARK_AQUA, ChatColor.DARK_GREEN, ChatColor.DARK_RED, ChatColor.DARK_BLUE
    };

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
        for (Team team : board.getTeams()) {
            team.unregister();
        }

        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(jugadores);

        int totalJugadores = jugadores.size();
        if (totalJugadores == 0) return;

        int numeroDeEquipos = (int) Math.ceil((double) totalJugadores / teamSize);

        List<Team> listaEquipos = new ArrayList<>();
        for (int i = 1; i <= numeroDeEquipos; i++) {
            String idEquipo = "team_" + i;
            Team team = board.registerNewTeam(idEquipo);

            ChatColor colorAsignado = colores[i % colores.length];
            team.setColor(colorAsignado);

            String nombreInicial = "team_" + i;
            team.setDisplayName(nombreInicial);
            team.setPrefix(colorAsignado + "[" + nombreInicial + "] ");

            team.setAllowFriendlyFire(false);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            listaEquipos.add(team);
        }

        for (int i = 0; i < jugadores.size(); i++) {
            Team teamAsignado = listaEquipos.get(i % numeroDeEquipos);
            Player p = jugadores.get(i);

            teamAsignado.addEntry(p.getName());
            p.sendMessage("§e§lUHC ELOUD §e§l> §fHas sido asignado al " + teamAsignado.getColor() + teamAsignado.getDisplayName());
        }
    }

    public boolean renombrarEquipo(Player player, String nuevoNombre) {
        Team team = board.getEntryTeam(player.getName());

        if (team == null) return false;

        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);

        String nombreAnterior = team.getDisplayName();
        team.setDisplayName(nuevoNombre);
        team.setPrefix(team.getColor() + "[" + nuevoNombre + "] ");

        for (String entry : team.getEntries()) {
            Player member = Bukkit.getPlayer(entry);
            if (member != null) {
                member.sendMessage("§a§lEQUIPO §7» §fTu equipo ahora se llama: " + team.getColor() + nuevoNombre);
                member.playSound(member.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1, 1);
            }
        }

        if (nombreAnterior.contains("team_")) {
            Bukkit.broadcastMessage("§e§lUHC ELOUD §e§l> §fSe ha fundado el equipo: " + team.getColor() + nuevoNombre);
        } else {
            Bukkit.broadcastMessage("§e§lUHC ELOUD §e§l> §fEl equipo " + team.getColor() + nombreAnterior + " §fahora se conoce como: " + team.getColor() + nuevoNombre);
        }

        for (Player all : Bukkit.getOnlinePlayers()) {
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