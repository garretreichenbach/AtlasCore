package thederpgamer.edencore.gui.exchangemenu;

import api.common.GameClient;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.controller.PlayerBlockTypeDropdownInputNew;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.controller.manager.ingame.catalog.CatalogControlManager;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.meta.MetaObject;
import org.schema.game.common.data.element.meta.weapon.Weapon;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.EdenCore;
import thederpgamer.edencore.data.DataManager;
import thederpgamer.edencore.data.exchangedata.ExchangeData;
import thederpgamer.edencore.data.exchangedata.ExchangeDataManager;
import thederpgamer.edencore.gui.elements.PlayerSearchableDropdownInput;
import thederpgamer.edencore.utils.ItemUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class AddExchangeItemDialog extends PlayerInput {

	private final AddExchangeBlueprintPanel panel;
	protected static int mode;

	public AddExchangeItemDialog(GameClientState state, int mode) {
		super(state);
		AddExchangeItemDialog.mode = mode; // Mode can be used to determine the type of exchange data being added (ships, stations, etc.)
		(panel = new AddExchangeBlueprintPanel(getState(), this)).onInit();
	}

	@Override
	public void callback(GUIElement callingGuiElement, MouseEvent event) {
		if(event.pressedLeftMouse() && callingGuiElement.getUserPointer() instanceof String) {
			switch((String) callingGuiElement.getUserPointer()) {
				case "OK":
					ExchangeData data = panel.getExchangeData();
					if(data != null) {
						try {
							if(ExchangeDataManager.getInstance(false).existsName(data.getName())) {
								GameClient.showPopupMessage(Lng.str("An item by that name already exists!"), 0);
								deactivate();
								return;
							}
							data.setCategory(mode);
							ExchangeDataManager.getInstance(false).addData(data, false);
							ExchangeDataManager.getInstance(false).sendPacket(data, DataManager.ADD_DATA, true);
						} catch(Exception exception) {
							EdenCore.getInstance().logException("Failed to add exchange data due to an error", exception);
						}
						deactivate();
					}
					break;
				case "CANCEL":
				case "X":
					deactivate();
					break;
			}
		}
	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public AddExchangeBlueprintPanel getInputPanel() {
		return panel;
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	public static class AddExchangeBlueprintPanel extends GUIInputPanel {

		private ExchangeData exchangeData;
		private GUIActivatableTextBar nameInput;
		private GUIActivatableTextBar descriptionInput;
		private GUIActivatableTextBar priceInput;
		private GUIActivatableTextBar countInput;
		private PlayerBlockTypeDropdownInputNew itemDisplay;
		private PlayerSearchableDropdownInput catalogSelectInput;

		public AddExchangeBlueprintPanel(InputState state, GUICallback guiCallback) {
			super("Add_Exchange_Blueprint_Panel", state, 500, 500, guiCallback, Lng.str("Add Blueprint"), "");
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			exchangeData = new ExchangeData();
			GUIHorizontalButtonTablePane buttonPane = null;
			if(mode == ExchangeDialog.SHIPS || mode == ExchangeDialog.STATIONS) {
				buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, contentPane.getContent(0));
				buttonPane.onInit();
				buttonPane.addButton(0, 0, Lng.str("SELECT ENTRY"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement callingGuiElement, MouseEvent event) {
						if(event.pressedLeftMouse()) {
							HashSet<CatalogPermission> catalogEntries = getCatalogEntries();
							ObjectArrayList<GUIElement> entries = new ObjectArrayList<>();
							for(CatalogPermission permission : catalogEntries) {
								GUIAncor guiAncor = new GUIAncor(getState(), 300, 22);
								guiAncor.setUserPointer(permission);
								GUITextOverlay t = new GUITextOverlay(30, 10, FontLibrary.FontSize.SMALL, getState());
								t.setTextSimple(permission.getUid());
								t.setPos(2, 2, 0);
								t.setUserPointer(permission);
								guiAncor.attach(t);
								entries.add(guiAncor);
							}

							(catalogSelectInput = new PlayerSearchableDropdownInput("Add_Blueprint_Dialog_Select_Entry", (GameClientState) getState(), Lng.str("Select Blueprint"), entries) {

								@Override
								public ObjectArrayList<GUIElement> filterElements(String text) {
									ObjectArrayList<GUIElement> elements = new ObjectArrayList<>();
									for(GUIElement element : this.elements) {
										if(text.isEmpty() || (element.getUserPointer() instanceof CatalogPermission && ((CatalogPermission) element.getUserPointer()).getUid().toLowerCase().contains(text.toLowerCase()))) {
											elements.add(element);
										}
									}
									return elements;
								}

								public CatalogControlManager getPlayerCatalogControlManager() {
									return getState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getCatalogControlManager();
								}

								@Override
								public void onDeactivate() {
									getPlayerCatalogControlManager().suspend(false);
								}

								@Override
								public boolean isOccluded() {
									return false;
								}

								@Override
								public void pressedOK(GUIListElement current) {
									if(current != null && current.getContent().getUserPointer() != null) {
										if(current.getContent().getUserPointer() instanceof CatalogPermission) {
											CatalogPermission permission = (CatalogPermission) current.getContent().getUserPointer();
											exchangeData.setFromCatalogEntry(permission);
											nameInput.setText(permission.getUid());
											descriptionInput.setText(permission.description);
											priceInput.setText(String.valueOf(1));
											exchangeData.setClassification(permission.getClassification());
											exchangeData.setProducer(GameClient.getClientPlayerState().getName());
										}
									}
									deactivate();
								}
							}).activate();
						}
					}

					private HashSet<CatalogPermission> getCatalogEntries() {
						HashSet<CatalogPermission> catalogEntries = new HashSet<>();
						if(mode == ExchangeDialog.SHIPS) {
							for(CatalogPermission permission : GameClient.getClientPlayerState().getCatalog().getPersonalCatalog()) {
								if(permission.type == BlueprintType.SHIP) {
									// Only allow ships to be added to the exchange data
									catalogEntries.add(permission);
								}
							}
						} else if(mode == ExchangeDialog.STATIONS) {
							for(CatalogPermission permission : GameClient.getClientPlayerState().getCatalog().getPersonalCatalog()) {
								if(permission.type == BlueprintType.SPACE_STATION) {
									// Only allow stations to be added to the exchange data
									catalogEntries.add(permission);
								}
							}
						}
						return catalogEntries;
					}

					@Override
					public boolean isOccluded() {
						return catalogSelectInput != null && catalogSelectInput.isActive();
					}
				}, new GUIActivationCallback() {
					@Override
					public boolean isVisible(InputState state) {
						return true;
					}

					@Override
					public boolean isActive(InputState state) {
						return catalogSelectInput == null || !catalogSelectInput.isActive();
					}
				});
				contentPane.getContent(0).attach(buttonPane);

				nameInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 64, 1, "Name", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						exchangeData.setName(s); // Set the name in the exchange data object
						return s; // Return the current string to be displayed in the text box
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				nameInput.setPos(0, buttonPane.getPos().y + buttonPane.getHeight(), 0);
				nameInput.setText(exchangeData.getName());
				contentPane.getContent(0).attach(nameInput);

				priceInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 3, 1, "Price (In Bars)", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						try {
							int price = Integer.parseInt(s.trim());
							exchangeData.setPrice(price); // Set the price in the exchange data object
						} catch(NumberFormatException e) {
							exchangeData.setPrice(1); // Default to 1 if parsing fails
						}
						return String.valueOf(exchangeData.getPrice());
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				priceInput.setPos(0, nameInput.getPos().y + nameInput.getHeight() + 2, 0);
				contentPane.getContent(0).attach(priceInput);

				descriptionInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 512, 2, "Description", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						exchangeData.setDescription(s); // Set the description in the exchange data object
						return s; // Return the current string to be displayed in the text box
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				descriptionInput.setPos(0, priceInput.getPos().y + priceInput.getHeight() + 2, 0);
			} else {
				if(mode == ExchangeDialog.ITEMS) {
					buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, contentPane.getContent(0));
					buttonPane.onInit();
					buttonPane.addButton(0, 0, Lng.str("SELECT ITEM"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse()) {
								(itemDisplay = new PlayerBlockTypeDropdownInputNew("Add_Exchange_Item_Dialog_Select_Item", (GameClientState) getState(), Lng.str("Select Item"), new ObjectArrayList<GUIElement>(), 1, 1, false) {
									@Override
									public void onAdditionalElementOk(Object o) {

									}

									@Override
									public void onOk(ElementInformation elementInformation) {
										if(elementInformation != null) {
											exchangeData.setItemId(elementInformation.getId());
											exchangeData.setItemCount(itemDisplay.getNumberValue());
											nameInput.setText(elementInformation.getName());
											descriptionInput.setText(elementInformation.getDescription());
										}
										deactivate();
									}

									@Override
									public void onOkMeta(MetaObject metaObject) {

									}
								}).activate();
							}
						}

						@Override
						public boolean isOccluded() {
							return itemDisplay != null && itemDisplay.isActive();
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return itemDisplay == null || !itemDisplay.isActive();
						}
					});
					contentPane.getContent(0).attach(buttonPane);
				} else if(mode == ExchangeDialog.WEAPONS) {
					buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, contentPane.getContent(0));
					buttonPane.onInit();
					buttonPane.addButton(0, 0, Lng.str("SELECT ITEM"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse()) {
								(itemDisplay = new PlayerBlockTypeDropdownInputNew("Add_Exchange_Item_Dialog_Select_Item", (GameClientState) getState(), Lng.str("Select Item"), new ObjectArrayList<GUIElement>(), 1, 1, true) {
									@Override
									public void onAdditionalElementOk(Object o) {

									}

									@Override
									public void onOk(ElementInformation elementInformation) {

									}

									@Override
									public void onOkMeta(MetaObject metaObject) {
										if(metaObject instanceof Weapon) {
											exchangeData.setItemId(metaObject.getSubObjectId());
											exchangeData.setItemCount(itemDisplay.getNumberValue());
											nameInput.setText(metaObject.getName());
										}
										deactivate();
									}

									@Override
									public ObjectArrayList<GUIElement> getElements(GameClientState state, String contain, ObjectArrayList<GUIElement> additionalElements) {
										ObjectArrayList<GUIElement> elements = new ObjectArrayList<>();
										for(Weapon.WeaponSubType weaponSubType : Weapon.WeaponSubType.values()) {
											GUIAncor guiAncor = new GUIAncor(state, 800, 32);
											elements.add(guiAncor);
											GUITextOverlay t = new GUITextOverlay(100, 32, FontLibrary.getBoldArial12White(), state);
											t.setTextSimple(ItemUtils.getSubTypeName(weaponSubType));
											guiAncor.setUserPointer(weaponSubType.type);
											GUIBlockSprite b = new GUIBlockSprite(state, weaponSubType.type);
											b.setLayer(-1);
											b.getScale().set(0.5f, 0.5f, 0.5f);
											guiAncor.attach(b);
											t.getPos().x = 50;
											t.getPos().y = 7;
											guiAncor.attach(t);
										}
										return elements;
									}
								}).activate();
							}
						}

						@Override
						public boolean isOccluded() {
							return itemDisplay != null && itemDisplay.isActive();
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return itemDisplay == null || !itemDisplay.isActive();
						}
					});
					contentPane.getContent(0).attach(buttonPane);
				}
				nameInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 64, 1, "Name", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						exchangeData.setName(s); // Set the name in the exchange data object
						return s; // Return the current string to be displayed in the text box
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				nameInput.setText(exchangeData.getName());
				nameInput.setPos(0, buttonPane.getPos().y + buttonPane.getHeight(), 0);
				contentPane.getContent(0).attach(nameInput);

				priceInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 3, 1, "Price (In Bars)", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						try {
							int price = Integer.parseInt(s.trim());
							exchangeData.setPrice(price); // Set the price in the exchange data object
						} catch(NumberFormatException e) {
							exchangeData.setPrice(1); // Default to 1 if parsing fails
						}
						return String.valueOf(exchangeData.getPrice());
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				priceInput.setPos(0, nameInput.getPos().y + nameInput.getHeight() + 2, 0);
				contentPane.getContent(0).attach(priceInput);

				descriptionInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 512, 2, "Description", contentPane.getContent(0), new TextCallback() {
					@Override
					public String[] getCommandPrefixes() {
						return new String[0];
					}

					@Override
					public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
						return "";
					}

					@Override
					public void onFailedTextCheck(String msg) {

					}

					@Override
					public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

					}

					@Override
					public void newLine() {

					}
				}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
					@Override
					public String onInputChanged(String s) {
						exchangeData.setDescription(s); // Set the description in the exchange data object
						return s; // Return the current string to be displayed in the text box
					}
				}) {
					@Override
					public void cleanUp() {

					}
				};
				descriptionInput.setPos(0, priceInput.getPos().y + priceInput.getHeight() + 2, 0);
			}
			descriptionInput.setText(exchangeData.getDescription());
			contentPane.getContent(0).attach(descriptionInput);
		}

		private boolean isValid() {
			if(exchangeData != null) {
				switch(mode) {
					case ExchangeDialog.SHIPS:
					case ExchangeDialog.STATIONS:
						return !Objects.equals(exchangeData.getProducer().toUpperCase(Locale.ENGLISH), "NO FACTION") && !exchangeData.getName().isEmpty() && !exchangeData.getDescription().isEmpty() && exchangeData.getPrice() > 0;
					case ExchangeDialog.WEAPONS:
					case ExchangeDialog.ITEMS:
						return !Objects.equals(exchangeData.getProducer().toUpperCase(Locale.ENGLISH), "NO FACTION") && !exchangeData.getName().isEmpty() && !exchangeData.getDescription().isEmpty() && exchangeData.getPrice() > 0 && exchangeData.getItemCount() > 0;
				}
			}
			return false;
		}

		public ExchangeData getExchangeData() {
			if(isValid()) return exchangeData;
			else return null;
		}
	}
}
