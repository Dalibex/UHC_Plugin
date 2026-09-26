package me.dalibex.UHC_DBasic.gamemodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.gamemodes.resourcerush.ResourceRushObjectiveTracker;
import me.dalibex.UHC_DBasic.gamemodes.scoreboard.ResourceRushScoreboardRenderer;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import me.dalibex.UHC_DBasic.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import net.kyori.adventure.title.Title;

public class ResourceRush extends AbstractUHCGameMode {

    private final ResourceRushObjectiveTracker objectiveTracker = new ResourceRushObjectiveTracker();
    private final ResourceRushScoreboardRenderer scoreboardRenderer = new ResourceRushScoreboardRenderer();
    private final List<BukkitTask> delayedTasks = new ArrayList<>();
    private int sessionGeneration = 0;
    private boolean terminando = false;

    public ResourceRush(UHC_DBasic plugin, GameManager gm) {
        super(plugin, gm);
    }

    @Override
    public String getName() {
        return "Resource Rush";
    }

    @Override
    public void onTick(int chapterSeconds, int totalSeconds) {
        super.onTick(chapterSeconds, totalSeconds);

        // Delay initial objectives until the match state is fully initialized.
        if (chapterSeconds == 1) {
            scheduleForCurrentSession(() -> {
                if (gm.getTotalSeconds() > 0) updateActiveObjectives(1);
            }, 100L); 
        }
    }

    @Override
    protected void onChapterChange(int nuevoCap) {
        LanguageManager lang = plugin.getLang();

        scheduleForCurrentSession(() -> updateActiveObjectives(nuevoCap), 100L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(lang.get("game-events.chapter-start", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%chapter%", String.valueOf(nuevoCap)));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        if (me.dalibex.UHC_DBasic.managers.SkinsManager.isRotationEpisode(nuevoCap)) {
            runSkinRotation();
        }

        maybeFormTeams(nuevoCap, this::syncResourceRushTeams);

        maybeEnablePvp(nuevoCap);
    }

    @Override
    protected void handleInitialSecond(LanguageManager lang) {
        super.handleInitialSecond(lang);
        syncResourceRushTeams();
    }

    @Override
    protected Sound getPvpEnabledSound() {
        return Sound.ENTITY_WITHER_SPAWN;
    }

    private void scheduleForCurrentSession(Runnable action, long delay) {
        int generation = sessionGeneration;
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            delayedTasks.remove(taskRef[0]);
            if (generation == sessionGeneration && !terminando && gm.isMatchActive()) action.run();
        }, delay);
        delayedTasks.add(taskRef[0]);
    }

    private void updateActiveObjectives(int capitulo) {
        if (gm.getTotalSeconds() <= 0) return;

        for (Material material : objectiveTracker.addObjectivesForChapter(capitulo)) announceObjective(material);
    }

    private void announceObjective(Material mat) {
        LanguageManager lang = plugin.getLang();
        String translationKey = (mat.isBlock() ? "block.minecraft." : "item.minecraft.") + mat.name().toLowerCase();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String prefix = lang.get("resource-rush.ruleta-anuncio", p);
            Component itemComp = Component.text()
                    .append(Component.translatable(translationKey))
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showItem(mat.getKey(), 1))
                    .build();

            Component msg = legacySection().deserialize(prefix).decoration(TextDecoration.ITALIC, false)
                    .append(legacySection().deserialize("§e["))
                    .append(itemComp)
                    .append(legacySection().deserialize("§e]"));

            p.sendMessage(msg);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        }
    }

    @Override
    public void updateScoreboard(Player player, String chapterTime, String totalTime, boolean matchActive) {
        LanguageManager lang = plugin.getLang();
        Scoreboard board = player.getScoreboard();

        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = getOrCreateSidebar(board, player, lang);
        List<String> keys = scoreboardRenderer.render(plugin, gm, objectiveTracker, obj, player, getName(), chapterTime, totalTime, matchActive);
        reconcileSidebarKeys(obj, player, keys);
    }

    @Override
    public void checkVictory() {
        if (!gm.isMatchActive() || gm.getTotalSeconds() <= 0 || terminando) return;

        Set<String> eliminados = gm.getEliminatedPlayers();

        List<Player> vivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL && !eliminados.contains(p.getName()))
                .collect(Collectors.toList());

        Set<String> entidadesVivas = vivos.stream()
                .map(p -> {
                    Team t = plugin.getTeamManager().getPlayerTeam(p.getName());
                    return (t != null) ? t.getName() : p.getName();
                }).collect(Collectors.toSet());

        // Disconnected live participants keep the match open until they return or are marked eliminated.
        for (String nombre : gm.getInitialParticipants()) {
            if (eliminados.contains(nombre)) continue;
            if (Bukkit.getPlayer(nombre) != null) continue;
            Team t = plugin.getTeamManager().getPlayerTeam(nombre);
            String clave = (t != null) ? t.getName() : nombre;
            if (!objectiveTracker.podiumContains(clave)) entidadesVivas.add(clave);
        }

        long unfinishedTeams = entidadesVivas.stream()
                .filter(clave -> !objectiveTracker.podiumContains(clave))
                .count();

        if (entidadesVivas.isEmpty() || unfinishedTeams == 0) finishWithPodium();
    }

    private void finishWithPodium() {
        if (terminando) return;
        terminando = true;

        LanguageManager lang = plugin.getLang();
        finishGameSession();

        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getSkinsManager().revealIdentity(online);
            plugin.getSkinsManager().updateVisualIdentity(online);
        }

        if (objectiveTracker.podiumEmpty()) {
            Bukkit.broadcast(legacySection().deserialize(lang.get("victory.no-survivors", null)));
        } else {
            Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-header", null)));
            List<String> podium = objectiveTracker.podium();
            for (int i = 0; i < Math.min(3, podium.size()); i++) {
                String medalKey = "resource-rush.medals." + (i + 1);
                String medal = lang.get(medalKey, null);
                if (medal == null || medal.isBlank() || medal.equals(medalKey)) medal = "§7#" + (i + 1);
                String clave = podium.get(i);
                Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(clave);
                String nombre = (t != null) ? legacySection().serialize(t.displayName()) : clave;
                Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-line", null)
                        .replace("%medal%", medal).replace("%team%", nombre)));
            }
            Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-footer", null)));

            String winnerKey = objectiveTracker.firstPodiumKey();
            Team ganador = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(winnerKey);
            applyFinalEffects(winnerKey, ganador);
        }
    }

    public void completeObjective(Player p, Material mat) {
        if (!objectiveTracker.isActiveObjective(mat)) return;

        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        String clave = (team != null) ? team.getName() : p.getName();
        ResourceRushObjectiveTracker.ObjectiveCompletion completion = objectiveTracker.completeObjective(clave, mat);
        if (!completion.completed()) return;

        announceAchievement(p, team, mat, completion.done());
        if (completion.finished()) handleTeamFinish(p, team);
    }

    private void announceAchievement(Player p, Team team, Material mat, int done) {
        LanguageManager lang = plugin.getLang();
        String teamName = (team != null) ? legacySection().serialize(team.displayName()) : "§f";
        String name = (team != null) ? teamName + "§8[§f" + p.getName() + "§8]" : p.getName();
        String color = (team != null) ? TextUtil.legacyColor(team.color()) : "§f";
        String itemName = mat.name().replace("_", " ").toLowerCase();

        String raw = lang.get("resource-rush.objective-global", null);
        String msg = raw
                .replace("%color%", color).replace("%team%", name)
                .replace("%item%", itemName).replace("%done%", String.valueOf(done));

        for (Player all : Bukkit.getOnlinePlayers()) {
            all.sendMessage(legacySection().deserialize(msg).decoration(TextDecoration.ITALIC, false));
            all.playSound(all.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
        }
    }

    private void handleTeamFinish(Player p, Team team) {
        LanguageManager lang = plugin.getLang();
        String color = (team != null) ? TextUtil.legacyColor(team.color()) : "§f";
        String nombre = (team != null) ? legacySection().serialize(team.displayName()) : p.getName();

        Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.team-finished", null)
                .replace("%color%", color).replace("%team%", nombre)));

        String alert = lang.get("resource-rush.finish-alert", p);
        if (team != null) {
            teamMembers(team).forEach(e -> {
                Player m = Bukkit.getPlayer(e);
                if (m != null) m.sendMessage(alert);
            });
        } else {
            p.sendMessage(alert);
        }

        scheduleForCurrentSession(() -> {
            String specMsg = lang.get("resource-rush.spectator-message", null);
            if (team != null) {
                teamMembers(team).forEach(e -> {
                    Player m = Bukkit.getPlayer(e);
                    if (m != null && m.isOnline()) {
                        m.setGameMode(GameMode.SPECTATOR);
                        m.sendMessage(legacySection().deserialize(specMsg));
                    }
                });
            } else if (p.isOnline()) {
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage(legacySection().deserialize(specMsg));
            }
        }, 200L);

        checkVictory();
    }

    private void applyFinalEffects(String winnerKey, Team ganador) {
        LanguageManager lang = plugin.getLang();
        List<Player> winners = new ArrayList<>();
        Set<String> entries = (ganador != null) ? teamMembers(ganador) : Collections.singleton(winnerKey);
        for (String entry : entries) {
            Player p = Bukkit.getPlayer(entry);
            if (p != null && !gm.getEliminatedPlayers().contains(entry)) {
                winners.add(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
            }
        }

        String color = (ganador != null) ? TextUtil.legacyColor(ganador.color()) : "§f";
        String teamName = (ganador != null) ? legacySection().serialize(ganador.displayName()) : winnerKey;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(legacySection().deserialize(""));
            p.sendMessage(legacySection().deserialize(lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", teamName)));
            p.showTitle(Title.title(
                        legacySection().deserialize(lang.get("victory.title", p)),
                        legacySection().deserialize(lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", teamName)),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(1000))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        applyVictoryEffects(winners);
    }

    /** Returns plugin team entries using the authoritative team cache. */
    private Set<String> teamMembers(Team team) {
        if (team == null) return Collections.emptySet();
        if (team.getName().startsWith(ScoreboardHelper.TEAM_PREFIX)) {
            return new HashSet<>(plugin.getTeamManager().getMemberNames(team));
        }
        return new HashSet<>(team.getEntries());
    }

    private void syncResourceRushTeams() {
        for (String participant : gm.getInitialParticipants()) {
            Team t = plugin.getTeamManager().getPlayerTeam(participant);
            if (t != null) objectiveTracker.migrateProgress(participant, t.getName());
        }
    }

    @Override
    public void onReset() {
        sessionGeneration++;
        delayedTasks.forEach(BukkitTask::cancel);
        delayedTasks.clear();
        super.onReset();
        this.objectiveTracker.reset();
        this.terminando = false;
        Bukkit.getOnlinePlayers().forEach(p -> p.playerListName(Component.text(p.getName())));
    }

    public List<Material> getPlayerAchievements(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        return objectiveTracker.achievements((team != null) ? team.getName() : p.getName());
    }

    public List<Material> getActiveObjectives() {
        return objectiveTracker.activeObjectives();
    }

    public boolean isActiveObjective(Material material) {
        return objectiveTracker.isActiveObjective(material);
    }
}
