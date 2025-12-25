package me.dalibex.UHC_DBasic.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

public class RightPanelManager {

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

    // --- EVENTO DE MUERTE Y DETECCIÓN DE VICTORIA ---
    public void comprobarVictoria() {
        if (tiempoTotalSegundos <= 0 || partidaTask == null) return;

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        List<Team> equiposVivos = new ArrayList<>();

        for (Team team : board.getTeams()) {
            boolean tieneVivos = false;
            for (String entry : team.getEntries()) {
                if (!jugadoresEliminados.contains(entry)) {
                    tieneVivos = true;
                    break;
                }
            }
            if (tieneVivos) {
                equiposVivos.add(team);
            }
        }

        if (equiposVivos.size() == 1) {
            Team ganador = equiposVivos.get(0);
            finalizarPartida(ganador);
        }
        else if (equiposVivos.isEmpty()) {
            finalizarPartida(null);
        }
    }
    private void finalizarPartida(Team ganador) {
        if (partidaTask != null) {
            partidaTask.cancel();
            partidaTask = null;
        }

        if (ganador != null) {
            List<String> nombresGanadores = new ArrayList<>();
            List<Player> jugadoresGanadores = new ArrayList<>();

            for (String entry : ganador.getEntries()) {
                if (!jugadoresEliminados.contains(entry)) {
                    nombresGanadores.add(entry);
                    Player p = Bukkit.getPlayer(entry);
                    if (p != null) jugadoresGanadores.add(p);
                }
            }

            String listaNombres = String.join(", ", nombresGanadores);
            ChatColor colorTeam = ganador.getColor();
            String nombreEquipo = ganador.getDisplayName();

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§e§l🎉 FELICIDADES, " + colorTeam + nombreEquipo + "§e:");
            Bukkit.broadcastMessage("§f" + listaNombres);
            Bukkit.broadcastMessage("§6§l¡Han sido los ganadores del UHC!");
            Bukkit.broadcastMessage("");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§6🏆 ¡VICTORIA! 🏆", colorTeam + nombreEquipo + " ha ganado", 10, 100, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                mostrarScoreboardVictoria(p, ganador);
            }

            new BukkitRunnable() {
                int segundos = 0;
                @Override
                public void run() {
                    if (segundos >= 10) {
                        this.cancel();
                        return;
                    }
                    for (Player winner : jugadoresGanadores) {
                        if (winner.isOnline()) {
                            lanzarCohete(winner.getLocation());
                        }
                    }
                    segundos++;
                }
            }.runTaskTimer(plugin, 0L, 20L);

        } else {
            Bukkit.broadcastMessage("§c☠ La partida ha terminado sin supervivientes.");
        }
    }
    private void lanzarCohete(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();

        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.YELLOW)
                .with(FireworkEffect.Type.BALL_LARGE)
                .withTrail()
                .build());
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }
    private void mostrarScoreboardVictoria(Player player, Team ganador) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", "dummy", "§6§l!VICTORIA!");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.numberFormat(NumberFormat.blank());
        obj.getScore("§1 ").setScore(2);

        String textoGanador = "§fEquipo ganador: " + ganador.getColor() + ganador.getDisplayName();
        obj.getScore(textoGanador).setScore(1);

        player.setScoreboard(board);
    }
    // ------------------------------------------------

    // --- CONTROL DE PARTIDA Y REINICIO, TIMERS ---
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
                        Bukkit.broadcastMessage("§c§l¡EL TIEMPO HA TERMINADO! §c⚔");
                        Bukkit.broadcastMessage("§fDirígete a X=0 Z=0 para la pelea final");
                        Bukkit.broadcastMessage("§8§m------------------------------------");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }

                    if (capitulo == 4 && tm.getTeamSize() > 1 && !equiposFormados) {
                        tm.shuffleTeams();
                        entregarBrujulasDeSeguimiento();
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

    private void entregarBrujulasDeSeguimiento() {
        ItemStack trackingCompass = new ItemStack(Material.COMPASS);
        ItemMeta meta = trackingCompass.getItemMeta();

        meta.setDisplayName("§b§lLocalizador de Compañeros");
        meta.setLore(Arrays.asList("§7Apunta hacia el aliado más cercano.", "§e¡No la pierdas!"));

        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        trackingCompass.setItemMeta(meta);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!jugadoresEliminados.contains(p.getName())) {

                java.util.Map<Integer, ItemStack> sobrantes = p.getInventory().addItem(trackingCompass);
                if (!sobrantes.isEmpty()) {
                    for (ItemStack item : sobrantes.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), item);
                    }
                    p.sendMessage("§b§l» §c¡Inventario lleno! §fTu localizador ha caído al suelo.");
                } else {
                    p.sendMessage("§b§l» §fHas recibido un §bLocalizador §fde equipo.");
                }

                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            }
        }
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
        int h = segundosTotales / 3600;
        int m = (segundosTotales % 3600) / 60;
        int s = segundosTotales % 60;

        if (h > 0) {
            return String.format("%02d:%02d:%02d", h, m, s);
        } else {
            return String.format("%02d:%02d", m, s);
        }
    }
    // ------------------------------------------------

    // Getters y setters
    public void setPausado(boolean estado) { this.pausado = estado; }
    public boolean isPausado() { return pausado; }
    public int getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public int getSegundosPorCapitulo() {return segundosPorCapitulo; }
    public void setSegundosPorCapitulo(int segundos) {this.segundosPorCapitulo = segundos;}
    public Set<String> getJugadoresEliminados() { return jugadoresEliminados; }
}