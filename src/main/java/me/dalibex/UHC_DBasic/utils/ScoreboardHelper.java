package me.dalibex.UHC_DBasic.utils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Clase de utilidad para centralizar la construcción de líneas del Scoreboard.
 * Evita la duplicación de lógica visual entre diferentes modos de juego.
 * Cada método registra además las claves generadas en {@code keys} para que
 * el modo pueda limpiar solo las líneas obsoletas entre ticks.
 */
public class ScoreboardHelper {

    /** Nombre del objetivo de sidebar "uhc" registrado por jugador. */
    public static final String SIDEBAR_OBJECTIVE = "uhc";

    /** Nombre del objetivo de vida del tab ("vida_tab"). */
    public static final String HEALTH_OBJECTIVE = "vida_tab";

    /** Prefijo de los equipos internos del mundo ("h_"). */
    public static final String TEAM_PREFIX = "h_";

    /**
     * Sincroniza el objetivo de vida que Minecraft renderiza en el TAB.
     */
    public static void syncTabHealthObjective(Scoreboard board, Player player, LanguageManager lang, boolean active) {
        Objective objVida = board.getObjective(HEALTH_OBJECTIVE);
        if (active) {
            if (objVida == null) {
                objVida = board.registerNewObjective(HEALTH_OBJECTIVE, Criteria.HEALTH,
                        lang.getComponent("scoreboard.health-icon", player), RenderType.HEARTS);
            }
            objVida.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else if (objVida != null) {
            objVida.unregister();
        }
    }

    /**
     * Añade las líneas del sidebar del lobby: modo, esperando y jugadores online.
     */
    public static void addLobbyScores(Objective obj, List<String> keys, String modeName, Player player, LanguageManager lang) {
        score(obj, "§1 ", 7, keys);
        score(obj, lang.get("scoreboard.mode-label", player).replace("%mode%", modeName), 6, keys);
        score(obj, "§2 ", 5, keys);
        score(obj, lang.get("scoreboard.waiting", player), 4, keys);
        score(obj, "§3 ", 3, keys);
        score(obj, lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size())), 2, keys);
        score(obj, "§4 ", 1, keys);
    }

    /**
     * Añade las líneas de información de fase y PVP al scoreboard.
     */
    public static void addPhaseInfo(Objective obj, AtomicInteger next, List<String> keys, Player player, LanguageManager lang, GameManager gm) {
        int capitulo = gm.getChapter();
        String pvpStatus = (capitulo < gm.getPvpEnabledEpisode())
                ? lang.get("scoreboard.pvp-pact", player)
                : lang.get("scoreboard.pvp-active", player);

        score(obj, "§1 ", next.getAndDecrement(), keys);
        if (capitulo < 10) {
            score(obj, lang.get("scoreboard.phase", player).replace("%chapter%", String.valueOf(capitulo)), next.getAndDecrement(), keys);
        } else {
            score(obj, lang.get("scoreboard.finalized", player), next.getAndDecrement(), keys);
            if (gm.getCurrentMode() instanceof me.dalibex.UHC_DBasic.gamemodes.Classic) {
                score(obj, lang.get("scoreboard.go-center", player), next.getAndDecrement(), keys);
                score(obj, "§2 ", next.getAndDecrement(), keys);
            }
        }
        score(obj, lang.get("scoreboard.pvp-label", player).replace("%status%", pvpStatus), next.getAndDecrement(), keys);
        score(obj, "§3 ", next.getAndDecrement(), keys);
    }

    /**
     * Añade la información del equipo y compañeros vivos/muertos.
     */
    public static void addTeamInfo(Objective obj, AtomicInteger next, List<String> keys, Player player, LanguageManager lang, TeamManager tm, GameManager gm) {
        Team team = tm.getPlayerTeam(player.getName());
        int teamSize = tm.getTeamSize();
        int capitulo = gm.getChapter();

        if (teamSize == 1) {
            String line = (team != null && !tm.isDefaultName(team)) ?
                    lang.get("scoreboard.team-label", player).replace("%color%", TextUtil.legacyColor(team.color())).replace("%name%", legacySection().serialize(team.displayName())) 
                    : lang.get("scoreboard.team-rename-warn", player);
            score(obj, line, next.getAndDecrement(), keys);
        } else {
            boolean manual = tm.isCustomTeamsEnabled();
            if (capitulo < tm.getTeamsFormedEpisode() && !manual) {
                for (int i = 1; i < teamSize; i++) {
                    score(obj, " §d👥 §f: §k??????" + (" ".repeat(i)), next.getAndDecrement(), keys);
                }
            } else {
                String line = (team != null && !tm.isDefaultName(team)) ?
                        lang.get("scoreboard.team-mates-label", player).replace("%color%", TextUtil.legacyColor(team.color())).replace("%name%", legacySection().serialize(team.displayName())) 
                        : (team != null ? lang.get("scoreboard.team-rename-warn", player) : lang.get("scoreboard.team-assigning", player));
                
                score(obj, line, next.getAndDecrement(), keys);
                if (team != null) {
                    for (String entry : tm.getMemberNames(team)) {
                        if (entry.equalsIgnoreCase(player.getName())) continue;
                        addMateLine(obj, next, keys, player, entry, lang, gm);
                    }
                }
            }
        }
    }

    private static void addMateLine(Objective obj, AtomicInteger next, List<String> keys, Player viewer, String entry, LanguageManager lang, GameManager gm) {
        String healthText;
        String colorPrefix = "§f";
        String nombreParaMostrar = entry;
        Player m = Bukkit.getPlayer(entry);

        if (gm.getEliminatedPlayers().contains(entry)) {
            colorPrefix = "§7§m";
            healthText = lang.get("scoreboard.mate-dead", viewer);
        } else {
            if (m != null && m.isOnline()) {
                nombreParaMostrar = m.getName();
                colorPrefix = "§f";
                double h = m.getHealth();
                String c = (h > 15) ? "§a" : (h > 10) ? "§2" : (h > 5) ? "§e" : "§c";
                healthText = " " + c + (int)h + "§4❤";
            } else {
                healthText = lang.get("scoreboard.mate-offline", viewer);
            }
        }
        score(obj, "§6> " + colorPrefix + nombreParaMostrar + healthText, next.getAndDecrement(), keys);
    }

    /**
     * Añade los cronómetros de tiempo total y tiempo hasta el siguiente capítulo.
     */
    public static void addTimers(Objective obj, AtomicInteger next, List<String> keys, String tiempo, String tiempoTotal, Player player, LanguageManager lang, GameManager gm) {
        score(obj, "§6 ", next.getAndDecrement(), keys);
        score(obj, lang.get("scoreboard.time-total-label", player), next.getAndDecrement(), keys);
        score(obj, "§6> §f" + tiempoTotal, next.getAndDecrement(), keys);
        score(obj, "§7 ", next.getAndDecrement(), keys);
        
        if (gm.getChapter() < 10) {
            score(obj, lang.get("scoreboard.time-next-label", player), next.getAndDecrement(), keys);
            score(obj, "§6> §f" + tiempo, next.getAndDecrement(), keys);
        }
    }

    private static void score(Objective obj, String key, int value, List<String> keys) {
        obj.getScore(key).setScore(value);
        keys.add(key);
    }
}
