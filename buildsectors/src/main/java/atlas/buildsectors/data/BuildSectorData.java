package atlas.buildsectors.data;

import api.common.GameCommon;
import api.common.GameServer;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import atlas.buildsectors.AtlasBuildSectors;
import atlas.buildsectors.gui.BuildSectorEntityScrollableList;
import atlas.core.data.SerializableData;
import atlas.core.utils.EntityUtils;
import atlas.core.utils.PlayerUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.controller.rails.RailRelation;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.schine.network.objects.Sendable;

import java.io.IOException;
import java.util.*;

/**
 * Data model for a player's private build sector.
 *
 * @author TheDerpGamer
 */
public class BuildSectorData extends SerializableData {

    public static final int OWNER = 0;
    public static final int FRIEND = 1;
    public static final int OTHER = 2;
    private static final byte VERSION = 0;

    protected String owner = "";
    protected Vector3i sector = new Vector3i();
    protected Set<BuildSectorEntityData> entities = new HashSet<>();
    protected HashMap<String, HashMap<PermissionTypes, Boolean>> permissions = new HashMap<>();

    public BuildSectorData(String owner) {
        super("BUILD_SECTOR_DATA");
        this.owner = owner;
        sector = BuildSectorDataManager.calculateRandomSector();
        addPlayer(owner, OWNER, true);
    }

    public BuildSectorData(PacketReadBuffer readBuffer) throws IOException {
        super("BUILD_SECTOR_DATA");
        deserializeNetwork(readBuffer);
    }

    public BuildSectorData(JSONObject data) {
        super("BUILD_SECTOR_DATA");
        deserialize(data);
    }

    @Override
    public String getDataTypeName() {
        return "BUILD_SECTOR_DATA";
    }

    @Override
    public int hashCode() {
        return sector.hashCode() + dataUUID.hashCode();
    }

    @Override
    public JSONObject serialize() {
        JSONObject data = new JSONObject();
        data.put("version", VERSION);
        data.put("uuid", getUUID());
        data.put("owner", owner);
        JSONObject sectorData = new JSONObject();
        sectorData.put("x", sector.x);
        sectorData.put("y", sector.y);
        sectorData.put("z", sector.z);
        data.put("sector", sectorData);
        JSONArray entitiesArray = new JSONArray();
        for(BuildSectorEntityData entity : entities) entitiesArray.put(entity.serialize());
        data.put("entities", entitiesArray);
        JSONArray permissionsArray = new JSONArray();
        for(String username : permissions.keySet()) {
            for(Map.Entry<PermissionTypes, Boolean> permission : permissions.get(username).entrySet()) {
                JSONObject permissionData = new JSONObject();
                permissionData.put("name", username);
                permissionData.put(permission.getKey().name(), permission.getValue());
                permissionsArray.put(permissionData);
            }
        }
        data.put("permissions", permissionsArray);
        return data;
    }

    @Override
    public void deserialize(JSONObject data) {
        sector = new Vector3i();
        entities = new HashSet<>();
        permissions = new HashMap<>();
        dataUUID = data.getString("uuid");
        owner = data.getString("owner");
        JSONObject sectorData = data.getJSONObject("sector");
        sector.set(sectorData.getInt("x"), sectorData.getInt("y"), sectorData.getInt("z"));
        JSONArray entitiesArray = data.getJSONArray("entities");
        for(int i = 0; i < entitiesArray.length(); i++) entities.add(new BuildSectorEntityData(entitiesArray.getJSONObject(i)));
        JSONArray permissionsArray = data.getJSONArray("permissions");
        for(int i = 0; i < permissionsArray.length(); i++) {
            JSONObject permissionData = permissionsArray.getJSONObject(i);
            String username = permissionData.getString("name");
            // Get-or-create the map for this user so multiple entries accumulate correctly.
            HashMap<PermissionTypes, Boolean> permMap = permissions.computeIfAbsent(username, k -> new HashMap<>());
            for(PermissionTypes type : PermissionTypes.values()) {
                if(permissionData.has(type.name())) {
                    permMap.put(type, permissionData.getBoolean(type.name()));
                }
            }
        }
    }

    @Override
    public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeByte(VERSION);
        writeBuffer.writeString(dataUUID);
        writeBuffer.writeString(owner);
        writeBuffer.writeInt(sector.x);
        writeBuffer.writeInt(sector.y);
        writeBuffer.writeInt(sector.z);
        if(entities.isEmpty()) writeBuffer.writeBoolean(false);
        else {
            writeBuffer.writeBoolean(true);
            writeBuffer.writeInt(entities.size());
            for(BuildSectorEntityData entity : entities) entity.serializeNetwork(writeBuffer);
        }
        if(permissions.isEmpty()) writeBuffer.writeBoolean(false);
        else {
            writeBuffer.writeBoolean(true);
            writeBuffer.writeInt(permissions.size());
            for(String username : permissions.keySet()) {
                writeBuffer.writeString(username);
                if(permissions.get(username) == null || permissions.get(username).isEmpty()) writeBuffer.writeBoolean(false);
                else {
                    writeBuffer.writeBoolean(true);
                    writeBuffer.writeInt(permissions.get(username).size());
                    for(Map.Entry<PermissionTypes, Boolean> permission : permissions.get(username).entrySet()) {
                        writeBuffer.writeString(permission.getKey().name());
                        writeBuffer.writeBoolean(permission.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
        sector = new Vector3i();
        entities = new HashSet<>();
        permissions = new HashMap<>();
        readBuffer.readByte(); // version
        dataUUID = readBuffer.readString();
        owner = readBuffer.readString();
        sector = new Vector3i(readBuffer.readInt(), readBuffer.readInt(), readBuffer.readInt());
        if(readBuffer.readBoolean()) {
            int entityCount = readBuffer.readInt();
            for(int i = 0; i < entityCount; i++) entities.add(new BuildSectorEntityData(readBuffer));
        }
        if(readBuffer.readBoolean()) {
            int permissionCount = readBuffer.readInt();
            for(int i = 0; i < permissionCount; i++) {
                String username = readBuffer.readString();
                if(!readBuffer.readBoolean()) {
                    permissions.put(username, new HashMap<PermissionTypes, Boolean>());
                } else {
                    int permissionSize = readBuffer.readInt();
                    HashMap<PermissionTypes, Boolean> permissionMap = new HashMap<>();
                    for(int j = 0; j < permissionSize; j++) {
                        PermissionTypes type = PermissionTypes.valueOf(readBuffer.readString());
                        permissionMap.put(type, readBuffer.readBoolean());
                    }
                    permissions.put(username, permissionMap);
                }
            }
        }
    }

    public static SegmentController getEntity(String entityUID) {
        for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
            if(sendable instanceof SegmentController && ((SegmentController) sendable).getUniqueIdentifier().equals(entityUID)) {
                return (SegmentController) sendable;
            }
        }
        return null;
    }

    public Vector3i getSector() { return sector; }

    public String getOwner() { return owner; }

    public void addPlayer(String name, int type, boolean server) {
        setDefaultPerms(name, type);
        BuildSectorDataManager.getInstance(server).updateData(this, server);
    }

    public void removePlayer(String name, boolean server) {
        permissions.remove(name);
        BuildSectorDataManager.getInstance(server).updateData(this, server);
    }

    public boolean getPermission(String user, PermissionTypes type) {
        HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
        if(permissionMap != null) {
            Boolean value = permissionMap.get(type);
            return value != null && value;
        }
        return false;
    }

    public void setPermission(String user, PermissionTypes type, boolean value, boolean server) {
        HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
        if(permissionMap != null) permissionMap.put(type, value);
        else {
            HashMap<PermissionTypes, Boolean> newMap = new HashMap<>();
            newMap.put(type, value);
            permissions.put(user, newMap);
        }
        BuildSectorDataManager.getInstance(server).updateData(this, server);
    }

    public boolean getPermissionForEntity(String user, String entityUID, PermissionTypes... types) {
        SegmentController entity = getEntity(entityUID);
        if(entity == null || !entity.existsInState()) return false;
        BuildSectorEntityData entityData = getEntityData(entity);
        if(entityData != null) {
            for(PermissionTypes type : types) {
                if(entityData.getPermission(user, type)) return true;
            }
        }
        return false;
    }

    public boolean getPermissionForEntityOrGlobal(String user, String entityUID, PermissionTypes type) {
        SegmentController entity = getEntity(entityUID);
        if(entity == null || !entity.existsInState()) return false;
        switch(type) {
            case EDIT_SPECIFIC:
                return getPermissionForEntity(user, entityUID, PermissionTypes.EDIT_SPECIFIC, PermissionTypes.EDIT_ANY) || (getPermission(user, PermissionTypes.EDIT_OWN) && entity.getSpawner().equals(user));
            case DELETE_SPECIFIC:
                return getPermissionForEntity(user, entityUID, PermissionTypes.DELETE_SPECIFIC, PermissionTypes.DELETE_ANY) || (getPermission(user, PermissionTypes.DELETE_OWN) && entity.getSpawner().equals(user));
            case TOGGLE_AI_SPECIFIC:
                return getPermissionForEntity(user, entityUID, PermissionTypes.TOGGLE_AI_SPECIFIC, PermissionTypes.TOGGLE_AI_ANY) || (getPermission(user, PermissionTypes.TOGGLE_AI_OWN) && entity.getSpawner().equals(user));
            case TOGGLE_DAMAGE_SPECIFIC:
                return getPermissionForEntity(user, entityUID, PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, PermissionTypes.TOGGLE_DAMAGE_ANY) || (getPermission(user, PermissionTypes.TOGGLE_DAMAGE_OWN) && entity.getSpawner().equals(user));
            case EDIT_ENTITY_PERMISSIONS:
                return getPermissionForEntity(user, entityUID, PermissionTypes.EDIT_ENTITY_PERMISSIONS) || getPermission(user, PermissionTypes.EDIT_PERMISSIONS);
            default:
                return getPermissionForEntity(user, entityUID, type);
        }
    }

    public Set<BuildSectorEntityData> getEntities() {
        if(entities == null) entities = new HashSet<>();
        if(GameCommon.isOnSinglePlayer() || GameCommon.isClientConnectedToServer()) {
            doEntityUpdateCheck();
        } else {
            prune(); // server-side: safe prune only, no entity scan
        }
        return entities;
    }

    /**
     * Client-side refresh: prunes stale entity entries then scans the local object
     * container for any entities in this sector that aren't yet registered.
     *
     * <p><strong>Only call from the client.</strong> On the server, call {@link #prune()}
     * directly — the GUI list and the local-object scan are not available there.
     */
    public void doEntityUpdateCheck() {
        prune();
        // Entity-scan is client-side only: the server doesn't have a local GUI object list.
        if(!GameCommon.isOnSinglePlayer() && !GameCommon.isClientConnectedToServer()) return;
        for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
            if(sendable instanceof SegmentController) {
                SegmentController entity = (SegmentController) sendable;
                if(entity.getSector(new Vector3i()).equals(sector) && entity.existsInState() && getEntityData(entity) == null) {
                    // Pass server=false: this is a client-side cache update only.
                    addEntity(entity, false);
                }
            }
        }
    }

    /**
     * Removes entity entries whose underlying entity is null or has left this sector.
     * Safe to call on both client and server (no GUI side-effects).
     */
    public void prune() {
        ObjectArrayList<BuildSectorEntityData> toRemove = new ObjectArrayList<>();
        for(BuildSectorEntityData entityData : entities) {
            if(entityData.getEntity() == null || !entityData.getEntity().getSector(new Vector3i()).equals(sector)) {
                toRemove.add(entityData);
            }
        }
        if(!toRemove.isEmpty()) {
            for(BuildSectorEntityData entityData : toRemove) entities.remove(entityData);
            // Notify the GUI list only if we're on the client and it exists.
            BuildSectorEntityScrollableList.update();
        }
    }

    public BuildSectorEntityData getEntityData(SegmentController entity) {
        if(entities == null) entities = new HashSet<>();
        if(entity == null) return null;
        for(BuildSectorEntityData entityData : entities) {
            if(entityData.getEntity() != null && entityData.getEntity().equals(entity)) return entityData;
        }
        return null;
    }

    public void addEntity(SegmentController entity, boolean server) {
        BuildSectorEntityData data = new BuildSectorEntityData(entity);
        data.setDefaultEntityPerms(owner, OWNER);
        entities.add(data);
        BuildSectorDataManager.getInstance(server).updateData(this, server);
    }

    public void removeEntity(SegmentController entity, boolean server) {
        if(entities == null) entities = new HashSet<>();
        BuildSectorEntityData toRemove = null;
        for(BuildSectorEntityData entityData : entities) {
            if(entityData.getEntity() == null) continue;
            if(entityData.getEntity().equals(entity)) {
                toRemove = entityData;
                break;
            }
        }
        if(toRemove != null) {
            entities.remove(toRemove);
            BuildSectorDataManager.getInstance(server).updateData(this, server);
        }
    }

    public void updateEntity(SegmentController entity, boolean server) {
        BuildSectorEntityData entityData = getEntityData(entity);
        if(entityData == null) addEntity(entity, server);
        else {
            entityData.entityUID = entity.getUniqueIdentifier();
            entityData.entityType = EntityType.fromEntity(entity);
            BuildSectorDataManager.getInstance(server).updateData(this, server);
        }
    }

    public void spawnEntity(BlueprintEntry blueprint, PlayerState spawner, boolean onDock, String name) {
        spawnEntity(blueprint, spawner, onDock, name, spawner.getFactionId());
    }

    public void spawnEntity(BlueprintEntry blueprint, PlayerState spawner, boolean onDock, String name, int factionId) {
        assert spawner.isOnServer() : "Cannot spawn entity on client";
        try {
            SegmentPiece dockPiece = onDock ? PlayerUtils.getBlockLookingAt(spawner) : null;
            SegmentController entity = EntityUtils.spawnEntryOnDock(spawner, blueprint, name, factionId, dockPiece);
            addEntity(entity, true);
            if(factionId == FactionManager.PIRATES_ID) toggleEntityAI(entity, false);
        } catch(Exception exception) {
            AtlasBuildSectors.getInstance().logException("An error occurred while spawning entity", exception);
        }
    }

    public void toggleEntityAI(SegmentController entity, boolean value) {
        EntityUtils.toggleAI(entity, value);
    }

    public Sector getServerSector() throws Exception {
        return GameServer.getServerState().getUniverse().getSector(sector);
    }

    public Set<String> getAllUsers() {
        if(permissions == null) permissions = new HashMap<>();
        return new HashSet<>(permissions.keySet());
    }

    public HashMap<PermissionTypes, Boolean> getPermissionsForEntity(String entityUID, String username) {
        BuildSectorEntityData entityData = getEntityData(getEntity(entityUID));
        return entityData != null ? entityData.permissions.get(username) : null;
    }

    public HashMap<PermissionTypes, Boolean> getPermissionsForUser(String username) {
        if(permissions == null) permissions = new HashMap<>();
        return permissions.get(username);
    }

    public void setPermissionForEntity(String entityUID, String username, PermissionTypes type, boolean value, boolean server) {
        BuildSectorEntityData entityData = getEntityData(getEntity(entityUID));
        if(entityData != null) entityData.setPermission(username, type, value, server);
    }

    private void setDefaultPerms(String user, int type) {
        if(permissions == null) permissions = new HashMap<>();
        switch(type) {
            case OWNER:
                permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                    put(PermissionTypes.EDIT_OWN, true); put(PermissionTypes.EDIT_ANY, true);
                    put(PermissionTypes.SPAWN, true); put(PermissionTypes.SPAWN_ENEMIES, true);
                    put(PermissionTypes.DELETE_OWN, true); put(PermissionTypes.DELETE_ANY, true);
                    put(PermissionTypes.TOGGLE_AI_OWN, true); put(PermissionTypes.TOGGLE_AI_ANY, true);
                    put(PermissionTypes.TOGGLE_DAMAGE_OWN, true); put(PermissionTypes.TOGGLE_DAMAGE_ANY, true);
                    put(PermissionTypes.INVITE, true); put(PermissionTypes.KICK, true);
                    put(PermissionTypes.EDIT_PERMISSIONS, true);
                }});
                break;
            case FRIEND:
                permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                    put(PermissionTypes.EDIT_OWN, true); put(PermissionTypes.EDIT_ANY, true);
                    put(PermissionTypes.SPAWN, true); put(PermissionTypes.SPAWN_ENEMIES, false);
                    put(PermissionTypes.DELETE_OWN, true); put(PermissionTypes.DELETE_ANY, false);
                    put(PermissionTypes.TOGGLE_AI_OWN, true); put(PermissionTypes.TOGGLE_AI_ANY, false);
                    put(PermissionTypes.TOGGLE_DAMAGE_OWN, true); put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
                    put(PermissionTypes.INVITE, true); put(PermissionTypes.KICK, false);
                    put(PermissionTypes.EDIT_PERMISSIONS, false);
                }});
                break;
            case OTHER:
                permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                    put(PermissionTypes.EDIT_OWN, true); put(PermissionTypes.EDIT_ANY, false);
                    put(PermissionTypes.SPAWN, true); put(PermissionTypes.SPAWN_ENEMIES, false);
                    put(PermissionTypes.DELETE_OWN, false); put(PermissionTypes.DELETE_ANY, false);
                    put(PermissionTypes.TOGGLE_AI_OWN, false); put(PermissionTypes.TOGGLE_AI_ANY, false);
                    put(PermissionTypes.TOGGLE_DAMAGE_OWN, false); put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
                    put(PermissionTypes.INVITE, false); put(PermissionTypes.KICK, false);
                    put(PermissionTypes.EDIT_PERMISSIONS, false);
                }});
                break;
        }
    }

    // ── EntityType ────────────────────────────────────────────────────────────

    public enum EntityType {
        SHIP, STATION, DOCKED, TURRET;

        public static EntityType fromEntity(SegmentController entity) {
            if(entity.getType() == SimpleTransformableSendableObject.EntityType.SHIP) {
                if(entity.railController.isTurretDocked()) return TURRET;
                else if(entity.isDocked()) return DOCKED;
                else return SHIP;
            } else return STATION;
        }
    }

    // ── PermissionTypes ───────────────────────────────────────────────────────

    public enum PermissionTypes {
        EDIT_SPECIFIC("Edit Specific Ship", "Whether the player can edit a specific ship."),
        EDIT_OWN("Edit Own Ships", "Whether the player can edit their own ships."),
        EDIT_ANY("Edit Other Ships", "Whether the player can edit ships owned by other players."),
        SPAWN("Spawn Ships", "Whether the player can spawn ships from their catalog."),
        SPAWN_ENEMIES("Spawn Enemies", "Whether the player can spawn enemy ships."),
        DELETE_SPECIFIC("Delete Specific Ship", "Whether the player can delete a specific ship."),
        DELETE_OWN("Delete Own Ships", "Whether the player can delete their own ships."),
        DELETE_ANY("Delete Other Ships", "Whether the player can delete ships owned by other players."),
        TOGGLE_AI_SPECIFIC("Toggle Specific AI", "Whether the player can toggle AI on a specific ship."),
        TOGGLE_AI_OWN("Toggle Own AI", "Whether the player can toggle AI on their own ships."),
        TOGGLE_AI_ANY("Toggle Other AI", "Whether the player can toggle AI on ships owned by other players."),
        TOGGLE_DAMAGE_SPECIFIC("Toggle Specific Damage", "Whether the player can toggle damage on a specific ship."),
        TOGGLE_DAMAGE_OWN("Toggle Own Damage", "Whether the player can toggle damage on their own ships."),
        TOGGLE_DAMAGE_ANY("Toggle Other Damage", "Whether the player can toggle damage on ships owned by other players."),
        INVITE("Invite Players", "Whether the player can invite other players to the sector."),
        KICK("Kick Players", "Whether the player can kick other players from the sector."),
        EDIT_PERMISSIONS("Edit Permissions", "Whether the player can edit permissions for other players."),
        EDIT_ENTITY_PERMISSIONS("Edit Entity Permissions", "Whether the player can edit permissions for specific entities.");

        private final String display;
        private final String description;

        PermissionTypes(String display, String description) {
            this.display = display;
            this.description = description;
        }

        public String getDisplay() { return display; }
        public String getDescription() { return description; }

        public static Set<PermissionTypes> getListValues() {
            return new HashSet<>(Arrays.asList(
                EDIT_OWN, EDIT_ANY, SPAWN, SPAWN_ENEMIES,
                DELETE_OWN, DELETE_ANY, TOGGLE_AI_OWN, TOGGLE_AI_ANY,
                TOGGLE_DAMAGE_OWN, TOGGLE_DAMAGE_ANY, INVITE, KICK, EDIT_PERMISSIONS
            ));
        }

        public static Set<PermissionTypes> getEntitySpecificValues() {
            return new HashSet<>(Arrays.asList(
                EDIT_SPECIFIC, DELETE_SPECIFIC, TOGGLE_AI_SPECIFIC, TOGGLE_DAMAGE_SPECIFIC, EDIT_ENTITY_PERMISSIONS
            ));
        }
    }

    // ── BuildSectorEntityData ─────────────────────────────────────────────────

    public class BuildSectorEntityData extends SerializableData {

        private static final byte VERSION = 0;

        private String entityUID;
        private EntityType entityType;
        protected HashMap<String, HashMap<PermissionTypes, Boolean>> permissions = new HashMap<>();
        private boolean invulnerable = true;

        public BuildSectorEntityData(SegmentController entity) {
            super("BUILD_SECTOR_ENTITY_DATA");
            entityUID = entity.getUniqueIdentifier();
            entityType = EntityType.fromEntity(entity);
            setDefaultEntityPerms(entity.getSpawner(), OWNER);
            setInvulnerable(true, entity.isOnServer());
        }

        public BuildSectorEntityData(PacketReadBuffer readBuffer) throws IOException {
            super("BUILD_SECTOR_ENTITY_DATA");
            deserializeNetwork(readBuffer);
        }

        public BuildSectorEntityData(JSONObject data) {
            super("BUILD_SECTOR_ENTITY_DATA");
            deserialize(data);
        }

        @Override
        public String getDataTypeName() { return "BUILD_SECTOR_ENTITY_DATA"; }

        @Override
        public JSONObject serialize() {
            JSONObject data = new JSONObject();
            data.put("version", VERSION);
            data.put("uuid", getUUID());
            data.put("entityUID", entityUID);
            data.put("entityType", entityType.name());
            JSONArray permissionsArray = new JSONArray();
            for(String name : permissions.keySet()) {
                for(Map.Entry<PermissionTypes, Boolean> permission : permissions.get(name).entrySet()) {
                    JSONObject permissionData = new JSONObject();
                    permissionData.put("name", name);
                    permissionData.put(permission.getKey().name(), permission.getValue());
                    permissionsArray.put(permissionData);
                }
            }
            data.put("permissions", permissionsArray);
            data.put("invulnerable", invulnerable);
            return data;
        }

        @Override
        public void deserialize(JSONObject data) {
            permissions = new HashMap<>();
            dataUUID = data.getString("uuid");
            entityUID = data.getString("entityUID");
            entityType = EntityType.valueOf(data.getString("entityType"));
            JSONArray permissionsArray = data.getJSONArray("permissions");
            for(int i = 0; i < permissionsArray.length(); i++) {
                JSONObject permissionData = permissionsArray.getJSONObject(i);
                String name = permissionData.getString("name");
                // Get-or-create so all entries for the same user accumulate correctly.
                HashMap<PermissionTypes, Boolean> permMap = permissions.computeIfAbsent(name, k -> new HashMap<>());
                for(PermissionTypes type : PermissionTypes.values()) {
                    if(permissionData.has(type.name())) {
                        permMap.put(type, permissionData.getBoolean(type.name()));
                    }
                }
            }
            invulnerable = data.getBoolean("invulnerable");
        }

        @Override
        public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
            writeBuffer.writeByte(VERSION);
            writeBuffer.writeString(dataUUID);
            writeBuffer.writeString(entityUID);
            writeBuffer.writeString(entityType.name());
            writeBuffer.writeInt(permissions.size());
            for(String name : permissions.keySet()) {
                writeBuffer.writeString(name);
                writeBuffer.writeInt(permissions.get(name).size());
                for(Map.Entry<PermissionTypes, Boolean> permission : permissions.get(name).entrySet()) {
                    writeBuffer.writeString(permission.getKey().name());
                    writeBuffer.writeBoolean(permission.getValue());
                }
            }
            writeBuffer.writeBoolean(invulnerable);
        }

        @Override
        public void deserializeNetwork(PacketReadBuffer readBuffer) throws IOException {
            permissions = new HashMap<>();
            readBuffer.readByte(); // version
            dataUUID = readBuffer.readString();
            entityUID = readBuffer.readString();
            entityType = EntityType.valueOf(readBuffer.readString().toUpperCase(Locale.ENGLISH));
            int permissionCount = readBuffer.readInt();
            for(int i = 0; i < permissionCount; i++) {
                String name = readBuffer.readString();
                int permissionSize = readBuffer.readInt();
                HashMap<PermissionTypes, Boolean> permissionMap = new HashMap<>();
                for(int j = 0; j < permissionSize; j++) {
                    PermissionTypes type = PermissionTypes.valueOf(readBuffer.readString().toUpperCase(Locale.ENGLISH));
                    permissionMap.put(type, readBuffer.readBoolean());
                }
                permissions.put(name, permissionMap);
            }
            invulnerable = readBuffer.readBoolean();
        }

        void setDefaultEntityPerms(String user, int type) {
            switch(type) {
                case OWNER:
                    permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                        put(PermissionTypes.EDIT_SPECIFIC, true); put(PermissionTypes.DELETE_SPECIFIC, true);
                        put(PermissionTypes.TOGGLE_AI_SPECIFIC, true); put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, true);
                        put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, true); put(PermissionTypes.EDIT_OWN, true);
                        put(PermissionTypes.EDIT_ANY, true); put(PermissionTypes.SPAWN, true);
                        put(PermissionTypes.SPAWN_ENEMIES, true); put(PermissionTypes.DELETE_OWN, true);
                        put(PermissionTypes.DELETE_ANY, true); put(PermissionTypes.TOGGLE_AI_OWN, true);
                        put(PermissionTypes.TOGGLE_AI_ANY, true); put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
                        put(PermissionTypes.TOGGLE_DAMAGE_ANY, true); put(PermissionTypes.INVITE, true);
                        put(PermissionTypes.KICK, true); put(PermissionTypes.EDIT_PERMISSIONS, true);
                    }});
                    break;
                case FRIEND:
                    permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                        put(PermissionTypes.EDIT_SPECIFIC, true); put(PermissionTypes.DELETE_SPECIFIC, true);
                        put(PermissionTypes.TOGGLE_AI_SPECIFIC, true); put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, true);
                        put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, false); put(PermissionTypes.EDIT_OWN, true);
                        put(PermissionTypes.EDIT_ANY, true); put(PermissionTypes.SPAWN, true);
                        put(PermissionTypes.SPAWN_ENEMIES, false); put(PermissionTypes.DELETE_OWN, true);
                        put(PermissionTypes.DELETE_ANY, false); put(PermissionTypes.TOGGLE_AI_OWN, true);
                        put(PermissionTypes.TOGGLE_AI_ANY, false); put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
                        put(PermissionTypes.TOGGLE_DAMAGE_ANY, false); put(PermissionTypes.INVITE, true);
                        put(PermissionTypes.KICK, false); put(PermissionTypes.EDIT_PERMISSIONS, false);
                    }});
                    break;
                case OTHER:
                    permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
                        put(PermissionTypes.EDIT_SPECIFIC, false); put(PermissionTypes.DELETE_SPECIFIC, false);
                        put(PermissionTypes.TOGGLE_AI_SPECIFIC, false); put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, false);
                        put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, false); put(PermissionTypes.EDIT_OWN, true);
                        put(PermissionTypes.EDIT_ANY, false); put(PermissionTypes.SPAWN, true);
                        put(PermissionTypes.SPAWN_ENEMIES, false); put(PermissionTypes.DELETE_OWN, false);
                        put(PermissionTypes.DELETE_ANY, false); put(PermissionTypes.TOGGLE_AI_OWN, false);
                        put(PermissionTypes.TOGGLE_AI_ANY, false); put(PermissionTypes.TOGGLE_DAMAGE_OWN, false);
                        put(PermissionTypes.TOGGLE_DAMAGE_ANY, false); put(PermissionTypes.INVITE, false);
                        put(PermissionTypes.KICK, false); put(PermissionTypes.EDIT_PERMISSIONS, false);
                    }});
                    break;
            }
        }

        public String getEntityUID() { return entityUID; }
        public SegmentController getEntity() { return BuildSectorData.getEntity(entityUID); }
        public EntityType getEntityType() { return entityType; }
        public String getSpawner() {
            SegmentController e = getEntity();
            return e != null ? e.getSpawner() : "";
        }

        public boolean getPermission(String user, PermissionTypes type) {
            HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
            if(permissionMap != null) {
                Boolean value = permissionMap.get(type);
                return value != null && value;
            }
            return false;
        }

        public void setPermission(String user, PermissionTypes type, boolean value, boolean server) {
            HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
            if(permissionMap != null) permissionMap.put(type, value);
            else {
                HashMap<PermissionTypes, Boolean> newMap = new HashMap<>();
                newMap.put(type, value);
                permissions.put(user, newMap);
            }
            BuildSectorDataManager.getInstance(server).updateData(BuildSectorData.this, server);
        }

        public boolean isAIActive() {
            if(getEntity() instanceof Ship) {
                Ship ship = (Ship) getEntity();
                return ship.getAiConfiguration().get(Types.ACTIVE).isOn();
            } else if(getEntity() instanceof SpaceStation) {
                SpaceStation station = (SpaceStation) getEntity();
                return station.getAiConfiguration().get(Types.ACTIVE).isOn();
            }
            return false;
        }

        public void setAIActive(boolean value) {
            try {
                if(getEntity() instanceof Ship) {
                    Ship ship = (Ship) getEntity();
                    ship.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
                    for(RailRelation docked : ship.railController.next) {
                        if(docked.docked.getSegmentController() instanceof Ship)
                            setAiRecursive((Ship) docked.docked.getSegmentController(), value);
                    }
                } else if(getEntity() instanceof SpaceStation) {
                    SpaceStation station = (SpaceStation) getEntity();
                    station.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
                    for(RailRelation docked : station.railController.next) {
                        if(docked.docked.getSegmentController() instanceof Ship)
                            setAiRecursive((Ship) docked.docked.getSegmentController(), value);
                    }
                }
            } catch(Exception exception) {
                AtlasBuildSectors.getInstance().logException("An error occurred while setting AI for entity", exception);
            }
        }

        private void setAiRecursive(Ship ship, boolean value) {
            try {
                ship.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
                for(RailRelation docked : ship.railController.next) {
                    if(docked.docked.getSegmentController() instanceof Ship)
                        setAiRecursive((Ship) docked.docked.getSegmentController(), value);
                }
            } catch(Exception exception) {
                AtlasBuildSectors.getInstance().logException("An error occurred while setting AI for entity", exception);
            }
        }

        public void delete() {
            SegmentController entity = getEntity();
            if(entity != null) EntityUtils.delete(entity);
        }

        public void deleteTurrets() {
            for(RailRelation docked : getEntity().railController.next) {
                if(docked.docked.getSegmentController() instanceof Ship)
                    EntityUtils.delete(docked.docked.getSegmentController());
            }
        }

        public boolean isInvulnerable() { return invulnerable; }

        public void setInvulnerable(boolean invulnerable, boolean server) {
            this.invulnerable = invulnerable;
            if(getEntity() != null) getEntity().setVulnerable(!invulnerable);
            BuildSectorDataManager.getInstance(server).updateData(BuildSectorData.this, server);
        }
    }
}
