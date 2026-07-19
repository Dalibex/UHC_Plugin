package me.dalibex.UHC_DBasic.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Manager encargado de validar la presencia y estado de plugins dependientes.
 */
public class DependencyManager {

    private final List<String> mandatoryPlugins = new ArrayList<>();
    private final List<String> optionalPlugins = new ArrayList<>();

    public DependencyManager(UHC_DBasic plugin) {
        setupDependencies();
    }

    private void setupDependencies() {
        // Plugins obligatorios para el funcionamiento básico
        mandatoryPlugins.add("TAB");
        mandatoryPlugins.add("SkinsRestorer");

        // Plugins opcionales para funcionalidades extra
        // optionalPlugins.add("PlaceholderAPI");
    }

    /**
     * Verifica que todos los plugins obligatorios estén instalados y activos.
     * @return true si todo está correcto, false si falta alguna dependencia crítica.
     */
    public boolean checkDependencies() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        boolean allMandatoryPresent = true;

        console.sendMessage(legacySection().deserialize("&6[UHC] Checking dependencies..."));

        for (String pluginName : mandatoryPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage(legacySection().deserialize("&a  [✔] " + pluginName + " detected and active."));
            } else {
                console.sendMessage(legacySection().deserialize("&c  [✘] " + pluginName + " NOT DETECTED OR DISABLED."));
                allMandatoryPresent = false;
            }
        }

        for (String pluginName : optionalPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage(legacySection().deserialize("&b  [ℹ] " + pluginName + " detected (Optional)."));
            }
        }

        if (!allMandatoryPresent) {
            console.sendMessage(legacySection().deserialize("&c--------------------------------------------------"));
            console.sendMessage(legacySection().deserialize("&c [UHC ERROR] MISSING MANDATORY DEPENDENCIES"));
            console.sendMessage(legacySection().deserialize("&c Please download and install the missing plugins:"));
            for (String pluginName : mandatoryPlugins) {
                if (Bukkit.getPluginManager().getPlugin(pluginName) == null) {
                    console.sendMessage(legacySection().deserialize("&e  - " + pluginName));
                }
            }
            console.sendMessage(legacySection().deserialize("&c The plugin will be disabled to avoid errors."));
            console.sendMessage(legacySection().deserialize("&c--------------------------------------------------"));
        }

        return allMandatoryPresent;
    }

    public boolean isPluginPresent(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null && 
               Bukkit.getPluginManager().isPluginEnabled(name);
    }
}
