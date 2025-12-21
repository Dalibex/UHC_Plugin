package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.GameRules.*;

public class ConfirmCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    // Este es el "constructor" que recibe el plugin desde el Main
    public ConfirmCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Si no hay argumentos (el tamaño), no hacemos nada
        if (args.length == 0) return true;

        int size = Integer.parseInt(args[0]);
        World world = player.getWorld();

        // Configurar el borde
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);

        // CALCULAR POSICIONES
        getPlayerPos(world, size);

        // Tarea de cuenta atrás
        new BukkitRunnable() {
            int segundos = 10;

            @Override
            public void run() {
                // CONTADOR DE 10 SEGUNDOS
                if (segundos > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // Título en pantalla
                        p.sendTitle("§6" + segundos, "§ePreparándote...", 0, 22, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    segundos--;
                } else {
                    // AL LLEGAR A CERO EL CONTADOR

                    // SETEAR GAMERULES Y AJUSTES PARA DAR COMIENZO A PARTIDA
                    world.setGameRule(ADVANCE_TIME, true);
                    world.setGameRule(ADVANCE_WEATHER, true);
                    world.setGameRule(NATURAL_HEALTH_REGENERATION, false);
                    world.setGameRule(SPAWN_MONSTERS, true);
                    world.setDifficulty(Difficulty.HARD);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§a§l¡UHC HA COMENZADO!", "§7¡Buena suerte!", 10, 40, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }

                    // Llamamos al manager para que empiece el Scoreboard y el tiempo
                    plugin.getRightPanelManager().iniciarPartida();

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // 1 segundo de retraso para que cargue el TP

        return true;
    }

    private void getPlayerPos(World world, int size) {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        int cantidad = jugadores.size();
        double radio = size / 2.0;

        // CREAMOS UNA LISTA DE "ÍNDICES" (Posiciones 0, 1, 2, 3...)
        // o tantas posiciones como jugadores haya si son más de 4
        int numPosiciones = Math.max(4, cantidad);
        List<Integer> indicesAleatorios = new ArrayList<>();
        for (int i = 0; i < numPosiciones; i++) {
            indicesAleatorios.add(i);
        }
        // BARAJAMOS LOS ÍNDICES
        java.util.Collections.shuffle(indicesAleatorios);

        // ASIGNAR JUGADORES AL ÍNDICE Y CALCULAR SOBRE EL RESULTADO
        for (int j = 0; j < cantidad; j++) {
            Player p = jugadores.get(j);

            int i = indicesAleatorios.get(j); // Tomar índice de la lista

            // Añadimos Math.PI / 4 (45 grados) para que el primer jugador vaya a una ESQUINA
            double angulo = (2 * Math.PI * i / numPosiciones) + (Math.PI / 4);

            double xCircular = Math.cos(angulo);
            double zCircular = Math.sin(angulo);

            double xFinal, zFinal;
            if (Math.abs(xCircular) > Math.abs(zCircular)) {
                xFinal = (xCircular > 0) ? radio : -radio;
                zFinal = radio * (zCircular / Math.abs(xCircular));
            } else {
                zFinal = (zCircular > 0) ? radio : -radio;
                xFinal = radio * (xCircular / Math.abs(zCircular));
            }

            // AJUSTE DE PRECISIÓN:
            // Margen para que el centro del bloque sea 499.5
            if (xFinal > 0) xFinal -= 0.5; else xFinal += 0.5;
            if (zFinal > 0) zFinal -= 0.5; else zFinal += 0.5;

            int y = world.getHighestBlockYAt((int) xFinal, (int) zFinal);

            // CHECK POR SI EL JUGADOR SPAWNEA EN AGUA O LAVA PONER BLOQUE
            org.bukkit.block.Block bloqueSuelo = world.getBlockAt((int) xFinal, y, (int) zFinal);
            if (bloqueSuelo.isLiquid()) {
                y += 1;
                world.getBlockAt((int) xFinal, y, (int) zFinal).setType(org.bukkit.Material.GLASS);
            }

            Location loc = new Location(world, Math.floor(xFinal) + 0.5, y + 1, Math.floor(zFinal) + 0.5);
            p.teleport(loc);

            // Aplicar efectos: Ceguera + Resitencia, Saturación, Regeneración 10 + Slowness 100
            // 220 ticks son 11 segundos, la cuenta atrás son 10
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.BLINDNESS, 220, 1, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.RESISTANCE, 220, 10, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.REGENERATION, 220, 10, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.SATURATION, 220, 10, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.SLOWNESS, 220, 20, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.JUMP_BOOST, 220, 99, false, false));
        }
    }
}