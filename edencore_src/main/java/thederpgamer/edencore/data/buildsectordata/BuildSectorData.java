package thederpgamer.edencore.data.buildsectordata;

import api.common.GameCommon;
import api.common.GameServer;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
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
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.SerializableData;
import thederpgamer.edencore.data.playerdata.PlayerData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;
import thederpgamer.edencore.gui.buildsectormenu.BuildSectorEntityScrollableList;
import thederpgamer.edencore.utils.EntityUtils;
import thederpgamer.edencore.utils.PlayerUtils;

import java.io.IOException;
import java.util.*;

/**
 * [Description]
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
		super(DataType.BUILD_SECTOR_DATA);
		this.owner = owner;
		sector = BuildSectorDataManager.calculateRandomSector();
		addPlayer(owner, OWNER, true);
	}

	public BuildSectorData(PacketReadBuffer readBuffer) throws IOException {
		deserializeNetwork(readBuffer);
		dataType = DataType.BUILD_SECTOR_DATA;
	}

	public BuildSectorData(JSONObject data) {
		deserialize(data);
		dataType = DataType.BUILD_SECTOR_DATA;
	}

	@Override
	public int hashCode() {
		return sector.hashCode() + dataUUID.hashCode();
	}

	@Override
	public JSONObject serialize() {
		doEntityUpdateCheck();
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
		byte version = (byte) data.getInt("version");
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
			for(PermissionTypes type : PermissionTypes.values()) {
				if(permissionData.has(type.name())) {
					boolean value = permissionData.getBoolean(type.name());
					HashMap<PermissionTypes, Boolean> permission = new HashMap<>();
					permission.put(type, value);
					permissions.put(username, permission);
				}
			}
		}
	}

	@Override
	public void serializeNetwork(PacketWriteBuffer writeBuffer) throws IOException {
		doEntityUpdateCheck();
		writeBuffer.writeByte(VERSION);
		writeBuffer.writeString(dataUUID);
		writeBuffer.writeString(owner);
		writeBuffer.writeInt(sector.x); // Write the x coordinate of the sector
		writeBuffer.writeInt(sector.y); // Write the y coordinate of the sector
		writeBuffer.writeInt(sector.z); // Write the z coordinate of the sector
		if(entities.isEmpty()) writeBuffer.writeBoolean(false);
		else {
			writeBuffer.writeBoolean(true); // Indicate that there are entities to serialize
			writeBuffer.writeInt(entities.size());
			for(BuildSectorEntityData entity : entities) {
				entity.serializeNetwork(writeBuffer); // Serialize each entity
			}
		}
		if(permissions.isEmpty()) writeBuffer.writeBoolean(false);
		else {
			writeBuffer.writeBoolean(true); // Indicate that there are permissions to serialize
			writeBuffer.writeInt(permissions.size());
			for(String username : permissions.keySet()) {
				writeBuffer.writeString(username); // Write the username
				if(permissions.get(username) == null || permissions.get(username).isEmpty()) writeBuffer.writeBoolean(false);
				else {
					writeBuffer.writeBoolean(true); // Indicate that there are permissions for this user
					writeBuffer.writeInt(permissions.get(username).size()); // Write the number of permissions for this user
					for(Map.Entry<PermissionTypes, Boolean> permission : permissions.get(username).entrySet()) {
						writeBuffer.writeString(permission.getKey().name()); // Write the permission type
						writeBuffer.writeBoolean(permission.getValue()); // Write the permission value
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
		byte version = readBuffer.readByte();
		dataUUID = readBuffer.readString();
		dataType = DataType.BUILD_SECTOR_DATA; // Set the data type for consistency
		owner = readBuffer.readString();
		sector = new Vector3i(readBuffer.readInt(), readBuffer.readInt(), readBuffer.readInt()); // Read the sector coordinates
		if(readBuffer.readBoolean()) {
			int entityCount = readBuffer.readInt();
			entities = new HashSet<>();
			for(int i = 0; i < entityCount; i++) {
				BuildSectorEntityData entityData = new BuildSectorEntityData(readBuffer);
				entities.add(entityData); // Deserialize each entity
			}
		}
		if(readBuffer.readBoolean()) {
			int permissionCount = readBuffer.readInt();
			for(int i = 0; i < permissionCount; i++) {
				String username = readBuffer.readString(); // Read the username
				if(!readBuffer.readBoolean()) {
					// No permissions for this user
					permissions.put(username, new HashMap<PermissionTypes, Boolean>()); // Store an empty permissions map
				} else {
					int permissionSize = readBuffer.readInt(); // Read the number of permissions for this user
					HashMap<PermissionTypes, Boolean> permissionMap = new HashMap<>();
					for(int j = 0; j < permissionSize; j++) {
						String typeString = readBuffer.readString(); // Read the permission type
						boolean value = readBuffer.readBoolean(); // Read the permission value
						PermissionTypes type = PermissionTypes.valueOf(typeString); // Convert to enum
						permissionMap.put(type, value); // Store in the map
					}
					permissions.put(username, permissionMap); // Store the permissions for this user
				}
			}
		}
	}

	public static SegmentController getEntity(String entityUID) {
		for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
			if(sendable instanceof SegmentController && ((SegmentController) sendable).getUniqueIdentifier().equals(entityUID)) return (SegmentController) sendable;
		}
		return null;
	}

	public Vector3i getSector() {
		return sector;
	}

	public String getOwner() {
		return owner;
	}

	public PlayerData getOwnerData(boolean server) {
		return PlayerDataManager.getInstance(server).getFromName(owner, server);
	}

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
			if(value != null) return value;
			else return false;
		} else return false;
	}

	public void setPermission(String user, PermissionTypes type, boolean value, boolean server) {
		HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
		if(permissionMap != null) permissionMap.put(type, value);
		else {
			HashMap<PermissionTypes, Boolean> newPermissionMap = new HashMap<>();
			newPermissionMap.put(type, value);
			permissions.put(user, newPermissionMap);
		}
		BuildSectorDataManager.getInstance(server).updateData(this, server);
	}

	public boolean getPermissionForEntity(String user, String entityUID, PermissionTypes... types) {
		SegmentController entity = getEntity(entityUID);
		if(entity == null || !entity.existsInState()) return false; // Entity does not exist
		BuildSectorEntityData entityData = getEntityData(getEntity(entityUID));
		if(entityData != null) {
			for(PermissionTypes type : types) {
				if(entityData.getPermission(user, type)) return true;
			}
		}
		return false;
	}

	public boolean getPermissionForEntityOrGlobal(String user, String entityUID, PermissionTypes type) {
		SegmentController entity = getEntity(entityUID);
		if(entity == null || !entity.existsInState()) return false; // Entity does not exist
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
		doEntityUpdateCheck();
		return entities;
	}

	public void doEntityUpdateCheck() {
		prune();
		for(Sendable sendable : GameCommon.getGameState().getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
			if(sendable instanceof SegmentController) {
				SegmentController entity = (SegmentController) sendable;
				if(entity.getSector(new Vector3i()).equals(sector) && entity.existsInState() && getEntityData(entity) == null) addEntity(entity, entity.isOnServer());
			}
		}
	}

	public void prune() {
		boolean hasDirty = false;
		ObjectArrayList<BuildSectorEntityData> toRemove = new ObjectArrayList<>();
		for(BuildSectorEntityData entityData : entities) {
			if(entityData.getEntity() == null || !entityData.getEntity().getSector(new Vector3i()).equals(sector)) {
				toRemove.add(entityData);
				hasDirty = true;
			}
		}
		for(BuildSectorEntityData entityData : toRemove) entities.remove(entityData); //Prevent concurrency issues
		if(hasDirty) BuildSectorEntityScrollableList.update();
	}

	public BuildSectorEntityData getEntityData(SegmentController entity) {
		if(entities == null) entities = new HashSet<>();
		if(entity == null) return null;
		for(BuildSectorEntityData entityData : entities) {
			if(entityData.getEntity() != null) {
				if(entityData.getEntity().equals(entity)) return entityData;
			}
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
			if(entityData.getEntity() == null) continue; // Skip if entity is null
			if(entityData.getEntity().equals(entity)) {
				toRemove = entityData;
				entityData.delete(); // Clean up the entity if needed
				break; // Found the entity to remove, exit loop
			}
		}
		if(toRemove != null) entities.remove(toRemove);
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
			EdenCore.getInstance().logException("An error occurred while spawning entity", exception);
		}
	}

	public void toggleEntityAI(SegmentController entity, boolean value) {
		EntityUtils.toggleAI(entity, value);
	}

	public Sector getServerSector() throws Exception {
		return GameServer.getServerState().getUniverse().getSector(sector);
	}

	private void setDefaultPerms(String user, int type) {
		if(permissions == null) permissions = new HashMap<>();
		switch(type) {
			case OWNER:
				permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
					put(PermissionTypes.EDIT_OWN, true);
					put(PermissionTypes.EDIT_ANY, true);
					put(PermissionTypes.SPAWN, true);
					put(PermissionTypes.SPAWN_ENEMIES, true);
					put(PermissionTypes.DELETE_OWN, true);
					put(PermissionTypes.DELETE_ANY, true);
					put(PermissionTypes.TOGGLE_AI_OWN, true);
					put(PermissionTypes.TOGGLE_AI_ANY, true);
					put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
					put(PermissionTypes.TOGGLE_DAMAGE_ANY, true);
					put(PermissionTypes.INVITE, true);
					put(PermissionTypes.KICK, true);
					put(PermissionTypes.EDIT_PERMISSIONS, true);
				}});
				break;
			case FRIEND:
				permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
					put(PermissionTypes.EDIT_OWN, true);
					put(PermissionTypes.EDIT_ANY, true);
					put(PermissionTypes.SPAWN, true);
					put(PermissionTypes.SPAWN_ENEMIES, false);
					put(PermissionTypes.DELETE_OWN, true);
					put(PermissionTypes.DELETE_ANY, false);
					put(PermissionTypes.TOGGLE_AI_OWN, true);
					put(PermissionTypes.TOGGLE_AI_ANY, false);
					put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
					put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
					put(PermissionTypes.INVITE, true);
					put(PermissionTypes.KICK, false);
					put(PermissionTypes.EDIT_PERMISSIONS, false);
				}});
				break;
			case OTHER:
				permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
					put(PermissionTypes.EDIT_OWN, true);
					put(PermissionTypes.EDIT_ANY, false);
					put(PermissionTypes.SPAWN, true);
					put(PermissionTypes.SPAWN_ENEMIES, false);
					put(PermissionTypes.DELETE_OWN, false);
					put(PermissionTypes.DELETE_ANY, false);
					put(PermissionTypes.TOGGLE_AI_OWN, false);
					put(PermissionTypes.TOGGLE_AI_ANY, false);
					put(PermissionTypes.TOGGLE_DAMAGE_OWN, false);
					put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
					put(PermissionTypes.INVITE, false);
					put(PermissionTypes.KICK, false);
					put(PermissionTypes.EDIT_PERMISSIONS, false);
				}});
				break;
		}
	}

	public Set<String> getAllUsers() {
		if(permissions == null) permissions = new HashMap<>();
		return new HashSet<>(permissions.keySet());
	}

	public HashMap<PermissionTypes, Boolean> getPermissionsForEntity(String entityUID, String username) {
		BuildSectorEntityData entityData = getEntityData(getEntity(entityUID));
		if(entityData != null) return entityData.permissions.get(username);
		else return null;
	}

	public HashMap<PermissionTypes, Boolean> getPermissionsForUser(String username) {
		if(permissions == null) permissions = new HashMap<>();
		return permissions.get(username);
	}

	public void setPermissionForEntity(String entityUID, String username, PermissionTypes type, boolean value, boolean server) {
		BuildSectorEntityData entityData = getEntityData(getEntity(entityUID));
		if(entityData != null) entityData.setPermission(username, type, value, server);
	}

	public enum EntityType {
		SHIP,
		STATION,
		DOCKED,
		TURRET;

		public static EntityType fromEntity(SegmentController entity) {
			if(entity.getType() == SimpleTransformableSendableObject.EntityType.SHIP) {
				if(entity.railController.isTurretDocked()) return TURRET;
				else if(entity.isDocked()) return DOCKED;
				else return SHIP;
			} else return STATION;
		}
	}

	public class BuildSectorEntityData extends SerializableData {

		private static final byte VERSION = 0;

		private String entityUID;
		private EntityType entityType;
		protected HashMap<String, HashMap<PermissionTypes, Boolean>> permissions = new HashMap<>();
		private boolean invulnerable = true;

		public BuildSectorEntityData(SegmentController entity) {
			super(DataType.BUILD_SECTOR_ENTITY_DATA);
			entityUID = entity.getUniqueIdentifier();
			entityType = EntityType.fromEntity(entity);
			setDefaultEntityPerms(entity.getSpawner(), OWNER);
			setInvulnerable(true, entity.isOnServer());
		}

		public BuildSectorEntityData(PacketReadBuffer readBuffer) throws IOException {
			deserializeNetwork(readBuffer);
			dataType = DataType.BUILD_SECTOR_ENTITY_DATA;
		}

		public BuildSectorEntityData(JSONObject data) {
			deserialize(data);
			dataType = DataType.BUILD_SECTOR_ENTITY_DATA;
		}

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
			byte version = (byte) data.getInt("version");
			dataUUID = data.getString("uuid");
			entityUID = data.getString("entityUID");
			entityType = EntityType.valueOf(data.getString("entityType"));
			JSONArray permissionsArray = data.getJSONArray("permissions");
			for(int i = 0; i < permissionsArray.length(); i++) {
				JSONObject permissionData = permissionsArray.getJSONObject(i);
				String name = permissionData.getString("name");
				for(PermissionTypes type : PermissionTypes.values()) {
					if(permissionData.has(type.name())) {
						boolean value = permissionData.getBoolean(type.name());
						HashMap<PermissionTypes, Boolean> permission = new HashMap<>();
						permission.put(type, value);
						permissions.put(name, permission);
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
				writeBuffer.writeInt(permissions.get(name).size()); // Write the number of permissions for this user
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
			byte version = readBuffer.readByte();
			dataUUID = readBuffer.readString();
			entityUID = readBuffer.readString();
			entityType = EntityType.valueOf(readBuffer.readString().toUpperCase(Locale.ENGLISH));
			int permissionCount = readBuffer.readInt();
			for(int i = 0; i < permissionCount; i++) {
				String name = readBuffer.readString();
				int permissionSize = readBuffer.readInt();
				HashMap<PermissionTypes, Boolean> permissionMap = new HashMap<>();
				if(permissionSize > 0) {
					// Read the permissions for this user
					for(int j = 0; j < permissionSize; j++) {
						String typeString = readBuffer.readString(); // Read the permission type
						boolean value = readBuffer.readBoolean(); // Read the permission value
						PermissionTypes type = PermissionTypes.valueOf(typeString.toUpperCase(Locale.ENGLISH)); // Convert to enum
						permissionMap.put(type, value); // Store in the map
					}
				}
				permissions.put(name, permissionMap); // Store the permissions for this user
			}
			invulnerable = readBuffer.readBoolean(); // Read the invulnerable flag
		}

		private void setDefaultEntityPerms(String user, int type) {
			//These permissions are specific to an entity and are stored in the entity object itself rather than the sector object
			//If set, these will override "global" build sector permissions
			switch(type) {
				case OWNER:
					permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
						put(PermissionTypes.EDIT_SPECIFIC, true);
						put(PermissionTypes.DELETE_SPECIFIC, true);
						put(PermissionTypes.TOGGLE_AI_SPECIFIC, true);
						put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, true);
						put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, true);
						put(PermissionTypes.EDIT_OWN, true);
						put(PermissionTypes.EDIT_ANY, true);
						put(PermissionTypes.SPAWN, true);
						put(PermissionTypes.SPAWN_ENEMIES, true);
						put(PermissionTypes.DELETE_OWN, true);
						put(PermissionTypes.DELETE_ANY, true);
						put(PermissionTypes.TOGGLE_AI_OWN, true);
						put(PermissionTypes.TOGGLE_AI_ANY, true);
						put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
						put(PermissionTypes.TOGGLE_DAMAGE_ANY, true);
						put(PermissionTypes.INVITE, true);
						put(PermissionTypes.KICK, true);
						put(PermissionTypes.EDIT_PERMISSIONS, true);
					}});
					break;
				case FRIEND:
					permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
						put(PermissionTypes.EDIT_SPECIFIC, true);
						put(PermissionTypes.DELETE_SPECIFIC, true);
						put(PermissionTypes.TOGGLE_AI_SPECIFIC, true);
						put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, true);
						put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, false);
						put(PermissionTypes.EDIT_OWN, true);
						put(PermissionTypes.EDIT_ANY, true);
						put(PermissionTypes.SPAWN, true);
						put(PermissionTypes.SPAWN_ENEMIES, false);
						put(PermissionTypes.DELETE_OWN, true);
						put(PermissionTypes.DELETE_ANY, false);
						put(PermissionTypes.TOGGLE_AI_OWN, true);
						put(PermissionTypes.TOGGLE_AI_ANY, false);
						put(PermissionTypes.TOGGLE_DAMAGE_OWN, true);
						put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
						put(PermissionTypes.INVITE, true);
						put(PermissionTypes.KICK, false);
						put(PermissionTypes.EDIT_PERMISSIONS, false);
					}});
					break;
				case OTHER:
					permissions.put(user, new HashMap<PermissionTypes, Boolean>() {{
						put(PermissionTypes.EDIT_SPECIFIC, false);
						put(PermissionTypes.DELETE_SPECIFIC, false);
						put(PermissionTypes.TOGGLE_AI_SPECIFIC, false);
						put(PermissionTypes.TOGGLE_DAMAGE_SPECIFIC, false);
						put(PermissionTypes.EDIT_ENTITY_PERMISSIONS, false);
						put(PermissionTypes.EDIT_OWN, true);
						put(PermissionTypes.EDIT_ANY, false);
						put(PermissionTypes.SPAWN, true);
						put(PermissionTypes.SPAWN_ENEMIES, false);
						put(PermissionTypes.DELETE_OWN, false);
						put(PermissionTypes.DELETE_ANY, false);
						put(PermissionTypes.TOGGLE_AI_OWN, false);
						put(PermissionTypes.TOGGLE_AI_ANY, false);
						put(PermissionTypes.TOGGLE_DAMAGE_OWN, false);
						put(PermissionTypes.TOGGLE_DAMAGE_ANY, false);
						put(PermissionTypes.INVITE, false);
						put(PermissionTypes.KICK, false);
						put(PermissionTypes.EDIT_PERMISSIONS, false);
					}});
					break;
			}
		}

		public String getEntityUID() {
			return entityUID;
		}

		public SegmentController getEntity() {
			return BuildSectorData.getEntity(entityUID);
		}

		public EntityType getEntityType() {
			return entityType;
		}

		public String getSpawner() {
			return getEntity().getSpawner();
		}

		public boolean getPermission(String user, PermissionTypes type) {
			HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
			if(permissionMap != null) {
				Boolean value = permissionMap.get(type);
				if(value != null) return value;
				else return false;
			} else return false;
		}

		public void setPermission(String user, PermissionTypes type, boolean value, boolean server) {
			HashMap<PermissionTypes, Boolean> permissionMap = permissions.get(user);
			if(permissionMap != null) permissionMap.put(type, value);
			else {
				HashMap<PermissionTypes, Boolean> newPermissionMap = new HashMap<>();
				newPermissionMap.put(type, value);
				permissions.put(user, newPermissionMap);
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
			} else return false;
		}

		public void setAIActive(boolean value) {
			try {
				if(getEntity() instanceof Ship) {
					Ship ship = (Ship) getEntity();
					ship.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
					for(RailRelation docked : ship.railController.next) {
						if(docked.docked.getSegmentController() instanceof Ship) setAiRecursive((Ship) docked.docked.getSegmentController(), value);
					}
				} else if(getEntity() instanceof SpaceStation) {
					SpaceStation station = (SpaceStation) getEntity();
					station.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
					for(RailRelation docked : station.railController.next) {
						if(docked.docked.getSegmentController() instanceof Ship) setAiRecursive((Ship) docked.docked.getSegmentController(), value);
					}
				}
			} catch(Exception exception) {
				EdenCore.getInstance().logException("An error occurred while setting AI for entity", exception);
			}
		}

		private void setAiRecursive(Ship ship, boolean value) {
			try {
				ship.getAiConfiguration().get(Types.ACTIVE).switchSetting(String.valueOf(value), true);
				for(RailRelation docked : ship.railController.next) {
					if(docked.docked.getSegmentController() instanceof Ship) setAiRecursive((Ship) docked.docked.getSegmentController(), value);
				}
			} catch(Exception exception) {
				EdenCore.getInstance().logException("An error occurred while setting AI for entity", exception);
			}
		}

		public void delete() {
			SegmentController entity = getEntity();
			if(entity != null) EntityUtils.delete(entity);
		}

		public void deleteTurrets() {
			for(RailRelation docked : getEntity().railController.next) {
				if(docked.docked.getSegmentController() instanceof Ship) EntityUtils.delete(docked.docked.getSegmentController());
			}
		}

		public boolean isInvulnerable() {
			return invulnerable;
		}
		
		public void setInvulnerable(boolean invulnerable, boolean server) {
			this.invulnerable = invulnerable;
			if(getEntity() != null) getEntity().setVulnerable(!invulnerable);
			BuildSectorDataManager.getInstance(server).updateData(BuildSectorData.this, server);
		}
	}

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

		public String getDisplay() {
			return display;
		}

		public String getDescription() {
			return description;
		}

		public static Set<PermissionTypes> getListValues() {
			Set<PermissionTypes> values = new HashSet<>();
			values.add(EDIT_OWN);
			values.add(EDIT_ANY);
			values.add(SPAWN);
			values.add(SPAWN_ENEMIES);
			values.add(DELETE_OWN);
			values.add(DELETE_ANY);
			values.add(TOGGLE_AI_OWN);
			values.add(TOGGLE_AI_ANY);
			values.add(TOGGLE_DAMAGE_OWN);
			values.add(TOGGLE_DAMAGE_ANY);
			values.add(INVITE);
			values.add(KICK);
			values.add(EDIT_PERMISSIONS);
			return values;
		}

		public static Set<PermissionTypes> getEntitySpecificValues() {
			Set<PermissionTypes> values = new HashSet<>();
			values.add(EDIT_SPECIFIC);
			values.add(DELETE_SPECIFIC);
			values.add(TOGGLE_AI_SPECIFIC);
			values.add(TOGGLE_DAMAGE_SPECIFIC);
			values.add(EDIT_ENTITY_PERMISSIONS);
			return values;
		}
	}
}
