package atlas.buildsectors.data;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.network.packets.PacketUtil;
import api.utils.game.PlayerUtils;
import atlas.buildsectors.AtlasBuildSectors;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Sector;
import org.schema.game.server.data.ServerConfig;

import java.util.*;

/**
 * Manages build sector data for all players.
 *
 * @author TheDerpGamer
 */
public class BuildSectorDataManager extends DataManager<BuildSectorData> {

    private final Set<BuildSectorData> clientCache = new HashSet<>();
    private static BuildSectorDataManager serverInstance;
    private static BuildSectorDataManager clientInstance;

    public static BuildSectorDataManager getInstance(boolean server) {
        if(server) {
            if(serverInstance == null) serverInstance = new BuildSectorDataManager();
            return serverInstance;
        } else {
            if(clientInstance == null) {
                clientInstance = new BuildSectorDataManager();
                clientInstance.requestFromServer();
            }
            return clientInstance;
        }
    }

    @Override
    public Set<BuildSectorData> getServerCache() {
        List<Object> objects = PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), BuildSectorData.class);
        Set<BuildSectorData> data = new HashSet<>();
        for(Object object : objects) data.add((BuildSectorData) object);
        return data;
    }

    @Override
    public String getDataTypeName() {
        return "BUILD_SECTOR_DATA";
    }

    @Override
    public Set<BuildSectorData> getClientCache() {
        return Collections.unmodifiableSet(clientCache);
    }

    @Override
    public void addToClientCache(BuildSectorData data) {
        clientCache.add(data);
    }

    @Override
    public void removeFromClientCache(BuildSectorData data) {
        clientCache.remove(data);
    }

    @Override
    public void updateClientCache(BuildSectorData data) {
        clientCache.remove(data);
        clientCache.add(data);
    }

    @Override
    public void createMissingData(Object... args) {
        try {
            String playerName = args[0].toString();
            if(!dataExistsForPlayer(playerName, true)) {
                BuildSectorData sectorData = new BuildSectorData(playerName);
                PersistentObjectUtil.addObject(AtlasCore.getInstance().getSkeleton(), sectorData);
                PersistentObjectUtil.save(AtlasCore.getInstance().getSkeleton());

                try {
                    Sector sector = sectorData.getServerSector();
                    // LOCK_NO_ENTER / LOCK_NO_EXIT are no longer needed: MixinSectorSwitch
                    // enforces permission-based entry for all jump types.
                    // PROT_NO_ATTACK: prevents damage inside the build sector.
                    // NO_FP_LOSS:     prevents fleet-point penalties inside.
                    int mask = 0;
                    mask |= Sector.SectorMode.NO_FP_LOSS.code;
                    mask |= Sector.SectorMode.PROT_NO_ATTACK.code;
                    sector.setProtectionMode(mask);
                } catch(Exception exception) {
                    AtlasBuildSectors.getInstance().logException(
                        "Failed to configure sector for player " + playerName, exception);
                }
            }
        } catch(Exception exception) {
            AtlasBuildSectors.getInstance().logException(
                "An error occurred while initializing build sector data", exception);
        }
    }

    public boolean dataExistsForPlayer(String playerName, boolean server) {
        for(BuildSectorData data : (server ? getServerCache() : getClientCache())) {
            if(data.getOwner().equals(playerName)) return true;
        }
        return false;
    }

    public boolean isBuildSector(Vector3i sector) {
        for(BuildSectorData data : clientCache) {
            if(data.getSector().equals(sector)) return true;
        }
        return false;
    }

    public BuildSectorData getCurrentBuildSector(PlayerState playerState) {
        for(BuildSectorData data : clientCache) {
            if(data.getSector().equals(playerState.getCurrentSector())) return data;
        }
        return null;
    }

    public boolean isPlayerInAnyBuildSector(PlayerState playerState) {
        if(playerState == null) return false;
        return getCurrentBuildSector(playerState) != null;
    }

    public Set<BuildSectorData> getAccessibleSectors(PlayerState playerState) {
        Set<BuildSectorData> accessibleSectors = new HashSet<>();
        boolean server = playerState.isOnServer();
        for(BuildSectorData data : getCache(server)) {
            if(data.getPermissionsForUser(playerState.getName()) != null || data.getOwner().equals(playerState.getName()))
                accessibleSectors.add(data);
        }
        return accessibleSectors;
    }

    public BuildSectorData getFromPlayer(PlayerState player) {
        for(BuildSectorData data : getCache(player.isOnServer())) {
            if(data.getOwner().equals(player.getName())) return data;
        }
        return null;
    }

    public BuildSectorData getFromPlayerName(String playerName, boolean server) {
        for(BuildSectorData data : getCache(server)) {
            if(data.getOwner().equals(playerName)) return data;
        }
        return null;
    }

    public void enterBuildSector(org.schema.game.common.data.player.PlayerState player, BuildSectorData buildSectorData) {
        PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
            AtlasBuildSectors.ENTER_BUILD_SECTOR, player.getName(), buildSectorData.getUUID()));
    }

    public void leaveBuildSector(org.schema.game.common.data.player.PlayerState player) {
        PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
            AtlasBuildSectors.LEAVE_BUILD_SECTOR, player.getName()));
    }

    /**
     * Returns the {@link BuildSectorData} whose owner name matches {@code uuid},
     * searching the server or client cache as specified.
     */
    public BuildSectorData getFromUUID(String uuid, boolean server) {
        for(BuildSectorData data : getCache(server)) {
            if(data.getUUID().equals(uuid)) return data;
        }
        return null;
    }

    /**
     * Returns the {@link BuildSectorData} for the given sector coordinates, or
     * {@code null} if the coordinates don't correspond to any build sector.
     */
    public BuildSectorData getFromSector(Vector3i coord, boolean server) {
        for(BuildSectorData data : getCache(server)) {
            if(data.getSector().equals(coord)) return data;
        }
        return null;
    }

    /**
     * Returns {@code true} if the given sector coordinates belong to any registered
     * build sector. Uses the server cache when called server-side.
     */
    public boolean isBuildSectorCoord(Vector3i coord, boolean server) {
        return getFromSector(coord, server) != null;
    }

    /**
     * Generates a random sector coordinate that:
     * <ul>
     *   <li>is at least {@code BUILD_SECTOR_DISTANCE / 2} away from the origin in every axis</li>
     *   <li>does not collide with any existing build sector</li>
     * </ul>
     * Retries up to 1 000 times before giving up (should never be reached in practice).
     */
    public static Vector3i calculateRandomSector() {
        int dist = AtlasBuildSectors.BUILD_SECTOR_DISTANCE;
        int minAxis = dist / 2; // keep sectors well away from normal space
        Random random = new Random();
        BuildSectorDataManager mgr = getInstance(true);
        for(int attempt = 0; attempt < 1000; attempt++) {
            int x = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
            int y = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
            int z = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
            Vector3i candidate = new Vector3i(x, y, z);
            if(!mgr.isBuildSectorCoord(candidate, true)) return candidate;
        }
        // Extreme fallback — should never happen with BUILD_SECTOR_DISTANCE = 1_000_000
        AtlasBuildSectors.getInstance().logWarning("calculateRandomSector: exhausted 1000 attempts, using unverified coordinates.");
        int x = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
        int y = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
        int z = (random.nextBoolean() ? 1 : -1) * (minAxis + random.nextInt(dist - minAxis));
        return new Vector3i(x, y, z);
    }
}
