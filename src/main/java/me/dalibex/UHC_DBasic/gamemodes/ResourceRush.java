package me.dalibex.UHC_DBasic.gamemodes;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
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

public class ResourceRush implements UHCGameMode {

    private final UHC_DBasic plugin;
    private final GameManager gm;

    private final ArrayList<Player> poolObjetos = new ArrayList<>();
    private final Map<Integer, List<Material>> poolsPorCapitulo = new HashMap<>();
    private final List<Material> objetivosActivos = new ArrayList<>();

    private final Map<String, List<Material>> progresoGlobal = new HashMap<>();
    private final List<String> podioFinal = new ArrayList<>();

    private boolean shulkerEntregado = false;
    private boolean equiposFormados = false;

    public ResourceRush(UHC_DBasic plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
        inicializarPool();
    }

    private void inicializarPool() {
        poolsPorCapitulo.clear();
        objetivosActivos.clear();
        poolObjetos.clear();

        poolsPorCapitulo.put(1, Arrays.asList(
                Material.DIAMOND_BLOCK, Material.GOLDEN_APPLE, Material.TNT, Material.SPYGLASS,
                Material.LAVA_BUCKET, Material.SADDLE, Material.ENDER_PEARL, Material.DIAMOND_HOE
        ));
        poolsPorCapitulo.put(2, Arrays.asList(
                Material.BLAZE_ROD, Material.GHAST_TEAR, Material.BREWING_STAND, Material.JUKEBOX,
                Material.GLOW_ITEM_FRAME, Material.MUSIC_DISC_TEARS, Material.GOLDEN_CARROT, Material.TARGET
        ));
        poolsPorCapitulo.put(3, Arrays.asList(
                Material.ANVIL, Material.ENCHANTING_TABLE, Material.PLAYER_HEAD, Material.PAINTING,
                Material.YELLOW_STAINED_GLASS, Material.MAGMA_CREAM, Material.LEAD, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE
        ));
        poolsPorCapitulo.put(4, Arrays.asList(
                Material.ANCIENT_DEBRIS, Material.CRYING_OBSIDIAN, Material.DRIED_GHAST, Material.DIAMOND_CHESTPLATE,
                Material.TURTLE_HELMET, Material.DEEPSLATE_GOLD_ORE, Material.PISTON, Material.FIRE_CHARGE
        ));
        poolsPorCapitulo.put(5, Arrays.asList(
                Material.NETHERITE_SCRAP, Material.RESPAWN_ANCHOR, Material.CAKE, Material.POISONOUS_POTATO,
                Material.COMPASS, Material.CROSSBOW, Material.PHANTOM_MEMBRANE, Material.PUMPKIN_PIE
        ));
        poolsPorCapitulo.put(6, Arrays.asList(
                Material.HONEY_BOTTLE, Material.RAW_GOLD_BLOCK, Material.RABBIT_FOOT, Material.NETHER_WART,
                Material.MAP, Material.HONEY_BLOCK, Material.CAMPFIRE, Material.DISPENSER
        ));
        poolsPorCapitulo.put(7, Arrays.asList(
                Material.GILDED_BLACKSTONE, Material.CLOCK, Material.AMETHYST_SHARD, Material.FERMENTED_SPIDER_EYE,
                Material.RECOVERY_COMPASS, Material.WARPED_FUNGUS_ON_A_STICK, Material.DETECTOR_RAIL, Material.LECTERN
        ));
        poolsPorCapitulo.put(8, Arrays.asList(
                Material.BEE_NEST, Material.LIGHTNING_ROD, Material.GLOW_BERRIES, Material.BOOKSHELF,
                Material.NAME_TAG, Material.SOUL_LANTERN, Material.NETHER_WART_BLOCK, Material.DIAMOND_AXE
        ));
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
        LanguageManager lang = plugin.getLang();
        TeamManager tm = plugin.getTeamManager();
        int segundosCap = gm.getSegundosPorCapitulo();
        int capituloActual = gm.getCapitulo();

        if (plugin.getAdminPanel().isShulkerOneEnabled() && !shulkerEntregado && cronometroSegundos > 1) {
            entregarObjetoGlobal("items.shulker.name", Material.ORANGE_SHULKER_BOX);
            shulkerEntregado = true;
        }

        if (cronometroSegundos % segundosCap == 0 && cronometroSegundos != 0) {
            gm.setCapitulo(capituloActual + 1);
            int nuevoCap = gm.getCapitulo();

            // Delay de 5s para la ruleta
            Bukkit.getScheduler().runTaskLater(plugin, () -> actualizarObjetivosActivos(nuevoCap), 100L);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.get("game-events.chapter-start", p)
                        .replace("%prefix%", lang.get("general.prefix", p))
                        .replace("%chapter%", String.valueOf(nuevoCap)));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }

            if (nuevoCap == 8 && plugin.getAdminPanel().isShulkerTwoEnabled()) {
                entregarObjetoGlobal("items.shulker.name", Material.LIGHT_BLUE_SHULKER_BOX);
            }

            // FORMACIÓN DE EQUIPOS
            if (nuevoCap == 3 && tm.getTeamSize() > 1 && !equiposFormados) {
                tm.shuffleTeams();

                // Sincronizar objetivos de Resource Rush
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
                    if (t != null) {
                        List<Material> progresoIndiv = progresoGlobal.get(p.getName());
                        if (progresoIndiv != null) {
                            List<Material> progresoEq = progresoGlobal.computeIfAbsent(t.getName(), k -> new ArrayList<>());
                            for (Material m : progresoIndiv) {
                                if (!progresoEq.contains(m)) progresoEq.add(m);
                            }
                            progresoGlobal.remove(p.getName());
                        }
                    }
                }

                entregarBrujulasDeSeguimiento(lang);
                equiposFormados = true;
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(lang.get("game-events.teams-formed", p));
            }

            if (nuevoCap == 4) {
                for (World w : Bukkit.getWorlds()) w.setGameRule(PVP, true);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (String s : lang.getList("game-events.pvp-enabled", p)) p.sendMessage(s);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                }
            }
        }

        if (cronometroSegundos == 1) {
            if (tm.getTeamSize() == 1) {
                tm.shuffleTeams();
                equiposFormados = true;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (gm.getTiempoTotalSegundos() > 0) {
                    actualizarObjetivosActivos(1);
                }
            }, 100L); // 5 segundos
        }
    }

    private void actualizarObjetivosActivos(int capitulo) {
        if (gm.getTiempoTotalSegundos() <= 0) return;

        int aAñadir;
        if (capitulo <= 3) {
            aAñadir = 2;
        } else if (capitulo <= 9) {
            aAñadir = 1;
        } else {
            return;
        }

        List<Material> poolDelCap = poolsPorCapitulo.getOrDefault(capitulo, poolsPorCapitulo.get(8));
        if (poolDelCap == null) return;

        int añadidosEnEsteTick = 0;
        for (Material nuevoMat : poolDelCap) {
            if (añadidosEnEsteTick >= aAñadir) break;

            if (!objetivosActivos.contains(nuevoMat)) {
                objetivosActivos.add(nuevoMat);
                añadidosEnEsteTick++;

                enviarMensajeRuleta(nuevoMat);
            }
        }
    }

    /**
     * Método auxiliar para el anuncio con hover y traducción
     */
    private void enviarMensajeRuleta(Material mat) {
        LanguageManager lang = plugin.getLang();
        String translationKey = (mat.isBlock() ? "block.minecraft." : "item.minecraft.") + mat.name().toLowerCase();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String prefixStr = ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.ruleta-anuncio", p));

            net.md_5.bungee.api.chat.TranslatableComponent itemComp = new net.md_5.bungee.api.chat.TranslatableComponent(translationKey);
            itemComp.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            itemComp.setBold(true);
            itemComp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_ITEM,
                    new net.md_5.bungee.api.chat.hover.content.Item(mat.getKey().toString(), 1, null)));

            net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(prefixStr);
            msg.addExtra("§e[");
            msg.addExtra(itemComp);
            msg.addExtra("§e]");

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

        Objective objVida = board.getObjective("vida_tab");
        if (partidaActiva) {
            if (objVida == null) {
                objVida = board.registerNewObjective("vida_tab", "health",
                        ChatColor.translateAlternateColorCodes('&', lang.get("scoreboard.health-icon", player)),
                        org.bukkit.scoreboard.RenderType.HEARTS);
                objVida.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
        } else {
            if (objVida != null) objVida.unregister();
        }

        if (!partidaActiva) {
            obj.getScore("§1 ").setScore(7);
            obj.getScore(lang.get("scoreboard.mode-label", player).replace("%mode%", getName())).setScore(6);
            obj.getScore("§2 ").setScore(5);
            obj.getScore(lang.get("scoreboard.waiting", player)).setScore(4);
            obj.getScore("§3 ").setScore(3);
            obj.getScore(lang.get("scoreboard.players", player).replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))).setScore(2);
            obj.getScore("§4 ").setScore(1);
        } else {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
            int capitulo = gm.getCapitulo();
            String pvpStatus = (capitulo < 4) ? lang.get("scoreboard.pvp-pact", player) : lang.get("scoreboard.pvp-active", player);

            int next = 35;
            obj.getScore("§4 ").setScore(next--);
            obj.getScore(lang.get("scoreboard.phase", player).replace("%chapter%", String.valueOf(capitulo))).setScore(next--);
            obj.getScore(lang.get("scoreboard.pvp-label", player).replace("%status%", pvpStatus)).setScore(next--);
            obj.getScore("§6 ").setScore(next--);

            // BLOQUE EQUIPOS
            int teamSize = plugin.getTeamManager().getTeamSize();
            if (teamSize == 1) {
                String line = (team != null && !team.getPrefix().contains("team_")) ?
                        lang.get("scoreboard.team-label", player).replace("%color%", team.getColor().toString()).replace("%name%", team.getDisplayName()) : lang.get("scoreboard.team-rename-warn", player);
                obj.getScore(line).setScore(next--);
            } else {
                if (capitulo < 3) {
                    for (int i = 1; i < teamSize; i++) obj.getScore(" §d👥 §f: §k??????" + (" ".repeat(i))).setScore(next--);
                } else {
                    String line = (team != null && !team.getPrefix().contains("team_")) ? lang.get("scoreboard.team-mates-label", player).replace("%color%", team.getColor().toString()).replace("%name%", team.getDisplayName()) :
                            (team != null ? lang.get("scoreboard.team-rename-warn", player) : lang.get("scoreboard.team-assigning", player));
                    obj.getScore(line).setScore(next--);
                    if (team != null) {
                        for (String entry : team.getEntries()) {
                            if (entry.equals(player.getName())) continue;
                            String health; String color = "§f";
                            if (gm.getJugadoresEliminados().contains(entry)) { color = "§7§m"; health = lang.get("scoreboard.mate-dead", player); }
                            else {
                                Player m = Bukkit.getPlayer(entry);
                                if (m != null && m.isOnline()) {
                                    double h = m.getHealth();
                                    String c = (h > 15) ? "§a" : (h > 10) ? "§2" : (h > 5) ? "§e" : "§c";
                                    health = " " + c + (int)h + "§4❤";
                                } else health = lang.get("scoreboard.mate-offline", player);
                            }
                            obj.getScore("§6> " + color + entry + health).setScore(next--);
                        }
                    }
                }
            }

            // BLOQUE OBJETIVOS
            obj.getScore("§8 ").setScore(next--);
            int realizados = 0;
            String clave = (team != null) ? team.getName() : player.getName();
            realizados = progresoGlobal.getOrDefault(clave, new ArrayList<>()).size();

            String raw = lang.get("scoreboard-rr.rr-counter", player);

            String lineObj = ChatColor.translateAlternateColorCodes('&', raw.replace("%done%", String.valueOf(realizados)).replace("%total%", String.valueOf(objetivosActivos.size())));
            obj.getScore(lineObj).setScore(next--);

            // TIMER FINAL
            obj.getScore("§9 ").setScore(next--);
            obj.getScore(lang.get("scoreboard.time-total-label", player)).setScore(next--);
            obj.getScore("§6> §f" + tiempoTotal).setScore(next--);
            obj.getScore("§7 ").setScore(next--);
            obj.getScore(lang.get("scoreboard.time-next-label", player)).setScore(next--);
            obj.getScore("§6> §f" + tiempo).setScore(next--);

            actualizarNametags(player, board);
        }
    }

    private void actualizarNametags(Player player, Scoreboard board) {
        Team myTeam = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setPlayerListName(onlinePlayer.getName());
            String teamKey = "h_" + onlinePlayer.getName();
            Team t = board.getTeam(teamKey);
            if (t == null) t = board.registerNewTeam(teamKey);
            if (!t.hasEntry(onlinePlayer.getName())) t.addEntry(onlinePlayer.getName());
            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            boolean esCompanero = (myTeam != null && myTeam.hasEntry(onlinePlayer.getName()));
            t.setColor((onlinePlayer.equals(player) || esCompanero) ? ChatColor.LIGHT_PURPLE : ChatColor.RED);
        }
    }

    @Override
    public void checkVictory() {
        if (gm.getTiempoTotalSegundos() <= 0) return;

        List<Player> vivos = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode() == GameMode.SURVIVAL && !gm.getJugadoresEliminados().contains(p.getName()))
                .collect(Collectors.toList());

        Set<String> entidadesConVida = new HashSet<>();
        for (Player p : vivos) {
            Team t = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());
            entidadesConVida.add(t != null ? t.getName() : p.getName());
        }

        int equiposTotales = Bukkit.getScoreboardManager().getMainScoreboard().getTeams().size();

        if (equiposTotales == 0) {
            equiposTotales = Bukkit.getOnlinePlayers().size();
        }

        int limitePodio = Math.min(3, equiposTotales);

        if (podioFinal.size() >= limitePodio && limitePodio > 0) {
            anunciarPodioFinal();
            return;
        }

        if (entidadesConVida.isEmpty()) {
            anunciarPodioFinal();
        }
    }

    private void anunciarPodioFinal() {
        LanguageManager lang = plugin.getLang();
        gm.detenerPartidaTask();

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-header", null)));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-title", null)));

        for (int i = 0; i < podioFinal.size(); i++) {
            String medal = lang.get("resource-rush.medals." + (i + 1), null);
            String claveGanador = podioFinal.get(i);

            String nombreAMostrar;
            if (plugin.getTeamManager().getTeamSize() == 1) {
                Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(claveGanador);
                if (t != null && !t.getEntries().isEmpty()) {
                    nombreAMostrar = t.getEntries().iterator().next();
                } else {
                    nombreAMostrar = claveGanador;
                }
            } else {
                Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(claveGanador);
                nombreAMostrar = (t != null) ? t.getDisplayName() : claveGanador;
            }

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    lang.get("resource-rush.podio-line", null)
                            .replace("%medal%", medal)
                            .replace("%team%", nombreAMostrar)));
        }
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', lang.get("resource-rush.podio-footer", null)));

        Team ganadorReal = null;
        if (!podioFinal.isEmpty()) {
            ganadorReal = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(podioFinal.get(0));
        }
        ejecutarEfectosFinales(ganadorReal);
    }

    public void completarObjetivo(Player p, Material mat) {
        LanguageManager lang = plugin.getLang();
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(p.getName());

        String clave = (team != null) ? team.getName() : p.getName();
        List<Material> logros = progresoGlobal.computeIfAbsent(clave, k -> new ArrayList<>());

        if (objetivosActivos.contains(mat) && !logros.contains(mat)) {
            logros.add(mat);

            String nombreAMostrar;
            if (plugin.getTeamManager().getTeamSize() > 1 && team != null) {
                nombreAMostrar = team.getDisplayName() + " &8[" + ChatColor.WHITE + p.getName() + "&8]";
            } else {
                nombreAMostrar = p.getName();
            }

            String color = (team != null) ? team.getColor().toString() : "§f";
            String itemName = mat.name().replace("_", " ").toLowerCase();

            String rawMsg = lang.get("resource-rush.objective-global", null);

            String globalMsg = ChatColor.translateAlternateColorCodes('&', rawMsg
                    .replace("%color%", color)
                    .replace("%team%", nombreAMostrar)
                    .replace("%item%", itemName)
                    .replace("%done%", String.valueOf(logros.size())));

            for (Player all : Bukkit.getOnlinePlayers()) {
                all.sendMessage(globalMsg);
                all.playSound(all.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
            }

            // Lógica de finalización (12 objetos)
            if (logros.size() >= 12 && !podioFinal.contains(clave)) {
                podioFinal.add(clave);

                String nombreFin = (plugin.getTeamManager().getTeamSize() > 1 && team != null) ? team.getDisplayName() : p.getName();

                Bukkit.broadcastMessage(lang.get("resource-rush.team-finished", null)
                        .replace("%color%", color)
                        .replace("%team%", nombreFin));

                for (Player all : Bukkit.getOnlinePlayers()) {
                    all.playSound(all.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1f);
                }
                checkVictory();
            }
        }
    }

    @Override
    public void onReset() {
        this.shulkerEntregado = false;
        this.equiposFormados = false;
        this.progresoGlobal.clear();
        this.podioFinal.clear();
        this.objetivosActivos.clear();
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team t : new HashSet<>(board.getTeams())) t.unregister();
        for (Player p : Bukkit.getOnlinePlayers()) p.setPlayerListName(p.getName());
        inicializarPool();
    }

    private void ejecutarEfectosFinales(Team ganador) {
        LanguageManager lang = plugin.getLang();

        if (ganador != null) {
            List<String> names = new ArrayList<>();
            List<Player> vivosParaCohetes = new ArrayList<>();

            for (String entry : ganador.getEntries()) {
                if (gm.getJugadoresEliminados().contains(entry)) {
                    names.add("§7§m" + entry + "§r");
                } else {
                    names.add("§f" + entry);
                    Player p = Bukkit.getPlayer(entry);
                    if (p != null) {
                        vivosParaCohetes.add(p);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
                    }
                }
            }

            String listaIntegrantes = String.join("§7, ", names);
            String color = ganador.getColor().toString();
            String nombreEquipo = ganador.getDisplayName();

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("");
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        lang.get("victory.broadcast-header", p)
                                .replace("%color%", color)
                                .replace("%team%", nombreEquipo)));

                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        lang.get("victory.team-members", p)
                                .replace("%members%", listaIntegrantes)));

                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        lang.get("victory.broadcast-footer", p)));
                p.sendMessage("");

                p.sendTitle(
                        ChatColor.translateAlternateColorCodes('&', lang.get("victory.title", p)),
                        ChatColor.translateAlternateColorCodes('&', lang.get("victory.subtitle", p)
                                .replace("%color%", color)
                                .replace("%team%", nombreEquipo)),
                        10, 100, 20
                );

                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                mostrarScoreboardVictoria(p, ganador, lang);
            }

            new BukkitRunnable() {
                int seg = 0;
                public void run() {
                    if (seg++ >= 10) { this.cancel(); return; }
                    for (Player w : vivosParaCohetes) {
                        if (w.isOnline()) lanzarCohete(w.getLocation());
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

        } else {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    lang.get("victory.no-survivors", null)));
        }
    }

    private void mostrarScoreboardVictoria(Player player, Team ganador, LanguageManager lang) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("victoria", "dummy", lang.get("victory.scoreboard-title", player));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore(lang.get("victory.scoreboard-winner", player).replace("%color%", ganador.getColor().toString()).replace("%team%", ganador.getDisplayName())).setScore(1);
        player.setScoreboard(board);
    }

    private void lanzarCohete(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder().withColor(Color.GREEN).withFade(Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
        fw.setFireworkMeta(fwm);
    }

    private void entregarObjetoGlobal(String nombreKey, Material material) {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getJugadoresEliminados().contains(p.getName())) continue;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', lang.get(nombreKey, p)));
            item.setItemMeta(meta);
            p.getInventory().addItem(item);
        }
    }

    private void entregarBrujulasDeSeguimiento(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getJugadoresEliminados().contains(p.getName())) continue;
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang.get("tracking-compass.name", p));
                meta.setLore(lang.getList("tracking-compass.lore", p));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                compass.setItemMeta(meta);
            }
            p.getInventory().addItem(compass);
        }
    }

    public List<Material> getObjetivosActivos() { return objetivosActivos; }
}