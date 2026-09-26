package me.dalibex.UHC_DBasic;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.dalibex.UHC_DBasic.admin.AdminPanelManager;
import me.dalibex.UHC_DBasic.commands.AbandonCommand;
import me.dalibex.UHC_DBasic.commands.AdminPanelCommand;
import me.dalibex.UHC_DBasic.commands.AssignTeamCommand;
import me.dalibex.UHC_DBasic.commands.CancelStartCommand;
import me.dalibex.UHC_DBasic.commands.ConfirmStartCommand;
import me.dalibex.UHC_DBasic.commands.HelpCommand;
import me.dalibex.UHC_DBasic.commands.LangCommand;
import me.dalibex.UHC_DBasic.commands.PrepareWorldCommand;
import me.dalibex.UHC_DBasic.commands.SetTeamEpisodeCommand;
import me.dalibex.UHC_DBasic.commands.SetPvpEpisodeCommand;
import me.dalibex.UHC_DBasic.commands.SetTimeCommand;
import me.dalibex.UHC_DBasic.commands.StartCommand;
import me.dalibex.UHC_DBasic.commands.TeamCommand;
import me.dalibex.UHC_DBasic.listeners.AdminPanelListener;
import me.dalibex.UHC_DBasic.listeners.GameLogicListener;
import me.dalibex.UHC_DBasic.listeners.GameplayRestrictionListener;
import me.dalibex.UHC_DBasic.listeners.ItemsListener;
import me.dalibex.UHC_DBasic.listeners.PlayerConnectionListener;
import me.dalibex.UHC_DBasic.listeners.ResourceRushListener;
import me.dalibex.UHC_DBasic.managers.ChatManager;
import me.dalibex.UHC_DBasic.managers.DependencyManager;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.MatchSettingsManager;
import me.dalibex.UHC_DBasic.managers.SkinsManager;
import me.dalibex.UHC_DBasic.managers.SpecialCraftsManager;
import me.dalibex.UHC_DBasic.managers.TABManager;
import me.dalibex.UHC_DBasic.managers.teams.TeamManager;
import me.dalibex.UHC_DBasic.managers.WorldManager;
import me.dalibex.UHC_DBasic.utils.CommandTabs;
import me.dalibex.UHC_DBasic.utils.UpdateChecker;

public final class UHC_DBasic extends JavaPlugin {

    private static final long INITIAL_RESET_DELAY_TICKS = 60L;

    private GameManager gameManager;
    private TeamManager teamManager;
    private SkinsManager skinsManager;
    private WorldManager worldManager;
    private TABManager tabManager;
    private MatchSettingsManager matchSettingsManager;
    private AdminPanelManager adminPanelManager;
    private ChatManager chatManager;
    private PlayerConnectionListener connectionListener;
    private GameLogicListener gameLogicListener;
    private AdminPanelListener adminPanelListener;
    private GameplayRestrictionListener gameplayRestrictionListener;
    private ItemsListener itemsListener;
    private SpecialCraftsManager specialCraftsManager;
    private LanguageManager languageManager;
    private ResourceRushListener resourceRushListener;
    private DependencyManager dependencyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dependencyManager = new DependencyManager(this);
        if (!dependencyManager.checkDependencies()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        logBanner();
        new UpdateChecker(this).checkForUpdates();

        teamManager = new TeamManager(this);
        gameManager = new GameManager(this);
        skinsManager = new SkinsManager(this);
        worldManager = new WorldManager(this);
        tabManager = new TABManager(this);
        matchSettingsManager = new MatchSettingsManager();

        languageManager = new LanguageManager(this);

        adminPanelManager = new AdminPanelManager(this);
        chatManager = new ChatManager(this);

        connectionListener = new PlayerConnectionListener(this);
        gameLogicListener = new GameLogicListener(this);
        adminPanelListener = new AdminPanelListener(this);
        gameplayRestrictionListener = new GameplayRestrictionListener(this);
        itemsListener = new ItemsListener(this);
        specialCraftsManager = new SpecialCraftsManager(this);
        resourceRushListener = new ResourceRushListener(this);

        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(gameLogicListener, this);
        getServer().getPluginManager().registerEvents(adminPanelListener, this);
        getServer().getPluginManager().registerEvents(gameplayRestrictionListener, this);
        getServer().getPluginManager().registerEvents(itemsListener, this);
        getServer().getPluginManager().registerEvents(resourceRushListener, this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        registerCommands();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            tabManager.registerPlaceholders();
            gameManager.fullReset();
        }, INITIAL_RESET_DELAY_TICKS);

        getLogger().info("Startup completed successfully.");
    }

    private void registerCommands() {
        getCommand("uhcadmin").setExecutor(new AdminPanelCommand(this));
        getCommand("uhcadmin").setTabCompleter(CommandTabs.NO_SUGGESTIONS);
        getCommand("reset").setExecutor(new PrepareWorldCommand(this));
        getCommand("reset").setTabCompleter(CommandTabs.NO_SUGGESTIONS);
        getCommand("uhccommands").setExecutor(new HelpCommand(this));
        getCommand("uhccommands").setTabCompleter(CommandTabs.NO_SUGGESTIONS);

        TeamCommand teamCmd = new TeamCommand(this);
        getCommand("team").setExecutor(teamCmd);
        getCommand("team").setTabCompleter(teamCmd);

        SetTimeCommand setTimeCmd = new SetTimeCommand(this);
        getCommand("settime").setExecutor(setTimeCmd);
        getCommand("settime").setTabCompleter(setTimeCmd);

        StartCommand startCmd = new StartCommand(this);
        getCommand("start").setExecutor(startCmd);
        getCommand("start").setTabCompleter(startCmd);

        ConfirmStartCommand confirmCmd = new ConfirmStartCommand(this, startCmd);
        getCommand("confirmstart").setExecutor(confirmCmd);
        getCommand("confirmstart").setTabCompleter(confirmCmd);

        CancelStartCommand cancelCmd = new CancelStartCommand(this, startCmd);
        getCommand("cancelstart").setExecutor(cancelCmd);
        getCommand("cancelstart").setTabCompleter(CommandTabs.NO_SUGGESTIONS);

        LangCommand langCmd = new LangCommand(this);
        getCommand("lang").setExecutor(langCmd);
        getCommand("lang").setTabCompleter(langCmd);

        AssignTeamCommand assignCmd = new AssignTeamCommand(this);
        getCommand("assignteam").setExecutor(assignCmd);
        getCommand("assignteam").setTabCompleter(assignCmd);

        AbandonCommand abandonCmd = new AbandonCommand(this);
        getCommand("abandon").setExecutor(abandonCmd);
        getCommand("abandon").setTabCompleter(abandonCmd);

        SetTeamEpisodeCommand setTeamEpisodeCmd = new SetTeamEpisodeCommand(this);
        getCommand("setteamepisode").setExecutor(setTeamEpisodeCmd);
        getCommand("setteamepisode").setTabCompleter(setTeamEpisodeCmd);

        SetPvpEpisodeCommand setPvpEpisodeCmd = new SetPvpEpisodeCommand(this);
        getCommand("setpvpepisode").setExecutor(setPvpEpisodeCmd);
        getCommand("setpvpepisode").setTabCompleter(setPvpEpisodeCmd);

    }

    private void logBanner() {
        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        String tabSetupMode = getConfig().getString("tab.setup-mode", "once");
        console.sendMessage(" ");
        console.sendMessage("§6§l==================================================");
        console.sendMessage("§6§l   _  _ _  _ ____    ____ _    ____ _  _ ___  ");
        console.sendMessage("§6§l   |  | |__| |       |___ |    |  | |  | |  \\ ");
        console.sendMessage("§6§l   |__| |  | |___    |___ |___ |__| |__| |__/ ");
        console.sendMessage("§6 ");
        console.sendMessage("§f   UHC ELOUD");
        console.sendMessage("§f   Developed by: Dalibex");
        console.sendMessage("§f   Version: " + getPluginMeta().getVersion());
        console.sendMessage("§f   Dependencies: OK");
        console.sendMessage("§f   TAB setup: " + tabSetupMode);
        console.sendMessage("§f   Status: ACTIVE AND LOADED");
        console.sendMessage("§6§l==================================================");
        console.sendMessage(" ");
    }

    // --- Getters ---
    public GameManager getGameManager() { return gameManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public SkinsManager getSkinsManager() { return skinsManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public TABManager getTABManager() { return tabManager; }
    public MatchSettingsManager getMatchSettings() { return matchSettingsManager; }
    public AdminPanelManager getAdminPanel() { return adminPanelManager; }
    public ChatManager getChatManager() { return chatManager; }
    public SpecialCraftsManager getSpecialCraftsManager() { return specialCraftsManager; }
    public LanguageManager getLang() { return languageManager; }
    public PlayerConnectionListener getConnectionListener() { return connectionListener; }
    public GameLogicListener getGameLogicListener() { return gameLogicListener; }
    public AdminPanelListener getAdminPanelListener() { return adminPanelListener; }
    public ItemsListener getItemsListener() { return itemsListener; }
    public DependencyManager getDependencyManager() { return dependencyManager; }

    /** Returns true when a sender has plugin admin permissions. */
    public boolean isAdmin(CommandSender sender) {
        if (sender == null) return false;
        if (sender instanceof Player p && p.isOp()) return true;
        return sender.hasPermission("uhc.admin");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.cancelStartup();
        getLogger().info("UHC_DBasic Plugin Disabled");
    }
}
