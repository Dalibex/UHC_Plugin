package me.dalibex.UHC_DBasic.managers;

import me.dalibex.UHC_DBasic.UHC_DBasic;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.Arrays;

public class TeamManager {

    private final Scoreboard board;
    private int teamSize = 1;
    private final ChatColor COLOR_UNICO = ChatColor.AQUA;

    private boolean customTeamsEnabled = false;

    // Colores de tinte para asignar a cada equipo (máximo razonable)
    private static final Material[] TEAM_DYES = {
            Material.RED_DYE, Material.BLUE_DYE, Material.GREEN_DYE,
            Material.YELLOW_DYE, Material.ORANGE_DYE, Material.PURPLE_DYE,
            Material.CYAN_DYE, Material.PINK_DYE, Material.LIME_DYE,
            Material.LIGHT_BLUE_DYE, Material.MAGENTA_DYE, Material.WHITE_DYE
    };

    private static final String[] TEAM_COLOR_KEYS = {
            "red", "blue", "green", "yellow", "orange", "purple",
            "cyan", "pink", "lime", "light_blue", "magenta", "white"
    };

    private static final ChatColor[] TEAM_CHAT_COLORS = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.DARK_GREEN,
            ChatColor.YELLOW, ChatColor.GOLD, ChatColor.DARK_PURPLE,
            ChatColor.DARK_AQUA, ChatColor.LIGHT_PURPLE, ChatColor.GREEN,
            ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.WHITE
    };


    public TeamManager() {
        this.board = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    // --- Estado de equipos personalizados ---
    public boolean isCustomTeamsEnabled() {
        return customTeamsEnabled;
    }

    public void setCustomTeamsEnabled(boolean enabled) {
        this.customTeamsEnabled = enabled;
    }

    public void setTeamSize(int size) {
        this.teamSize = size;
    }

    public int getTeamSize() {
        return teamSize;
    }

    // --- Inicializar equipos para modo personalizado ---
    public void initializeCustomTeams() {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);

        // Limpiar equipos anteriores
        for (Team team : board.getTeams()) {
            team.unregister();
        }

        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        if (jugadoresOnline == 0 || teamSize <= 1) return;

        int numeroDeEquipos = (int) Math.ceil((double) jugadoresOnline / teamSize);

        for (int i = 0; i < numeroDeEquipos; i++) {
            String colorKey = TEAM_COLOR_KEYS[i % TEAM_COLOR_KEYS.length];
            Team team = board.registerNewTeam(colorKey);
            ChatColor color = TEAM_CHAT_COLORS[i % TEAM_CHAT_COLORS.length];
            team.setColor(color);
            
            LanguageManager lang = plugin.getLang();
            String localizedName = lang.get("teams.colors." + colorKey, null);
            team.setDisplayName(localizedName != null ? localizedName : colorKey);

            String prefix = lang.get("teams.prefix-format", null)
                    .replace("%color%", color.toString())
                    .replace("%name%", team.getDisplayName());
            team.setPrefix(prefix);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    // --- Buscar equipo por nombre localizado o ID ---
    public Team getTeamByColorSearch(String input) {
        String lower = input.toLowerCase();
        for (Team team : board.getTeams()) {
            if (team.getName().equalsIgnoreCase(lower)) return team;
            if (team.getDisplayName().equalsIgnoreCase(input)) return team;
        }
        return null;
    }

    // --- Dar item selector de equipo a un jugador ---
    public void giveTeamSelectorItem(Player p) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        // Primero quitar si ya tiene uno
        removeTeamSelectorItem(p);

        ItemStack selector = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = selector.getItemMeta();
        meta.setDisplayName(lang.get("items.team-selector.name", p));
        meta.setLore(lang.getList("items.team-selector.lore", p));
        selector.setItemMeta(meta);

        p.getInventory().setItem(8, selector);
    }

    // --- Quitar item selector de equipo ---
    public void removeTeamSelectorItem(Player p) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        String selectorName = lang.get("items.team-selector.name", p);

        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(selectorName)) {
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    // --- Abrir GUI selector de equipo ---
    public void openTeamSelectorGUI(Player p) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        Set<Team> teams = board.getTeams();
        int size = teams.size();
        // Calcular tamaño del inventario (múltiplo de 9, mínimo 9)
        int invSize = Math.max(9, (int) Math.ceil(size / 9.0) * 9);

        Inventory gui = Bukkit.createInventory(null, invSize,
                lang.get("menus.team-selector.title", p));

        int slot = 0;
        for (Team team : teams) {
            int teamIndex = getTeamIndex(team);
            Material dyeMat = TEAM_DYES[teamIndex % TEAM_DYES.length];

            ItemStack dyeItem = new ItemStack(dyeMat);
            ItemMeta meta = dyeItem.getItemMeta();

            String teamName = team.getDisplayName();
            meta.setDisplayName(lang.get("menus.team-selector.team-item.name", p)
                    .replace("%name%", teamName));

            List<String> lore = new ArrayList<>();
            int currentMembers = team.getEntries().size();

            for (String line : lang.getList("menus.team-selector.team-item.lore", p)) {
                lore.add(line.replace("%current%", String.valueOf(currentMembers))
                        .replace("%max%", String.valueOf(teamSize)));
            }

            // Mostrar miembros actuales
            for (String entry : team.getEntries()) {
                lore.add(lang.get("menus.team-selector.member-format", p)
                        .replace("%player%", entry));
            }

            // Mostrar huecos disponibles
            int huecos = teamSize - currentMembers;
            for (int h = 0; h < huecos; h++) {
                lore.add(lang.get("menus.team-selector.empty-slot", p));
            }

            // Si el jugador ya está en este equipo, resaltar
            Team playerTeam = board.getEntryTeam(p.getName());
            if (playerTeam != null && playerTeam.equals(team)) {
                lore.add("");
                lore.add("§a✔ Tu equipo actual");
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.setLore(lore);
            dyeItem.setItemMeta(meta);
            gui.setItem(slot, dyeItem);
            slot++;
        }

        p.openInventory(gui);
    }

    // --- Intentar unir un jugador a un equipo por slot ---
    public boolean tryJoinTeam(Player p, int slot) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        List<Team> teamList = new ArrayList<>(board.getTeams());
        if (slot < 0 || slot >= teamList.size()) return false;

        Team targetTeam = teamList.get(slot);

        // Si ya está en este equipo
        Team currentTeam = board.getEntryTeam(p.getName());
        if (currentTeam != null && currentTeam.equals(targetTeam)) {
            p.sendMessage(lang.get("menus.team-selector.already-in-team", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }

        // Si el equipo está lleno
        if (targetTeam.getEntries().size() >= teamSize) {
            p.sendMessage(lang.get("menus.team-selector.already-full", p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }

        // Quitar del equipo anterior si tenía uno
        if (currentTeam != null) {
            currentTeam.removeEntry(p.getName());
        }

        // Asignar al nuevo equipo
        targetTeam.addEntry(p.getName());

        String joinedMsg = lang.get("menus.team-selector.joined", p)
                .replace("%name%", targetTeam.getColor() + targetTeam.getDisplayName());
        p.sendMessage(joinedMsg);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        return true;
    }

    // --- Intentar que un jugador abandone su equipo ---
    public boolean tryLeaveTeam(Player p) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        Team currentTeam = board.getEntryTeam(p.getName());
        if (currentTeam != null) {
            currentTeam.removeEntry(p.getName());
            p.sendMessage(lang.get("menus.team-selector.left", p));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return true;
        }
        return false;
    }

    // --- Verificar si todos los jugadores tienen equipo ---
    public boolean allPlayersHaveTeam() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            Team team = board.getEntryTeam(p.getName());
            if (team == null) return false;
        }
        return true;
    }

    // --- Limpiar equipos personalizados y quitar ítems ---
    public void clearCustomTeams() {
        for (Team team : board.getTeams()) {
            team.unregister();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeTeamSelectorItem(p);
        }
    }

    // --- Quitar ítems de selector a todos ---
    public void removeAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeTeamSelectorItem(p);
        }
    }

    // --- Dar selector a todos los jugadores online ---
    public void giveAllSelectorItems() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                giveTeamSelectorItem(p);
            }
        }
    }

    // --- Índice del equipo basado en su ID ---
    private int getTeamIndex(Team team) {
        String name = team.getName(); // "red", "blue", etc.
        List<String> keys = Arrays.asList(TEAM_COLOR_KEYS);
        int index = keys.indexOf(name.toLowerCase());
        return Math.max(0, index);
    }

    // --- SISTEMA ORIGINAL: shuffleTeams (equipos aleatorios) ---
    public void shuffleTeams() {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();

        for (Team team : board.getTeams()) {
            team.unregister();
        }

        List<String> vivosNames = new ArrayList<>();
        List<String> muertosNames = new ArrayList<>();

        for (String name : plugin.getGameManager().getParticipantesIniciales()) {
            if (plugin.getGameManager().getJugadoresEliminados().contains(name)) {
                muertosNames.add(name);
            } else {
                vivosNames.add(name);
            }
        }

        int totalJugadores = vivosNames.size() + muertosNames.size();
        if (totalJugadores == 0) return;

        Collections.shuffle(vivosNames);
        Collections.shuffle(muertosNames);

        int numeroDeEquipos = (int) Math.ceil((double) totalJugadores / teamSize);
        List<Team> listaEquipos = new ArrayList<>();

        for (int i = 1; i <= numeroDeEquipos; i++) {
            String idEquipo = "team_" + i;
            Team team = board.registerNewTeam(idEquipo);
            team.setColor(COLOR_UNICO);
            team.setDisplayName(idEquipo);

            String prefix = lang.get("teams.prefix-format", null)
                    .replace("%color%", COLOR_UNICO.toString())
                    .replace("%name%", idEquipo);
            team.setPrefix(prefix);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            listaEquipos.add(team);
        }

        // REPARTO DE VIVOS
        for (int i = 0; i < vivosNames.size(); i++) {
            Team teamAsignado = listaEquipos.get(i % numeroDeEquipos);
            String name = vivosNames.get(i);
            asignarEquipoPorNombre(name, teamAsignado, lang);
        }

        // REPARTO DE MUERTOS
        if (!listaEquipos.isEmpty()) {
            for (String name : muertosNames) {
                Team equipoMasVacio = listaEquipos.stream()
                        .min(Comparator.comparingInt(t -> t.getEntries().size()))
                        .orElse(listaEquipos.get(0));

                asignarEquipoPorNombre(name, equipoMasVacio, lang);
            }
        }
    }

    /**
     * Método auxiliar corregido para aceptar Nombres (String)
     * Maneja automáticamente si el jugador está online u offline
     */
    private void asignarEquipoPorNombre(String name, Team team, LanguageManager lang) {
        team.addEntry(name);

        Player p = Bukkit.getPlayer(name);
        if (p != null && p.isOnline()) {
            String msg = lang.get("teams.assigned", p)
                    .replace("%prefix%", lang.get("general.prefix", p))
                    .replace("%color%", team.getColor().toString())
                    .replace("%name%", team.getDisplayName());
            p.sendMessage(msg);
        }
    }

    public boolean areInSameTeam(Player a, Player b) {
        if (a == null || b == null) return false;

        Team teamA = board.getEntryTeam(a.getName());
        Team teamB = board.getEntryTeam(b.getName());

        if (teamA == null || teamB == null) return false;

        return teamA.equals(teamB);
    }

    public boolean renombrarEquipo(Player player, String nuevoNombre) {
        UHC_DBasic plugin = UHC_DBasic.getPlugin(UHC_DBasic.class);
        LanguageManager lang = plugin.getLang();
        Team team = board.getEntryTeam(player.getName());

        if (team == null) return false;

        if (nuevoNombre.length() > 16) nuevoNombre = nuevoNombre.substring(0, 16);

        String nombreAnterior = team.getDisplayName();
        team.setDisplayName(nuevoNombre);

        String prefix = lang.get("teams.prefix-format", null)
                .replace("%color%", team.getColor().toString())
                .replace("%name%", nuevoNombre);
        team.setPrefix(prefix);

        for (String entry : team.getEntries()) {
            Player member = Bukkit.getPlayer(entry);
            if (member != null) {
                member.playSound(member.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1, 1);
            }
        }

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (nombreAnterior.contains("team_")) {
                String foundedMsg = lang.get("teams.founded", all)
                        .replace("%prefix%", lang.get("general.prefix", all))
                        .replace("%color%", team.getColor().toString())
                        .replace("%name%", nuevoNombre);
                all.sendMessage(foundedMsg);
            } else {
                String renamedMsg = lang.get("teams.renamed", all)
                        .replace("%prefix%", lang.get("general.prefix", all))
                        .replace("%color%", team.getColor().toString())
                        .replace("%old%", nombreAnterior)
                        .replace("%new%", nuevoNombre);
                all.sendMessage(renamedMsg);
            }
            all.playSound(all.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.2f);
        }
        return true;
    }

    public void borrarTodosLosEquipos() {
        for (Team team : board.getTeams()) {
            team.unregister();
        }
    }
}