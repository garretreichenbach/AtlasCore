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
                    int mask = 0;
                    mask |= Sector.SectorMode.LOCK_NO_EXIT.code;
                    mask |= Sector.SectorMode.LOCK_NO_ENTER.code;
                    mask |= Sector.SectorMode.NO_FP_LOSS.code;
                    mask |= Sector.SectorMode.PROT_NO_ATTACK.code;
                    sector.setProtectionMode(mask);
                } catch(Exception exception) {
                    AtlasBuildSectors.getInstance().logException(
                        "Failed to lock sector for player " + playerName, exception);
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

    public static Vector3i calculateRandomSector() {
        int distanceMod = AtlasBuildSectors.BUILD_SECTOR_DISTANCE;
        Random random = new Random();
        int x = random.nextInt(distanceMod * 2) - distanceMod;
        int y = random.nextInt(distanceMod * 2) - distanceMod;
        int z = random.nextInt(distanceMod * 2) - distanceMod;
        return new Vector3i(x, y, z);
    }
}
