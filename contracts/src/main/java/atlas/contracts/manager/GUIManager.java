package atlas.contracts.manager;

import api.listener.events.gui.MainWindowTabAddEvent;
import atlas.contracts.gui.contract.contractlist.ContractsTab;
import org.schema.schine.common.language.Lng;

/**
 * Holds the shop-window contracts tab and attaches it when the SHOP window is built.
 */
public class GUIManager {

	private static GUIManager instance;

	public ContractsTab contractsTab;

	public static GUIManager getInstance() {
		return instance;
	}

	public static void initialize() {
		instance = new GUIManager();
	}

	public void createContractsShopTab(MainWindowTabAddEvent event) {
		if(event.getTitle().equals(Lng.str("SHOP")) && contractsTab == null) {
			contractsTab = new ContractsTab(event.getWindow());
			contractsTab.onInit();
			event.getWindow().getTabs().add(contractsTab);
		}
	}
}
