package atlas.banking.gui;

import atlas.banking.data.BankingData;
import atlas.banking.data.BankingDataManager;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public class PlayerBankingTransactionScrollableList extends ScrollableTableList<BankingData.BankTransactionData> implements GUIActiveInterface {

	private final GUIElement parent;
	private final BankingData bankingData;

	public PlayerBankingTransactionScrollableList(InputState state, GUIElement parent, BankingData bankingData) {
		super(state, 100, 100, parent);
		this.parent = parent;
		this.bankingData = bankingData;
	}

	@Override
	protected Collection<BankingData.BankTransactionData> getElementList() {
		return Collections.unmodifiableCollection(bankingData.getTransactionHistory());
	}

	@Override
	public void initColumns() {
		addColumn("Subject", 10.0f, new Comparator<BankingData.BankTransactionData>() {
			@Override
			public int compare(BankingData.BankTransactionData o1, BankingData.BankTransactionData o2) {
				return o1.getSubject().compareTo(o2.getSubject());
			}
		});
		addColumn("From", 7.0f, new Comparator<BankingData.BankTransactionData>() {
			@Override
			public int compare(BankingData.BankTransactionData o1, BankingData.BankTransactionData o2) {
				BankingData from1 = BankingDataManager.getInstance(false).getFromUUID(o1.getFromUUID(), false);
				BankingData from2 = BankingDataManager.getInstance(false).getFromUUID(o2.getFromUUID(), false);
				String fromName1 = from1 != null ? from1.getPlayerName() : o1.getFromUUID();
				String fromName2 = from2 != null ? from2.getPlayerName() : o2.getFromUUID();
				return fromName1.compareTo(fromName2);
			}
		});
		addColumn("Time", 7.0f, new Comparator<BankingData.BankTransactionData>() {
			@Override
			public int compare(BankingData.BankTransactionData o1, BankingData.BankTransactionData o2) {
				return Long.compare(o1.getTimestamp(), o2.getTimestamp());
			}
		});
		activeSortColumnIndex = 2;
	}

	@Override
	public void updateListEntries(GUIElementList guiElementList, Set<BankingData.BankTransactionData> set) {
		guiElementList.deleteObservers();
		guiElementList.addObserver(this);
		for(BankingData.BankTransactionData data : set) {
			BankingData from = BankingDataManager.getInstance(false).getFromUUID(data.getFromUUID(), false);
			String fromName = from != null ? from.getPlayerName() : data.getFromUUID();
			GUIClippedRow subjectRow = getSimpleRow(data.getSubject(), this);
			GUIClippedRow fromRow = getSimpleRow(fromName, this);
			GUIClippedRow timeRow = getSimpleRow(String.valueOf(data.getTimestamp()), this);
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

	public class PlayerBankingTransactionScrollableListRow extends ScrollableTableList<BankingData.BankTransactionData>.Row {

		public PlayerBankingTransactionScrollableListRow(InputState state, BankingData.BankTransactionData userData, GUIElement... elements) {
			super(state, userData, elements);
			highlightSelect = true;
			highlightSelectSimple = true;
			setAllwaysOneSelected(true);
		}
	}
}
