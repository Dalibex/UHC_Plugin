package me.dalibex.UHC_DBasic.gamemodes;

import java.util.List;

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

import me.dalibex.UHC_DBasic.UHC_DBasic;
import me.dalibex.UHC_DBasic.managers.GameManager;
import me.dalibex.UHC_DBasic.managers.LanguageManager;
import me.dalibex.UHC_DBasic.managers.TeamManager;
import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

/**
 * Clase base abstracta para todos los modos de juego de UHC.
 * Centraliza la lógica común como la entrega de objetos especiales,
 * gestión de capítulos, efectos de victoria y rotación de identidades.
 */
public abstract class AbstractUHCGameMode implements UHCGameMode {

    protected final UHC_DBasic plugin;
    protected final GameManager gm;
    protected boolean shulkerOneEntregado = false;
    protected boolean shulkerTwoEntregado = false;
    protected boolean equiposFormados = false;

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
        int segundosCap = gm.getSegundosPorCapitulo();
        int capituloActual = gm.getCapitulo();

        // Entrega de primer Shulker (Episodio 1)
        if (plugin.getAdminPanel().isShulkerOneEnabled() && !shulkerOneEntregado && cronometroSegundos > 1) {
            entregarObjetoGlobal("items.shulker.name", Material.ORANGE_SHULKER_BOX);
            shulkerOneEntregado = true;
        }

        // Cálculo de cambio de capítulo
        int capituloCalculado = (cronometroSegundos / segundosCap) + 1;
        if (capituloCalculado > capituloActual) {
            gm.setCapitulo(capituloCalculado);
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
        if (tm.getTeamSize() > 1 && !equiposFormados && tm.isCustomTeamsEnabled()) {
            entregarBrujulasDeSeguimiento(lang);
            equiposFormados = true;
            broadcastChapterOne(lang);
        } else if (tm.getTeamSize() == 1) {
            tm.shuffleTeams();
            equiposFormados = true;
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
    protected void entregarObjetoGlobal(String nombreKey, Material material) {
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getJugadoresEliminados().contains(p.getName())) continue;
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
    protected void entregarBrujulasDeSeguimiento(LanguageManager lang) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.getJugadoresEliminados().contains(p.getName())) continue;
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
    protected void ejecutarRotacionDeSkins() {
        gm.rotarSkins();
        LanguageManager lang = plugin.getLang();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                String nombreSkinNueva = gm.getUltimaSkinAsignada()
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
    protected void lanzarCohete(Location loc) {
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
    protected void aplicarEfectosVictoria(List<Player> ganadores) {
        for (Player p : ganadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 255));
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (count++ >= 10 || !p.isOnline()) { this.cancel(); return; }
                    lanzarCohete(p.getLocation());
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    @Override
    public void onReset() {
        this.shulkerOneEntregado = false;
        this.shulkerTwoEntregado = false;
        this.equiposFormados = false;
    }
}
