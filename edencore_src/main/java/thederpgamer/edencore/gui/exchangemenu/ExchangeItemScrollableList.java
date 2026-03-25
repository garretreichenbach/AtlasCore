package thederpgamer.edencore.gui.exchangemenu;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.inventory.InventoryUtils;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.common.data.element.meta.weapon.Weapon;
import org.schema.game.common.data.player.BlueprintPlayerHandleRequest;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.network.objects.remote.RemoteBlueprintPlayerRequest;
import org.schema.game.server.data.admin.AdminCommands;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.DataManager;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;
import thederpgamer.edencore.data.exchangedata.ExchangeData;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;
import thederpgamer.edencore.element.ElementManager;
import thederpgamer.edencore.manager.PlayerActionManager;
import thederpgamer.edencore.network.PlayerActionCommandPacket;

import java.util.*;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ExchangeItemScrollableList extends ScrollableTableList<ExchangeData> implements GUIActiveInterface {

	private static final BlueprintClassification[] shipClassifications = {
			BlueprintClassification.ATTACK,
			BlueprintClassification.CARGO,
			BlueprintClassification.CARRIER,
			BlueprintClassification.DEFENSE,
			BlueprintClassification.MINING,
			BlueprintClassification.SCAVENGER,
			BlueprintClassification.SCOUT,
			BlueprintClassification.SUPPORT
	};

	private static final BlueprintClassification[] stationClassifications = {
			BlueprintClassification.DEFENSE_STATION,
			BlueprintClassification.FACTORY_STATION,
			BlueprintClassification.MINING_STATION,
			BlueprintClassification.OUTPOST_STATION,
			BlueprintClassification.WAYPOINT_STATION,
			BlueprintClassification.SHIPYARD_STATION,
			BlueprintClassification.SHOPPING_STATION,
			BlueprintClassification.TRADE_STATION
	};
	protected final int type;
	private final GUIAncor pane;

	public ExchangeItemScrollableList(InputState state, GUIAncor pane, int type) {
		super(state, 10, 10, pane);
		this.pane = pane;
		this.type = type;
	}

	@Override
	public void initColumns() {
		if(type == ExchangeDialog.SHIPS || type == ExchangeDialog.STATIONS) {
			addColumn(Lng.str("Name"), 15.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
			});
			addColumn(Lng.str("Producer"), 10.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return o1.getProducer().compareToIgnoreCase(o2.getProducer());
				}
			});
			addColumn(Lng.str("Price"), 5.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return Integer.compare(o1.getPrice(), o2.getPrice());
				}
			});
			addColumn(Lng.str("Category"), 10.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return o1.getCategory().compareTo(o2.getCategory());
				}
			});
			addColumn(Lng.str("Mass"), 5.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return Float.compare(o1.getMass(), o2.getMass());
				}
			});

			addTextFilter(new GUIListFilterText<ExchangeData>() {
				public boolean isOk(String s, ExchangeData item) {
					return item.getName().toLowerCase().contains(s.toLowerCase());
				}
			}, ControllerElement.FilterRowStyle.FULL);
			addTextFilter(new GUIListFilterText<ExchangeData>() {
				public boolean isOk(String s, ExchangeData item) {
					return item.getProducer().toLowerCase().contains(s.toLowerCase());
				}
			}, ControllerElement.FilterRowStyle.LEFT);
			switch(type) {
				case ExchangeDialog.SHIPS:
					addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(shipClassifications) {
						public boolean isOk(BlueprintClassification classification, ExchangeData item) {
							return classification == null || item.getClassification() == classification;
						}

					}, new CreateGUIElementInterface<BlueprintClassification>() {
						@Override
						public GUIElement create(BlueprintClassification classification) {
							GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown dropDown;
							(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(classification.getName().toUpperCase(Locale.ENGLISH));
							dropDown.setPos(4.0F, 4.0F, 0.0F);
							anchor.setUserPointer(classification);
							anchor.attach(dropDown);
							return anchor;
						}

						@Override
						public GUIElement createNeutral() {
							GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown dropDown;
							(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
							dropDown.setPos(4.0F, 4.0F, 0.0F);
							anchor.setUserPointer(null);
							anchor.attach(dropDown);
							return anchor;
						}
					}, ControllerElement.FilterRowStyle.RIGHT);
					break;
				case ExchangeDialog.STATIONS:
					addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(BlueprintClassification.stationValues().toArray(stationClassifications)) {
						public boolean isOk(BlueprintClassification classification, ExchangeData item) {
							return classification == null || item.getClassification() == classification;
						}

					}, new CreateGUIElementInterface<BlueprintClassification>() {
						@Override
						public GUIElement create(BlueprintClassification classification) {
							GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown dropDown;
							(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(classification.getName().toUpperCase(Locale.ENGLISH));
							dropDown.setPos(4.0F, 4.0F, 0.0F);
							anchor.setUserPointer(classification);
							anchor.attach(dropDown);
							return anchor;
						}

						@Override
						public GUIElement createNeutral() {
							GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown dropDown;
							(dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
							dropDown.setPos(4.0F, 4.0F, 0.0F);
							anchor.setUserPointer(null);
							anchor.attach(dropDown);
							return anchor;
						}
					}, ControllerElement.FilterRowStyle.RIGHT);
					break;
			}
		} else if(type == ExchangeDialog.ITEMS || type == ExchangeDialog.WEAPONS) {
			addColumn(Lng.str("Type"), 5.0f, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return Short.compare(o1.getItemId(), o2.getItemId());
				}
			});
			addColumn(Lng.str("Name"), 15.0F, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
			});
			addColumn(Lng.str("Price"), 5.0f, new Comparator<ExchangeData>() {
				@Override
				public int compare(ExchangeData o1, ExchangeData o2) {
					return Integer.compare(o1.getPrice(), o2.getPrice());
				}
			});
			addTextFilter(new GUIListFilterText<ExchangeData>() {
				public boolean isOk(String s, ExchangeData item) {
					return item.getName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH)) || item.getItemInfo().getName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
				}
			}, ControllerElement.FilterRowStyle.FULL);
			columnsHeight = 52;
		} else
			throw new IllegalArgumentException("ExchangeItemScrollableList does not support the given type: " + type);
		activeSortColumnIndex = 0;
	}

	@Override
	protected Collection<ExchangeData> getElementList() {
		switch(type) {
			case ExchangeDialog.SHIPS:
				return ExchangeDialog.getShipList();
			case ExchangeDialog.STATIONS:
				return ExchangeDialog.getStationList();
			case ExchangeDialog.ITEMS:
				return ExchangeDialog.getItemsList();
			case ExchangeDialog.WEAPONS:
				return ExchangeDialog.getWeaponsList();
			default:
				return Collections.emptyList();
		}
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<ExchangeData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(ExchangeData data : set) {
			if(type == ExchangeDialog.SHIPS || type == ExchangeDialog.STATIONS) {
				GUIClippedRow nameRow = getSimpleRow(data.getName(), this);
				GUIClippedRow producerRow = getSimpleRow(data.getProducer(), this);
				GUIClippedRow priceRow = getSimpleRow(String.valueOf(data.getPrice()), this);
				GUIClippedRow categoryRow = getSimpleRow(data.getCategory(), this);
				GUIClippedRow massRow = getSimpleRow(StringTools.massFormat(data.getMass()), this);
				ExchangeItemScrollableListRow entryListRow = new ExchangeItemScrollableListRow(getState(), data, nameRow, producerRow, priceRow, categoryRow, massRow);
				GUIAncor anchor = new GUIAncor(getState(), pane.getWidth() - 28.0f, 28.0f) {
					@Override
					public void draw() {
						super.draw();
						setWidth(pane.getWidth() - 28.0f);
					}
				};
				GUIHorizontalButtonTablePane buttonTablePane = redrawButtonPane(data, anchor);
				anchor.attach(buttonTablePane);
				GUITextOverlayTableInnerDescription description = new GUITextOverlayTableInnerDescription(10, 10, getState());
				description.onInit();
				description.setTextSimple(data.getDescription());
				entryListRow.expanded = new GUIElementList(getState());
				entryListRow.expanded.add(new GUIListElement(anchor, getState()));
				entryListRow.onInit();
				guiElementList.addWithoutUpdate(entryListRow);
			} else {
				GUIClippedRow typeRow = (type == ExchangeDialog.ITEMS) ? createIconRow(data.getItemId()) : createMetaRow(data.getItemId());
				GUIClippedRow nameRow = getSimpleRow(data.getName(), this);
				GUIClippedRow priceRow = getSimpleRow(String.valueOf(data.getPrice()), this);
				ExchangeItemScrollableListRow entryListRow = new ExchangeItemScrollableListRow(getState(), data, typeRow, nameRow, priceRow);
				GUIAncor anchor = new GUIAncor(getState(), pane.getWidth() - 28.0f, 28.0f) {
					@Override
					public void draw() {
						super.draw();
						setWidth(pane.getWidth() - 28.0f);
					}
				};
				GUIHorizontalButtonTablePane buttonTablePane = redrawButtonPane(data, anchor);
				anchor.attach(buttonTablePane);
				entryListRow.expanded = new GUIElementList(getState());
				entryListRow.expanded.add(new GUIListElement(anchor, getState()));
				entryListRow.onInit();
				guiElementList.addWithoutUpdate(entryListRow);
			}
		}
		guiElementList.updateDim();
	}

	private ScrollableTableList<ExchangeData>.GUIClippedRow createIconRow(short type) {
		GUIBlockSprite sprite = new GUIBlockSprite(getState(), type);
		GUIClippedRow iconRowElement = new GUIClippedRow(getState());
		iconRowElement.attach(sprite);
		return iconRowElement;
	}

	private ScrollableTableList<ExchangeData>.GUIClippedRow createMetaRow(short type) {
		GUIBlockSprite sprite = new GUIBlockSprite(getState(), type);
		sprite.setLayer(-1);
		GUIClippedRow iconRowElement = new GUIClippedRow(getState());
		iconRowElement.attach(sprite);
		return iconRowElement;
	}

	private GUIHorizontalButtonTablePane redrawButtonPane(final ExchangeData data, GUIAncor anchor) {
		boolean isOwner = GameClient.getClientPlayerState().getName().equals(data.getProducer()) && (type == ExchangeDialog.SHIPS || type == ExchangeDialog.STATIONS);
		GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
		buttonPane.onInit();
		if(isOwner || GameClient.getClientPlayerState().isAdmin()) {
			buttonPane.addButton(0, 0, Lng.str("REMOVE"), GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new PlayerOkCancelInput("Confirm", getState(), Lng.str("Confirm"), Lng.str("Do you want to remove this item?")) {
							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								ExchangeDataManager.getInstance(false).removeData(data, false);
								ExchangeDataManager.getInstance(false).sendPacket(data, DataManager.REMOVE_DATA, true);
								deactivate();
								flagDirty();
							}
						}).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});
		} else {
			buttonPane.addButton(0, 0, Lng.str("BUY"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new PlayerOkCancelInput("Confirm", getState(), Lng.str("Confirm"), Lng.str("Do you want to buy this item?")) {
							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								String error = canBuy(data);
								if(error != null) {
									((GameClientState) getState()).getController().popupAlertTextMessage(error);
								} else {
									if(type == ExchangeDialog.SHIPS || type == ExchangeDialog.STATIONS) {
										buyBlueprint(data);
									} else if(type == ExchangeDialog.ITEMS || type == ExchangeDialog.WEAPONS) {
										buyItem(data);
									}
									deactivate();
								}
							}
						}).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});
		}
		return buttonPane;
	}

	public String canBuy(ExchangeData data) {
		GameClientState state = (GameClientState) getState();
		if(!state.getPlayer().getInventory().hasFreeSlot())
			return "You don't have enough space in your inventory to buy this item!";
		if(BuildSectorDataManager.getInstance(false).isPlayerInAnyBuildSector(state.getPlayer()))
			return "You can't do this while in a build sector!";
		if(type == ExchangeDialog.SHIPS || type == ExchangeDialog.STATIONS) {
			if(!hasPermission(data)) return "Selected blueprint is not available or you don't have access to it!";
			else {
				Inventory playerInventory = state.getPlayer().getInventory();
				int amount = InventoryUtils.getItemAmount(playerInventory, ElementManager.getItem("Gold Bar").getId());
				if(amount < data.getPrice()) return "You don't have enough Gold Bars to buy this blueprint!";
			}
		} else {
			Inventory playerInventory = state.getPlayer().getInventory();
			int amount = InventoryUtils.getItemAmount(playerInventory, ElementManager.getItem("Gold Bar").getId());
			if(amount < data.getPrice()) return "You don't have enough Gold Bars to buy this item!";
		}
		return null;
	}

	private boolean hasPermission(ExchangeData data) {
		for(CatalogPermission permission : ((GameClientState) getState()).getCatalog().getAvailableCatalog()) {
			if(permission.getUid().toLowerCase(Locale.ENGLISH).equals(data.getCatalogName().toLowerCase(Locale.ENGLISH)) && permission.others()) {
				return true;
			}
		}
		return false;
	}

	private void buyItem(ExchangeData data) {
		Inventory playerInventory = ((GameClientState) getState()).getPlayer().getInventory();
		if(type == ExchangeDialog.WEAPONS) {
			PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(PlayerActionManager.GIVE_ITEM, GameClient.getClientPlayerState().getName(), String.valueOf(data.getItemId()), "1", "true"));
			InventoryUtils.consumeItems(playerInventory, ElementManager.getItem("Gold Bar").getId(), data.getPrice());
		} else {
			PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(PlayerActionManager.GIVE_ITEM, GameClient.getClientPlayerState().getName(), String.valueOf(data.getItemId()), String.valueOf(data.getItemCount()), "false"));
		}
	}

	private void buyBlueprint(ExchangeData data) {
		BlueprintPlayerHandleRequest req = new BlueprintPlayerHandleRequest();
		req.catalogName = data.getCatalogName();
		req.entitySpawnName = "";
		req.save = false;
		req.toSaveShip = -1;
		req.directBuy = true;
		((GameClientState) getState()).getPlayer().getNetworkObject().catalogPlayerHandleBuffer.add(new RemoteBlueprintPlayerRequest(req, false));
		InventoryUtils.consumeItems(((GameClientState) getState()).getPlayer().getInventory(), ElementManager.getItem("Gold Bar").getId(), data.getPrice());
		PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(PlayerActionManager.ADD_BARS, GameClient.getClientPlayerState().getName(), data.getProducer(), String.valueOf(data.getPrice())));
	}

	public class ExchangeItemScrollableListRow extends ScrollableTableList<ExchangeData>.Row {

		public ExchangeItemScrollableListRow(InputState state, ExchangeData data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
