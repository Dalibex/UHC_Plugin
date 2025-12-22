package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.GameRules.*;

public class PrepareWorldCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public PrepareWorldCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cNo tienes permisos para ejecutar este comando.");
            return true;
        }

        World world = player.getWorld();

        plugin.getRightPanelManager().setStandBy();

        // 1. Ciclo de dia noche desactivado
        world.setTime(0L);
        world.setGameRule(ADVANCE_TIME, false);

        // 2. Pparar el ciclo de clima
        world.setGameRule(ADVANCE_WEATHER, false);

        // 3. Habilitar Regeneración Natural (Para el lobby/espera)
        world.setGameRule(NATURAL_HEALTH_REGENERATION, true);

        // 4. No spawnear mobs hostiles hasta que empiece la partida
        world.setGameRule(SPAWN_MONSTERS, false);

        // 5. Resetear el borde (que no moleste)
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(5999984);

        player.sendMessage("§e§lUHC ELOUD > §fMundo preparado correctamente");

        return true;
    }
}
