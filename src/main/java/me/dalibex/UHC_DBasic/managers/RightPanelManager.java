package me.dalibex.UHC_DBasic.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;

public class RightPanelManager implements Listener {

    private final UHC_DBasic plugin;
    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20*60;
    private BukkitTask partidaTask;
    boolean equiposFormados = false;
    private boolean pausado = false;

    private final Set<String> jugadoresEliminados = new HashSet<>();

    public RightPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    // Evento para muerte y guardarla en la lista negra
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        jugadoresEliminados.add(event.getEntity().getName());
    }

    public void setStandBy() {
        if (partidaTask != null) {
            partidaTask.cancel();
            partidaTask = null;
        }
        cronometroSegundos = 0;
        tiempoTotalSegundos = 0;
        capitulo = 1;
        equiposFormados = false;
        jugadoresEliminados.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            actualizarScoreboard(p, "§eEsperando...", "", false);
        }
    }

    public void iniciarPartida() {
        if (partidaTask != null) partidaTask.cancel();
        pausado = false;
        equiposFormados = false;
        jugadoresEliminados.clear();

        partidaTask = new BukkitRunnable() {
            @Override
            public void run() {
                TeamManager tm = plugin.getTeamManager();
                if (pausado) return;

                if (cronometroSegundos == 1 && tm.getTeamSize() == 1 && !equiposFormados) {
                    tm.shuffleTeams();
                    equiposFormados = true;
                }

                cronometroSegundos++;
                tiempoTotalSegundos++;

                int tiempoRestanteEnCapitulo = segundosPorCapitulo - (cronometroSegundos % segundosPorCapitulo);

                if (cronometroSegundos % segundosPorCapitulo == 0) {
                    capitulo++;
                    if (capitulo < 10) {
                        Bukkit.broadcastMessage("§e§lUHC ELOUD > §fHa comenzado la §aParte " + capitulo);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                    }

                    if (capitulo == 10) {
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        Bukkit.broadcastMessage("§c§l¡EL TIEMPO A TERMINADO! §c⚔");
                        Bukkit.broadcastMessage("§fDirígete a X=0 Z=0 para la pelea final");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }

                    if (capitulo == 4 && tm.getTeamSize() > 1 && !equiposFormados) {
                        tm.shuffleTeams();
                        equiposFormados = true;
                        Bukkit.broadcastMessage("§6§l¡LOS EQUIPOS HAN SIDO FORMADOS! ⚔");
                    }

                    if (capitulo == 5) {
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        Bukkit.broadcastMessage("§c§l¡EL PVP SE HA ACTIVADO! §c⚔");
                        Bukkit.broadcastMessage("§fEl pacto de caballeros ha finalizado");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }
                }

                String tiempoFormateado = formatTime(tiempoRestanteEnCapitulo);
                String tiempoTotal = formatTime(tiempoTotalSegundos);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    actualizarScoreboard(p, tiempoFormateado, tiempoTotal, true);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void actualizarScoreboard(Player player, String tiempo, String tiempoTotal, boolean partidaActiva) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("uhc", "dummy", "§6§lELOUD UHC");
        obj.numberFormat(NumberFormat.blank());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (!partidaActiva) {
            obj.getScore("§1 ").setScore(5);
            obj.getScore(" §6>> §8Esperando... ").setScore(4);
            obj.getScore("§2 ").setScore(3);
            obj.getScore(" §fJugadores: §b" + Bukkit.getOnlinePlayers().size() + " §7👥 ").setScore(2);
            obj.getScore("§3 ").setScore(1);
        } else {
            int teamSize = plugin.getTeamManager().getTeamSize();
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
            String pvpStatus = (capitulo < 5) ? "§ePacto de caballeros " : "§4§lACTIVO §4⚔ ";

            obj.getScore("§4 ").setScore(23);
            if (capitulo < 10) {
                obj.getScore(" §fParte actual: §a" + capitulo).setScore(22);
            } else {
                obj.getScore(" §6§lFINALIZADO!").setScore(21);
                obj.getScore(" §fDirígete a X=0 Z=0").setScore(20);
                obj.getScore("§5 ").setScore(19);
            }
            obj.getScore(" §fPVP: " + pvpStatus).setScore(18);
            obj.getScore("§6 ").setScore(17);

            int nextScore = 16;
            if (teamSize == 1) {
                String lineaSolos = (team != null && !team.getPrefix().contains("team_"))
                        ? " §b🛡 §fEquipo: " + team.getColor() + team.getDisplayName()
                        : " §c⚠ §7Usa /nequipo";
                obj.getScore(lineaSolos).setScore(nextScore--);
            } else {
                if (capitulo < 4) {
                    for (int i = 1; i <= (teamSize - 1); i++) {
                        obj.getScore(" §d👥 §f: §k??????" + (new String(new char[i]).replace("\0", " "))).setScore(nextScore--);
                    }
                } else {
                    String lineaNombre;
                    if (team != null && !team.getPrefix().contains("team_")) {
                        lineaNombre = " §d👥 §fEquipo: " + team.getColor() + team.getDisplayName();
                    } else if (team != null) {
                        lineaNombre = " §c⚠ §7Usa /nequipo";
                    } else {
                        lineaNombre = " §d👥 §7Asignando...";
                    }

                    obj.getScore(lineaNombre).setScore(nextScore--);

                    if (team != null) {
                        boolean tieneCompañerosVivos = false;
                        for (String entry : team.getEntries()) {
                            if (entry.equals(player.getName())) continue;

                            String textoVida;
                            String prefixColor = "§f";

                            // LÓGICA DE MUERTE PERSISTENTE
                            if (jugadoresEliminados.contains(entry)) {
                                prefixColor = "§7§m";
                                textoVida = " §c✘";
                            } else {
                                Player member = Bukkit.getPlayer(entry);
                                if (member != null && member.isOnline()) {
                                    tieneCompañerosVivos = true;
                                    double salud = member.getHealth();
                                    String colorS = (salud > 15) ? "§a" : (salud > 10) ? "§2" : (salud > 5) ? "§e" : "§c";
                                    textoVida = " " + colorS + (int)salud + "§4❤";
                                } else {
                                    textoVida = " §7[OFF]";
                                }
                            }
                            obj.getScore(" §8> " + prefixColor + entry + textoVida).setScore(nextScore--);
                        }

                        if (!tieneCompañerosVivos && teamSize > 1) {
                            obj.getScore(" §7§oCompañeros: §c✘").setScore(nextScore--);
                        }
                    }
                }
            }

            obj.getScore("§6 ").setScore(nextScore--);
            obj.getScore(" §4⏳ §lTiempo Acumulado").setScore(nextScore--);
            obj.getScore("§6> §f" + tiempoTotal).setScore(nextScore--);
            obj.getScore("§7 ").setScore(nextScore--);

            if (capitulo < 10) {
                obj.getScore(" §5⌚ §lSiguiente parte").setScore(nextScore--);
                obj.getScore("§6> §f" + tiempo).setScore(nextScore--);
                obj.getScore("§8 ").setScore(nextScore--);
            }
        }

        player.setScoreboard(board);
    }

    private String formatTime(int segundosTotales) {
        int minutos = segundosTotales / 60;
        int segundos = segundosTotales % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }

    // Getters y setters para las actualizaciones del panel de admin
    public void setPausado(boolean estado) { this.pausado = estado; }
    public boolean isPausado() { return pausado; }
    public int getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public int getSegundosPorCapitulo() {return segundosPorCapitulo; }
    public void setSegundosPorCapitulo(int segundos) {this.segundosPorCapitulo = segundos;}
}