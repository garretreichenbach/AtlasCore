package org.schema.game.client.view.gui.catalog.newcatalog;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.gui.elements.ECCatalogScrollableListNew;

public class CatalogPanelNew extends GUIElement implements GUIActiveInterface {

	public GUIMainWindow catalogPanel;
	private GUIContentPane adminTab;
	//	private GUIContentPane personalTab;
	private boolean init;
	private GUIContentPane availableTab;
	private CatalogScrollableListNew availList;
	private CatalogScrollableListNew adminList;

	public CatalogPanelNew(InputState state) {
		super(state);
	}

	@Override
	public void cleanUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void draw() {
		if(!init) {
			onInit();

		}
		catalogPanel.draw();
	}

	@Override
	public void onInit() {
		if(catalogPanel != null) {
			catalogPanel.cleanUp();
		}
		catalogPanel = new GUIMainWindow(getState(), 1000, 700, "CatalogPanelNew");
		catalogPanel.onInit();

		catalogPanel.setCloseCallback(new GUICallback() {
			@Override
			public void callback(GUIElement callingGuiElement, MouseEvent event) {
				if(event.pressedLeftMouse()) {
					getState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
				}
			}

			@Override
			public boolean isOccluded() {
				return !getState().getController().getPlayerInputs().isEmpty();
			}
		});

		catalogPanel.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);

		recreateTabs();

		init = true;
	}

	public void recreateTabs() {
		Object beforeTab = null;
		if(catalogPanel.getSelectedTab() < catalogPanel.getTabs().size()) {
			beforeTab = catalogPanel.getTabs().get(catalogPanel.getSelectedTab()).getTabName();
		}
		catalogPanel.clearTabs();

//		personalTab = catalogPanel.addTab(Lng.str("OWN");
		availableTab = catalogPanel.addTab(Lng.str("BLUEPRINTS"));

		if(getOwnPlayer().getNetworkObject().isAdminClient.get()) {
			adminTab = catalogPanel.addTab(Lng.str("ADMIN"));
			createAdminCatalogPane();
		}

//		createPersonalCatalogPane();
		createAvailableCatalogPane();

		catalogPanel.activeInterface = this;
		if(beforeTab != null) {
			for(int i = 0; i < catalogPanel.getTabs().size(); i++) {
				if(catalogPanel.getTabs().get(i).getTabName().equals(beforeTab)) {
					catalogPanel.setSelectedTab(i);
					break;
				}
			}
		}
	}

	@Override
	public void update(Timer timer) {
	}

	//	public void createPersonalCatalogPane(){
//		if(mList != null){
//			mList.cleanUp();
//		}
//		mList = new CatalogScrollableListNew(getState(), personalTab.getContent(0), CatalogScrollableListNew.PERSONAL, new GUICallback() {
//			
//			@Override
//			public boolean isOccluded() {
//				return false;
//			}
//			
//			@Override
//			public void callback(GUIElement callingGuiElement, MouseEvent event) {
//				
//			}
//		});
//		mList.onInit();
//		
//		personalTab.getContent(0).attach(mList);
//	}
	public void createAvailableCatalogPane() {
		if(availList != null) {
			availList.cleanUp();
		}

		CatalogOptionsButtonPanel c = new CatalogOptionsButtonPanel(getState(), this);
		c.onInit();
		availableTab.setContent(0, c);
		if(!CatalogOptionsButtonPanel.areMultiplayerButtonVisible()) {
			availableTab.setTextBoxHeightLast(58);
			availableTab.addNewTextBox(10);
		} else {
			availableTab.setTextBoxHeightLast(82);
			availableTab.addNewTextBox(10);
		}

		availList = new ECCatalogScrollableListNew(getState(), availableTab.getContent(1), CatalogScrollableListNew.AVAILABLE, getState().getGameState().isBuyBBWithCredits(), false);
		availList.onInit();

		availableTab.getContent(1).attach(availList);
	}

	public void createAdminCatalogPane() {
		if(adminList != null) {
			adminList.cleanUp();
		}
		CatalogAdminOptionsButtonPanel c = new CatalogAdminOptionsButtonPanel(getState(), this);
		c.onInit();
		adminTab.setContent(0, c);

		adminTab.setTextBoxHeightLast(82 + 25 + 25);
		adminTab.addNewTextBox(10);

		adminList = new CatalogScrollableListNew(getState(), adminTab.getContent(1), CatalogScrollableListNew.ADMIN, getState().getGameState().isBuyBBWithCredits(), false);
		adminList.onInit();

		adminTab.getContent(1).attach(adminList);
	}

	public PlayerState getOwnPlayer() {
		return CatalogPanelNew.this.getState().getPlayer();
	}

	public Faction getOwnFaction() {
		return CatalogPanelNew.this.getState().getFactionManager().getFaction(getOwnPlayer().getFactionId());
	}

	@Override
	public float getHeight() {
		return catalogPanel.getHeight();
	}

	@Override
	public GameClientState getState() {
		return ((GameClientState) super.getState());
	}

	@Override
	public float getWidth() {
		return catalogPanel.getWidth();
	}

	@Override
	public boolean isActive() {
		return getState().getController().getPlayerInputs().isEmpty();
	}

	public void reset() {
		if(catalogPanel != null) {
			catalogPanel.reset();
		}
	}
}