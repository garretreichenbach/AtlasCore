package thederpgamer.edencore.gui.bankingmenu;

import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;
import thederpgamer.edencore.data.playerdata.PlayerData;
import thederpgamer.edencore.data.playerdata.PlayerDataManager;
import thederpgamer.edencore.utils.DateUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerBankingTransactionScrollableList extends ScrollableTableList<PlayerData.PlayerBankTransactionData> implements GUIActiveInterface {

	private final GUIElement parent;
	private final PlayerData playerData;

	public PlayerBankingTransactionScrollableList(InputState state, GUIElement parent, PlayerData playerData) {
		super(state, 100, 100, parent);
		this.parent = parent;
		this.playerData = playerData;
	}

	@Override
	protected Collection<PlayerData.PlayerBankTransactionData> getElementList() {
		return Collections.unmodifiableCollection(playerData.getTransactionHistory());
	}

	@Override
	public void initColumns() {
		addColumn("Subject", 10.0f, new Comparator<PlayerData.PlayerBankTransactionData>() {
			@Override
			public int compare(PlayerData.PlayerBankTransactionData o1, PlayerData.PlayerBankTransactionData o2) {
				return o1.getSubject().compareTo(o2.getSubject()); // Normal string comparison
			}
		});
		addColumn("From", 7.0f, new Comparator<PlayerData.PlayerBankTransactionData>() {
			@Override
			public int compare(PlayerData.PlayerBankTransactionData o1, PlayerData.PlayerBankTransactionData o2) {
				String fromName = PlayerDataManager.getInstance(false).getFromUUID(o1.getFromUUID(), false).getName();
				String toName = PlayerDataManager.getInstance(false).getFromUUID(o2.getFromUUID(), false).getName();
				return fromName.compareTo(toName); // Compare the names of the sender
			}
		});
		addColumn("Time", 7.0f, new Comparator<PlayerData.PlayerBankTransactionData>() {
			@Override
			public int compare(PlayerData.PlayerBankTransactionData o1, PlayerData.PlayerBankTransactionData o2) {
				// Compare the time of the transactions
				return Long.compare(o1.getTime(), o2.getTime());
			}
		});
		activeSortColumnIndex = 2;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<PlayerData.PlayerBankTransactionData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(PlayerData.PlayerBankTransactionData data : set) {
			GUIClippedRow subjectRow = getSimpleRow(data.getSubject(), this);
			GUIClippedRow fromRow = getSimpleRow(PlayerDataManager.getInstance(false).getFromUUID(data.getFromUUID(), false).getName(), this);
			GUIClippedRow timeRow = getSimpleRow(DateUtils.getTimeFormatted(data.getTime()), this);
			PlayerBankingTransactionScrollableListRow row = new PlayerBankingTransactionScrollableListRow(getState(), data, subjectRow, fromRow, timeRow);
			GUIAncor anchor = new GUIAncor(getState(), parent.getWidth() - 28.0f, 52.0f) {
				@Override
				public void draw() {
					super.draw();
					setWidth(parent.getWidth() - 28.0f);
				}
			};
			GUITextOverlay messageText = new GUITextOverlay(10, 10, getState());
			messageText.onInit();
			messageText.setTextSimple(data.getMessage());
			anchor.attach(messageText);
			row.expanded = new GUIElementList(getState());
			row.expanded.add(new GUIListElement(anchor, getState()));
			row.onInit();
			guiElementList.addWithoutUpdate(row);
		}
		guiElementList.updateDim();
	}

	public class PlayerBankingTransactionScrollableListRow extends ScrollableTableList<PlayerData.PlayerBankTransactionData>.Row {

		public PlayerBankingTransactionScrollableListRow(InputState state, PlayerData.PlayerBankTransactionData userData, GUIElement... elements) {
			super(state, userData, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
