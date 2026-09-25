package me.dalibex.UHC_DBasic.gamemodes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
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
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import net.kyori.adventure.title.Title;

public class ResourceRush extends AbstractUHCGameMode {

    private final Map<Integer, List<Material>> poolsPorCapitulo = new HashMap<>();
    private final List<Material> objetivosActivos = new ArrayList<>();
    private final Set<Material> objetivosActivosSet = new LinkedHashSet<>();

    private final Map<String, LinkedHashSet<Material>> progresoGlobal = new HashMap<>();
    private final List<String> podioFinal = new ArrayList<>();
    private final Set<String> podioFinalSet = new LinkedHashSet<>();
    private final List<BukkitTask> delayedTasks = new ArrayList<>();
    private int sessionGeneration = 0;
    private boolean terminando = false;

    public ResourceRush(UHC_DBasic plugin, GameManager gm) {
        super(plugin, gm);
        initializePool();
    }

    private void initializePool() {
        poolsPorCapitulo.clear();
        objetivosActivos.clear();
        objetivosActivosSet.clear();
        progresoGlobal.clear();

        // Item pools by chapter.
        poolsPorCapitulo.put(1, Arrays.asList(Material.DIAMOND_BLOCK, Material.GOLDEN_APPLE, Material.TNT, Material.SPYGLASS, Material.LAVA_BUCKET, Material.SADDLE, Material.ENDER_PEARL, Material.DIAMOND_HOE));
        poolsPorCapitulo.put(2, Arrays.asList(Material.BLAZE_ROD, Material.GHAST_TEAR, Material.BREWING_STAND, Material.JUKEBOX, Material.GLOW_ITEM_FRAME, Material.MUSIC_DISC_TEARS, Material.GOLDEN_CARROT, Material.TARGET));
        poolsPorCapitulo.put(3, Arrays.asList(Material.ANVIL, Material.ENCHANTING_TABLE, Material.PLAYER_HEAD, Material.PAINTING, Material.YELLOW_STAINED_GLASS, Material.MAGMA_CREAM, Material.LEAD, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
        poolsPorCapitulo.put(4, Arrays.asList(Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.DRIED_GHAST, Material.DIAMOND_CHESTPLATE, Material.TURTLE_HELMET, Material.DEEPSLATE_GOLD_ORE, Material.PISTON, Material.FIRE_CHARGE));
        poolsPorCapitulo.put(5, Arrays.asList(Material.NETHERITE_SCRAP, Material.RESPAWN_ANCHOR, Material.CAKE, Material.POISONOUS_POTATO, Material.COMPASS, Material.CROSSBOW, Material.PHANTOM_MEMBRANE, Material.PUMPKIN_PIE));
        poolsPorCapitulo.put(6, Arrays.asList(Material.HONEY_BOTTLE, Material.RAW_GOLD_BLOCK, Material.RABBIT_FOOT, Material.NETHER_WART, Material.MAP, Material.HONEY_BLOCK, Material.CAMPFIRE, Material.DISPENSER));
        poolsPorCapitulo.put(7, Arrays.asList(Material.GILDED_BLACKSTONE, Material.CLOCK, Material.AMETHYST_SHARD, Material.FERMENTED_SPIDER_EYE, Material.RECOVERY_COMPASS, Material.WARPED_FUNGUS_ON_A_STICK, Material.DETECTOR_RAIL, Material.LECTERN));
        poolsPorCapitulo.put(8, Arrays.asList(Material.BEE_NEST, Material.LIGHTNING_ROD, Material.GLOW_BERRIES, Material.BOOKSHELF, Material.NAME_TAG, Material.SOUL_LANTERN, Material.NETHER_WART_BLOCK, Material.DIAMOND_AXE));

        for (List<Material> lista : poolsPorCapitulo.values()) {
            Collections.shuffle(lista);
        }
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

        int objectivesToAdd = (capitulo <= 3) ? 2 : (capitulo <= 9) ? 1 : 0;
        if (objectivesToAdd == 0) return;

        List<Material> poolDelCap = poolsPorCapitulo.getOrDefault(capitulo, poolsPorCapitulo.get(8));
        if (poolDelCap == null) return;

        int added = 0;
        for (Material mat : poolDelCap) {
            if (added >= objectivesToAdd) break;
            if (objetivosActivosSet.add(mat)) {
                objetivosActivos.add(mat);
                added++;
                announceObjective(mat);
            }
        }
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
        List<String> keys = new ArrayList<>();
        if (!matchActive) {
            ScoreboardHelper.addLobbyScores(obj, keys, getName(), player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(35);
            ScoreboardHelper.addPhaseInfo(obj, next, keys, player, lang, gm);
            ScoreboardHelper.addTeamInfo(obj, next, keys, player, lang, plugin.getTeamManager(), gm);

            // Resource Rush achievement section.
            keys.add("§8 ");
            obj.getScore("§8 ").setScore(next.getAndDecrement());
            Team team = plugin.getTeamManager().getPlayerTeam(player.getName());
            String clave = (team != null) ? team.getName() : player.getName();
            Set<Material> achievements = progresoGlobal.get(clave);
            int realizados = achievements == null ? 0 : achievements.size();
            String counter = lang.get("scoreboard-rr.rr-counter", player)
                    .replace("%done%", String.valueOf(realizados))
                    .replace("%total%", String.valueOf(objetivosActivos.size()));
            keys.add(counter);
            obj.getScore(counter).setScore(next.getAndDecrement());

            ScoreboardHelper.addTimers(obj, next, keys, chapterTime, totalTime, player, lang, gm);
        }

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
            if (!podioFinalSet.contains(clave)) entidadesVivas.add(clave);
        }

        long unfinishedTeams = entidadesVivas.stream()
                .filter(clave -> !podioFinalSet.contains(clave))
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

        if (podioFinal.isEmpty()) {
            Bukkit.broadcast(legacySection().deserialize(lang.get("victory.no-survivors", null)));
        } else {
            Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-header", null)));
            for (int i = 0; i < Math.min(3, podioFinal.size()); i++) {
                String medalKey = "resource-rush.medals." + (i + 1);
                String medal = lang.get(medalKey, null);
                if (medal == null || medal.isBlank() || medal.equals(medalKey)) medal = "§7#" + (i + 1);
                String clave = podioFinal.get(i);
                Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(clave);
                String nombre = (t != null) ? legacySection().serialize(t.displayName()) : clave;
                Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-line", null)
                        .replace("%medal%", medal).replace("%team%", nombre)));
            }
            Bukkit.broadcast(legacySection().deserialize(lang.get("resource-rush.podio-footer", null)));

            Team ganador = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(podioFinal.get(0));
            applyFinalEffects(podioFinal.get(0), ganador);
        }
    }

    public void completeObjective(Player p, Material mat) {
        if (!isActiveObjective(mat)) return;

        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        String clave = (team != null) ? team.getName() : p.getName();
        LinkedHashSet<Material> logros = progresoGlobal.computeIfAbsent(clave, k -> new LinkedHashSet<>());

        if (logros.add(mat)) {
            announceAchievement(p, team, mat, logros.size());

            if (logros.size() >= 12 && podioFinalSet.add(clave)) {
                podioFinal.add(clave);
                handleTeamFinish(p, team);
            }
        }
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
            if (t != null && progresoGlobal.containsKey(participant)) {
                LinkedHashSet<Material> ind = progresoGlobal.get(participant);
                LinkedHashSet<Material> eq = progresoGlobal.computeIfAbsent(t.getName(), k -> new LinkedHashSet<>());
                eq.addAll(ind);
                progresoGlobal.remove(participant);
            }
        }
    }

    @Override
    public void onReset() {
        sessionGeneration++;
        delayedTasks.forEach(BukkitTask::cancel);
        delayedTasks.clear();
        super.onReset();
        this.progresoGlobal.clear();
        this.podioFinal.clear();
        this.podioFinalSet.clear();
        this.objetivosActivos.clear();
        this.objetivosActivosSet.clear();
        this.terminando = false;
        Bukkit.getOnlinePlayers().forEach(p -> p.playerListName(Component.text(p.getName())));
        initializePool();
    }

    public List<Material> getPlayerAchievements(Player p) {
        Team team = plugin.getTeamManager().getPlayerTeam(p.getName());
        Set<Material> achievements = progresoGlobal.get((team != null) ? team.getName() : p.getName());
        return achievements == null ? List.of() : List.copyOf(achievements);
    }

    public List<Material> getActiveObjectives() {
        return List.copyOf(objetivosActivos);
    }

    public boolean isActiveObjective(Material material) {
        return objetivosActivosSet.contains(material);
    }
}
