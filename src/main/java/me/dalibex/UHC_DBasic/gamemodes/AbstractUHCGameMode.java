package me.dalibex.UHC_DBasic.gamemodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import me.dalibex.UHC_DBasic.utils.ScoreboardHelper;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Clase base abstracta para todos los modos de juego de UHC.
 * Centraliza la lógica común como la entrega de objetos especiales,
 * gestión de capítulos, efectos de victoria y rotación de identidades.
 */
public abstract class AbstractUHCGameMode implements UHCGameMode {

    protected final UHC_DBasic plugin;
    protected final GameManager gm;
    protected boolean shulkerOneDelivered = false;
    protected boolean shulkerTwoDelivered = false;
    protected boolean teamsFormed = false;

    /**
     * Claves de las líneas del sidebar del objetivo "uhc" por jugador,
     * para poder limpiar solo las líneas obsoletas sin re-registrar el
     * objetivo en cada tick (ver getOrCreateSidebar/clearStaleSidebarKeys).
     */
    private final Map<UUID, Set<String>> sidebarKeys = new HashMap<>();

    public AbstractUHCGameMode(UHC_DBasic plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
    }

    /**
     * Lógica base del tick ejecutada cada segundo.
     * Maneja la entrega de shulkers y la detección de cambio de capítulo.
     */
    @Override
    public void onTick(int cronometroSegundos, int tiempoTotalSegundos) {
        LanguageManager lang = plugin.getLang();
        int segundosCap = gm.getSecondsPerChapter();
        int capituloActual = gm.getChapter();

        // Entrega de primer Shulker (Episodio 1)
        if (plugin.getAdminPanel().isShulkerOneEnabled() && !shulkerOneDelivered && cronometroSegundos > 1) {
            giveGlobalItem("items.shulker.name", Material.ORANGE_SHULKER_BOX);
            shulkerOneDelivered = true;
        }

        // Cálculo de cambio de capítulo
        int capituloCalculado = (cronometroSegundos / segundosCap) + 1;
        if (capituloCalculado > capituloActual) {
            gm.setChapter(capituloCalculado);
            onChapterChange(capituloCalculado);
        }

        // Lógica específica del primer segundo (Brújulas si es manual)
        if (cronometroSegundos == 1) {
            handleInitialSecond(lang);
        }
    }

    /**
     * Hook ejecutado cuando el capítulo del juego cambia.
     * @param nuevoCap El número del nuevo capítulo.
     */
    protected abstract void onChapterChange(int nuevoCap);

    /**
     * Maneja la lógica de inicialización en el segundo 1 de la partida.
     */
    protected void handleInitialSecond(LanguageManager lang) {
        TeamManager tm = plugin.getTeamManager();
        if (tm.getTeamSize() > 1 && !teamsFormed && tm.isCustomTeamsEnabled()) {
            giveTrackingCompasses(lang);
            teamsFormed = true;
            broadcastChapterOne(lang);
        } else if (tm.getTeamSize() == 1) {
            tm.shuffleTeams();
            teamsFormed = true;
        }
    }

    /**
     * Envía los mensajes iniciales del capítulo 1.
     */
    protected void broadcastChapterOne(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(lang.get("game-events.chapter-start", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%chapter%", "1"));
            p.sendMessage(lang.get("game-events.teams-formed", p));
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f);
        }
    }

    /**
     * Entrega un objeto a todos los jugadores vivos.
     */
    protected void giveGlobalItem(String nombreKey, Material material) {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getEliminatedPlayers().contains(p.getName())) continue;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(lang.getComponent(nombreKey, p));
                item.setItemMeta(meta);
            }
            p.getInventory().addItem(item);
        }
    }

    /**
     * Entrega la brújula de seguimiento de aliados.
     */
    protected void giveTrackingCompasses(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getEliminatedPlayers().contains(p.getName())) continue;
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta meta = compass.getItemMeta();
            if (meta != null) {
                meta.displayName(lang.getComponent("tracking-compass.name", p));
                meta.lore(lang.getComponentList("tracking-compass.lore", p));
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                compass.setItemMeta(meta);
            }
            p.getInventory().addItem(compass);
        }
    }

    /**
     * Ejecuta la rotación de skins y notifica a los jugadores.
     */
    protected void runSkinRotation() {
        gm.rotateSkins();
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                String nombreSkinNueva = gm.getLastAssignedSkin()
                        .getOrDefault(p.getUniqueId(), "???");
                String rawMsg = lang.get("game-events.skins.identity-changed", p);
                String mensajePersonalizado = rawMsg.replace("%player%", nombreSkinNueva);
                p.sendMessage(legacySection().deserialize(mensajePersonalizado));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
            }
        }
    }

    /**
     * Lanza cohetes de celebración en una ubicación.
     */
    protected void launchFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        fwm.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .withFade(Color.YELLOW)
                .with(FireworkEffect.Type.BALL_LARGE)
                .build());
        fw.setFireworkMeta(fwm);
    }

    /**
     * Efectos visuales y sonoros para los ganadores.
     */
    protected void applyVictoryEffects(List<Player> ganadores) {
        for (Player p : ganadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count++ >= 10 || !p.isOnline()) { this.cancel(); return; }
                    launchFirework(p.getLocation());
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    /**
     * Obtiene (o crea una única vez) el objetivo de sidebar "uhc" del
     * scoreboard del jugador. Evita el unregister/register por tick que
     * provocaba churn de paquetes y objetos con 60 jugadores online.
     */
    protected Objective getOrCreateSidebar(Scoreboard board, Player player, LanguageManager lang) {
        Objective obj = board.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
        if (obj == null) {
            obj = board.registerNewObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE, Criteria.DUMMY, lang.getComponent("scoreboard.title", player));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.numberFormat(NumberFormat.blank());
        }
        return obj;
    }

    /**
     * Limpia las claves de línea del tick anterior que ya no se renderizan.
     */
    protected void clearStaleSidebarKeys(Objective obj, Player player) {
        Set<String> stale = sidebarKeys.remove(player.getUniqueId());
        if (stale == null) return;
        for (String key : stale) {
            Score score = obj.getScore(key);
            if (score.isScoreSet()) score.resetScore();
        }
    }

    /**
     * Registra las claves renderizadas en este tick para poder limpiarlas
     * en el siguiente.
     */
    protected void storeSidebarKeys(Player player, List<String> keys) {
        sidebarKeys.put(player.getUniqueId(), new HashSet<>(keys));
    }

    /**
     * Devuelve la caché de claves de sidebar y la vacía de esta instancia.
     * Permite transferir las líneas pendientes de limpiar a una instancia
     * nueva de gamemode (ver GameManager.cambiarModo).
     */
    public Map<UUID, Set<String>> takeSidebarKeys() {
        Map<UUID, Set<String>> transfer = new HashMap<>(sidebarKeys);
        sidebarKeys.clear();
        return transfer;
    }

    /**
     * Adopta la caché de claves de sidebar de una instancia anterior de
     * gamemode para que clearStaleSidebarKeys pueda limpiar las líneas
     * obsoletas tras un cambio de modo.
     */
    public void adoptSidebarKeys(Map<UUID, Set<String>> keys) {
        sidebarKeys.putAll(keys);
    }

    @Override
    public void onReset() {
        this.shulkerOneDelivered = false;
        this.shulkerTwoDelivered = false;
        this.teamsFormed = false;

        // Limpiar las líneas obsoletas de los scoreboards de los jugadores online
        for (Player p : Bukkit.getOnlinePlayers()) {
            Set<String> stale = sidebarKeys.remove(p.getUniqueId());
            if (stale == null) continue;
            Scoreboard board = p.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            Objective obj = board.getObjective(ScoreboardHelper.SIDEBAR_OBJECTIVE);
            if (obj == null) continue;
            for (String key : stale) {
                Score score = obj.getScore(key);
                if (score.isScoreSet()) score.resetScore();
            }
        }
        sidebarKeys.clear();
    }
}
