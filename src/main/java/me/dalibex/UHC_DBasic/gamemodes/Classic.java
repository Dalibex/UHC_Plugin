package me.dalibex.UHC_DBasic.gamemodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import static org.bukkit.GameRules.PVP;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import net.kyori.adventure.title.Title;

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
            runSkinRotation();
        }

        // Shulker 2 (Episodio 8)
        if (nuevoCap == 8 && plugin.getAdminPanel().isShulkerTwoEnabled()) {
            giveGlobalItem("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
        }

        // Formación de Equipos Aleatorios (Condicional: Ep 3)
        if (tm.getTeamSize() > 1 && !teamsFormed && !tm.isCustomTeamsEnabled() && nuevoCap == 3) {
            tm.shuffleTeams();
            giveTrackingCompasses(lang);
            teamsFormed = true;
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

        Objective obj = getOrCreateSidebar(board, player, lang);
        clearStaleSidebarKeys(obj, player);
        List<String> keys = new ArrayList<>();

        Objective objVida = board.getObjective(ScoreboardHelper.HEALTH_OBJECTIVE);
        if (partidaActiva) {
            if (objVida == null) {
                objVida = board.registerNewObjective(ScoreboardHelper.HEALTH_OBJECTIVE, Criteria.HEALTH,
                        lang.getComponent("scoreboard.health-icon", player),
                        org.bukkit.scoreboard.RenderType.HEARTS);
                objVida.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
        } else if (objVida != null) {
            objVida.unregister();
        }

        if (!partidaActiva) {
            ScoreboardHelper.addLobbyScores(obj, keys, getName(), player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(30);
            ScoreboardHelper.addPhaseInfo(obj, next, keys, player, lang, gm);
            ScoreboardHelper.addTeamInfo(obj, next, keys, player, lang, plugin.getTeamManager(), gm);
            ScoreboardHelper.addTimers(obj, next, keys, tiempo, tiempoTotal, player, lang, gm);
        }

        storeSidebarKeys(player, keys);
    }

    @Override
    public void checkVictory() {
        if (!gm.isGameStarted() || gm.getTotalSeconds() <= 5) return;

        Set<String> eliminados = gm.getEliminatedPlayers();

        List<Player> jugadoresVivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL)
                .filter(p -> !eliminados.contains(p.getName()))
                .collect(Collectors.toList());

        if (jugadoresVivos.isEmpty()) {
            // Un jugador que salió y sigue vivo mantiene la partida abierta;
            // solo se finaliza cuando no queda nadie vivo (online u offline).
            boolean offlineVivo = gm.getInitialParticipants().stream()
                    .anyMatch(nombre -> !eliminados.contains(nombre) && Bukkit.getPlayer(nombre) == null);
            if (!offlineVivo) {
                finishGame(null);
            }
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
            finishGame(equipoGanador);
        }
    }

    private Team createTempWinnerTeam(String playerName) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team temp = board.getTeam("winner_temp");
        if (temp != null) temp.unregister();
        temp = board.registerNewTeam("winner_temp");
        temp.addEntry(playerName);
        temp.displayName(Component.text(playerName));
        temp.color(NamedTextColor.GOLD);
        return temp;
    }

    private void finishGame(Team ganador) {
        LanguageManager lang = plugin.getLang();
        gm.stopGameTask();
        gm.setGameStarted(false);

        for (Player online : Bukkit.getOnlinePlayers()) {
            gm.revealIdentity(online);
            gm.updateVisualIdentity(online);
        }

        if (ganador != null) {
            broadcastVictory(ganador, lang);
            List<Player> winners = new ArrayList<>();
            for (String entry : ganador.getEntries()) {
                Player p = Bukkit.getPlayer(entry);
                if (p != null && !gm.getEliminatedPlayers().contains(entry)) winners.add(p);
            }
            applyVictoryEffects(winners);
        } else {
            Bukkit.broadcast(legacySection().deserialize(lang.get("victory.no-survivors", null)));
        }
    }

    private void broadcastVictory(Team ganador, LanguageManager lang) {
        String color = TextUtil.legacyColor(ganador.color());
        String nombreEquipo = legacySection().serialize(ganador.displayName());
        
        List<String> formattedNames = ganador.getEntries().stream()
                .map(entry -> gm.getEliminatedPlayers().contains(entry) ? "§7§m" + entry + "§r" : "§f" + entry)
                .collect(Collectors.toList());
        String membersList = String.join("§7, ", formattedNames);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(legacySection().deserialize(""));
            p.sendMessage(legacySection().deserialize(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", nombreEquipo)));
            p.sendMessage(legacySection().deserialize("§7Integrantes: " + membersList));
            p.sendMessage(legacySection().deserialize(lang.get("victory.broadcast-footer", p)));
            p.sendMessage(legacySection().deserialize(""));

            p.showTitle(Title.title(
                legacySection().deserialize(lang.get("victory.title", p)),
                legacySection().deserialize(lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", nombreEquipo)),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            
            showPostGameScoreboard(p, ganador, lang);
        }
    }

    private void showPostGameScoreboard(Player player, Team ganador, LanguageManager lang) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", Criteria.DUMMY, lang.getComponent("victory.scoreboard-title", player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());
        String colorCode = TextUtil.legacyColor(ganador.color());
        obj.getScore(lang.get("victory.scoreboard-winner", player)
                .replace("%color%", colorCode)
                .replace("%team%", legacySection().serialize(ganador.displayName()))).setScore(1);
        player.setScoreboard(board);
    }
}