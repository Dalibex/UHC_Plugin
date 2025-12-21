package me.dalibex.UHC_DBasic.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class RightPanelManager {

    private final UHC_DBasic plugin;
    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private final int SEGUNDOS_POR_CAPITULO = 20 * 60; // 1200 segundos = 20*60 = 20 min por parte
    private BukkitTask partidaTask; // Para poder cancelar el timer

    public RightPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    public void setStandBy() {
        // 1. Cancelamos la tarea si estaba corriendo
        if (partidaTask != null) {
            partidaTask.cancel();
            partidaTask = null;
        }

        // 2. Reseteamos valores
        cronometroSegundos = 0;
        tiempoTotalSegundos = 0;
        capitulo = 1;

        // 3. Ponemos el panel estático para todos
        for (Player p : Bukkit.getOnlinePlayers()) {
            actualizarScoreboard(p, "§eEsperando...", "",false);
        }
    }

    public void iniciarPartida() {
        // Evitar duplicar tareas si se inicia dos veces
        if (partidaTask != null) partidaTask.cancel();

        partidaTask = new BukkitRunnable() {
            @Override
            public void run() {
                cronometroSegundos++;
                tiempoTotalSegundos++;

                int tiempoRestanteEnCapitulo = SEGUNDOS_POR_CAPITULO - (cronometroSegundos % SEGUNDOS_POR_CAPITULO);

                // Detectar cambio de capítulo
                if (cronometroSegundos % SEGUNDOS_POR_CAPITULO == 0) {
                    capitulo++;

                    // AVISAR INICIO DE NUEVA PARTE
                    if(capitulo < 10) {
                        Bukkit.broadcastMessage("§e§lUHC ELOUD > §fHa comenzado la §aParte " + capitulo);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                    }

                    // AVISAR DE PVP TERMINADO
                    if (capitulo == 4) {
                        // Aviso de PVP Activo
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        Bukkit.broadcastMessage("§c§l¡EL PVP SE HA ACTIVADO! §c⚔");
                        Bukkit.broadcastMessage("§fEl pacto de caballeros ha finalizado");
                        Bukkit.broadcastMessage("§8§m------------------------------------");

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }

                    // AVISAR DE DIRIGIRSE A 0 0
                    if (capitulo == 10) {
                        Bukkit.broadcastMessage("§e§lEl tiempo ha terminado!");
                        Bukkit.broadcastMessage("§fDirígete a las coordenadas X=0 Z=0");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
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
            // DISEÑO MODO ESPERA
            obj.getScore("§1 ").setScore(5);
            obj.getScore(" §6>> §8Esperando... ").setScore(4);
            obj.getScore("§2 ").setScore(3);
            obj.getScore(" §fJugadores: §b" + Bukkit.getOnlinePlayers().size() + " §7👥 ").setScore(2);
            obj.getScore("§3 ").setScore(1);
        } else {
            // DISEÑO MODO PARTIDA
            String pvpStatus = (capitulo < 4) ? "§ePacto de caballeros " : "§4§lACTIVO §4⚔ ";
            if(capitulo < 10) {
                obj.getScore("§4 ").setScore(11);
                obj.getScore(" §fParte actual: §a" + capitulo).setScore(10);
            } else {
                obj.getScore("§4 ").setScore(12);
                obj.getScore(" §6§lTiempo finalizado!").setScore(11);
                obj.getScore(" §fDiríjete a X=0 Z=0").setScore(10);
            }
            obj.getScore("§5 ").setScore(9);
            obj.getScore(" §fPVP: " + pvpStatus).setScore(8);
            obj.getScore("§6 ").setScore(7);
            obj.getScore(" §4⏳ §lTiempo Acumulado ").setScore(6);
            obj.getScore("§6> §f" + tiempoTotal).setScore(5);
            obj.getScore("§7 ").setScore(4);
            if(capitulo < 10) {
                obj.getScore(" §5⌚ §lSiguiente parte ").setScore(3);
                obj.getScore("§6> §f" + tiempo).setScore(2);
                obj.getScore("§8 ").setScore(1);
            }
        }

        player.setScoreboard(board);
    }

    private String formatTime(int segundosTotales) {
        int minutos = segundosTotales / 60;
        int segundos = segundosTotales % 60;
        return String.format("%02d:%02d", minutos, segundos);
    }
}