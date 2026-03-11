package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager encargado de validar la presencia y estado de plugins dependientes.
 */
public class DependencyManager {

    private final UHC_DBasic plugin;
    private final List<String> mandatoryPlugins = new ArrayList<>();
    private final List<String> optionalPlugins = new ArrayList<>();

    public DependencyManager(UHC_DBasic plugin) {
        this.plugin = plugin;
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

        console.sendMessage(ChatColor.GOLD + "[UHC] Verificando dependencias...");

        for (String pluginName : mandatoryPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage(ChatColor.GREEN + "  [✔] " + pluginName + " detectado y activo.");
            } else {
                console.sendMessage(ChatColor.RED + "  [✘] " + pluginName + " NO DETECTADO O DESACTIVADO.");
                allMandatoryPresent = false;
            }
        }

        for (String pluginName : optionalPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null && 
                Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                console.sendMessage(ChatColor.AQUA + "  [ℹ] " + pluginName + " detectado (Opcional).");
            }
        }

        if (!allMandatoryPresent) {
            console.sendMessage(ChatColor.RED + "--------------------------------------------------");
            console.sendMessage(ChatColor.RED + " [UHC ERROR] FALTAN DEPENDENCIAS CRÍTICAS");
            console.sendMessage(ChatColor.RED + " El plugin se desactivará para evitar errores.");
            console.sendMessage(ChatColor.RED + "--------------------------------------------------");
        }

        return allMandatoryPresent;
    }

    public boolean isPluginPresent(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null && 
               Bukkit.getPluginManager().isPluginEnabled(name);
    }
}
