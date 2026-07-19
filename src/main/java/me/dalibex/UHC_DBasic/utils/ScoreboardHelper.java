package me.dalibex.UHC_DBasic.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Clase de utilidad para centralizar la construcción de líneas del Scoreboard.
 * Evita la duplicación de lógica visual entre diferentes modos de juego.
 */
public class ScoreboardHelper {

    /**
     * Añade las líneas de información de fase y PVP al scoreboard.
     */
    public static void addPhaseInfo(Objective obj, AtomicInteger next, Player player, LanguageManager lang, GameManager gm) {
        int capitulo = gm.getCapitulo();
        String pvpStatus = (capitulo < 4) ? lang.get("scoreboard.pvp-pact", player) : lang.get("scoreboard.pvp-active", player);

        obj.getScore("§1 ").setScore(next.getAndDecrement());
        if (capitulo < 10) {
            obj.getScore(lang.get("scoreboard.phase", player).replace("%chapter%", String.valueOf(capitulo))).setScore(next.getAndDecrement());
        } else {
            obj.getScore(lang.get("scoreboard.finalized", player)).setScore(next.getAndDecrement());
            if (gm.getModoActual() instanceof me.dalibex.UHC_DBasic.gamemodes.Classic) {
                obj.getScore(lang.get("scoreboard.go-center", player)).setScore(next.getAndDecrement());
                obj.getScore("§2 ").setScore(next.getAndDecrement());
            }
        }
        obj.getScore(lang.get("scoreboard.pvp-label", player).replace("%status%", pvpStatus)).setScore(next.getAndDecrement());
        obj.getScore("§3 ").setScore(next.getAndDecrement());
    }

    /**
     * Añade la información del equipo y compañeros vivos/muertos.
     */
    public static void addTeamInfo(Objective obj, AtomicInteger next, Player player, LanguageManager lang, TeamManager tm, GameManager gm) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        int teamSize = tm.getTeamSize();
        int capitulo = gm.getCapitulo();

        if (teamSize == 1) {
            String line = (team != null && !tm.isDefaultName(team)) ?
                    lang.get("scoreboard.team-label", player).replace("%color%", legacySection().serialize(Component.text("·").color(team.color())).replace("·", "")).replace("%name%", legacySection().serialize(team.displayName())) 
                    : lang.get("scoreboard.team-rename-warn", player);
            obj.getScore(line).setScore(next.getAndDecrement());
        } else {
            boolean manual = tm.isCustomTeamsEnabled();
            if (capitulo < 3 && !manual) {
                for (int i = 1; i < teamSize; i++) {
                    obj.getScore(" §d👥 §f: §k??????" + (" ".repeat(i))).setScore(next.getAndDecrement());
                }
            } else {
                String line = (team != null && !tm.isDefaultName(team)) ?
                        lang.get("scoreboard.team-mates-label", player).replace("%color%", legacySection().serialize(Component.text("·").color(team.color())).replace("·", "")).replace("%name%", legacySection().serialize(team.displayName())) 
                        : (team != null ? lang.get("scoreboard.team-rename-warn", player) : lang.get("scoreboard.team-assigning", player));
                
                obj.getScore(line).setScore(next.getAndDecrement());
                if (team != null) {
                    for (String entry : team.getEntries()) {
                        if (entry.equals(player.getName())) continue;
                        addMateLine(obj, next, player, entry, lang, gm);
                    }
                }
            }
        }
    }

    private static void addMateLine(Objective obj, AtomicInteger next, Player viewer, String entry, LanguageManager lang, GameManager gm) {
        String healthText;
        String colorPrefix = "§f";
        String nombreParaMostrar = entry;
        Player m = Bukkit.getPlayer(entry);

        if (gm.getJugadoresEliminados().contains(entry)) {
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
        obj.getScore("§6> " + colorPrefix + nombreParaMostrar + healthText).setScore(next.getAndDecrement());
    }

    /**
     * Añade los cronómetros de tiempo total y tiempo hasta el siguiente capítulo.
     */
    public static void addTimers(Objective obj, AtomicInteger next, String tiempo, String tiempoTotal, Player player, LanguageManager lang, GameManager gm) {
        obj.getScore("§6 ").setScore(next.getAndDecrement());
        obj.getScore(lang.get("scoreboard.time-total-label", player)).setScore(next.getAndDecrement());
        obj.getScore("§6> §f" + tiempoTotal).setScore(next.getAndDecrement());
        obj.getScore("§7 ").setScore(next.getAndDecrement());
        
        if (gm.getCapitulo() < 10) {
            obj.getScore(lang.get("scoreboard.time-next-label", player)).setScore(next.getAndDecrement());
            obj.getScore("§6> §f" + tiempo).setScore(next.getAndDecrement());
        }
    }
}
