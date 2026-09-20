package me.dalibex.UHC_DBasic.commands;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.CommandTabs;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;
import static org.bukkit.GameRules.*;

public class ConfirmStartCommand implements CommandExecutor, TabCompleter {

    private final UHC_DBasic plugin;
    private final StartCommand startCmd;

    public ConfirmStartCommand(UHC_DBasic plugin, StartCommand startCmd) {
        this.plugin = plugin;
        this.startCmd = startCmd;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String errorPrefix = plugin.getLang().get("general.error-prefix", player);

        if (args.length != 1) {
            player.sendMessage(plugin.getLang().get("start-menu.usage", player).replace("%error-prefix%", errorPrefix));
            return true;
        }

        if (!startCmd.hasPendingConfirmation() || plugin.getGameManager().isGameStarted()) {
            return true;
        }

        // Solo el admin que inició /start puede confirmarlo (o cualquier admin del plugin)
        if (plugin.isAdmin(player)) {
            UUID iniciador = startCmd.getConfirmerUuid();
            if (iniciador != null && !iniciador.equals(player.getUniqueId())) {
                player.sendMessage(plugin.getLang().get("start-menu.not-initiator", player));
                return true;
            }
        } else {
            player.sendMessage(plugin.getLang().get("general.no-permission", player));
            return true;
        }

        // Bloquear inicio si equipos personalizados activos y faltan jugadores por elegir
        TeamManager tm = plugin.getTeamManager();
        if (tm.isCustomTeamsEnabled() && tm.getTeamSize() > 1) {
            if (!tm.allPlayersHaveTeam()) {
                player.sendMessage(plugin.getLang().get("game.start-blocked-custom-teams", player));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                plugin.getGameManager().cancelStartup();
                return true;
            }
            
            int onlinePlayers = (int) Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count();
            int minRequired = (int) Math.ceil((double) onlinePlayers / tm.getTeamSize());
            int usedTeams = tm.getTeamsWithPlayersCount();
            
            if (usedTeams > minRequired) {
                player.sendMessage(plugin.getLang().get("game.start-blocked-team-count", player)
                        .replace("%min%", String.valueOf(minRequired))
                        .replace("%n%", String.valueOf(tm.getTeamSize())));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                plugin.getGameManager().cancelStartup();
                return true;
            }
        }

        if (!plugin.getGameManager().beginPreparation(player.getUniqueId())) return true;

        startUHCProcess(plugin.getGameManager().getPendingBorderSize());

        return true;
    }

    /**
     * Coordina el borde, el scatter (TP) y la cuenta atrás.
     */
    private void startUHCProcess(int size) {
        World world = plugin.getGameManager().getStartupWorld();
        LanguageManager lang = plugin.getLang();
        long generation = plugin.getGameManager().getStartupGeneration();

        // Limpiar ítems de selector de equipo INMEDIATAMENTE al confirmar
        plugin.getTeamManager().removeAllSelectorItems();

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(size);

        List<UUID> jugadores = new ArrayList<>(plugin.getGameManager().getEligibleRoster());
        int totalJugadores = jugadores.size();
        int numPosiciones = Math.max(4, totalJugadores);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < numPosiciones; i++) indices.add(i);
        java.util.Collections.shuffle(indices);

        // --- Tarea de Teletransporte Escalonado ---
        BukkitRunnable scatter = new BukkitRunnable() {
            int current = 0;

            @Override
            public void run() {
                if (!plugin.getGameManager().isStartupGeneration(generation)) {
                    cancel();
                    return;
                }
                if (current < totalJugadores) {
                    UUID playerId = jugadores.get(current);
                    Location location = calculateScatterLocation(world, indices.get(current), numPosiciones, size);
                    plugin.getGameManager().setPlannedScatterLocation(playerId, location);
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null) prepareAndTeleport(p, location);

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(lang.get("game.teleporting-progress", online)
                                .replace("%current%", String.valueOf(current + 1))
                                .replace("%total%", String.valueOf(totalJugadores)));
                    }
                    current++;
                } else {
                    this.cancel();

                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(lang.get("game.loading-world", online));
                    }

                    BukkitRunnable delay = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (plugin.getGameManager().enterCountdown(generation)) startCountdown(lang, generation);
                        }
                    };
                    plugin.getGameManager().trackStartupTask(delay.runTaskLater(plugin, 120L)); // 6 segundos de delay
                }
            }
        };
        plugin.getGameManager().trackStartupTask(scatter.runTaskTimer(plugin, 0L, 40L));
    }

    private Location calculateScatterLocation(World world, int i, int numPosiciones, int size) {
        double radio = size / 2.0;
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
        return loc;
    }

    private void prepareAndTeleport(Player p, Location loc) {
        p.teleport(loc);

        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        // Efectos con más tiempo para que duren hasta que terminen todos los turnos
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 3600, 1, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3600, 255, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3600, 255, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 3600, 255, false, false, false));
    }

    private void startCountdown(LanguageManager lang, long generation) {
        BukkitRunnable countdown = new BukkitRunnable() {
            int segundos = 10;

            @Override
            public void run() {
                if (!plugin.getGameManager().isStartupGeneration(generation)) {
                    cancel();
                    return;
                }
                if (segundos > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        String title = lang.get("game.countdown-title", p).replace("%time%", String.valueOf(segundos));
                        String subtitle = lang.get("game.countdown-subtitle", p);
                        p.showTitle(Title.title(
                            legacySection().deserialize(title),
                            legacySection().deserialize(subtitle),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    segundos--;
                } else {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (plugin.getGameManager().getEligibleRoster().contains(p.getUniqueId())) {
                            p.setGameMode(GameMode.SURVIVAL);
                        } else {
                            p.setGameMode(GameMode.SPECTATOR);
                        }
                        for (PotionEffect effect : p.getActivePotionEffects()) {
                            p.removePotionEffect(effect.getType());
                        }

                        String startTitle = lang.get("game.started-title", p);
                        String startSubtitle = lang.get("game.started-subtitle", p);
                        p.showTitle(Title.title(
                            legacySection().deserialize(startTitle),
                            legacySection().deserialize(startSubtitle),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000))));
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }

                    // --- GAMERULES A TODAS LAS DIMENSIONES ---
                    for (World w : Bukkit.getWorlds()) {
                        w.setGameRule(ADVANCE_TIME, true);
                        w.setGameRule(PVP, false);
                        w.setGameRule(ADVANCE_WEATHER, true);
                        w.setGameRule(NATURAL_HEALTH_REGENERATION, false);
                        w.setGameRule(SPAWN_MONSTERS, true);
                        w.setDifficulty(Difficulty.HARD);
                    }

                    plugin.getGameManager().startGame();
                    plugin.getGameManager().clearCompletedStartup();
                    this.cancel();
                }
            }
        };
        plugin.getGameManager().trackStartupTask(countdown.runTaskTimer(plugin, 20L, 20L));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && startCmd.hasPendingConfirmation()) {
            return CommandTabs.prefixFilter(List.of(String.valueOf(startCmd.getPendingSize())), args[0]);
        }
        return new ArrayList<>();
    }
}
