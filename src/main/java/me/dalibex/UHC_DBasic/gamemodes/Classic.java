package me.dalibex.UHC_DBasic.gamemodes;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.GameRules.PVP;

public class Classic extends AbstractUHCGameMode {

    public Classic(UHC_DBasic plugin, GameManager gm) {
        super(plugin, gm);
    }

    @Override
    public String getName() {
        return "Classic UHC";
    }

    @Override
    protected void onChapterChange(int nuevoCap) {
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (nuevoCap < 10) {
                p.sendMessage(lang.get("game-events.chapter-start", p)
                        .replace("%prefix%", lang.get("general.prefix", p))
                        .replace("%chapter%", String.valueOf(nuevoCap)));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else if (nuevoCap == 10) {
                for (String s : lang.getList("game-events.final-phase", p)) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
            }
        }

        // Rotación de Skins (Capítulos 2 al 10)
        if (nuevoCap <= 10) {
            ejecutarRotacionDeSkins();
        }

        // Shulker 2 (Episodio 8)
        if (nuevoCap == 8 && plugin.getAdminPanel().isShulkerTwoEnabled()) {
            entregarObjetoGlobal("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
        }

        // Formación de Equipos Aleatorios (Condicional: Ep 3)
        if (tm.getTeamSize() > 1 && !equiposFormados && !tm.isCustomTeamsEnabled() && nuevoCap == 3) {
            tm.shuffleTeams();
            entregarBrujulasDeSeguimiento(lang);
            equiposFormados = true;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.get("game-events.teams-formed", p));
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            }
        }

        // Activación de PVP (Episodio 4)
        if (nuevoCap == 4) {
            for (World w : Bukkit.getWorlds()) w.setGameRule(PVP, true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (String s : lang.getList("game-events.pvp-enabled", p)) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
            }
        }
    }

    @Override
    public void updateScoreboard(Player player, String tiempo, String tiempoTotal, boolean partidaActiva) {
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
        } else if (objVida != null) {
            objVida.unregister();
        }

        if (!partidaActiva) {
            renderLobbyScores(obj, player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(30);
            ScoreboardHelper.addPhaseInfo(obj, next, player, lang, gm);
            ScoreboardHelper.addTeamInfo(obj, next, player, lang, plugin.getTeamManager(), gm);
            ScoreboardHelper.addTimers(obj, next, tiempo, tiempoTotal, player, lang, gm);
        }
    }

    private void renderLobbyScores(Objective obj, Player player, LanguageManager lang) {
        obj.getScore("§1 ").setScore(7);
        obj.getScore(lang.get("scoreboard.mode-label", player).replace("%mode%", getName())).setScore(6);
        obj.getScore("§2 ").setScore(5);
        obj.getScore(lang.get("scoreboard.waiting", player)).setScore(4);
        obj.getScore("§3 ").setScore(3);
        obj.getScore(lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))).setScore(2);
        obj.getScore("§4 ").setScore(1);
    }

    @Override
    public void checkVictory() {
        if (!gm.isPartidaIniciada() || gm.getTiempoTotalSegundos() <= 5) return;

        List<Player> jugadoresVivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .filter(p -> !gm.getJugadoresEliminados().contains(p.getName()))
                .collect(Collectors.toList());

        if (jugadoresVivos.isEmpty()) {
            finalizarPartida(null);
            return;
        }

        // Lógica de detección: Un solo equipo/jugador restante
        Map<String, Team> equiposVivos = new HashMap<>();
        for (Player p : jugadoresVivos) {
            Team equipo = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (equipo != null) equiposVivos.put(equipo.getName(), equipo);
            else equiposVivos.put("SOLO_" + p.getName(), null);
        }

        if (equiposVivos.size() == 1) {
            String key = equiposVivos.keySet().iterator().next();
            Team equipoGanador = equiposVivos.get(key);
            
            if (equipoGanador == null) {
                // Caso jugador individual sin equipo (Solos temprano o bug)
                equipoGanador = createTempWinnerTeam(key.replace("SOLO_", ""));
            }
            finalizarPartida(equipoGanador);
        }
    }

    private Team createTempWinnerTeam(String playerName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team temp = board.getTeam("winner_temp");
        if (temp != null) temp.unregister();
        temp = board.registerNewTeam("winner_temp");
        temp.addEntry(playerName);
        temp.setDisplayName(playerName);
        temp.setColor(ChatColor.GOLD);
        return temp;
    }

    private void finalizarPartida(Team ganador) {
        LanguageManager lang = plugin.getLang();
        gm.detenerPartidaTask();
        gm.setPartidaIniciada(false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            gm.revelarIdentidad(online);
            gm.actualizarIdentidadVisual(online);
        }

        if (ganador != null) {
            broadcastVictory(ganador, lang);
            List<Player> winners = new ArrayList<>();
            for (String entry : ganador.getEntries()) {
                Player p = Bukkit.getPlayer(entry);
                if (p != null && !gm.getJugadoresEliminados().contains(entry)) winners.add(p);
            }
            aplicarEfectosVictoria(winners);
        } else {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("victory.no-survivors", null)));
        }
    }

    private void broadcastVictory(Team ganador, LanguageManager lang) {
        String color = ganador.getColor().toString();
        String nombreEquipo = ganador.getDisplayName();
        
        List<String> formattedNames = ganador.getEntries().stream()
                .map(entry -> gm.getJugadoresEliminados().contains(entry) ? "§7§m" + entry + "§r" : "§f" + entry)
                .collect(Collectors.toList());
        String membersList = String.join("§7, ", formattedNames);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", nombreEquipo));
            p.sendMessage("§7Integrantes: " + membersList);
            p.sendMessage(lang.get("victory.broadcast-footer", p));
            p.sendMessage("");

            p.sendTitle(lang.get("victory.title", p), 
                        lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", nombreEquipo), 
                        10, 100, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            
            showPostGameScoreboard(p, ganador, lang);
        }
    }

    private void showPostGameScoreboard(Player player, Team ganador, LanguageManager lang) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", "dummy", lang.get("victory.scoreboard-title", player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        obj.getScore(lang.get("victory.scoreboard-winner", player)
                .replace("%color%", ganador.getColor().toString())
                .replace("%team%", ganador.getDisplayName())).setScore(1);
        player.setScoreboard(board);
    }
}