package me.dalibex.UHC_DBasic.managers;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.GameRules.*;

public class RightPanelManager {

    private final UHC_DBasic plugin;
    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20*60;
    private BukkitTask partidaTask;
    private boolean partidaIniciada = false;
    boolean equiposFormados = false;
    private boolean pausado = false;
    private boolean shulkerEntregado = false;
    private final Set<String> jugadoresEliminados = new HashSet<>();
    private final List<String> participantesIniciales = new ArrayList<>();

    public RightPanelManager(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    // --- DETECCIÓN DE VICTORIA ---
    public void comprobarVictoria() {
        if (this.tiempoTotalSegundos <= 0) return;

        List<Player> jugadoresVivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .filter(p -> !jugadoresEliminados.contains(p.getName()))
                .collect(Collectors.toList());

        if (jugadoresVivos.isEmpty()) {
            finalizarPartida(null);
            return;
        }

        int episodioActual = (this.tiempoTotalSegundos / this.segundosPorCapitulo) + 1;
        int episodioEquipos = 3;

        if (episodioActual < episodioEquipos) {
            if (jugadoresVivos.size() == 1) {
                Player ganador = jugadoresVivos.get(0);
                Team equipo = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(ganador.getName());

                if (equipo == null) {
                    Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                    Team tempTeam = board.getTeam("ganador_temp");
                    if (tempTeam == null) tempTeam = board.registerNewTeam("ganador_temp");

                    tempTeam.addEntry(ganador.getName());
                    tempTeam.setDisplayName(ganador.getName());
                    tempTeam.setColor(org.bukkit.ChatColor.GOLD);

                    finalizarPartida(tempTeam);
                } else {
                    finalizarPartida(equipo);
                }
            }
        } else {
            Map<String, Team> equiposVivos = new HashMap<>();
            for (Player p : jugadoresVivos) {
                Team equipo = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
                if (equipo != null) {
                    equiposVivos.put(equipo.getName(), equipo);
                } else {
                    equiposVivos.put("SOLO_" + p.getName(), null);
                }
            }

            if (equiposVivos.size() == 1) {
                String key = equiposVivos.keySet().iterator().next();
                Team equipoGanador = equiposVivos.get(key);

                if (equipoGanador == null) {
                    String soloName = key.replace("SOLO_", "");
                    finalizarPartidaIndividual(soloName);
                } else {
                    finalizarPartida(equipoGanador);
                }
            }
        }
    }

    private void finalizarPartidaIndividual(String nombreGanador) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        if (board.getTeam("ganador_temp") != null) {
            board.getTeam("ganador_temp").unregister();
        }

        Team tempTeam = board.registerNewTeam("ganador_temp");
        tempTeam.addEntry(nombreGanador);
        tempTeam.setDisplayName(nombreGanador);
        tempTeam.setColor(ChatColor.GOLD);

        finalizarPartida(tempTeam);
    }

    private void finalizarPartida(Team ganador) {
        LanguageManager lang = plugin.getLang();
        // Detenemos el cronómetro de la partida
        if (partidaTask != null) {
            partidaTask.cancel();
            partidaTask = null;
        }

        if (ganador != null) {
            List<String> nombresFormateados = new ArrayList<>();
            List<Player> vivosParaCohetes = new ArrayList<>();

            for (String entry : ganador.getEntries()) {

                if (jugadoresEliminados.contains(entry)) {
                    nombresFormateados.add("§7§m" + entry + "§r");
                } else {
                    nombresFormateados.add("§f" + entry);

                    Player p = Bukkit.getPlayer(entry);
                    if (p != null && p.isOnline()) {
                        vivosParaCohetes.add(p);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255, false, false));
                    }
                }
            }

            String listaFinal = String.join("§7, ", nombresFormateados);
            String nombreEquipo = ganador.getDisplayName();
            String color = ganador.getColor().toString();

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("");
                p.sendMessage(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", nombreEquipo));
                p.sendMessage("§7Integrantes: " + listaFinal);
                p.sendMessage(lang.get("victory.broadcast-footer", p));
                p.sendMessage("");

                // Título gigante en pantalla
                p.sendTitle(lang.get("victory.title", p),
                        lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", nombreEquipo),
                        10, 100, 20);

                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                mostrarScoreboardVictoria(p, ganador, lang);
            }

            new BukkitRunnable() {
                int seg = 0;
                @Override
                public void run() {
                    if (seg >= 10) {
                        this.cancel();
                        return;
                    }
                    for (Player w : vivosParaCohetes) {
                        if (w.isOnline()) {
                            lanzarCohete(w.getLocation());
                        }
                    }
                    seg++;
                }
            }.runTaskTimer(plugin, 0L, 20L);

        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.get("victory.no-survivors", p));
            }
        }
    }

    private void mostrarScoreboardVictoria(Player player, Team ganador, LanguageManager lang) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", "dummy", lang.get("victory.scoreboard-title", player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        obj.getScore("§1 ").setScore(2);
        obj.getScore(lang.get("victory.scoreboard-winner", player).replace("%color%", ganador.getColor().toString()).replace("%team%", ganador.getDisplayName())).setScore(1);
        player.setScoreboard(board);
    }

    public void setStandBy() {
        if (partidaTask != null) { partidaTask.cancel(); partidaTask = null; }
        cronometroSegundos = 0; tiempoTotalSegundos = 0; capitulo = 1;
        equiposFormados = false; jugadoresEliminados.clear();
        shulkerEntregado = false; this.partidaIniciada = false;

        World world = Bukkit.getWorlds().get(0);
        int y = world.getHighestBlockYAt(0, 0);
        if (y < 60) y = 100;
        Location spawnLoc = new Location(world, 0.5, y + 1, 0.5);

        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        if (managerBoard.getObjective("uhc") != null) managerBoard.getObjective("uhc").unregister();
        if (managerBoard.getObjective("vida_tab") != null) managerBoard.getObjective("vida_tab").unregister();

        for (Team team : new HashSet<>(managerBoard.getTeams())) {
            if (team.getName().startsWith("h_")) team.unregister();
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(spawnLoc);
            p.setPlayerListName(p.getName());
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            actualizarScoreboard(p, "00:00", "00:00", false);
        }
    }

    public void iniciarPartida() {
        if (partidaTask != null) return;

        //RESETS DE SEGURIDAD
        this.partidaIniciada = true;
        this.cronometroSegundos = 0;
        this.tiempoTotalSegundos = 0;
        this.capitulo = 1;
        this.pausado = false;
        this.equiposFormados = false;
        this.jugadoresEliminados.clear();

        LanguageManager lang = plugin.getLang();
        registrarParticipantes();

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName(p.getName());
            p.damage(0.01);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        p.setHealth(20.0);
                        p.setFoodLevel(20);
                        p.setSaturation(20f);
                        p.setFallDistance(0);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }

        partidaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pausado) return;
                TeamManager tm = plugin.getTeamManager();

                if (cronometroSegundos == 1 && tm.getTeamSize() == 1 && !equiposFormados) { tm.shuffleTeams(); equiposFormados = true; }

                cronometroSegundos++; tiempoTotalSegundos++;
                int restante = segundosPorCapitulo - (cronometroSegundos % segundosPorCapitulo);

                if (plugin.getAdminPanel().isShulkerOneEnabled() && !shulkerEntregado) {
                    entregarObjetoGlobal("items.shulker.name", Material.ORANGE_SHULKER_BOX);
                    shulkerEntregado = true;
                }

                if (cronometroSegundos % segundosPorCapitulo == 0) {
                    capitulo++;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (capitulo < 10) {
                            p.sendMessage(lang.get("game-events.chapter-start", p).replace("%prefix%", lang.get("general.prefix", p)).replace("%chapter%", String.valueOf(capitulo)));
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        }
                        if (capitulo == 10) {
                            for (String s : lang.getList("game-events.final-phase", p)) p.sendMessage(s);
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }

                    if(capitulo == 8 && plugin.getAdminPanel().isShulkerTwoEnabled()) {
                        entregarObjetoGlobal("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
                    }
                    if (capitulo == 3 && tm.getTeamSize() > 1 && !equiposFormados) {
                        tm.shuffleTeams(); entregarBrujulasDeSeguimiento(lang);
                        equiposFormados = true;
                        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(lang.get("game-events.teams-formed", p));
                    }
                    if (capitulo == 4) {
                        for (World w : Bukkit.getWorlds()) {
                            w.setGameRule(PVP, true);
                        }
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            for (String s : lang.getList("game-events.pvp-enabled", p)) p.sendMessage(s);
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                        }
                    }
                }
                for (Player p : Bukkit.getOnlinePlayers()) actualizarScoreboard(p, formatTime(restante), formatTime(tiempoTotalSegundos), true);
                plugin.getEventHandler().onCompassTrack();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void entregarBrujulasDeSeguimiento(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (jugadoresEliminados.contains(p.getName())) continue;

            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.get("tracking-compass.name", p));
                meta.setLore(lang.getList("tracking-compass.lore", p));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                compass.setItemMeta(meta);
            }

            if (!p.getInventory().addItem(compass).isEmpty()) {
                p.getWorld().dropItemNaturally(p.getLocation(), compass);
                p.sendMessage(lang.get("tracking-compass.inv-full", p));
            } else p.sendMessage(lang.get("tracking-compass.received", p));
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
        }
    }

    public void actualizarScoreboard(Player player, String tiempo, String tiempoTotal, boolean partidaActiva) {
        LanguageManager lang = plugin.getLang();
        Scoreboard board = player.getScoreboard();

        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("uhc");
        if (obj != null) obj.unregister();

        obj = board.registerNewObjective("uhc", "dummy", ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.title", player)));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());

        Objective objVida = board.getObjective("vida_tab");
        if (partidaActiva) {
            if (objVida == null) {
                objVida = board.registerNewObjective("vida_tab", "health",
                        ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.health-icon", player)),
                        org.bukkit.scoreboard.RenderType.HEARTS);
                objVida.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
        } else {
            if (objVida != null) objVida.unregister();
        }

        if (!partidaActiva) {
            obj.getScore("§1 ").setScore(5);
            obj.getScore(lang.get("scoreboard.waiting", player)).setScore(4);
            obj.getScore("§2 ").setScore(3);
            obj.getScore(lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))).setScore(2);
            obj.getScore("§3 ").setScore(1);
        } else {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
            String pvpStatus = (capitulo < 4) ? lang.get("scoreboard.pvp-pact", player) : lang.get("scoreboard.pvp-active", player);

            obj.getScore("§4 ").setScore(23);
            if (capitulo < 10) obj.getScore(lang.get("scoreboard.phase", player).replace("%chapter%", String.valueOf(capitulo))).setScore(22);
            else {
                obj.getScore(lang.get("scoreboard.finalized", player)).setScore(21);
                obj.getScore(lang.get("scoreboard.go-center", player)).setScore(20);
                obj.getScore("§5 ").setScore(19);
            }
            obj.getScore(lang.get("scoreboard.pvp-label", player).replace("%status%", pvpStatus)).setScore(18);
            obj.getScore("§6 ").setScore(17);

            int next = 16;
            int teamSize = plugin.getTeamManager().getTeamSize();
            if (teamSize == 1) {
                String line = (team != null && !team.getPrefix().contains("team_")) ?
                        lang.get("scoreboard.team-label", player).replace("%color%", team.getColor().toString()).replace("%name%", team.getDisplayName()) : lang.get("scoreboard.team-rename-warn", player);
                obj.getScore(line).setScore(next--);
            } else {
                if (capitulo < 3) {
                    for (int i = 1; i < teamSize; i++) obj.getScore(" §d👥 §f: §k??????" + (" ".repeat(i))).setScore(next--);
                } else {
                    String line = (team != null && !team.getPrefix().contains("team_")) ? lang.get("scoreboard.team-mates-label", player).replace("%color%", team.getColor().toString()).replace("%name%", team.getDisplayName()) :
                            (team != null ? lang.get("scoreboard.team-rename-warn", player) : lang.get("scoreboard.team-assigning", player));
                    obj.getScore(line).setScore(next--);
                    if (team != null) {
                        for (String entry : team.getEntries()) {
                            if (entry.equals(player.getName())) continue;
                            String healthText; String colorPrefix = "§f";
                            if (jugadoresEliminados.contains(entry)) { colorPrefix = "§7§m"; healthText = lang.get("scoreboard.mate-dead", player); }
                            else {
                                Player m = Bukkit.getPlayer(entry);
                                if (m != null && m.isOnline()) {
                                    double h = m.getHealth();
                                    String c = (h > 15) ? "§a" : (h > 10) ? "§2" : (h > 5) ? "§e" : "§c";
                                    healthText = " " + c + (int)h + "§4❤";
                                } else healthText = lang.get("scoreboard.mate-offline", player);
                            }
                            obj.getScore("§6> " + colorPrefix + entry + healthText).setScore(next--);
                        }
                    }
                }
            }
            obj.getScore("§6 ").setScore(next--);
            obj.getScore(lang.get("scoreboard.time-total-label", player)).setScore(next--);
            obj.getScore("§6> §f" + tiempoTotal).setScore(next--);
            obj.getScore("§7 ").setScore(next--);
            if (capitulo < 10) {
                obj.getScore(lang.get("scoreboard.time-next-label", player)).setScore(next--);
                obj.getScore("§6> §f" + tiempo).setScore(next--);
            }

            // --- LÓGICA DE VISIBILIDAD DE EQUIPOS (TAB Y NAMETAGS) ---
            Team myTeam = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.setPlayerListName(onlinePlayer.getName());

                String teamKey = "h_" + onlinePlayer.getName();
                Team t = board.getTeam(teamKey);

                if (t == null) {
                    t = board.registerNewTeam(teamKey);
                    t.addEntry(onlinePlayer.getName());
                }

                t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

                boolean esCompanero = (myTeam != null && myTeam.hasEntry(onlinePlayer.getName()));
                boolean soyYo = onlinePlayer.equals(player);

                if (soyYo || esCompanero) {
                    t.setColor(ChatColor.LIGHT_PURPLE);
                } else {
                    t.setColor(ChatColor.RED);
                }

                t.setPrefix("");
                t.setSuffix("");
            }
        }
    }

    private String formatTime(int s) {
        int h = s / 3600; int m = (s % 3600) / 60; int sec = s % 60;
        return (h > 0) ? String.format("%02d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
    }

    private void lanzarCohete(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class); FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder().withColor(Color.GREEN).withFade(Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).withTrail().build());
        fwm.setPower(1); fw.setFireworkMeta(fwm);
    }

    private void entregarObjetoGlobal(String nombreKey, Material material) {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (jugadoresEliminados.contains(p.getName())) continue;

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', lang.get(nombreKey, p)));
                item.setItemMeta(meta);
            }

            HashMap<Integer, ItemStack> leftovers = p.getInventory().addItem(item);
            if (!leftovers.isEmpty()) {
                for (ItemStack leftover : leftovers.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), leftover);
                }
                p.sendMessage(lang.get("general.prefix", p) + lang.get("general.inv-full", p));
            }
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    // ----- PARTICIPANTES DE LA PARTIDA PARA LOS EQUIPOS -----
    public void registrarParticipantes() {
        participantesIniciales.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                participantesIniciales.add(p.getName());
            }
        }
    }
    public List<String> getParticipantesIniciales() {
        return participantesIniciales;
    }
    // --------------------------------------------------------

    public void setPausado(boolean e) { this.pausado = e; }
    public boolean isPausado() { return pausado; }
    public int getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public int getSegundosPorCapitulo() {return segundosPorCapitulo; }
    public void setSegundosPorCapitulo(int s) {this.segundosPorCapitulo = s;}
    public Set<String> getJugadoresEliminados() { return jugadoresEliminados; }
    public boolean isPartidaIniciada() {
        return partidaIniciada;
    }
    public void setPartidaIniciada(boolean estado) {
        this.partidaIniciada = estado;
    }
}