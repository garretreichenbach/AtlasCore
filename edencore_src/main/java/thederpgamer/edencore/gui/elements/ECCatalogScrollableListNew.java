package thederpgamer.edencore.gui.elements;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.controller.manager.ingame.catalog.CatalogPermissionEditDialog;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.data.GameStateInterface;
import org.schema.game.client.view.BuildModeDrawer;
import org.schema.game.client.view.gui.catalog.newcatalog.CatalogScrollableListNew;
import org.schema.game.client.view.gui.catalog.newcatalog.GUIBlueprintConsistenceScrollableList;
import org.schema.game.common.data.player.BlueprintPlayerHandleRequest;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.SimplePlayerCommands;
import org.schema.game.common.data.player.catalog.CatalogManager;
import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.network.objects.remote.RemoteBlueprintPlayerRequest;
import org.schema.game.server.data.EntityRequest;
import org.schema.game.server.data.admin.AdminCommands;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.GuiDateFormats;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.buildsectordata.BuildSectorData;
import thederpgamer.edencore.data.buildsectordata.BuildSectorDataManager;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class ECCatalogScrollableListNew extends CatalogScrollableListNew {

	protected CatalogPermission selected;
	private final boolean showPrice;
	private final boolean selectSingle;
	private final int mode;
	private boolean spawnDocked;
	private boolean useOwnFaction;
	private final GUIElement p;

	public ECCatalogScrollableListNew(InputState state, GUIElement p, int personalOnly, boolean showPrice, boolean selectSingle) {
		super(state, p, personalOnly, showPrice, selectSingle);
		this.showPrice = showPrice;
		this.selectSingle = selectSingle;
		this.p = p;
		mode = personalOnly;
		useOwnFaction = ((GameClientState) getState()).getPlayer().getFactionId() > 0;
		((GameClientState) getState()).getCatalogManager().addObserver(this);
		((GameClientState) getState()).getPlayer().getCatalog().addObserver(this);
	}

	@Override
	public void updateListEntries(GUIElementList mainList, Set<CatalogPermission> collection) {
		mainList.deleteObservers();
		mainList.addObserver(this);
		final CatalogManager catalogManager = ((GameClientState) getState()).getGameState().getCatalogManager();
		for(final CatalogPermission f : collection) {
			assert (f.getUid() != null);
			GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable typeText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable massText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable ratingText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable priceText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable classText = new GUITextOverlayTable(10, 10, getState());
			GUITextOverlayTable dateText = new GUITextOverlayTable(10, 10, getState());
			GUIClippedRow nameAnchorP = new GUIClippedRow(getState());
			GUIClippedRow classAnchorP = new GUIClippedRow(getState());
			nameAnchorP.attach(nameText);
			classAnchorP.attach(classText);
			nameText.setTextSimple(f.getUid());
			typeText.setTextSimple(f.type.type.getName());
			priceText.setTextSimple(f.price);
			classText.setTextSimple(f.getClassification().getName());
			massText.setTextSimple(StringTools.massFormat(f.mass));
			ratingText.setTextSimple(new Object() {
				@Override
				public String toString() {
					return String.valueOf(f.rating);
				}
			});
			dateText.setTextSimple(GuiDateFormats.mediumFormat.format(f.date));
			int heightInset = 5;
			nameText.getPos().y = heightInset;
			typeText.getPos().y = heightInset;
			massText.getPos().y = heightInset;
			ratingText.getPos().y = heightInset;
			priceText.getPos().y = heightInset;
			classText.getPos().y = heightInset;
			dateText.getPos().y = heightInset;
			final CatalogRow row;
			if(showPrice) row = new CatalogRow(getState(), f, nameAnchorP, typeText, classAnchorP, dateText, massText, priceText, ratingText);
			else row = new CatalogRow(getState(), f, nameAnchorP, typeText, classAnchorP, dateText, massText, ratingText);
			if(!selectSingle) {
				float height = 56.0f;
				row.expanded = new GUIElementList(getState());

				final String owner = Lng.str("Owner: %s\n", f.ownerUID);
				final String created = Lng.str("Created: %s\n", GuiDateFormats.catalogEntryCreated.format(f.date));
				final GUITextOverlayTableInnerDescription description = new GUITextOverlayTableInnerDescription(10, 10, getState());
				description.setTextSimple(new Object() {
					@Override
					public String toString() {
						return owner + created + Lng.str("Description:") + f.description;
					}
				});
				description.setPos(4, 2, 0);

				GUIAncor descriptionAnchor = new GUIAncor(getState(), 100, Math.max(112, description.getTextHeight() + 12)) {
					@Override
					public void draw() {
						setWidth(p.getWidth() - 28.0f);
						if(description.getTextHeight() != getHeight()) {
							setChanged();
							notifyObservers();
							setHeight(description.getTextHeight());
							row.expanded.updateDim();
						}
						super.draw();
					}
				};
				descriptionAnchor.attach(description);
				row.expanded.add(new GUIListElement(descriptionAnchor, descriptionAnchor, getState()));

				if(f.score != null) {
					final GUIAncor statsAnchor = new GUIAncor(getState(), 100, 128) {
						@Override
						public void draw() {
							setWidth(p.getWidth() - 28.0f);
							super.draw();
						}
					};
					GUITextOverlayTable statText = new GUITextOverlayTable(10, 10, getState());
					List<Object> a = new ObjectArrayList<>();
					f.score.addStrings(a);
					statText.setText(a);
					statText.setPos(4, 0, 0);
					statsAnchor.attach(statText);
					GUIPolygonStats st = new GUIPolygonStats(getState(), f.score) {
						@Override
						public void draw() {
							setPos(statsAnchor.getWidth() - (getWidth() * 2), -40, 0);
							super.draw();
						}
					};
					statsAnchor.attach(st);
					row.expanded.add(new GUIListElement(statsAnchor, statsAnchor, getState()));
				}

				GUIAncor buttonAnchor = new GUIAncor(getState(), 100.0f, 28.0f) {
					@Override
					public void draw() {
						setWidth(p.getWidth() - 28.0f);
						super.draw();
					}
				};

				BuildSectorData buildSectorData = BuildSectorDataManager.getInstance(false).getCurrentBuildSector(((GameClientState) getState()).getPlayer());
				final boolean canSpawn = isPlayerAdmin() || (buildSectorData != null && buildSectorData.getPermission(((GameClientState) getState()).getPlayer().getName(), BuildSectorData.PermissionTypes.SPAWN));
				int columns = 2;
				if(isPlayerAdmin() || buildSectorData != null) columns++;
				if(canEdit(f)) columns += 4;

				GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), columns, 1, buttonAnchor);
				buttonPane.onInit();
				PlayerState player = ((GameClientState) getState()).getPlayer();

				int x = 0;
				buttonPane.addButton(x, 0, Lng.str("BUY"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							if(((GameClientState) getState()).getGameState().isBuyBBWithCredits()) buyEntry(f);
							else buyEntryAsMeta(f);
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
				x++;
				if(canEdit(f)) {
					buttonPane.addButton(x, 0, Lng.str("EDIT DESCRIPTION"), GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse() && canEdit(f)) {
								(new PlayerTextAreaInput("CatalogScrollableListNew_EDIT_DESC", (GameClientState) getState(), 140, 3, Lng.str("Edit Faction Description"), "", f.description) {
									@Override
									public String[] getCommandPrefixes() {
										return null;
									}

									@Override
									public boolean isOccluded() {
										return false;
									}

									@Override
									public String handleAutoComplete(String s, TextCallback callback, String prefix) {
										return null;
									}

									@Override
									public void onDeactivate() {
									}

									@Override
									public boolean onInput(String entry) {
										if(canEdit(f)) {
											f.description = entry;
											if(mode == ADMIN && getState().getPlayer().getNetworkObject().isAdminClient.get()) {
												f.changeFlagForced = true;
												catalogManager.clientRequestCatalogEdit(f);
											} else {
												f.ownerUID = getState().getPlayer().getName();
												catalogManager.clientRequestCatalogEdit(f);
											}
											return true;
										}
										return false;
									}

									@Override
									public void onFailedTextCheck(String msg) {
									}
								}).activate();
							}
						}

						@Override
						public boolean isOccluded() {
							return !canEdit(f);
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return canEdit(f);
						}
					});
					x++;
					buttonPane.addButton(x, 0, Lng.str("EDIT PERMISSIONS"), GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse() && canEdit(f)) (new CatalogPermissionEditDialog(((GameClientState) getState()), f)).activate();
						}

						@Override
						public boolean isOccluded() {
							return !canEdit(f);
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return canEdit(f);
						}
					});
					x++;
				}
				buttonPane.addButton(x, 0, Lng.str("CONSISTENCE"), GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
					@Override
					public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
						if(mouseEvent.pressedLeftMouse()) {
							PlayerGameOkCancelInput detailsPopup = new PlayerGameOkCancelInput("blueprintConsistence", (GameClientState) getState(), 400, 400, Lng.str("Blueprint Details"), "") {
								@Override
								public void onDeactivate() {
								}

								@Override
								public void pressedOK() {
									deactivate();
								}
							};
							((GameClientState) getState()).getPlayer().sendSimpleCommand(SimplePlayerCommands.REQUEST_BLUEPRINT_ITEM_LIST, f.getUid());
							detailsPopup.getInputPanel().onInit();
							((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().setTextBoxHeightLast(25);
							((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().addNewTextBox(40);
							GUIHorizontalButtonTablePane buttons = new GUIHorizontalButtonTablePane(getState(), 1, 1, ((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().getContent(0));
							buttons.onInit();
							((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().getContent(0).attach(buttons);

							final GUIBlueprintConsistenceScrollableList sc = new GUIBlueprintConsistenceScrollableList(getState(), ((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().getContent(1));
							sc.onInit();
							((GUIDialogWindow) detailsPopup.getInputPanel().background).getMainContentPane().getContent(1).attach(sc);
							detailsPopup.getInputPanel().setOkButton(false);
							detailsPopup.getInputPanel().setOkButtonText(Lng.str("DONE"));
							buttons.addButton(0, 0, Lng.str("Blocks/Resources"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
								@Override
								public void callback(GUIElement callingGuiElement, MouseEvent event) {
									if(event.pressedLeftMouse()) {
										sc.setResources(!sc.isResources());
										((GameClientState) getState()).getPlayer().sendSimpleCommand(SimplePlayerCommands.REQUEST_BLUEPRINT_ITEM_LIST, f.getUid());
									}
								}

								@Override
								public boolean isOccluded() {
									return false;
								}
							}, new GUIActivationCallback() {
								@Override
								public boolean isVisible(InputState state) {
									return true;
								}

								@Override
								public boolean isActive(InputState state) {
									return true;
								}
							});
							detailsPopup.activate();
						}
					}

					@Override
					public boolean isOccluded() {
						return !isActive();
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
				x++;
				if(canSpawn) {
					buttonPane.addButton(x, 0, Lng.str("SPAWN"), GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse()) {
								load(f);
							}
						}

						@Override
						public boolean isOccluded() {
							return !canSpawn;
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return canSpawn;
						}
					});
					x++;
				}
				if(canEdit(f)) {
					buttonPane.addButton(x, 0, Lng.str("DELETE"), GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse()) {
								if(canEdit(f)) deleteEntry(f);
							}
						}

						@Override
						public boolean isOccluded() {
							return !canEdit(f);
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return canEdit(f);
						}
					});
					x++;
					buttonPane.addButton(x, 0, Lng.str("CHANGE OWNER"), GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
						@Override
						public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
							if(mouseEvent.pressedLeftMouse()) {
								if(canEdit(f)) changeOwner(f);
							}
						}

						@Override
						public boolean isOccluded() {
							return !isPlayerAdmin() && !canEdit(f);
						}
					}, new GUIActivationCallback() {
						@Override
						public boolean isVisible(InputState inputState) {
							return true;
						}

						@Override
						public boolean isActive(InputState inputState) {
							return isPlayerAdmin() || canEdit(f);
						}
					});
					x++;
				}
				buttonAnchor.attach(buttonPane);
				row.expanded.add(new GUIListElement(buttonAnchor, buttonAnchor, getState()));

				row.onInit();
				mainList.addWithoutUpdate(row);
			}
			mainList.updateDim();
		}
	}

	private void changeOwner(final CatalogPermission permission) {
		String description = Lng.str("Change the owner of \"%s\"", permission.getUid());
		PlayerGameTextInput pp = new PlayerGameTextInput("CatalogExtendedPanel_changeOwner", (GameClientState) getState(), 50, Lng.str("Change Owner"), description, permission.ownerUID) {
			@Override
			public void onDeactivate() {
			}

			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public String handleAutoComplete(String s, TextCallback callback, String prefix) {
				return s;
			}

			@Override
			public boolean isOccluded() {
				return getState().getController().getPlayerInputs().indexOf(this) != getState().getController().getPlayerInputs().size() - 1;
			}

			@Override
			public void onFailedTextCheck(String msg) {
				setErrorMessage(Lng.str("ONWER INVALID: %s", msg));

			}

			@Override
			public boolean onInput(String entry) {
				if(getState().getPlayer().getNetworkObject().isAdminClient.get()) {
					CatalogPermission p = new CatalogPermission(permission);
					p.ownerUID = entry;
					p.changeFlagForced = true;
					getState().getCatalogManager().clientRequestCatalogEdit(p);
				} else {
					System.err.println("ERROR: CANNOT CHANGE OWNER (PERMISSION DENIED)");
				}
				return true;
			}

		};
		pp.setInputChecker(new InputChecker() {
			@Override
			public boolean check(String s, TextCallback textCallback) {
				if(EntityRequest.isShipNameValid(s)) return true;
				else {
					textCallback.onFailedTextCheck(Lng.str("Must only contain Letters or numbers or (_-)!"));
					return false;
				}
			}
		});
		pp.activate();
	}

	private void deleteEntry(final CatalogPermission permission) {
		boolean admin = ((GameClientState) getState()).getPlayer().getNetworkObject().isAdminClient.get();
		if(!admin && !((GameClientState) getState()).getPlayer().getName().toLowerCase(Locale.ENGLISH).equals(permission.ownerUID.toLowerCase(Locale.ENGLISH))) {
			((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("ERROR:\nCannot delete!\nYou do not own this!"), 0);
		} else {

			PlayerGameOkCancelInput confirm = new PlayerGameOkCancelInput("CatalogScrollableListNew_deleteEntry", (GameClientState) getState(), Lng.str("Confirm"), Lng.str("Do you really want to delete this entry?\n(a backup will be created on the server)")) {

				@Override
				public boolean isOccluded() {
					return false;
				}

				@Override
				public void onDeactivate() {
				}

				@Override
				public void pressedOK() {
					if(getState().getPlayer().getNetworkObject().isAdminClient.get()) {
						CatalogPermission p = new CatalogPermission(permission);
						p.changeFlagForced = true;
						getState().getCatalogManager().clientRequestCatalogRemove(p);
					} else {
						CatalogPermission p = new CatalogPermission(permission);
						p.ownerUID = getState().getPlayer().getName();
						getState().getCatalogManager().clientRequestCatalogRemove(p);
					}

					deactivate();
				}
			};
			confirm.activate();
		}
	}

	private void buyEntry(final CatalogPermission permission) {

		if(!((GameClientState) getState()).isInShopDistance()) {
			((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("ERROR:\nCannot buy!\nYou are not near a shop!"), 0);
		}

		String description = Lng.str("Please type in a name for your new Ship!");
		PlayerGameTextInput pp = new PlayerGameTextInput("CatalogScrollableListNew_f_NewShip", (GameClientState) getState(), 400, 240, 50, Lng.str("New Ship"), description, permission.getUid() + "_" + System.currentTimeMillis()) {
			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public String handleAutoComplete(String s, TextCallback callback, String prefix) {
				return s;
			}

			@Override
			public boolean isOccluded() {
				return getState().getController().getPlayerInputs().indexOf(this) != getState().getController().getPlayerInputs().size() - 1;
			}

			@Override
			public void onDeactivate() {
			}

			@Override
			public void onFailedTextCheck(String msg) {
				setErrorMessage(Lng.str("SHIPNAME INVALID: %s", msg));
			}

			@Override
			public boolean onInput(String entry) {
				if(getState().getCharacter() == null || getState().getCharacter().getPhysicsDataContainer() == null || !getState().getCharacter().getPhysicsDataContainer().isInitialized()) {
					System.err.println("[ERROR] Character might not have been initialized");
					return false;
				}
				System.err.println("[CLIENT] BUYING CATALOG ENTRY: " + permission.getUid() + " FOR " + getState().getPlayer().getNetworkObject());
				BlueprintPlayerHandleRequest req = new BlueprintPlayerHandleRequest();
				req.catalogName = permission.getUid();
				req.entitySpawnName = entry;
				req.save = false;
				req.toSaveShip = -1;
				req.directBuy = true;
				req.setOwnFaction = useOwnFaction;
				getState().getPlayer().getNetworkObject().catalogPlayerHandleBuffer.add(new RemoteBlueprintPlayerRequest(req, false));
				return true;
			}

		};
		pp.setInputChecker(new InputChecker() {
			@Override
			public boolean check(String s, TextCallback textCallback) {
				if(EntityRequest.isShipNameValid(s)) return true;
				else {
					textCallback.onFailedTextCheck(Lng.str("Must only contain letters or numbers or (_-)!"));
					return false;
				}
			}
		});
		pp.getInputPanel().onInit();
		GUICheckBoxTextPair useFact = new GUICheckBoxTextPair(getState(), Lng.str("Set as own Faction (needs faction block)"), 280, FontLibrary.getBlenderProMedium14(), 24) {

			@Override
			public boolean isActivated() {
				return useOwnFaction;
			}

			@Override
			public void deactivate() {
				useOwnFaction = false;
			}

			@Override
			public void activate() {
				if(((GameClientState) getState()).getPlayer().getFactionId() > 0) useOwnFaction = true;
				else {
					((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("You are not in a faction!"), 0);
					useOwnFaction = false;
				}
			}
		};
		useFact.setPos(3, 35, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useFact);

		GUICheckBoxTextPair useSpawnDocked = new GUICheckBoxTextPair(getState(), new Object() {
			@Override
			public String toString() {
				if(BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable()) return Lng.str("Spawn docked");
				else return Lng.str("Spawn docked (must be aiming at a rail block)");
			}
		}, 280, FontLibrary.getBlenderProMedium14(), 24) {

			@Override
			public boolean isActivated() {
				return spawnDocked;
			}

			@Override
			public void deactivate() {
				spawnDocked = false;
			}

			@Override
			public void activate() {
				if((BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable())) spawnDocked = true;
				else {
					((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("Must be aiming at a rail block!"), 0);
					spawnDocked = false;
				}
			}
		};
		useSpawnDocked.setPos(3, 65, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useSpawnDocked);
		pp.activate();
	}

	private void buyEntryAsMeta(final CatalogPermission permission) {
		if(!((GameClientState) getState()).isInShopDistance()) {
			((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("ERROR:\nCannot buy!\nYou are not near a shop!"), 0);
		}
		String title = Lng.str("Buy Blueprint of %s", permission.getUid());
		String price = Lng.str("This blueprint is free!");
		if(permission.getEntry() == BlueprintType.SPACE_STATION) {
			price = Lng.str("A station blueprint costs %s Credits!\nYou currently have %s Credits.", StringTools.formatSeperated(((GameStateInterface) getState()).getGameState().getStationCost()), StringTools.formatSeperated(((GameClientState) getState()).getPlayer().getCredits()));
		}
		String desc = Lng.str("%s\n\nThis will put the blueprint in your inventory.\nRight click on it to provide the necessary materials.", price);
		(new PlayerGameOkCancelInput("CatalogScrollableListNew_buyEntryAsMeta", (GameClientState) getState(), title, desc) {

			@Override
			public boolean isOccluded() {
				return false;
			}

			@Override
			public void pressedOK() {
				BlueprintPlayerHandleRequest req = new BlueprintPlayerHandleRequest();
				req.catalogName = permission.getUid();
				req.entitySpawnName = "";
				req.save = false;
				req.toSaveShip = -1;
				req.directBuy = false;
				if(spawnDocked && BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable()) {
					req.spawnOnId = BuildModeDrawer.currentPiece.getSegmentController().getId();
					req.spawnOnBlock = BuildModeDrawer.currentPiece.getAbsoluteIndex();
				}
				getState().getPlayer().getNetworkObject().catalogPlayerHandleBuffer.add(new RemoteBlueprintPlayerRequest(req, true));
				deactivate();
			}

			@Override
			public void onDeactivate() {
			}
		}).activate();
	}

	private void load(final CatalogPermission permission) {
		String description = Lng.str("Please type in a name for your new Ship!");
		PlayerGameTextInput pp = new PlayerGameTextInput("CatalogScrollableListNew_f_load", (GameClientState) getState(), 400, 240, 50, Lng.str("New Ship"), description, permission.getUid() + "_" + System.currentTimeMillis()) {
			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public String handleAutoComplete(String s, TextCallback callback, String prefix) {
				return s;
			}

			@Override
			public boolean isOccluded() {
				return getState().getController().getPlayerInputs().indexOf(this) != getState().getController().getPlayerInputs().size() - 1;
			}

			@Override
			public void onDeactivate() {
			}

			@Override
			public void onFailedTextCheck(String msg) {
				setErrorMessage(Lng.str("SHIPNAME INVALID:") + " " + msg);

			}

			@Override
			public boolean onInput(String entry) {
				if(getState().getCharacter() == null || getState().getCharacter().getPhysicsDataContainer() == null || !getState().getCharacter().getPhysicsDataContainer().isInitialized()) {
					System.err.println("[ERROR] Character might not have been initialized");
					return false;
				}
				System.err.println("[CLIENT] BUYING CATALOG ENTRY: " + permission.getUid() + " FOR " + getState().getPlayer().getNetworkObject());
				BlueprintPlayerHandleRequest req = new BlueprintPlayerHandleRequest();
				req.catalogName = permission.getUid();
				req.entitySpawnName = entry;
				req.save = false;
				req.toSaveShip = -1;
				req.directBuy = true;
				req.setOwnFaction = useOwnFaction;
				if(spawnDocked && BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable()) {
					req.spawnOnId = BuildModeDrawer.currentPiece.getSegmentController().getId();
					req.spawnOnBlock = BuildModeDrawer.currentPiece.getAbsoluteIndex();
				}
				getState().getController().sendAdminCommand(spawnDocked ? AdminCommands.LOAD_AS_FACTION_DOCKED : AdminCommands.LOAD_AS_FACTION, req.catalogName, req.entitySpawnName, req.setOwnFaction ? getState().getPlayer().getFactionId() : 0);
				return true;
			}

		};
		pp.setInputChecker(new InputChecker() {
			@Override
			public boolean check(String s, TextCallback textCallback) {
				if(EntityRequest.isShipNameValid(s)) return true;
				else {
					textCallback.onFailedTextCheck(Lng.str("Must only contain letters or numbers or (_-)!"));
					return false;
				}
			}
		});
		pp.getInputPanel().onInit();
		GUICheckBoxTextPair useFact = new GUICheckBoxTextPair(getState(), Lng.str("Set as own Faction (needs faction block)"), 280, FontLibrary.getBlenderProMedium14(), 24) {

			@Override
			public boolean isActivated() {
				return useOwnFaction;
			}

			@Override
			public void deactivate() {
				useOwnFaction = false;
			}

			@Override
			public void activate() {
				if(((GameClientState) getState()).getPlayer().getFactionId() > 0) {
					useOwnFaction = true;
				} else {
					((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("You are not in a faction!"), 0);
					useOwnFaction = false;
				}
			}
		};
		useFact.setPos(3, 35, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useFact);

		GUICheckBoxTextPair useSpawnDocked = new GUICheckBoxTextPair(getState(), new Object() {
			@Override
			public String toString() {
				if(BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable()) return Lng.str("Spawn docked");
				else return Lng.str("Spawn docked (must be aiming at a rail block)");
			}
		}, 280, FontLibrary.getBlenderProMedium14(), 24) {
			@Override
			public boolean isActivated() {
				return spawnDocked;
			}

			@Override
			public void deactivate() {
				spawnDocked = false;
			}

			@Override
			public void activate() {
				System.err.println("LOAD DOCKED: " + BuildModeDrawer.currentPiece);
				if((BuildModeDrawer.currentPiece != null && BuildModeDrawer.currentPiece.isValid() && BuildModeDrawer.currentPiece.getInfo().isRailDockable())) {
					spawnDocked = true;
				} else {
					((GameClientState) getState()).getController().popupAlertTextMessage(Lng.str("Must be aiming at a rail block!"), 0);
					spawnDocked = false;
				}
			}
		};
		useSpawnDocked.setPos(3, 65, 0);
		((GUIDialogWindow) pp.getInputPanel().background).getMainContentPane().getContent(0).attach(useSpawnDocked);
		pp.activate();
	}

	private class CatalogRow extends ScrollableTableList<CatalogPermission>.Row {

		public CatalogRow(InputState state, CatalogPermission f, GUIElement... elements) {
			super(state, f, elements);
			highlightSelect = true;
		}

		@Override
		public float getExtendedHighlightBottomDist() {
			return 32;
		}

		@Override
		protected boolean isSimpleSelected() {
			return selectSingle && selectedSingle == f;
		}

		@Override
		protected void clickedOnRow() {
			selectedSingle = f;
			super.clickedOnRow();
		}
	}
}
