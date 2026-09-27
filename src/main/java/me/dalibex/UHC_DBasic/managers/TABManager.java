package me.dalibex.UHC_DBasic.managers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.utils.GamePhase;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;

/** Integrates TAB setup, identity relational placeholders, and tablist display names. */
public class TABManager {

    private static final int PLACEHOLDER_REFRESH_MS = 500;
    private static final long TAB_RELOAD_DELAY_TICKS = 40L;
    private static final int ANIMATION_INTERVAL_TICKS = 100;
    private static final long IDENTITY_SNAPSHOT_PERIOD_TICKS = 20L;

    private final UHC_DBasic plugin;
    private final Map<UUID, Boolean> appliedMatchIdentity = new HashMap<>();
    private volatile Map<UUID, Map<UUID, IdentityPair>> identitySnapshot = Map.of();
    private volatile boolean snapshotMatchActive = false;

    private record IdentityPair(String tabText, String nametagColor) { }

    public TABManager(UHC_DBasic plugin) {
        this.plugin = plugin;
        applyTABSetupPolicy();
        startIdentitySnapshotTask();
    }

    private void applyTABSetupPolicy() {
        String mode = plugin.getConfig().getString("tab.setup-mode", "once").toLowerCase(Locale.ROOT);
        if (mode.equals("off")) return;
        if (!mode.equals("once") && !mode.equals("force")) {
            plugin.getLogger().warning("Unknown tab.setup-mode '" + mode + "'; using 'once'.");
            mode = "once";
        }
        if (mode.equals("once") && plugin.getConfig().getBoolean("tab.setup-completed", false)) return;

        if (createTABAnimations() && setupTABAutomatically()) {
            plugin.getConfig().set("tab.setup-completed", true);
            plugin.saveConfig();
        }
    }

    public void registerPlaceholders() {
        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_uhc_identidad%", PLACEHOLDER_REFRESH_MS, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            return resolveCachedIdentityPlaceholder(viewer.getUniqueId(), target.getUniqueId(), target.getName(), false);
        });

        TabAPI.getInstance().getPlaceholderManager().registerRelationalPlaceholder("%rel_nametag_color%", PLACEHOLDER_REFRESH_MS, (viewer, target) -> {
            if (viewer == null || target == null) return "";
            return resolveCachedIdentityPlaceholder(viewer.getUniqueId(), target.getUniqueId(), target.getName(), true);
        });
    }

    private String resolveCachedIdentityPlaceholder(UUID viewerId, UUID targetId, String targetName, boolean colorOnly) {
        IdentityPair pair = identitySnapshot.getOrDefault(viewerId, Map.of()).get(targetId);
        if (pair == null) return colorOnly ? "§f" : "§f" + targetName;
        return colorOnly ? pair.nametagColor() : pair.tabText();
    }

    static String resolveIdentityText(String viewerName, String targetName, boolean colorOnly,
            boolean matchActive, boolean sameTeam, boolean revealed, String assignedSkinName) {
        String color;
        String displayName = targetName;
        if (!matchActive) {
            color = "§f";
        } else if (viewerName.equalsIgnoreCase(targetName) || sameTeam) {
            color = "§a";
        } else if (revealed) {
            color = "§c";
        } else {
            color = colorOnly ? "§c" : "§d";
            displayName = assignedSkinName;
        }
        return colorOnly ? color : color + displayName;
    }

    public void updateTabIdentity(Player p) {
        if (p == null) return;

        TabAPI tabApi = TabAPI.getInstance();
        me.neznamy.tab.api.TabPlayer tabPlayer = tabApi.getPlayer(p.getUniqueId());
        if (tabPlayer == null) return;

        GamePhase phase = plugin.getGameManager().getPhase();
        boolean matchIdentity = phase == GamePhase.RUNNING || phase == GamePhase.PAUSED;
        Boolean previous = appliedMatchIdentity.get(p.getUniqueId());
        if (previous != null && previous == matchIdentity) return;

        TabListFormatManager tfm = tabApi.getTabListFormatManager();
        NameTagManager ntm = tabApi.getNameTagManager();
        if (matchIdentity) {
            if (tfm != null) {
                tfm.setName(tabPlayer, "%rel_uhc_identidad%");
            }
            if (ntm != null) {
                ntm.setPrefix(tabPlayer, "%rel_nametag_color%");
            }
        } else {
            if (tfm != null) tfm.setName(tabPlayer, null);
            if (ntm != null) ntm.setPrefix(tabPlayer, null);
        }
        appliedMatchIdentity.put(p.getUniqueId(), matchIdentity);
    }

    public void forceUpdateTabIdentity(Player p) {
        if (p == null) return;
        appliedMatchIdentity.remove(p.getUniqueId());
        updateTabIdentity(p);
    }

    public void refreshAllIdentitiesStaggered(long initialDelayTicks) {
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                if (index >= players.size()) {
                    cancel();
                    return;
                }
                Player player = players.get(index++);
                if (player.isOnline()) forceUpdateTabIdentity(player);
            }
        }.runTaskTimer(plugin, initialDelayTicks, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, this::rebuildIdentitySnapshot, initialDelayTicks + players.size() + 1L);
    }

    public void rebuildIdentitySnapshot() {
        boolean matchActive = plugin.getGameManager().isMatchActive();
        snapshotMatchActive = matchActive;
        if (!matchActive) {
            identitySnapshot = Map.of();
            return;
        }

        Map<UUID, Map<UUID, IdentityPair>> next = new HashMap<>();
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        for (Player viewer : players) {
            Map<UUID, IdentityPair> byTarget = new HashMap<>();
            for (Player target : players) {
                String tabText = resolveIdentityText(viewer.getName(), target.getName(), false, true,
                        plugin.getTeamManager().areInSameTeam(viewer, target),
                        plugin.getSkinsManager().isIdentityRevealed(target.getName()),
                        plugin.getSkinsManager().getAssignedSkin(target.getName()));
                String nametagColor = resolveIdentityText(viewer.getName(), target.getName(), true, true,
                        plugin.getTeamManager().areInSameTeam(viewer, target),
                        plugin.getSkinsManager().isIdentityRevealed(target.getName()),
                        plugin.getSkinsManager().getAssignedSkin(target.getName()));
                byTarget.put(target.getUniqueId(), new IdentityPair(tabText, nametagColor));
            }
            next.put(viewer.getUniqueId(), Map.copyOf(byTarget));
        }
        identitySnapshot = Map.copyOf(next);
    }

    private void startIdentitySnapshotTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean matchActive = plugin.getGameManager().isMatchActive();
                if (matchActive || snapshotMatchActive) rebuildIdentitySnapshot();
            }
        }.runTaskTimer(plugin, IDENTITY_SNAPSHOT_PERIOD_TICKS, IDENTITY_SNAPSHOT_PERIOD_TICKS);
    }

    private boolean setupTABAutomatically() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null) return false;

        File configFile = new File(tabPlugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        config.set("scoreboard-teams.sorting-types", Arrays.asList("PLACEHOLDER_A_TO_Z:%player%", "GROUPS:owner,admin,mod,helper,builder,vip,default,none"));
        config.set("playerlist-objective.enabled", false);
        config.set("playerlist-objective.value", 0);

        config.set("header-footer.enabled", true);
        List<String> header = Arrays.asList(
                "<#FFFFFF>&m                                       </#FFFF00>",
                "",
                "%animation:UHC-Brillo%",
                ""
        );
        List<String> footer = Arrays.asList(
                "",
                "&fPing: &6%ping%ms",
                "%animation:Firma-Brillo%",
                "",
                "<#FFFFFF>&m                                       </#FFFF00>"
        );
        config.set("header-footer.designs.default.header", header);
        config.set("header-footer.designs.default.footer", footer);

        try {
            config.save(configFile);
            plugin.getLogger().info("TAB configuration updated. Reload scheduled in 2 seconds.");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            }, TAB_RELOAD_DELAY_TICKS);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "ERROR SAVING TAB CONFIG: " + e.getMessage());
            return false;
        }
    }

    private boolean createTABAnimations() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null) return false;

        File animFile = new File(tabPlugin.getDataFolder(), "animations.yml");
        FileConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);

        List<String> titleFrames = Arrays.asList(
                "&6&lUHC ELOUD", "&e&lUHC ELOUD", "&f&lU&e&lHC ELOUD",
                "&6&lU&f&lH&e&lC ELOUD", "&6&lUH&f&lC&e&l ELOUD", "&6&lUHC &f&lE&e&lLOUD",
                "&6&lUHC E&f&lL&e&lOUD", "&6&lUHC EL&f&lO&e&lUD", "&6&lUHC ELO&f&lU&e&lD",
                "&6&lUHC ELOU&f&lD", "&6&lUHC ELOUD"
        );
        animConfig.set("UHC-Brillo.texts", titleFrames);
        animConfig.set("UHC-Brillo.change-interval", ANIMATION_INTERVAL_TICKS);

        List<String> signatureFrames = Arrays.asList(
                "&6made by Dalibex", "&emade by Dalibex", "&fmade by Dalibex",
                "&6made by Dalibex", "&6made by Dalibex"
        );
        animConfig.set("Firma-Brillo.texts", signatureFrames);
        animConfig.set("Firma-Brillo.change-interval", ANIMATION_INTERVAL_TICKS);

        try {
            animConfig.save(animFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "ERROR SAVING animations.yml: " + e.getMessage());
            return false;
        }
    }
}
