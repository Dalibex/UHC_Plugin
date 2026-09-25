package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import me.dalibex.UHC_DBasic.UHC_DBasic;
/** Validates required and optional plugin dependencies. */
public class DependencyManager {

    private final List<String> mandatoryPlugins = new ArrayList<>();
    private final List<String> optionalPlugins = new ArrayList<>();

    public DependencyManager(UHC_DBasic plugin) {
        setupDependencies();
    }

    private void setupDependencies() {
        mandatoryPlugins.add("TAB");
        mandatoryPlugins.add("SkinsRestorer");

        // optionalPlugins.add("PlaceholderAPI");
    }

    /** Returns true when all mandatory dependencies are installed and enabled. */
    public boolean checkDependencies() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        boolean allMandatoryPresent = true;

        console.sendMessage("[UHC] Checking dependencies...");

        for (String pluginName : mandatoryPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage("  [OK] " + pluginName + " detected and active.");
            } else {
                console.sendMessage("  [FAIL] " + pluginName + " not detected or disabled.");
                allMandatoryPresent = false;
            }
        }

        for (String pluginName : optionalPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage("  [INFO] " + pluginName + " detected (optional).");
            }
        }

        if (!allMandatoryPresent) {
            console.sendMessage("--------------------------------------------------");
            console.sendMessage("[UHC ERROR] Missing mandatory dependencies");
            console.sendMessage("Please download and install the missing plugins:");
            for (String pluginName : mandatoryPlugins) {
                if (Bukkit.getPluginManager().getPlugin(pluginName) == null) {
                    console.sendMessage("  - " + pluginName);
                }
            }
            console.sendMessage("The plugin will be disabled to avoid errors.");
            console.sendMessage("--------------------------------------------------");
        }

        return allMandatoryPresent;
    }

    public boolean isPluginPresent(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null && 
               Bukkit.getPluginManager().isPluginEnabled(name);
    }
}
