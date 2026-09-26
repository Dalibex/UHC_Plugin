package me.dalibex.UHC_DBasic.managers.skins;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import me.dalibex.UHC_DBasic.UHC_DBasic;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Applies real victim textures to placed death heads. */
public class DeathHeadService {

    private final UHC_DBasic plugin;
    private final SkinsRestorer skinsApi;
    private final Map<String, SkinProperty> realSkinCache;
    private final LongSupplier generation;

    public DeathHeadService(UHC_DBasic plugin, SkinsRestorer skinsApi, Map<String, SkinProperty> realSkinCache,
                            LongSupplier generation) {
        this.plugin = plugin;
        this.skinsApi = skinsApi;
        this.realSkinCache = realSkinCache;
        this.generation = generation;
    }

    public void applyOwnHead(Skull skull, Player victim) {
        String victimName = victim.getName();
        UUID victimId = victim.getUniqueId();
        UUID worldId = skull.getWorld().getUID();
        int x = skull.getX();
        int y = skull.getY();
        int z = skull.getZ();
        long requestedGeneration = generation.getAsLong();
        SkinProperty cached = realSkinCache.get(SkinAssignmentPolicy.key(victimName));
        if (cached != null) {
            setHeadProfile(worldId, x, y, z, victimId, victimName, cached);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> resolveHeadSkin(
                worldId, x, y, z, victimId, victimName, requestedGeneration));
    }

    private void resolveHeadSkin(UUID worldId, int x, int y, int z, UUID victimId,
                                 String victimName, long requestedGeneration) {
        try {
            Optional<InputDataResult> result = skinsApi.getSkinStorage().findOrCreateSkinData(victimName);
            if (result.isPresent()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (requestedGeneration != generation.getAsLong()) return;
                    realSkinCache.put(SkinAssignmentPolicy.key(victimName), result.get().getProperty());
                    setHeadProfile(worldId, x, y, z, victimId, victimName, result.get().getProperty());
                });
            }
        } catch (DataRequestException | MineSkinException e) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger()
                    .warning(() -> "Could not resolve head for " + victimName + ": " + e.getMessage()));
        }
    }

    private void setHeadProfile(UUID worldId, int x, int y, int z, UUID victimId,
                                String victimName, SkinProperty property) {
        try {
            if (Bukkit.getWorld(worldId) == null) return;
            Location loc = new Location(Bukkit.getWorld(worldId), x, y, z);
            if (!(loc.getBlock().getState() instanceof Skull skull)) return;
            if (loc.getBlock().getType() != Material.PLAYER_HEAD) return;
            PlayerProfile profile = Bukkit.createProfile(victimId, victimName);
            profile.setProperty(new ProfileProperty("textures", property.getValue(), property.getSignature()));
            skull.setProfile(ResolvableProfile.resolvableProfile(profile));
            skull.update();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(() -> "Invalid head profile for " + victimName);
        }
    }
}
