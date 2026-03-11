package me.dalibex.UHC_DBasic.gamemodes;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.GameRules.PVP;

public class ResourceRush extends AbstractUHCGameMode {

    private final Map<Integer, List<Material>> poolsPorCapitulo = new HashMap<>();
    private final List<Material> objetivosActivos = new ArrayList<>();

    private final Map<String, List<Material>> progresoGlobal = new HashMap<>();
    private final List<String> podioFinal = new ArrayList<>();
    private boolean terminando = false;

    public ResourceRush(UHC_DBasic plugin, GameManager gm) {
        super(plugin, gm);
        inicializarPool();
    }

    private void inicializarPool() {
        poolsPorCapitulo.clear();
        objetivosActivos.clear();
        progresoGlobal.clear();

        // Configuración de pools de objetos por capítulo
        poolsPorCapitulo.put(1, Arrays.asList(Material.DIAMOND_BLOCK, Material.GOLDEN_APPLE, Material.TNT, Material.SPYGLASS, Material.LAVA_BUCKET, Material.SADDLE, Material.ENDER_PEARL, Material.DIAMOND_HOE));
        poolsPorCapitulo.put(2, Arrays.asList(Material.BLAZE_ROD, Material.GHAST_TEAR, Material.BREWING_STAND, Material.JUKEBOX, Material.GLOW_ITEM_FRAME, Material.MUSIC_DISC_TEARS, Material.GOLDEN_CARROT, Material.TARGET));
        poolsPorCapitulo.put(3, Arrays.asList(Material.ANVIL, Material.ENCHANTING_TABLE, Material.PLAYER_HEAD, Material.PAINTING, Material.YELLOW_STAINED_GLASS, Material.MAGMA_CREAM, Material.LEAD, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE));
        poolsPorCapitulo.put(4, Arrays.asList(Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.DRIED_GHAST, Material.DIAMOND_CHESTPLATE, Material.TURTLE_HELMET, Material.DEEPSLATE_GOLD_ORE, Material.PISTON, Material.FIRE_CHARGE));
        poolsPorCapitulo.put(5, Arrays.asList(Material.NETHERITE_SCRAP, Material.RESPAWN_ANCHOR, Material.CAKE, Material.POISONOUS_POTATO, Material.COMPASS, Material.CROSSBOW, Material.PHANTOM_MEMBRANE, Material.PUMPKIN_PIE));
        poolsPorCapitulo.put(6, Arrays.asList(Material.HONEY_BOTTLE, Material.RAW_GOLD_BLOCK, Material.RABBIT_FOOT, Material.NETHER_WART, Material.MAP, Material.HONEY_BLOCK, Material.CAMPFIRE, Material.DISPENSER));
        poolsPorCapitulo.put(7, Arrays.asList(Material.GILDED_BLACKSTONE, Material.CLOCK, Material.AMETHYST_SHARD, Material.FERMENTED_SPIDER_EYE, Material.RECOVERY_COMPASS, Material.WARPED_FUNGUS_ON_A_STICK, Material.DETECTOR_RAIL, Material.LECTERN));
        poolsPorCapitulo.put(8, Arrays.asList(Material.BEE_NEST, Material.LIGHTNING_ROD, Material.GLOW_BERRIES, Material.BOOKSHELF, Material.NAME_TAG, Material.SOUL_LANTERN, Material.NETHER_WART_BLOCK, Material.DIAMOND_AXE));

        for (List<Material> lista : poolsPorCapitulo.values()) {
            Collections.shuffle(lista);
        }
    }

    @Override
    public String getName() {
        return "Resource Rush";
    }

    @Override
    public void onTick(int cronometroSegundos, int tiempoTotalSegundos) {
        super.onTick(cronometroSegundos, tiempoTotalSegundos);

        // Activación de primeros objetivos con delay inicial
        if (cronometroSegundos == 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (gm.getTiempoTotalSegundos() > 0) actualizarObjetivosActivos(1);
            }, 100L); 
        }
    }

    @Override
    protected void onChapterChange(int nuevoCap) {
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();

        // Notificación de nuevo capítulo y actualización de objetivos
        Bukkit.getScheduler().runTaskLater(plugin, () -> actualizarObjetivosActivos(nuevoCap), 100L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(lang.get("game-events.chapter-start", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%chapter%", String.valueOf(nuevoCap)));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

        ejecutarRotacionDeSkins();

        // Shulker 2
        if (nuevoCap == 8 && plugin.getAdminPanel().isShulkerTwoEnabled()) {
            entregarObjetoGlobal("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
        }

        // Formación de Equipos Aleatorios (Ep 3)
        if (tm.getTeamSize() > 1 && !equiposFormados && !tm.isCustomTeamsEnabled() && nuevoCap == 3) {
            tm.shuffleTeams();
            sincronizarEquiposResourceRush();
            entregarBrujulasDeSeguimiento(lang);
            equiposFormados = true;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.get("game-events.teams-formed", p));
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
            }
        }

        // Activación de PVP (Episodio 4)
        if (nuevoCap == 4) {
            for (World w : Bukkit.getWorlds()) w.setGameRule(PVP, true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (String s : lang.getList("game-events.pvp-enabled", p)) p.sendMessage(s);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
            }
        }
    }

    private void actualizarObjetivosActivos(int capitulo) {
        if (gm.getTiempoTotalSegundos() <= 0) return;

        int aAñadir = (capitulo <= 3) ? 2 : (capitulo <= 9) ? 1 : 0;
        if (aAñadir == 0) return;

        List<Material> poolDelCap = poolsPorCapitulo.getOrDefault(capitulo, poolsPorCapitulo.get(8));
        if (poolDelCap == null) return;

        int añadidos = 0;
        for (Material mat : poolDelCap) {
            if (añadidos >= aAñadir) break;
            if (!objetivosActivos.contains(mat)) {
                objetivosActivos.add(mat);
                añadidos++;
                enviarAnuncioObjetivo(mat);
            }
        }
    }

    private void enviarAnuncioObjetivo(Material mat) {
        LanguageManager lang = plugin.getLang();
        String translationKey = (mat.isBlock() ? "block.minecraft." : "item.minecraft.") + mat.name().toLowerCase();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String prefix = ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.ruleta-anuncio", p));
            net.md_5.bungee.api.chat.TranslatableComponent itemComp = new net.md_5.bungee.api.chat.TranslatableComponent(translationKey);
            itemComp.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            itemComp.setBold(true);
            itemComp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM,
                    new net.md_5.bungee.api.chat.hover.content.Item(mat.getKey().toString(), 1, null)));

            net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefix);
            msg.addExtra("§e["); msg.addExtra(itemComp); msg.addExtra("§e]");

            p.spigot().sendMessage(msg);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
        }
    }

    @Override
    public void updateScoreboard(Player player, String tiempo, String tiempoTotal, boolean partidaActiva) {
        LanguageManager lang = plugin.getLang();
        Scoreboard board = player.getScoreboard();

        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("uhc");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("uhc", "dummy", ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.title", player)));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(NumberFormat.blank());

        if (!partidaActiva) {
            renderLobbyScores(obj, player, lang);
        } else {
            AtomicInteger next = new AtomicInteger(35);
            ScoreboardHelper.addPhaseInfo(obj, next, player, lang, gm);
            ScoreboardHelper.addTeamInfo(obj, next, player, lang, plugin.getTeamManager(), gm);
            
            // Sección específica de logros de Resource Rush
            obj.getScore("§8 ").setScore(next.getAndDecrement());
            Team team = board.getEntryTeam(player.getName());
            String clave = (team != null) ? team.getName() : player.getName();
            int realizados = progresoGlobal.getOrDefault(clave, new ArrayList<>()).size();
            String counter = lang.get("scoreboard-rr.rr-counter", player)
                    .replace("%done%", String.valueOf(realizados))
                    .replace("%total%", String.valueOf(objetivosActivos.size()));
            obj.getScore(ChatColor.translateAlternateColorCodes('&', counter)).setScore(next.getAndDecrement());

            ScoreboardHelper.addTimers(obj, next, tiempo, tiempoTotal, player, lang, gm);
        }
    }

    private void renderLobbyScores(Objective obj, Player player, LanguageManager lang) {
        obj.getScore("§1 ").setScore(7);
        obj.getScore(lang.get("scoreboard.mode-label", player).replace("%mode%", getName())).setScore(6);
        obj.getScore("§2 ").setScore(5);
        obj.getScore(lang.get("scoreboard.waiting", player)).setScore(4);
        obj.getScore("§3 ").setScore(3);
        obj.getScore(lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))).setScore(2);
        obj.getScore("§4 ").setScore(1);
    }

    @Override
    public void checkVictory() {
        if (!gm.isPartidaIniciada() || gm.getTiempoTotalSegundos() <= 0 || terminando) return;

        List<Player> vivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL && !gm.getJugadoresEliminados().contains(p.getName()))
                .collect(Collectors.toList());

        Set<String> entidadesVivas = vivos.stream()
                .map(p -> {
                    Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
                    return (t != null) ? t.getName() : p.getName();
                }).collect(Collectors.toSet());

        long equiposSinTerminar = entidadesVivas.stream()
                .filter(clave -> !podioFinal.contains(clave))
                .count();

        if (entidadesVivas.isEmpty()) {
            finalizarConPodio(); // Nadie vivo
        } else if (equiposSinTerminar == 0) {
            finalizarConPodio(); // Todos los vivos han terminado
        }
    }

    private void finalizarConPodio() {
        if (terminando) return;
        terminando = true;

        LanguageManager lang = plugin.getLang();
        gm.detenerPartidaTask();
        gm.setPartidaIniciada(false);

        if (podioFinal.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("victory.no-survivors", null)));
        } else {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-header", null)));
            for (int i = 0; i < podioFinal.size(); i++) {
                String medal = lang.get("resource-rush.medals." + (i + 1), null);
                String clave = podioFinal.get(i);
                Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(clave);
                String nombre = (t != null) ? t.getDisplayName() : clave;
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-line", null)
                        .replace("%medal%", medal).replace("%team%", nombre)));
            }
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-footer", null)));

            Team ganador = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(podioFinal.get(0));
            ejecutarEfectosFinales(ganador);
        }
    }

    public void completarObjetivo(Player p, Material mat) {
        if (!objetivosActivos.contains(mat)) return;
        
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
        String clave = (team != null) ? team.getName() : p.getName();
        List<Material> logros = progresoGlobal.computeIfAbsent(clave, k -> new ArrayList<>());

        if (!logros.contains(mat)) {
            logros.add(mat);
            anunciarLogro(p, team, mat, logros.size());

            if (logros.size() >= 12 && !podioFinal.contains(clave)) {
                podioFinal.add(clave);
                handleTeamFinish(p, team);
            }
        }
    }

    private void anunciarLogro(Player p, Team team, Material mat, int done) {
        LanguageManager lang = plugin.getLang();
        String name = (team != null) ? team.getDisplayName() + " &8[" + ChatColor.WHITE + p.getName() + "&8]" : p.getName();
        String color = (team != null) ? team.getColor().toString() : "§f";
        String itemName = mat.name().replace("_", " ").toLowerCase();

        String raw = lang.get("resource-rush.objective-global", null);
        String msg = ChatColor.translateAlternateColorCodes('&', raw
                .replace("%color%", color).replace("%team%", name)
                .replace("%item%", itemName).replace("%done%", String.valueOf(done)));

        for (Player all : Bukkit.getOnlinePlayers()) {
            all.sendMessage(msg);
            all.playSound(all.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
        }
    }

    private void handleTeamFinish(Player p, Team team) {
        LanguageManager lang = plugin.getLang();
        String color = (team != null) ? team.getColor().toString() : "§f";
        String nombre = (team != null) ? team.getDisplayName() : p.getName();

        Bukkit.broadcastMessage(lang.get("resource-rush.team-finished", null)
                .replace("%color%", color).replace("%team%", nombre));

        String alert = lang.get("resource-rush.finish-alert", p);
        if (team != null) team.getEntries().forEach(e -> { Player m = Bukkit.getPlayer(e); if (m != null) m.sendMessage(alert); });
        else p.sendMessage(alert);

        new BukkitRunnable() {
            @Override
            public void run() {
                String specMsg = lang.get("resource-rush.spectator-message", null);
                if (team != null) {
                    team.getEntries().forEach(e -> { 
                        Player m = Bukkit.getPlayer(e); 
                        if (m != null && m.isOnline()) { m.setGameMode(GameMode.SPECTATOR); m.sendMessage(ChatColor.translateAlternateColorCodes('&', specMsg)); }
                    });
                } else if (p.isOnline()) { p.setGameMode(GameMode.SPECTATOR); p.sendMessage(ChatColor.translateAlternateColorCodes('&', specMsg)); }
            }
        }.runTaskLater(plugin, 200L);

        checkVictory();
    }

    private void ejecutarEfectosFinales(Team ganador) {
        if (ganador == null) return;
        LanguageManager lang = plugin.getLang();
        List<Player> winners = new ArrayList<>();
        for (String entry : ganador.getEntries()) {
            Player p = Bukkit.getPlayer(entry);
            if (p != null && !gm.getJugadoresEliminados().contains(entry)) {
                winners.add(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
            }
        }
        
        String color = ganador.getColor().toString();
        String teamName = ganador.getDisplayName();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.get("victory.broadcast-header", p).replace("%color%", color).replace("%team%", teamName)));
            p.sendTitle(ChatColor.translateAlternateColorCodes('&', lang.get("victory.title", p)),
                        ChatColor.translateAlternateColorCodes('&', lang.get("victory.subtitle", p).replace("%color%", color).replace("%team%", teamName)), 
                        10, 100, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        aplicarEfectosVictoria(winners);
    }

    private void sincronizarEquiposResourceRush() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            if (t != null && progresoGlobal.containsKey(p.getName())) {
                List<Material> ind = progresoGlobal.get(p.getName());
                List<Material> eq = progresoGlobal.computeIfAbsent(t.getName(), k -> new ArrayList<>());
                ind.forEach(m -> { if (!eq.contains(m)) eq.add(m); });
                progresoGlobal.remove(p.getName());
            }
        }
    }

    @Override
    public void onReset() {
        super.onReset();
        this.progresoGlobal.clear();
        this.podioFinal.clear();
        this.objetivosActivos.clear();
        this.terminando = false;
        Bukkit.getOnlinePlayers().forEach(p -> p.setPlayerListName(p.getName()));
        inicializarPool();
    }

    public List<Material> getLogrosJugador(Player p) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
        return progresoGlobal.getOrDefault((team != null) ? team.getName() : p.getName(), new ArrayList<>());
    }

    public List<Material> getObjetivosActivos() { return objetivosActivos; }
}