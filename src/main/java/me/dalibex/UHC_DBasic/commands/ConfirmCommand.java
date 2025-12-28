package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.GameRules.*;

public class ConfirmCommand implements CommandExecutor {

    private final UHC_DBasic plugin;

    public ConfirmCommand(UHC_DBasic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) return true;

        int size = Integer.parseInt(args[0]);
        World world = player.getWorld();
        LanguageManager lang = plugin.getLang();

        // Configurar el borde
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);

        // CALCULAR POSICIONES Y TP
        getPlayerPos(world, size);

        // Tarea de cuenta atrás
        new BukkitRunnable() {
            int segundos = 10;

            @Override
            public void run() {
                if (segundos > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // Título localizado para cada jugador individualmente
                        String title = lang.get("game.countdown-title", p).replace("%time%", String.valueOf(segundos));
                        String subtitle = lang.get("game.countdown-subtitle", p);

                        p.sendTitle(title, subtitle, 0, 22, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    segundos--;
                } else {
                    // AL LLEGAR A CERO EL CONTADOR
                    world.setGameRule(ADVANCE_TIME, true);
                    world.setGameRule(ADVANCE_WEATHER, true);
                    world.setGameRule(NATURAL_HEALTH_REGENERATION, false);
                    world.setGameRule(SPAWN_MONSTERS, true);
                    world.setDifficulty(Difficulty.HARD);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.setGameMode(GameMode.SURVIVAL);

                        // Mensaje de inicio localizado para cada jugador
                        String startTitle = lang.get("game.started-title", p);
                        String startSubtitle = lang.get("game.started-subtitle", p);

                        p.sendTitle(startTitle, startSubtitle, 10, 40, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }

                    // Inicia la lógica de Scoreboard y cronómetro
                    plugin.getRightPanelManager().iniciarPartida();

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        return true;
    }

    private void getPlayerPos(World world, int size) {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        int cantidad = jugadores.size();
        double radio = size / 2.0;

        int numPosiciones = Math.max(4, cantidad);
        List<Integer> indicesAleatorios = new ArrayList<>();
        for (int i = 0; i < numPosiciones; i++) {
            indicesAleatorios.add(i);
        }
        java.util.Collections.shuffle(indicesAleatorios);

        for (int j = 0; j < cantidad; j++) {
            Player p = jugadores.get(j);
            int i = indicesAleatorios.get(j);

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

            if (xFinal > 0) xFinal -= 0.5; else xFinal += 0.5;
            if (zFinal > 0) zFinal -= 0.5; else zFinal += 0.5;
            int blockX = (int) Math.floor(xFinal);
            int blockZ = (int) Math.floor(zFinal);
            double spawnX = blockX + 0.5;
            double spawnZ = blockZ + 0.5;

            int blockY = world.getHighestBlockYAt(blockX, blockZ);
            org.bukkit.block.Block bloqueSuelo = world.getBlockAt(blockX, blockY, blockZ);

            if (bloqueSuelo.isLiquid() || bloqueSuelo.getType().toString().contains("AIR")) {
                if (bloqueSuelo.isLiquid()) {
                    blockY += 1;
                    world.getBlockAt(blockX, blockY, blockZ).setType(Material.GLASS);
                }
            }

            Location loc = new Location(world, spawnX, blockY + 1.5, spawnZ);
            p.teleport(loc);

            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 180, 1, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 320, 255, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 220, 255, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 220, 255, false, false));
        }
    }
}