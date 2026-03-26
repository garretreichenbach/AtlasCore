package atlas.buildsectors.gui;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import atlas.buildsectors.AtlasBuildSectors;
import atlas.buildsectors.data.BuildSectorData;
import atlas.buildsectors.data.BuildSectorDataManager;
import atlas.core.network.PlayerActionCommandPacket;
import org.schema.common.util.StringTools;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

/**
 * Scrollable list of entities in a build sector, with warp, permissions, AI-toggle,
 * invulnerability-toggle, and delete buttons.
 *
 * @author TheDerpGamer
 */
public class BuildSectorEntityScrollableList extends ScrollableTableList<BuildSectorData.BuildSectorEntityData> implements GUIActiveInterface {

	private static BuildSectorEntityScrollableList instance;
	protected final BuildSectorData buildSectorData;
	private final GUIElement parent;
	private GUIHorizontalButtonTablePane buttonPane;

	public BuildSectorEntityScrollableList(InputState state, GUIElement parent, BuildSectorData buildSectorData) {
		super(state, 100, 100, parent);
		this.parent = parent;
		this.buildSectorData = buildSectorData;
		instance = this;
	}

	/**
	 * Called by {@link BuildSectorData#prune()} to refresh the list after entities are removed.
	 */
	public static void update() {
		if(instance != null) instance.flagDirty();
	}

	private static boolean isObscured() {
		for(DialogInterface dialogInterface : GameClient.getClientState().getController().getPlayerInputs()) {
			if(dialogInterface instanceof BuildSectorEditEntityPermissionsDialog || dialogInterface instanceof BuildSectorEditEntityUserPermissionsDialog)
				return true;
		}
		return false;
	}

	@Override
	protected Collection<BuildSectorData.BuildSectorEntityData> getElementList() {
		if(buildSectorData == null) return Collections.emptyList();
		return buildSectorData.getEntities();
	}

	@Override
	public void initColumns() {
		addColumn("Name", 7.0f, (o1, o2) -> {
			if(o1.getEntity() == null || o2.getEntity() == null) return 0;
			return o1.getEntity().getName().compareToIgnoreCase(o2.getEntity().getName());
		});
		addColumn("Type", 5.0f, (o1, o2) -> {
			if(o1.getEntity() == null || o2.getEntity() == null) return 0;
			return o1.getEntity().getType().getName().compareToIgnoreCase(o2.getEntity().getType().getName());
		});
		addColumn("Mass", 3.0f, (o1, o2) -> {
			if(o1.getEntity() == null || o2.getEntity() == null) return 0;
			return Double.compare(o1.getEntity().getMass(), o2.getEntity().getMass());
		});
		addTextFilter(new GUIListFilterText<BuildSectorData.BuildSectorEntityData>() {
			@Override
			public boolean isOk(String s, BuildSectorData.BuildSectorEntityData entityData) {
				if(entityData.getEntity() == null) return false;
				return s.trim().isEmpty() || entityData.getEntity().getName().toLowerCase(Locale.ENGLISH).contains(s.trim().toLowerCase(Locale.ENGLISH));
			}
		}, ControllerElement.FilterRowStyle.LEFT);
		addDropdownFilter(new GUIListFilterDropdown<BuildSectorData.BuildSectorEntityData, BuildSectorData.EntityType>(BuildSectorData.EntityType.values()) {
			@Override
			public boolean isOk(BuildSectorData.EntityType entityType, BuildSectorData.BuildSectorEntityData entityData) {
				return entityType == null || entityData.getEntityType() == entityType;
			}
		}, new CreateGUIElementInterface<BuildSectorData.EntityType>() {
			@Override
			public GUIElement create(BuildSectorData.EntityType o) {
				GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
				GUITextOverlayTableDropDown dropDown;
				(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(o.name());
				dropDown.setPos(4.0F, 4.0F, 0.0F);
				anchor.setUserPointer(o);
				anchor.attach(dropDown);
				return anchor;
			}

			@Override
			public GUIElement createNeutral() {
				GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
				GUITextOverlayTableDropDown dropDown;
				(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple("ALL");
				dropDown.setPos(4.0F, 4.0F, 0.0F);
				anchor.setUserPointer(null);
				anchor.attach(dropDown);
				return anchor;
			}
		}, ControllerElement.FilterRowStyle.RIGHT);
		activeSortColumnIndex = 0;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<BuildSectorData.BuildSectorEntityData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(BuildSectorData.BuildSectorEntityData entityData : set) {
			if(entityData.getEntity() == null) continue;
			GUIClippedRow nameRow = getSimpleRow(entityData.getEntity().getName(), this);
			GUIClippedRow typeRow = getSimpleRow(entityData.getEntityType().name(), this);
			GUIClippedRow massRow = getSimpleRow(StringTools.massFormat(entityData.getEntity().getMass()), this);
			BuildSectorEntityScrollableListRow entryListRow = new BuildSectorEntityScrollableListRow(getState(), entityData, nameRow, typeRow, massRow);
			GUIAncor anchor = new GUIAncor(getState(), parent.getWidth() - 28.0f, 54.0f) {
				@Override
				public void draw() {
					super.draw();
					setWidth(parent.getWidth() - 28.0f);
				}
			};
			if(buttonPane != null) buttonPane.cleanUp();
			redrawButtonPane(entityData, anchor);
			anchor.attach(buttonPane);
			entryListRow.expanded = new GUIElementList(getState());
			entryListRow.expanded.add(new GUIListElement(anchor, getState()));
			entryListRow.onInit();
			guiElementList.addWithoutUpdate(entryListRow);
		}
		guiElementList.updateDim();
	}

	private void redrawButtonPane(BuildSectorData.BuildSectorEntityData entityData, GUIAncor anchor) {
		buttonPane = new GUIHorizontalButtonTablePane(getState(), 3, 2, anchor);
		buttonPane.onInit();
		String user = ((GameClientState) getState()).getPlayerName();

		buttonPane.addButton(0, 0, "WARP TO", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
						AtlasBuildSectors.ENTER_BUILD_SECTOR, buildSectorData.getUUID()));
				}
			}

			@Override
			public boolean isOccluded() {
				if(isObscured()) return true;
				if(!GameClient.getClientPlayerState().getCurrentSector().equals(buildSectorData.getSector()))
					return true;
				return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.EDIT_SPECIFIC);
			}
		}, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) {
				return true;
			}

			@Override
			public boolean isActive(InputState inputState) {
				if(!GameClient.getClientPlayerState().getCurrentSector().equals(buildSectorData.getSector()))
					return false;
				return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.EDIT_SPECIFIC);
			}
		});

		buttonPane.addButton(1, 0, "EDIT PERMISSIONS", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse())
					(new BuildSectorEditEntityPermissionsDialog(entityData, buildSectorData)).activate();
			}

			@Override
			public boolean isOccluded() {
				if(isObscured()) return true;
				return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
			}
		}, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) {
				return true;
			}

			@Override
			public boolean isActive(InputState inputState) {
				return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.EDIT_ENTITY_PERMISSIONS);
			}
		});

		if(entityData.isAIActive()) {
			buttonPane.addButton(2, 0, "DEACTIVATE AI", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
							AtlasBuildSectors.TOGGLE_AI, entityData.getEntityUID(), "false"));
						clear();
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_AI_SPECIFIC);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) { return true; }

				@Override
				public boolean isActive(InputState inputState) {
					return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_AI_SPECIFIC);
				}
			});
		} else {
			buttonPane.addButton(2, 0, "ACTIVATE AI", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
							AtlasBuildSectors.TOGGLE_AI, entityData.getEntityUID(), "true"));
						clear();
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_AI_SPECIFIC);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) { return true; }

				@Override
				public boolean isActive(InputState inputState) {
					return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_AI_SPECIFIC);
				}
			});
		}

		buttonPane.addButton(0, 1, "DELETE", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
						AtlasBuildSectors.DELETE_ENTITY, entityData.getEntityUID()));
					clear();
				}
			}

			@Override
			public boolean isOccluded() {
				if(isObscured()) return true;
				return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.DELETE_SPECIFIC);
			}
		}, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) { return true; }

			@Override
			public boolean isActive(InputState inputState) {
				return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.DELETE_SPECIFIC);
			}
		});

		buttonPane.addButton(1, 1, "DELETE TURRETS", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
			@Override
			public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
				if(mouseEvent.pressedLeftMouse()) {
					// Turrets are docked children — deleting the root entity on the server
					// will also remove all docked objects (EntityUtils.delete handles recursion).
					PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
						AtlasBuildSectors.DELETE_ENTITY, entityData.getEntityUID()));
					clear();
				}
			}

			@Override
			public boolean isOccluded() {
				if(isObscured()) return true;
				return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.DELETE_SPECIFIC);
			}
		}, new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) { return true; }

			@Override
			public boolean isActive(InputState inputState) {
				return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.DELETE_SPECIFIC);
			}
		});

		if(entityData.isInvulnerable()) {
			buttonPane.addButton(2, 1, "SET VULNERABLE", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
							AtlasBuildSectors.SET_INVULNERABLE, entityData.getEntityUID(), "false"));
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_DAMAGE_SPECIFIC);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) { return true; }

				@Override
				public boolean isActive(InputState inputState) {
					return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_DAMAGE_SPECIFIC);
				}
			});
		} else {
			buttonPane.addButton(2, 1, "SET INVULNERABLE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(
							AtlasBuildSectors.SET_INVULNERABLE, entityData.getEntityUID(), "true"));
					}
				}

				@Override
				public boolean isOccluded() {
					if(isObscured()) return true;
					return !buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_DAMAGE_SPECIFIC);
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) { return true; }

				@Override
				public boolean isActive(InputState inputState) {
					return buildSectorData.getPermissionForEntityOrGlobal(user, entityData.getEntityUID(), BuildSectorData.PermissionTypes.TOGGLE_DAMAGE_SPECIFIC);
				}
			});
		}
	}

	public class BuildSectorEntityScrollableListRow extends ScrollableTableList<BuildSectorData.BuildSectorEntityData>.Row {

		public BuildSectorEntityScrollableListRow(InputState state, BuildSectorData.BuildSectorEntityData entity, GUIElement... elements) {
			super(state, entity, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
