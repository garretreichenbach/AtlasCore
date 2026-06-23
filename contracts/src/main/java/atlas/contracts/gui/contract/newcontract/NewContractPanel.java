package atlas.contracts.gui.contract.newcontract;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.gui.SimplePopup;
import atlas.contracts.AtlasContracts;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.manager.ConfigManager;
import atlas.core.network.PlayerActionCommandPacket;
import atlas.core.utils.GoldBarUtils;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.client.view.gui.advanced.tools.*;
import org.schema.game.common.controller.BlockTypeSearchRunnableManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewContractPanel extends GUIInputPanel implements BlockTypeSearchRunnableManager.BlockTypeSearchProgressCallback {

	private final GUICallback guiCallback;
	private GUIActivatableTextBar rewardInput;
	private GUIActivatableTextBar nameInput;       // bounty: target player name
	private GUIActivatableTextBar itemInput;       // items: count
	private GUIActivatableTextBar escortRouteInput; // escort: route length
	private GUIActivatableTextBar escortCargoInput; // escort: cargo count
	private GUIAdvTextBar textBar;
	private ElementInformation selectedBlockType;
	private GUIAdvDropdown dropdown;
	private GUIAdvBlockDisplay display;
	private boolean textChanged;
	private String curText;
	private GUIHorizontalButtonTablePane buttonPane;

	public NewContractPanel(InputState inputState, GUICallback guiCallback) {
		super("NewContractPanel", inputState, 300, 300, guiCallback, "New Contract", "");
		this.guiCallback = guiCallback;
		curText = "";
	}

	public int getReward() {
		return parse(rewardInput, 0);
	}

	public String getName() {
		return nameInput != null ? nameInput.getText() : "";
	}

	private int getCount() {
		return parse(itemInput, 0);
	}

	private int getEscortRouteLength() {
		return parse(escortRouteInput, ConfigManager.getEscortRouteMinLength());
	}

	private int getEscortCargoCount() {
		return parse(escortCargoInput, ConfigManager.getEscortCargoCount());
	}

	private static int parse(GUIActivatableTextBar bar, int def) {
		if(bar == null) return def;
		try {
			return Integer.parseInt(bar.getText().trim());
		} catch(Exception e) {
			return def;
		}
	}

	private TextCallback noopCallback() {
		return new TextCallback() {
			@Override
			public String[] getCommandPrefixes() {
				return null;
			}

			@Override
			public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
				return null;
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
		};
	}

	private GUIActivatableTextBar newTextBar(String label, int maxChars, int y) {
		GUIActivatableTextBar bar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, maxChars, 1, label, getContent(), noopCallback(), null);
		bar.onInit();
		bar.setPos(0, y, 0);
		getContent().attach(bar);
		return bar;
	}

	/** Tears down all dynamic inputs so each panel can rebuild cleanly (avoids stale/overlapping fields). */
	private void clearDynamicInputs() {
		curText = "";
		if(dropdown != null) {
			dropdown.cleanUp();
			getContent().detach(dropdown);
			dropdown = null;
		}
		if(display != null) {
			display.cleanUp();
			getContent().detach(display);
			display = null;
		}
		if(textBar != null) {
			textBar.cleanUp();
			getContent().detach(textBar);
			textBar = null;
		}
		nameInput = detach(nameInput);
		itemInput = detach(itemInput);
		escortRouteInput = detach(escortRouteInput);
		escortCargoInput = detach(escortCargoInput);
		rewardInput = detach(rewardInput);
	}

	private GUIActivatableTextBar detach(GUIActivatableTextBar bar) {
		if(bar != null) {
			bar.deactivateBar();
			bar.cleanUp();
			getContent().detach(bar);
		}
		return null;
	}

	public void drawBountyPanel() {
		clearDynamicInputs();
		nameInput = newTextBar("TARGET PLAYER", 30, 30);
		rewardInput = newTextBar("REWARD (GOLD BARS)", 10, 60);
		rewardInput.setText(String.valueOf(ConfigManager.getBountyDefaultReward()));
	}

	public void drawItemsPanel() {
		clearDynamicInputs();
		// Populate the dropdown on first draw (needsListUpdate() reads this) and pre-select a sensible
		// default so the panel is usable without forcing the player to type in the search box first.
		textChanged = true;
		ArrayList<ElementInformation> production = getProductionFilter();
		if(!production.isEmpty()) selectedBlockType = production.get(0);
		addDropdown(new DropdownResult() {
			private List<GUIElement> blockElements;

			@Override
			public DropdownCallback initCallback() {
				return new DropdownCallback() {
					@Override
					public void onChanged(Object value) {
						if(value instanceof ElementInformation) selectedBlockType = (ElementInformation) value;
					}
				};
			}

			@Override
			public String getToolTipText() {
				return "Select the item to request";
			}

			@Override
			public String getName() {
				return "Resource";
			}

			@Override
			public boolean needsListUpdate() {
				return textChanged;
			}

			@Override
			public Collection<? extends GUIElement> getDropdownElements(GUIElement guiElement) {
				blockElements = getProductionElements();
				return blockElements;
			}

			@Override
			public int getDropdownHeight() {
				return 26;
			}

			@Override
			public Object getDefault() {
				return (blockElements != null && !blockElements.isEmpty()) ? blockElements.get(0) : null;
			}

			@Override
			public void flagListNeedsUpdate(boolean flag) {
				textChanged = flag;
			}
		});

		addBlockDisplay(new BlockDisplayResult() {
			@Override
			public BlockSelectCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return "Selected Item";
			}

			@Override
			public short getDefault() {
				return ElementKeyMap.RESS_CRYS_BASTYN;
			}

			@Override
			public short getCurrentValue() {
				return selectedBlockType != null ? selectedBlockType.getId() : 0;
			}

			@Override
			public float getIconScale() {
				return 0.5f;
			}

			@Override
			public float getWeight() {
				return 0.3f;
			}
		});

		addTextBar(new TextBarResult() {
			@Override
			public TextBarCallback initCallback() {
				return super.callback;
			}

			@Override
			public String getToolTipText() {
				return Lng.str("Enter items to search for in the drop down");
			}

			@Override
			public String getName() {
				return Lng.str("Search Items");
			}

			@Override
			public String onTextChanged(String text) {
				String t = text.trim();
				if(!t.equals(curText)) {
					curText = t;
					textChanged = true;
				}
				return text;
			}
		});

		itemInput = newTextBar("COUNT", 10, 30);
		rewardInput = newTextBar("REWARD (GOLD BARS)", 10, 60);
		rewardInput.setText(String.valueOf(ConfigManager.getBountyDefaultReward()));
	}

	public void drawEscortPanel() {
		clearDynamicInputs();
		escortRouteInput = newTextBar("ROUTE LENGTH (" + ConfigManager.getEscortRouteMinLength() + "-" + ConfigManager.getEscortRouteMaxLength() + ")", 4, 30);
		escortRouteInput.setText(String.valueOf(ConfigManager.getEscortRouteMinLength()));
		escortCargoInput = newTextBar("CARGO COUNT (1-10)", 4, 60);
		escortCargoInput.setText(String.valueOf(ConfigManager.getEscortCargoCount()));
		rewardInput = newTextBar("REWARD (GOLD BARS)", 10, 90);
		rewardInput.setText(String.valueOf(Math.max(ConfigManager.getBountyMinReward(), (int) ConfigManager.getEscortBaseReward())));
	}

	@Override
	public float getHeight() {
		return getContent().getHeight();
	}

	@Override
	public float getWidth() {
		return getContent().getWidth();
	}

	private void addDropdown(DropdownResult result) {
		dropdown = new GUIAdvDropdown(getState(), getContent(), result);
		dropdown.setPos(0, 120, 0);
		getContent().attach(dropdown);
	}

	private void addBlockDisplay(BlockDisplayResult displayResult) {
		display = new GUIAdvBlockDisplay(getState(), getContent(), displayResult);
		display.setPos(0, 120, 0);
		getContent().attach(display);
	}

	private void addTextBar(TextBarResult textBarResult) {
		textBar = new GUIAdvTextBar(getState(), getContent(), textBarResult);
		textBar.setPos(0, 90, 0);
		getContent().attach(textBar);
	}

	@Override
	public void onDone() {
	}

	@Override
	public void onInit() {
		super.onInit();
		setPos(0, 0, 0);
		// Escort creation is hidden until escort contracts are enabled (no trader/cargo blueprints yet).
		boolean escortEnabled = ConfigManager.isEscortEnabled();
		int typeCount = escortEnabled ? 3 : 2;
		(buttonPane = new GUIHorizontalButtonTablePane(getState(), typeCount, 1, getContent())).onInit();
		GUIActivationCallback activationCallback = new GUIActivationCallback() {
			@Override
			public boolean isVisible(InputState inputState) {
				return true;
			}

			@Override
			public boolean isActive(InputState inputState) {
				return NewContractPanel.this.isActive();
			}
		};

		buttonPane.addButton(0, 0, "BOUNTY", GUIHorizontalArea.HButtonColor.BLUE, guiCallback, activationCallback).setUserPointer("BOUNTY");
		buttonPane.addButton(1, 0, "ITEMS", GUIHorizontalArea.HButtonColor.BLUE, guiCallback, activationCallback).setUserPointer("ITEMS");
		if(escortEnabled) {
			buttonPane.addButton(2, 0, "ESCORT", GUIHorizontalArea.HButtonColor.BLUE, guiCallback, activationCallback).setUserPointer("ESCORT");
		}

		buttonPane.setPos(0, 0, 0);
		getContent().attach(buttonPane);
		background.setWidth(getWidth());
	}

	public static ArrayList<ElementInformation> getResourcesFilter() {
		ArrayList<ElementInformation> filter = new ArrayList<>();
		ArrayList<ElementInformation> elementList = new ArrayList<>();
		ElementKeyMap.getCategoryHirarchy().getChild("Manufacturing").getInfoElementsRecursive(elementList);
		for(ElementInformation info : elementList) {
			if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe() && !info.getName().contains("Paint") && !info.getName().contains("Hardener") && !info.getName().contains("Scrap")) filter.add(info);
		}
		return filter;
	}

	public static ArrayList<ElementInformation> getProductionFilter() {
		ArrayList<ElementInformation> filter = new ArrayList<>();
		ArrayList<ElementInformation> elementList = new ArrayList<>();
		ElementKeyMap.getCategoryHirarchy().getChild("General").getInfoElementsRecursive(elementList);
		ElementKeyMap.getCategoryHirarchy().getChild("Ship").getInfoElementsRecursive(elementList);
		ElementKeyMap.getCategoryHirarchy().getChild("SpaceStation").getInfoElementsRecursive(elementList);
		for(ElementInformation info : elementList) {
			if(!info.isDeprecated() && info.isShoppable() && info.isInRecipe()) filter.add(info);
		}
		return filter;
	}

	private List<GUIElement> getProductionElements() {
		ArrayList<GUIElement> elementList = new ArrayList<>();
		GameClientState gameClientState = GameClient.getClientState();
		for(ElementInformation elementInfo : getProductionFilter()) {
			GUIAncor anchor = new GUIAncor(gameClientState, getWidth(), 26.0F);
			elementList.add(anchor);
			GUITextOverlay textOverlay = new GUITextOverlay(100, 26, FontLibrary.getBoldArial12White(), gameClientState);
			textOverlay.setTextSimple(elementInfo.getName());
			anchor.setUserPointer(elementInfo);
			GUIBlockSprite blockSprite = new GUIBlockSprite(gameClientState, elementInfo.getId());
			blockSprite.getScale().set(0.4F, 0.4F, 0.0F);
			anchor.attach(blockSprite);
			textOverlay.getPos().x = 50.0F;
			textOverlay.getPos().y = 7.0F;
			anchor.attach(textOverlay);
		}
		return elementList;
	}

	/** Sends a bounty-creation request to the server. Returns true if the request was sent. */
	public boolean createBountyContract() {
		PlayerState currentPlayer = GameClient.getClientPlayerState();
		if(nameInput == null || nameInput.getText().trim().isEmpty()) {
			popup("You must enter a target player name!");
			return false;
		}
		int reward = getReward();
		if(reward < ConfigManager.getBountyMinReward()) {
			popup("The reward must be at least " + ConfigManager.getBountyMinReward() + " Gold Bars!");
			return false;
		}
		if(GoldBarUtils.getBalance(currentPlayer) < reward) {
			popup("You don't have " + reward + " Gold Bars!");
			return false;
		}
		PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(AtlasContracts.CREATE_BOUNTY, nameInput.getText().trim(), String.valueOf(reward)));
		return true;
	}

	/** Sends an items-contract creation request to the server. Returns true if the request was sent. */
	public boolean createItemsContract() {
		PlayerState currentPlayer = GameClient.getClientPlayerState();
		if(selectedBlockType == null) {
			popup("You must select an item to request!");
			return false;
		}
		int count = getCount();
		if(count <= 0) {
			popup("You must enter a valid count!");
			return false;
		}
		int reward = getReward();
		if(reward < ConfigManager.getBountyMinReward()) {
			popup("The reward must be at least " + ConfigManager.getBountyMinReward() + " Gold Bars!");
			return false;
		}
		if(GoldBarUtils.getBalance(currentPlayer) < reward) {
			popup("You don't have " + reward + " Gold Bars!");
			return false;
		}
		PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(AtlasContracts.CREATE_ITEMS, String.valueOf(selectedBlockType.getId()), String.valueOf(count), String.valueOf(reward)));
		return true;
	}

	/** Sends an escort-contract creation request to the server. Returns true if the request was sent. */
	public boolean createEscortContract() {
		PlayerState currentPlayer = GameClient.getClientPlayerState();
		int reward = getReward();
		if(reward < ConfigManager.getBountyMinReward()) {
			popup("The reward must be at least " + ConfigManager.getBountyMinReward() + " Gold Bars!");
			return false;
		}
		if(GoldBarUtils.getBalance(currentPlayer) < reward) {
			popup("You don't have " + reward + " Gold Bars!");
			return false;
		}
		PacketUtil.sendPacketToServer(new PlayerActionCommandPacket(AtlasContracts.CREATE_ESCORT, String.valueOf(getEscortRouteLength()), String.valueOf(getEscortCargoCount()), String.valueOf(reward)));
		return true;
	}

	private void popup(String message) {
		(new SimplePopup(getState(), "Cannot Add Contract", message)).activate();
	}
}
