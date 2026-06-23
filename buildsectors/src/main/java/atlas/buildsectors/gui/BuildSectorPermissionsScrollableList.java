package atlas.buildsectors.gui;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import atlas.buildsectors.AtlasBuildSectors;
import atlas.buildsectors.data.BuildSectorData;
import atlas.core.network.PlayerActionCommandPacket;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.graphicsengine.movie.craterstudio.data.tuples.Pair;
import org.schema.schine.input.InputState;

import java.util.*;

/**
 * Scrollable list of permissions (name + boolean value) for a user in a build sector.
 * Used by both global permission editing and entity-specific permission editing.
 *
 * @author TheDerpGamer
 */
public class BuildSectorPermissionsScrollableList extends ScrollableTableList<Pair<BuildSectorData.PermissionTypes, Boolean>> implements GUIActiveInterface {

	private final GUIElement parent;
	private final String entityUID;
	private final String username;
	private final BuildSectorData buildSectorData;

	/**
	 * Constructor for global (sector-level) permissions.
	 */
	public BuildSectorPermissionsScrollableList(InputState state, GUIElement parent, String username, BuildSectorData buildSectorData) {
		this(state, parent, username, buildSectorData, null);
	}

	/**
	 * Constructor for entity-specific permissions.
	 */
	public BuildSectorPermissionsScrollableList(InputState state, GUIElement parent, String username, BuildSectorData buildSectorData, String entityUID) {
		super(state, 100, 100, parent);
		this.parent = parent;
		this.username = username;
		this.buildSectorData = buildSectorData;
		this.entityUID = entityUID;
	}

	@Override
	public void initColumns() {
		addColumn("Permission", 5.0f, (o1, o2) -> o1.first().getDisplay().compareToIgnoreCase(o2.first().getDisplay()));
		addColumn("Value", 5.0f, (o1, o2) -> Boolean.compare(o1.second(), o2.second()));
		addTextFilter(new GUIListFilterText<Pair<BuildSectorData.PermissionTypes, Boolean>>() {
			@Override
			public boolean isOk(String s, Pair<BuildSectorData.PermissionTypes, Boolean> pair) {
				return pair.first().getDisplay().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.LEFT);
		addDropdownFilter(new GUIListFilterDropdown<Pair<BuildSectorData.PermissionTypes, Boolean>, String>("TRUE", "FALSE") {
			@Override
			public boolean isOk(String o, Pair<BuildSectorData.PermissionTypes, Boolean> pair) {
				return o == null || o.isEmpty() || "ANY".equals(o) || o.equals(pair.second().toString().toUpperCase());
			}
		}, new CreateGUIElementInterface<String>() {
			@Override
			public GUIElement create(String o) {
				GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
				GUITextOverlayTableDropDown dropDown;
				(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(o);
				dropDown.setPos(4.0F, 4.0F, 0.0F);
				anchor.setUserPointer(o);
				anchor.attach(dropDown);
				return anchor;
			}

			@Override
			public GUIElement createNeutral() {
				GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
				GUITextOverlayTableDropDown dropDown;
				(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple("ANY");
				dropDown.setPos(4.0F, 4.0F, 0.0F);
				anchor.setUserPointer("ANY");
				anchor.attach(dropDown);
				return anchor;
			}
		}, ControllerElement.FilterRowStyle.RIGHT);
		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<Pair<BuildSectorData.PermissionTypes, Boolean>> getElementList() {
		if(buildSectorData == null) return Collections.emptySet();
		HashMap<BuildSectorData.PermissionTypes, Boolean> permissions = (entityUID != null)
			? buildSectorData.getPermissionsForEntity(entityUID, username)
			: buildSectorData.getPermissionsForUser(username);
		Set<Pair<BuildSectorData.PermissionTypes, Boolean>> permissionSet = new HashSet<>();
		// Both getters return null when the user has no permission map yet — guard before iterating.
		if(permissions != null) {
			for(Map.Entry<BuildSectorData.PermissionTypes, Boolean> entry : permissions.entrySet()) {
				permissionSet.add(new Pair<>(entry.getKey(), entry.getValue()));
			}
		}
		return permissionSet;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<Pair<BuildSectorData.PermissionTypes, Boolean>> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(Pair<BuildSectorData.PermissionTypes, Boolean> permission : set) {
			GUIClippedRow permissionRow = getSimpleRow(permission.first().getDisplay(), this);
			GUIClippedRow valueRow = getSimpleRow(permission.second().toString(), this);
			BuildSectorPermissionsScrollableListRow entryListRow = new BuildSectorPermissionsScrollableListRow(getState(), permission, permissionRow, valueRow);
			GUIAncor anchor = new GUIAncor(getState(), parent.getWidth() - 28.0f, 28.0f) {
				@Override
				public void draw() {
					super.draw();
					setWidth(parent.getWidth() - 28.0f);
				}
			};
			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 1, anchor);
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "SET TRUE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						sendSetPermission(permission.first(), true);
						flagDirty();
					}
				}

				@Override
				public boolean isOccluded() {
					if(!getState().getController().getPlayerInputs().isEmpty() && !getState().getController().getPlayerInputs().contains(getDialog()))
						return true;
					return !canEditPermissions();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return canEditPermissions();
				}
			});
			buttonPane.addButton(1, 0, "SET FALSE", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						sendSetPermission(permission.first(), false);
						flagDirty();
					}
				}

				@Override
				public boolean isOccluded() {
					if(!getState().getController().getPlayerInputs().isEmpty() && !getState().getController().getPlayerInputs().contains(getDialog()))
						return true;
					return !canEditPermissions();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return canEditPermissions();
				}
			});
			anchor.attach(buttonPane);
			entryListRow.expanded = new GUIElementList(getState());
			entryListRow.expanded.add(new GUIListElement(anchor, getState()));
			entryListRow.onInit();
			guiElementList.addWithoutUpdate(entryListRow);
		}
		guiElementList.updateDim();
	}

	private BuildSectorDialog getDialog() {
		return BuildSectorDialog.getInstance();
	}

	/**
	 * Whether the <em>current client</em> (the editor, not the user being edited) may
	 * change permissions here. Mirrors the server-side authorisation check so the
	 * buttons are only shown when the action would actually succeed.
	 */
	private boolean canEditPermissions() {
		String editor = GameClient.getClientPlayerState().getName();
		if(buildSectorData.getOwner().equals(editor)) return true;
		BuildSectorData.PermissionTypes required = (entityUID != null)
			? BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS
			: BuildSectorData.PermissionTypes.EDIT_PERMISSIONS;
		return buildSectorData.getPermission(editor, required);
	}

	/**
	 * Sends the permission change to the server, which re-validates the sender's
	 * authority, persists it, and replicates to all clients. The client no longer
	 * mutates its own cache directly (that change never persisted on multiplayer).
	 */
	private void sendSetPermission(BuildSectorData.PermissionTypes type, boolean value) {
		if(entityUID != null) {
			PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
				AtlasBuildSectors.SET_ENTITY_PERMISSION,
				buildSectorData.getUUID(), entityUID, username, type.name(), String.valueOf(value)));
		} else {
			PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
				AtlasBuildSectors.SET_PERMISSION,
				buildSectorData.getUUID(), username, type.name(), String.valueOf(value)));
		}
	}

	public class BuildSectorPermissionsScrollableListRow extends ScrollableTableList<Pair<BuildSectorData.PermissionTypes, Boolean>>.Row {

		public BuildSectorPermissionsScrollableListRow(InputState state, Pair<BuildSectorData.PermissionTypes, Boolean> data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
