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

public class RightPanelManager {

    private final UHC_DBasic plugin;
    private int cronometroSegundos = 0;
    private int tiempoTotalSegundos = 0;
    private int capitulo = 1;
    private int segundosPorCapitulo = 20*60;
    private BukkitTask partidaTask;
    boolean equiposFormados = false;
    private boolean pausado = false;
    private boolean shulkerEntregado = false;

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
            if (tieneVivos) equiposVivos.add(team);
        }

        if (equiposVivos.size() == 1) finalizarPartida(equiposVivos.get(0));
        else if (equiposVivos.isEmpty()) finalizarPartida(null);
    }

    private void finalizarPartida(Team ganador) {
        LanguageManager lang = plugin.getLang();
        if (partidaTask != null) { partidaTask.cancel(); partidaTask = null; }

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
            String nombreEquipo = ganador.getDisplayName();
            String color = ganador.getColor().toString();

            // Broadcast multilingüe: se envía a cada jugador en su idioma
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("");
                p.sendMessage(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", nombreEquipo));
                p.sendMessage("§f" + listaNombres);
                p.sendMessage(lang.get("victory.broadcast-footer", p));
                p.sendMessage("");

                p.sendTitle(lang.get("victory.title", p), lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", nombreEquipo), 10, 100, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                mostrarScoreboardVictoria(p, ganador, lang);
            }

            new BukkitRunnable() {
                int seg = 0;
                @Override public void run() {
                    if (seg >= 10) { this.cancel(); return; }
                    for (Player w : jugadoresGanadores) if (w.isOnline()) lanzarCohete(w.getLocation());
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
        // Título de victoria localizado
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

        Scoreboard managerBoard = Bukkit.getScoreboardManager().getMainScoreboard();

        if (managerBoard.getObjective("uhc") != null) managerBoard.getObjective("uhc").unregister();
        if (managerBoard.getObjective("vida_tab") != null) managerBoard.getObjective("vida_tab").unregister();

        for (Team team : new HashSet<>(managerBoard.getTeams())) {
            if (team.getName().startsWith("h_")) team.unregister();
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            actualizarScoreboard(p, "00:00", "00:00", false);
        }
    }

    public void iniciarPartida() {
        if (partidaTask != null) partidaTask.cancel();
        pausado = false; equiposFormados = false; jugadoresEliminados.clear();
        LanguageManager lang = plugin.getLang();

        for(Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0, false, false, false));
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

        // SIDEBAR (UHC) localizada
        Objective obj = board.getObjective("uhc");
        if (obj != null) obj.unregister();

        obj = board.registerNewObjective("uhc", "dummy", ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.title", player)));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());

        // TAB (VIDA) localizada
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
                            obj.getScore(" §8> " + colorPrefix + entry + healthText).setScore(next--);
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

            Team mainBoardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String teamKey = "h_" + onlinePlayer.getName();
                Team t = board.getTeam(teamKey);
                if (t == null) {
                    t = board.registerNewTeam(teamKey);
                    t.addEntry(onlinePlayer.getName());
                }
                t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                boolean esCompanero = (mainBoardTeam != null && mainBoardTeam.hasEntry(onlinePlayer.getName()));
                boolean soyYo = onlinePlayer.equals(player);

                if (soyYo || esCompanero) {
                    t.setPrefix("");
                    t.setSuffix("");
                    t.setColor(ChatColor.WHITE);
                } else {
                    int fakeId = Math.abs(onlinePlayer.getName().hashCode() % 100);
                    String prefix = lang.get("scoreboard.anonymous-prefix", player).replace("%id%", String.valueOf(fakeId));
                    String suffix = lang.get("scoreboard.anonymous-suffix", player) + " " + ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.health-icon", player));
                    t.setPrefix(prefix);
                    t.setSuffix(suffix);
                    t.setColor(ChatColor.RED);
                }
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

    public void setPausado(boolean e) { this.pausado = e; }
    public boolean isPausado() { return pausado; }
    public int getTiempoTotalSegundos() { return tiempoTotalSegundos; }
    public int getSegundosPorCapitulo() {return segundosPorCapitulo; }
    public void setSegundosPorCapitulo(int s) {this.segundosPorCapitulo = s;}
    public Set<String> getJugadoresEliminados() { return jugadoresEliminados; }
}