package thederpgamer.edencore.gui.bankingmenu;

import api.common.GameClient;
import api.utils.game.PlayerUtils;
import api.utils.gui.SimplePlayerTextInput;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.controller.PlayerOkCancelInput;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.playerdata.PlayerData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class BankingDialog extends PlayerInput {

	private final BankingPanel panel;

	public BankingDialog() {
		super(GameClient.getClientState());
		(panel = new BankingPanel(getState(), this)).onInit();
	}

	@Override
	public void onDeactivate() {

	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public BankingPanel getInputPanel() {
		return panel;
	}

	public static class BankingPanel extends GUIInputPanel {

		private GUITextOverlay storedCreditsText;
		private PlayerData playerData;

		public BankingPanel(InputState state, GUICallback guiCallback) {
			super("BankingPanel", state, 800, 500, guiCallback, "", "");
			playerData = PlayerDataManager.getInstance(false).getClientOwnData();
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(48);
			storedCreditsText = new GUITextOverlay(10, 10, getState());
			storedCreditsText.setFont(FontLibrary.FontSize.MEDIUM.getFont());
			storedCreditsText.onInit();
			storedCreditsText.setTextSimple("Stored Credits: " + playerData.getStoredCredits());
			contentPane.getContent(0).attach(storedCreditsText);

			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 3, 1, contentPane.getContent(0));
			buttonPane.onInit();
			buttonPane.addButton(0, 0, "Deposit Credits", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new SimplePlayerTextInput("Deposit Credits", "Enter Amount to Deposit") {
							@Override
							public boolean onInput(String s) {
								try {
									long amount = Long.parseLong(s.trim());
									if(amount > 0 && amount <= GameClient.getClientPlayerState().getCredits()) {
										playerData.setStoredCredits(playerData.getStoredCredits() + amount);
										PlayerDataManager.getInstance(false).setPlayerCredits(GameClient.getClientPlayerState(), GameClient.getClientPlayerState().getCredits() - amount);
										return true;
									}
								} catch(NumberFormatException ignored) {
								}
								return false;
							}
						}).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return GameClient.getClientPlayerState().getCredits() <= 0;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return GameClient.getClientPlayerState().getCredits() > 0;
				}
			});
			buttonPane.addButton(1, 0, "Withdraw Credits", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						(new SimplePlayerTextInput("Withdraw Credits", "Enter Amount to Withdraw") {
							@Override
							public boolean onInput(String s) {
								try {
									long amount = Long.parseLong(s.trim());
									if(amount > 0 && amount <= playerData.getStoredCredits()) {
										playerData.setStoredCredits(playerData.getStoredCredits() - amount);
										PlayerDataManager.getInstance(false).setPlayerCredits(GameClient.getClientPlayerState(), GameClient.getClientPlayerState().getCredits() + amount);
										return true;
									}
								} catch(NumberFormatException ignored) {
								}
								return false;
							}
						}).activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return playerData.getStoredCredits() <= 0;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return playerData.getStoredCredits() > 0;
				}
			});
			buttonPane.addButton(2, 0, "Send Credits", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {

				private GUIActivatableTextBar amountInput;
				private GUIActivatableTextBar playerInput;
				private GUIActivatableTextBar subjectInput;
				private GUIActivatableTextBar messageInput;

				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						PlayerOkCancelInput input = new PlayerOkCancelInput("Send_Credits", getState(), "Send Credits", "") {
							@Override
							public void onDeactivate() {

							}

							@Override
							public void pressedOK() {
								String amountText = amountInput.getText().trim();
								String playerText = playerInput.getText().trim();
								long amount = 0;
								PlayerData playerData = null;
								try {
									amount = Long.parseLong(amountText);
									playerData = PlayerDataManager.getInstance(false).getFromName(playerText, false);
								} catch(Exception ignored) {}
								if(amount > 0) {
									if(playerData == null) {
										PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Invalid player name.");
										return;
									}
									if(amount <= playerData.getStoredCredits()) {
										if(playerData.getName().equals(GameClient.getClientPlayerState().getName())) {
											PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "You cannot send credits to yourself.");
											return;
										}
										playerData.setStoredCredits(playerData.getStoredCredits() + amount);
										playerData.addTransaction(new PlayerData.PlayerBankTransactionData(amount, PlayerDataManager.getInstance(false).getClientOwnData(), playerData, subjectInput.getText(), messageInput.getText(), PlayerData.PlayerBankTransactionData.TransactionType.TRANSFER));
										PlayerDataManager.getInstance(false).getClientOwnData().setStoredCredits(playerData.getStoredCredits() - amount);
										deactivate();
									} else PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Invalid amount to send.");
								} else PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "Invalid amount to send.");
							}
						};
						input.getInputPanel().background.onInit();
						GUIContentPane contentPane = ((GUIDialogWindow) input.getInputPanel().background).getMainContentPane();
						if(contentPane.getContent(0) == null) contentPane.setContent(0, new GUIAncor(getState()));
						amountInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 10, 1, Lng.str("Amount"), contentPane.getContent(0), new TextCallback() {
							@Override
							public String[] getCommandPrefixes() {
								return new String[0];
							}

							@Override
							public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
								return "";
							}

							@Override
							public void onFailedTextCheck(String s) {

							}

							@Override
							public void onTextEnter(String s, boolean b, boolean b1) {

							}

							@Override
							public void newLine() {

							}
						}, new OnInputChangedCallback() {
							@Override
							public String onInputChanged(String s) {
								return s;
							}
						});
						contentPane.getContent(0).attach(amountInput);

						playerInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 64, 1, Lng.str("Player Name"), contentPane.getContent(0), new TextCallback() {
							@Override
							public String[] getCommandPrefixes() {
								return new String[0];
							}

							@Override
							public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
								return "";
							}

							@Override
							public void onFailedTextCheck(String s) {

							}

							@Override
							public void onTextEnter(String s, boolean b, boolean b1) {

							}

							@Override
							public void newLine() {

							}
						}, new OnInputChangedCallback() {
							@Override
							public String onInputChanged(String s) {
								return s;
							}
						});
						contentPane.getContent(0).attach(playerInput);
						playerInput.setPos(0, amountInput.getHeight() + amountInput.getPos().y + 2, 0);

						subjectInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 64, 1, Lng.str("Subject"), contentPane.getContent(0), new TextCallback() {
							@Override
							public String[] getCommandPrefixes() {
								return new String[0];
							}

							@Override
							public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
								return "";
							}

							@Override
							public void onFailedTextCheck(String s) {

							}

							@Override
							public void onTextEnter(String s, boolean b, boolean b1) {

							}

							@Override
							public void newLine() {

							}
						}, new OnInputChangedCallback() {
							@Override
							public String onInputChanged(String s) {
								return s;
							}
						});
						contentPane.getContent(0).attach(subjectInput);
						subjectInput.setPos(0, playerInput.getHeight() + playerInput.getPos().y + 2, 0);

						messageInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 512, 3, Lng.str("Message"), contentPane.getContent(0), new TextCallback() {
							@Override
							public String[] getCommandPrefixes() {
								return new String[0];
							}

							@Override
							public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
								return "";
							}

							@Override
							public void onFailedTextCheck(String s) {

							}

							@Override
							public void onTextEnter(String s, boolean b, boolean b1) {

							}

							@Override
							public void newLine() {

							}
						}, contentPane.getTextboxes().get(0), new OnInputChangedCallback() {
							@Override
							public String onInputChanged(String s) {
								return s;
							}
						}) {
							@Override
							public void cleanUp() {

							}
						};
						contentPane.getContent(0).attach(messageInput);
						messageInput.setPos(0, subjectInput.getHeight() + subjectInput.getPos().y + 2, 0);
						input.activate();
					}
				}

				@Override
				public boolean isOccluded() {
					return getState().getController().getPlayerInputs().isEmpty();
				}
			}, new GUIActivationHighlightCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return playerData.getStoredCredits() > 0;
				}

				@Override
				public boolean isHighlighted(InputState inputState) {
					return playerData.getStoredCredits() > 0;
				}
			});
			contentPane.getContent(0).attach(buttonPane);
			buttonPane.getPos().y += storedCreditsText.getHeight() + 10;

			contentPane.addNewTextBox(300);
			PlayerBankingTransactionScrollableList transactionList = new PlayerBankingTransactionScrollableList(getState(), contentPane.getContent(1), playerData);
			transactionList.onInit();
			contentPane.getContent(1).attach(transactionList);
		}

		@Override
		public void draw() {
			super.draw();
			storedCreditsText.setTextSimple("Stored Credits: " + playerData.getStoredCredits());
		}
	}
}
