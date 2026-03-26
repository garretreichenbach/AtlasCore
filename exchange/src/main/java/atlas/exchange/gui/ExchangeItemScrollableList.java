package atlas.exchange.gui;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.game.inventory.InventoryUtils;
import atlas.core.api.SubModRegistry;
import atlas.core.data.DataManager;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.exchange.AtlasExchange;
import atlas.exchange.data.ExchangeData;
import atlas.exchange.data.ExchangeDataManager;
import atlas.exchange.element.ElementRegistry;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.common.data.player.BlueprintPlayerHandleRequest;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.network.objects.remote.RemoteBlueprintPlayerRequest;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

/**
 * Scrollable list for the exchange marketplace. Renders differently for
 * ships/stations (name, producer, price, category, mass) versus items/weapons
 * (type icon, name, price). BUY and REMOVE buttons are shown based on ownership.
 *
 * @author TheDerpGamer
 */
public class ExchangeItemScrollableList extends ScrollableTableList<ExchangeData> implements GUIActiveInterface {

	private static final BlueprintClassification[] shipClassifications = {BlueprintClassification.ATTACK, BlueprintClassification.CARGO, BlueprintClassification.CARRIER, BlueprintClassification.DEFENSE, BlueprintClassification.MINING, BlueprintClassification.SCAVENGER, BlueprintClassification.SCOUT, BlueprintClassification.SUPPORT};

	private static final BlueprintClassification[] stationClassifications = {BlueprintClassification.DEFENSE_STATION, BlueprintClassification.FACTORY_STATION, BlueprintClassification.MINING_STATION, BlueprintClassification.OUTPOST_STATION, BlueprintClassification.WAYPOINT_STATION, BlueprintClassification.SHIPYARD_STATION, BlueprintClassification.SHOPPING_STATION, BlueprintClassification.TRADE_STATION};

	protected final int type;
	private final GUIAncor pane;

	public ExchangeItemScrollableList(InputState state, GUIAncor pane, int type) {
		super(state, 10, 10, pane);
		this.pane = pane;
		this.type = type;
	}

	// ── Data source ──────────────────────────────────────────────────────────

	/**
	 * Returns true if the player is currently inside any build sector.
	 * Uses reflection so exchange does not need a compile-time dependency on AtlasBuildSectors.
	 */
	private static boolean isInBuildSector(GameClientState state) {
		if(!SubModRegistry.isLoaded("atlas_buildsectors")) return false;
		try {
			Class<?> cls = Class.forName("atlas.buildsectors.data.BuildSectorDataManager");
			Object manager = cls.getMethod("getInstance", boolean.class).invoke(null, false);
			return (boolean) cls.getMethod("isPlayerInAnyBuildSector", org.schema.game.common.data.player.PlayerState.class).invoke(manager, state.getPlayer());
		} catch(Exception e) {
			return false;
		}
	}

	// ── Row rendering ────────────────────────────────────────────────────────

	@Override
	public void initColumns() {
		if(type == ExchangeData.SHIPS || type == ExchangeData.STATIONS) {
			addColumn(Lng.str("Name"), 15.0F, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			addColumn(Lng.str("Producer"), 10.0F, (o1, o2) -> o1.getProducer().compareToIgnoreCase(o2.getProducer()));
			addColumn(Lng.str("Price"), 5.0F, Comparator.comparingInt(ExchangeData::getPrice));
			addColumn(Lng.str("Category"), 10.0F, Comparator.comparing(o -> o.getCategory().name()));
			addColumn(Lng.str("Mass"), 5.0F, (o1, o2) -> Float.compare(o1.getMass(), o2.getMass()));
			addTextFilter(new GUIListFilterText<ExchangeData>() {
				@Override
				public boolean isOk(String s, ExchangeData item) {
					return item.getName().toLowerCase().contains(s.toLowerCase());
				}
			}, ControllerElement.FilterRowStyle.FULL);
			addTextFilter(new GUIListFilterText<ExchangeData>() {
				@Override
				public boolean isOk(String s, ExchangeData item) {
					return item.getProducer().toLowerCase().contains(s.toLowerCase());
				}
			}, ControllerElement.FilterRowStyle.LEFT);
			switch(type) {
				case ExchangeData.SHIPS:
					addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(shipClassifications) {
						@Override
						public boolean isOk(BlueprintClassification c, ExchangeData item) {
							return c == null || item.getClassification() == c;
						}
					}, new CreateGUIElementInterface<BlueprintClassification>() {
						@Override
						public GUIElement create(BlueprintClassification c) {
							GUIAncor a = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown d;
							(d = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(c.getName().toUpperCase(Locale.ENGLISH));
							d.setPos(4.0F, 4.0F, 0.0F);
							a.setUserPointer(c);
							a.attach(d);
							return a;
						}

						@Override
						public GUIElement createNeutral() {
							GUIAncor a = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown d;
							(d = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
							d.setPos(4.0F, 4.0F, 0.0F);
							a.setUserPointer(null);
							a.attach(d);
							return a;
						}
					}, ControllerElement.FilterRowStyle.RIGHT);
					break;
				case ExchangeData.STATIONS:
					addDropdownFilter(new GUIListFilterDropdown<ExchangeData, BlueprintClassification>(BlueprintClassification.stationValues().toArray(stationClassifications)) {
						@Override
						public boolean isOk(BlueprintClassification c, ExchangeData item) {
							return c == null || item.getClassification() == c;
						}
					}, new CreateGUIElementInterface<BlueprintClassification>() {
						@Override
						public GUIElement create(BlueprintClassification c) {
							GUIAncor a = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown d;
							(d = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(c.getName().toUpperCase(Locale.ENGLISH));
							d.setPos(4.0F, 4.0F, 0.0F);
							a.setUserPointer(c);
							a.attach(d);
							return a;
						}

						@Override
						public GUIElement createNeutral() {
							GUIAncor a = new GUIAncor(getState(), 10.0F, 24.0F);
							GUITextOverlayTableDropDown d;
							(d = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(Lng.str("ALL"));
							d.setPos(4.0F, 4.0F, 0.0F);
							a.setUserPointer(null);
							a.attach(d);
							return a;
						}
					}, ControllerElement.FilterRowStyle.RIGHT);
					break;
			}
		} else if(type == ExchangeData.ITEMS || type == ExchangeData.WEAPONS) {
			addColumn(Lng.str("Type"), 5.0f, Comparator.comparingInt(ExchangeData::getItemId));
			addColumn(Lng.str("Name"), 15.0F, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			addColumn(Lng.str("Price"), 5.0f, Comparator.comparingInt(ExchangeData::getPrice));
			addTextFilter(new GUIListFilterText<ExchangeData>() {
				@Override
				public boolean isOk(String s, ExchangeData item) {
					return item.getName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH)) || item.getItemInfo().getName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
				}
			}, ControllerElement.FilterRowStyle.FULL);
			columnsHeight = 52;
		} else {
			throw new IllegalArgumentException("ExchangeItemScrollableList does not support type: " + type);
		}
		activeSortColumnIndex = 0;
	}

	// ── Icon helpers ─────────────────────────────────────────────────────────

	@Override
	protected Collection<ExchangeData> getElementList() {
		switch(type) {
			case ExchangeData.SHIPS:
				return ExchangeDialog.getShipList();
			case ExchangeData.STATIONS:
				return ExchangeDialog.getStationList();
			case ExchangeData.ITEMS:
				return ExchangeDialog.getItemsList();
			case ExchangeData.WEAPONS:
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
			if(type == ExchangeData.SHIPS || type == ExchangeData.STATIONS) {
				GUIClippedRow nameRow = getSimpleRow(data.getName(), this);
				GUIClippedRow producerRow = getSimpleRow(data.getProducer(), this);
				GUIClippedRow priceRow = getSimpleRow(String.valueOf(data.getPrice()), this);
				GUIClippedRow categoryRow = getSimpleRow(data.getCategory().name(), this);
				GUIClippedRow massRow = getSimpleRow(StringTools.massFormat(data.getMass()), this);
				ExchangeItemScrollableListRow entryListRow = new ExchangeItemScrollableListRow(getState(), data, nameRow, producerRow, priceRow, categoryRow, massRow);
				GUIAncor anchor = new GUIAncor(getState(), pane.getWidth() - 28.0f, 28.0f) {
					@Override
					public void draw() {
						super.draw();
						setWidth(pane.getWidth() - 28.0f);
					}
				};
				anchor.attach(redrawButtonPane(data, anchor));
				entryListRow.expanded = new GUIElementList(getState());
				entryListRow.expanded.add(new GUIListElement(anchor, getState()));
				entryListRow.onInit();
				guiElementList.addWithoutUpdate(entryListRow);
			} else {
				GUIClippedRow typeRow = (type == ExchangeData.ITEMS) ? createIconRow(data.getItemId()) : createMetaRow(data.getItemId());
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
				anchor.attach(redrawButtonPane(data, anchor));
				entryListRow.expanded = new GUIElementList(getState());
				entryListRow.expanded.add(new GUIListElement(anchor, getState()));
				entryListRow.onInit();
				guiElementList.addWithoutUpdate(entryListRow);
			}
		}
		guiElementList.updateDim();
	}

	// ── Button pane ──────────────────────────────────────────────────────────

	private GUIClippedRow createIconRow(short id) {
		GUIBlockSprite sprite = new GUIBlockSprite(getState(), id);
		GUIClippedRow row = new GUIClippedRow(getState());
		row.attach(sprite);
		return row;
	}

	// ── Purchase validation ──────────────────────────────────────────────────

	private GUIClippedRow createMetaRow(short id) {
		GUIBlockSprite sprite = new GUIBlockSprite(getState(), id);
		sprite.setLayer(-1);
		GUIClippedRow row = new GUIClippedRow(getState());
		row.attach(sprite);
		return row;
	}

	private GUIHorizontalButtonTablePane redrawButtonPane(ExchangeData data, GUIAncor anchor) {
		boolean isOwner = GameClient.getClientPlayerState().getName().equals(data.getProducer()) && (type == ExchangeData.SHIPS || type == ExchangeData.STATIONS);
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
									if(type == ExchangeData.SHIPS || type == ExchangeData.STATIONS) buyBlueprint(data);
									else if(type == ExchangeData.ITEMS || type == ExchangeData.WEAPONS) buyItem(data);
								}
								deactivate();
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

	// ── Purchase execution ───────────────────────────────────────────────────

	public String canBuy(ExchangeData data) {
		GameClientState state = (GameClientState) getState();
		if(!state.getPlayer().getInventory().hasFreeSlot()) {
			return "You don't have enough space in your inventory to buy this item!";
		}
		if(isInBuildSector(state)) return "You can't do this while in a build sector!";
		short goldBarId = ElementRegistry.GOLD_BAR.getId();
		if(type == ExchangeData.SHIPS || type == ExchangeData.STATIONS) {
			if(!hasPermission(data)) return "Selected blueprint is not available or you don't have access to it!";
			if(goldBarId != -1) {
				int amount = InventoryUtils.getItemAmount(state.getPlayer().getInventory(), goldBarId);
				if(amount < data.getPrice()) return "You don't have enough Gold Bars to buy this blueprint!";
			}
		} else {
			if(goldBarId != -1) {
				int amount = InventoryUtils.getItemAmount(state.getPlayer().getInventory(), goldBarId);
				if(amount < data.getPrice()) return "You don't have enough Gold Bars to buy this item!";
			}
		}
		return null;
	}

	private boolean hasPermission(ExchangeData data) {
		for(CatalogPermission permission : ((GameClientState) getState()).getCatalog().getAvailableCatalog()) {
			if(permission.getUid().toLowerCase(Locale.ENGLISH).equals(data.getCatalogName().toLowerCase(Locale.ENGLISH)) && permission.others())
				return true;
		}
		return false;
	}

	// ── Utility ──────────────────────────────────────────────────────────────

	private void buyItem(ExchangeData data) {
		Inventory playerInventory = ((GameClientState) getState()).getPlayer().getInventory();
		short goldBarId = ElementRegistry.GOLD_BAR.getId();
		if(type == ExchangeData.WEAPONS) {
			PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(AtlasExchange.GIVE_ITEM, GameClient.getClientPlayerState().getName(), String.valueOf(data.getItemId()), "1", "true"));
			if(goldBarId != -1) InventoryUtils.consumeItems(playerInventory, goldBarId, data.getPrice());
		} else {
			PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(AtlasExchange.GIVE_ITEM, GameClient.getClientPlayerState().getName(), String.valueOf(data.getItemId()), String.valueOf(data.getItemCount()), "false"));
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
		short goldBarId = ElementRegistry.GOLD_BAR.getId();
		if(goldBarId != -1) {
			InventoryUtils.consumeItems(((GameClientState) getState()).getPlayer().getInventory(), goldBarId, data.getPrice());
		}
		if(AtlasExchange.ADD_BARS != -1) {
			PacketUtil.sendPacket(GameClient.getClientPlayerState(), new PlayerActionCommandPacket(AtlasExchange.ADD_BARS, GameClient.getClientPlayerState().getName(), data.getProducer(), String.valueOf(data.getPrice())));
		}
	}

	// ── Row class ────────────────────────────────────────────────────────────

	public class ExchangeItemScrollableListRow extends ScrollableTableList<ExchangeData>.Row {

		public ExchangeItemScrollableListRow(InputState state, ExchangeData data, GUIElement... elements) {
			super(state, data, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
