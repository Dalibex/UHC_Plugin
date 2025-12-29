package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

        List<Player> vivos = new ArrayList<>();
        List<Player> muertos = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.getRightPanelManager().getJugadoresEliminados().contains(p.getName())
                    || p.getGameMode() == GameMode.SPECTATOR) {
                muertos.add(p);
            } else {
                vivos.add(p);
            }
        }

        int totalJugadores = vivos.size() + muertos.size();
        if (totalJugadores == 0) return;

        Collections.shuffle(vivos);
        Collections.shuffle(muertos);

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

        for (int i = 0; i < vivos.size(); i++) {
            Team teamAsignado = listaEquipos.get(i % numeroDeEquipos);
            Player p = vivos.get(i);
            asignarJugadorAEquipo(p, teamAsignado, lang);
        }

        if (!listaEquipos.isEmpty()) {
            for (Player p : muertos) {
                Team equipoMasVacio = listaEquipos.stream()
                        .min(Comparator.comparingInt(t -> t.getEntries().size()))
                        .orElse(listaEquipos.get(0));

                asignarJugadorAEquipo(p, equipoMasVacio, lang);
            }
        }
    }

    /**
     * Método auxiliar para asignación
     */
    private void asignarJugadorAEquipo(Player p, Team team, LanguageManager lang) {
        team.addEntry(p.getName());

        String msg = lang.get("teams.assigned", p)
                .replace("%prefix%", lang.get("general.prefix", p))
                .replace("%color%", team.getColor().toString())
                .replace("%name%", team.getDisplayName());
        p.sendMessage(msg);
    }

    public boolean renombrarEquipo(Player player, String nuevoNombre) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Team team = board.getEntryTeam(player.getName());

        if (team == null) return false;

        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);

        String nombreAnterior = team.getDisplayName();
        team.setDisplayName(nuevoNombre);

        // El prefijo visual del equipo es global para todos
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

        // Anuncios de broadcast multilingües
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