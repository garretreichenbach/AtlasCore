package thederpgamer.edencore.gui.buildsectormenu;

import api.common.GameClient;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.graphicsengine.movie.craterstudio.data.tuples.Pair;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;

import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class BuildSectorPermissionsScrollableList extends ScrollableTableList<Pair<BuildSectorData.PermissionTypes, Boolean>> implements GUIActiveInterface {

	private final GUIElement parent;
	private final String entityUID;
	private final String username;
	private final BuildSectorData buildSectorData;

	public BuildSectorPermissionsScrollableList(InputState state, GUIElement parent, String username, BuildSectorData buildSectorData) {
		this(state, parent, username, buildSectorData, null);
	}

	public BuildSectorPermissionsScrollableList(InputState state, GUIElement parent, String username, BuildSectorData buildSectorData, String entityUID) {
		super(state, 100, 100, parent);
		this.parent = parent;
		this.username = username;
		this.buildSectorData = buildSectorData;
		this.entityUID = entityUID;
	}

	@Override
	public void initColumns() {
		addColumn("Permission", 5.0f, new Comparator<Pair<BuildSectorData.PermissionTypes, Boolean>>() {
			@Override
			public int compare(Pair<BuildSectorData.PermissionTypes, Boolean> o1, Pair<BuildSectorData.PermissionTypes, Boolean> o2) {
				return o1.first().getDisplay().compareToIgnoreCase(o2.first().getDisplay());
			}
		});
		addColumn("Value", 5.0f, new Comparator<Pair<BuildSectorData.PermissionTypes, Boolean>>() {
			@Override
			public int compare(Pair<BuildSectorData.PermissionTypes, Boolean> o1, Pair<BuildSectorData.PermissionTypes, Boolean> o2) {
				// Compare boolean values, false < true
				return Boolean.compare(o1.second(), o2.second());
			}
		});
		addTextFilter(new GUIListFilterText<Pair<BuildSectorData.PermissionTypes, Boolean>>() {
			@Override
			public boolean isOk(String s, Pair<BuildSectorData.PermissionTypes, Boolean> permissionTypesBooleanPair) {
				return permissionTypesBooleanPair.first().getDisplay().toLowerCase().contains(s.toLowerCase());
			}
		}, ControllerElement.FilterRowStyle.LEFT);
		addDropdownFilter(new GUIListFilterDropdown<Pair<BuildSectorData.PermissionTypes, Boolean>, String>("TRUE", "FALSE") {
			@Override
			public boolean isOk(String o, Pair<BuildSectorData.PermissionTypes, Boolean> permissionTypesBooleanPair) {
				return o == null || o.isEmpty() || o.equals("ANY") || o.equals(permissionTypesBooleanPair.second().toString().toUpperCase());
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
		if(entityUID != null) {
			HashMap<BuildSectorData.PermissionTypes, Boolean> permissions = buildSectorData.getPermissionsForEntity(entityUID, username);
			Set<Pair<BuildSectorData.PermissionTypes, Boolean>> permissionSet = new HashSet<>();
			for(Map.Entry<BuildSectorData.PermissionTypes, Boolean> entry : permissions.entrySet()) permissionSet.add(new Pair<>(entry.getKey(), entry.getValue()));
			return permissionSet;
		} else {
			HashMap<BuildSectorData.PermissionTypes, Boolean> permissions = buildSectorData.getPermissionsForUser(username);
			Set<Pair<BuildSectorData.PermissionTypes, Boolean>> permissionSet = new HashSet<>();
			for(Map.Entry<BuildSectorData.PermissionTypes, Boolean> entry : permissions.entrySet()) permissionSet.add(new Pair<>(entry.getKey(), entry.getValue()));
			return permissionSet;
		}
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<Pair<BuildSectorData.PermissionTypes, Boolean>> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(final Pair<BuildSectorData.PermissionTypes, Boolean> permission : set) {
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
						if(entityUID != null) buildSectorData.setPermissionForEntity(entityUID, username, permission.first(), true, false);
						else buildSectorData.setPermission(username, permission.first(), true, false);
						flagDirty();
					}
				}

				@Override
				public boolean isOccluded() {
					if(!getState().getController().getPlayerInputs().isEmpty() && !getState().getController().getPlayerInputs().contains(getDialog())) return true;
					if(entityUID != null) return !buildSectorData.getPermissionForEntityOrGlobal(username, entityUID, BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
					else if(buildSectorData.getOwner().equals(GameClient.getClientPlayerState().getName())) return true;
					return !buildSectorData.getPermission(username, BuildSectorData.PermissionTypes.EDIT_PERMISSIONS);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					if(entityUID != null) return buildSectorData.getPermissionForEntityOrGlobal(username, entityUID, BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
					else if(buildSectorData.getOwner().equals(GameClient.getClientPlayerState().getName())) return false;
					return buildSectorData.getPermission(username, BuildSectorData.PermissionTypes.EDIT_PERMISSIONS);
				}
			});
			buttonPane.addButton(1, 0, "SET FALSE", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						if(entityUID != null) buildSectorData.setPermissionForEntity(entityUID, username, permission.first(), false, false);
						else buildSectorData.setPermission(username, permission.first(), false, false);
						flagDirty();
					}
				}

				@Override
				public boolean isOccluded() {
					if(!getState().getController().getPlayerInputs().isEmpty() && !getState().getController().getPlayerInputs().contains(getDialog())) return true;
					if(entityUID != null) return !buildSectorData.getPermissionForEntityOrGlobal(username, entityUID, BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
					else if(buildSectorData.getOwner().equals(GameClient.getClientPlayerState().getName())) return true;
					return !buildSectorData.getPermission(username, BuildSectorData.PermissionTypes.EDIT_PERMISSIONS);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					if(entityUID != null) return buildSectorData.getPermissionForEntityOrGlobal(username, entityUID, BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
					else if(buildSectorData.getOwner().equals(GameClient.getClientPlayerState().getName())) return false;
					return buildSectorData.getPermission(username, BuildSectorData.PermissionTypes.EDIT_PERMISSIONS);
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

	public class BuildSectorPermissionsScrollableListRow extends ScrollableTableList<Pair<BuildSectorData.PermissionTypes, Boolean>>.Row {

		public BuildSectorPermissionsScrollableListRow(InputState state, Pair<BuildSectorData.PermissionTypes, Boolean> data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
