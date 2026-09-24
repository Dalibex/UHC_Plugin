package me.dalibex.UHC_DBasic.gamemodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.AdminPanelManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;

/** Base class for shared UHC mode behavior: chapters, special items, victory effects, and identity rotation. */
public abstract class AbstractUHCGameMode implements UHCGameMode {

    protected final UHC_DBasic plugin;
    protected final GameManager gm;
    protected boolean shulkerOneDelivered = false;
    protected boolean shulkerTwoDelivered = false;
    protected boolean teamsFormed = false;

    /** Sidebar keys per player, used to clear stale lines without re-registering the objective each tick. */
    private final Map<UUID, Set<String>> sidebarKeys = new HashMap<>();

    public AbstractUHCGameMode(UHC_DBasic plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
    }

    /** Runs shared per-second logic: configured shulkers and chapter changes. */
    @Override
    public void onTick(int chapterSeconds, int totalSeconds) {
        LanguageManager lang = plugin.getLang();
        int secondsPerChapter = gm.getSecondsPerChapter();
        int currentChapter = gm.getChapter();

        deliverConfiguredShulkers(currentChapter, chapterSeconds > 1);

        int calculatedChapter = (chapterSeconds / secondsPerChapter) + 1;
        if (calculatedChapter > currentChapter) {
            gm.setChapter(calculatedChapter);
            onChapterChange(calculatedChapter);
            deliverConfiguredShulkers(calculatedChapter, true);
        }

        if (chapterSeconds == 1) {
            handleInitialSecond(lang);
        }
    }

    /** Hook executed when the game chapter changes. */
    protected abstract void onChapterChange(int newChapter);

    private void deliverConfiguredShulkers(int chapter, boolean allowDelivery) {
        AdminPanelManager admin = plugin.getAdminPanel();
        if (allowDelivery && admin.isShulkerOneEnabled() && !shulkerOneDelivered
                && chapter == admin.getShulkerOneEpisode()) {
            giveGlobalItem("items.shulker.name", Material.ORANGE_SHULKER_BOX);
            shulkerOneDelivered = true;
        }
        if (allowDelivery && admin.isShulkerTwoEnabled() && !shulkerTwoDelivered
                && chapter == admin.getShulkerTwoEpisode()) {
            giveGlobalItem("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
            shulkerTwoDelivered = true;
        }
    }

    /** Handles first-second match initialization. */
    protected void handleInitialSecond(LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (tm.getTeamSize() == 1) {
            tm.shuffleTeams();
        } else {
            // Chapter 1 has no chapter-change event, so configured episode 1
            // team formation must be triggered from the initial-second path.
            maybeFormTeams(1, null);
        }
    }

    /** Forms teams at the configured chapter and delivers tracking compasses. */
    protected void maybeFormTeams(int newChapter, Runnable onFormed) {
        TeamManager tm = plugin.getTeamManager();
        if (teamsFormed || tm.getTeamSize() <= 1) return;
        if (newChapter < tm.getTeamsFormedEpisode()) return;

        if (!tm.isCustomTeamsEnabled()) {
            tm.shuffleTeams();
        }
        teamsFormed = true;
        if (onFormed != null) onFormed.run();

        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(lang.get("game-events.teams-formed", p));
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
        }
        giveTrackingCompasses(lang);
    }

    /** Gives an item to every alive player. */
    protected void giveGlobalItem(String nameKey, Material material) {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getEliminatedPlayers().contains(p.getName())) continue;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(lang.getComponent(nameKey, p));
                item.setItemMeta(meta);
            }
            giveOrDrop(p, item, "general.inv-full");
        }
    }

    /** Gives allied tracking compasses. */
    protected void giveTrackingCompasses(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getEliminatedPlayers().contains(p.getName())) continue;
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.displayName(lang.getComponent("tracking-compass.name", p));
                meta.lore(lang.getComponentList("tracking-compass.lore", p));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "tracking_compass"), PersistentDataType.BYTE, (byte) 1);
                compass.setItemMeta(meta);
            }
            giveOrDrop(p, compass, "tracking-compass.inv-full");
        }
    }

    private void giveOrDrop(Player player, ItemStack item, String fullInventoryMessageKey) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (leftovers.isEmpty()) return;
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.sendMessage(plugin.getLang().get(fullInventoryMessageKey, player));
    }

    /** Starts gradual skin rotation through SkinsManager. */
    protected void runSkinRotation() {
        plugin.getSkinsManager().rotateSkins();
    }

    /** Launches a celebration firework at a location. */
    protected void launchFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.YELLOW)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        fw.setFireworkMeta(fwm);
    }

    /** Applies visual and sound effects to winners. */
    protected void applyVictoryEffects(List<Player> winners) {
        for (Player p : winners) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count++ >= 10 || !p.isOnline()) { this.cancel(); return; }
                    launchFirework(p.getLocation());
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    /** Gets or creates the per-player sidebar objective without per-tick re-registration. */
    protected Objective getOrCreateSidebar(Scoreboard board, Player player, LanguageManager lang) {
        Objective obj = board.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
        if (obj == null) {
            obj = board.registerNewObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE, Criteria.DUMMY, lang.getComponent("scoreboard.title", player));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.numberFormat(NumberFormat.blank());
        }
        return obj;
    }

    /** Clears only previously rendered lines that are no longer present. */
    protected void reconcileSidebarKeys(Objective obj, Player player, List<String> currentKeys) {
        Set<String> stale = sidebarKeys.get(player.getUniqueId());
        if (stale == null) {
            storeSidebarKeys(player, currentKeys);
            return;
        }
        stale = new HashSet<>(stale);
        stale.removeAll(currentKeys);
        for (String key : stale) {
            Score score = obj.getScore(key);
            if (score.isScoreSet()) score.resetScore();
        }
        storeSidebarKeys(player, currentKeys);
    }

    /** Stores rendered keys for next-tick cleanup. */
    protected void storeSidebarKeys(Player player, List<String> keys) {
        sidebarKeys.put(player.getUniqueId(), new HashSet<>(keys));
    }

    /** Moves the game session to ENDING and cancels main tasks. */
    protected void finishGameSession() {
        gm.enterEnding();
    }

    /** Transfers sidebar key cache to a new game mode instance. */
    public Map<UUID, Set<String>> takeSidebarKeys() {
        Map<UUID, Set<String>> transfer = new HashMap<>(sidebarKeys);
        sidebarKeys.clear();
        return transfer;
    }

    /** Adopts sidebar key cache from a previous game mode instance. */
    public void adoptSidebarKeys(Map<UUID, Set<String>> keys) {
        sidebarKeys.putAll(keys);
    }

    @Override
    public void onReset() {
        this.shulkerOneDelivered = false;
        this.shulkerTwoDelivered = false;
        this.teamsFormed = false;
        plugin.getItemsListener().clearCompassTargetCache();

        // Clear stale lines from online player scoreboards.
        for (Player p : Bukkit.getOnlinePlayers()) {
            Set<String> stale = sidebarKeys.remove(p.getUniqueId());
            if (stale == null) continue;
            Scoreboard board = p.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            Objective obj = board.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
            if (obj == null) continue;
            for (String key : stale) {
                Score score = obj.getScore(key);
                if (score.isScoreSet()) score.resetScore();
            }
        }
        sidebarKeys.clear();
    }
}
