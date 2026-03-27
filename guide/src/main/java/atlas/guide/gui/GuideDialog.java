package atlas.guide.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import atlas.guide.manager.GuideManager;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.mainmenu.MarkdownDocRenderer;
import org.schema.game.client.view.mainmenu.MarkdownGuiBlockRenderer;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * In-game guide dialog with a two-panel layout: a searchable topic list on the left
 * and rendered markdown content on the right.
 *
 * <p>Each document registered via {@link GuideManager} appears as a selectable topic.
 * Content is rendered using StarMade's native {@link MarkdownDocRenderer} and
 * {@link MarkdownGuiBlockRenderer}, supporting headings, paragraphs, bullet/ordered
 * lists, code blocks, tables, blockquotes, block sprites, and all inline styles.
 */
public class GuideDialog extends PlayerInput {

	private final GuidePanel panel;

	public GuideDialog() {
		super(GameClient.getClientState());
		(panel = new GuidePanel(getState(), this)).onInit();
	}

	@Override
	public void onDeactivate() {
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
	}

	@Override
	public GuidePanel getInputPanel() {
		return panel;
	}

	public static class GuidePanel extends GUIInputDialogPanel {

		// ── layout constants ──────────────────────────────────────────────────
		private static final int WINDOW_WIDTH = 1280;
		private static final int WINDOW_HEIGHT = 760;
		private static final int LEFT_WIDTH = 280;
		private static final int PADDING = 12;
		private static final int SEARCH_HEIGHT = 24;
		private static final int TOPIC_BUTTON_HEIGHT = 28;
		private static final int TOPIC_BUTTON_GAP = 4;
		private static final int TOPIC_INDENT = 10;
		private static final int CONTENT_MARGIN = 12;
		private static final int SCROLLBAR_WIDTH = 16;
		private static final int RIGHT_PANEL_MARGIN = 14;
		private static final int TOPICS_CONTENT_HORIZONTAL_PADDING = 4;
		private static final int TOPICS_SCROLL_BOTTOM_CLAMP = 12;
		private static final int CONTENT_SCROLL_BOTTOM_CLAMP = 18;

		// ── state ─────────────────────────────────────────────────────────────
		private final List<String> allTitles = new ArrayList<>(GuideManager.getTitles());
		private final List<String> filteredTitles = new ArrayList<>();
		private final Set<String> collapsedCategories = new HashSet<>();
		private String selectedTitle;
		private String searchQuery = "";

		// ── gui elements ──────────────────────────────────────────────────────
		private GUIAncor searchAnchor;
		private GUIActivatableTextBar searchBar;
		private GUIScrollablePanel topicsScrollPanel;
		private GUIAncor topicsContent;
		private GUIAncor topicsPane;
		private GUIScrollablePanel contentScrollPanel;
		private GUIAncor contentBlocks;
		private GUIAncor contentPane;
		private GUITextOverlay emptyTopicsOverlay;
		private GUIContentPane mainContentPane;
		private GUIAncor rootContentPane;

		public GuidePanel(InputState state, GUICallback guiCallback) {
			super(state, "ATLAS_GUIDE", "Guide", "", WINDOW_WIDTH, WINDOW_HEIGHT, guiCallback);
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();

			mainContentPane = ((GUIDialogWindow) background).getMainContentPane();
			rootContentPane = mainContentPane.getContent(0);
			GUIElement root = rootContentPane;

			// ── search bar ────────────────────────────────────────────────────
			searchAnchor = new GUIAncor(getState(), LEFT_WIDTH - (PADDING * 2), SEARCH_HEIGHT);
			root.attach(searchAnchor);

			searchBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Search topics...", searchAnchor, new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return new String[0];
				}

				@Override
				public String handleAutoComplete(String s, TextCallback cb, String s1) {
					return "";
				}

				@Override
				public void onFailedTextCheck(String s) {
				}

				@Override
				public void onTextEnter(String input, boolean send, boolean keep) {
				}

				@Override
				public void newLine() {
				}
			}, input -> {
				searchQuery = input == null ? "" : input.trim();
				filterTopics();
				rebuildTopicButtons();
				return input;
			});
			searchBar.onInit();
			root.attach(searchBar);
			searchBar.activateBar();

			// ── left: topic list ─────────────────────────────────────────────
			topicsPane = new GUIAncor(getState(), LEFT_WIDTH, WINDOW_HEIGHT - 80);
			root.attach(topicsPane);

			topicsScrollPanel = new GUIScrollablePanel(LEFT_WIDTH, WINDOW_HEIGHT - 80, topicsPane, getState());
			topicsScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			topicsContent = new GUIAncor(getState(), LEFT_WIDTH - 12, WINDOW_HEIGHT - 80);
			topicsScrollPanel.setContent(topicsContent);
			topicsPane.attach(topicsScrollPanel);

			emptyTopicsOverlay = new GUITextOverlay(LEFT_WIDTH - 24, 18, FontLibrary.FontSize.MEDIUM, getState());
			emptyTopicsOverlay.setTextSimple("No matching topics");
			emptyTopicsOverlay.onInit();

			// ── right: rendered content ───────────────────────────────────────
			contentPane = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			root.attach(contentPane);

			contentScrollPanel = new GUIScrollablePanel(WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, contentPane, getState());
			contentScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			contentBlocks = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			contentScrollPanel.setContent(contentBlocks);
			contentPane.attach(contentScrollPanel);

			// ── initial population ────────────────────────────────────────────
			filterTopics();
			rebuildTopicButtons();
			selectTopic(filteredTitles.isEmpty() ? null : filteredTitles.get(0));
			layoutComponents();
		}

		@Override
		public void draw() {
			layoutComponents();
			super.draw();
		}

		// ── layout ────────────────────────────────────────────────────────────

		private void layoutComponents() {
			if(searchAnchor == null) return;

			float availableWidth = rootContentPane != null && rootContentPane.getWidth() > 0 ? rootContentPane.getWidth() : getWidth();
			float availableHeight = rootContentPane != null && rootContentPane.getHeight() > 0 ? rootContentPane.getHeight() : getHeight();

			float contentHeight = Math.max(120.0F, availableHeight - 20.0F);
			float leftHeight = Math.max(120.0F, contentHeight - SEARCH_HEIGHT - 16.0F);
			float leftScrollHeight = Math.max(80.0F, leftHeight - TOPICS_SCROLL_BOTTOM_CLAMP);
			float rightX = LEFT_WIDTH + (PADDING * 2);
			float rightWidth = Math.max(300.0F, availableWidth - rightX - RIGHT_PANEL_MARGIN);
			float contentScrollHeight = Math.max(80.0F, contentHeight - CONTENT_SCROLL_BOTTOM_CLAMP);

			searchAnchor.setWidth(Math.max(80.0F, LEFT_WIDTH - (PADDING * 2)));
			searchAnchor.setHeight(SEARCH_HEIGHT);
			searchAnchor.setPos(PADDING, 6.0F, 0.0F);
			searchBar.setPos(PADDING, 6.0F, 0.0F);

			topicsPane.setWidth(LEFT_WIDTH);
			topicsPane.setHeight(leftHeight);
			topicsPane.setPos(PADDING, SEARCH_HEIGHT + 16.0F, 0.0F);
			topicsScrollPanel.setWidth(LEFT_WIDTH);
			topicsScrollPanel.setHeight(leftScrollHeight);
			topicsScrollPanel.setPos(0.0F, 0.0F, 0.0F);
			topicsContent.setWidth(LEFT_WIDTH - SCROLLBAR_WIDTH - (TOPIC_INDENT * 2) - TOPICS_CONTENT_HORIZONTAL_PADDING);

			contentPane.setWidth(rightWidth);
			contentPane.setHeight(contentHeight);
			contentPane.setPos(rightX, 6.0F, 0.0F);
			contentScrollPanel.setWidth(rightWidth);
			contentScrollPanel.setHeight(contentScrollHeight);
			contentScrollPanel.setPos(0.0F, 0.0F, 0.0F);
			contentBlocks.setWidth(rightWidth - SCROLLBAR_WIDTH - 8);
		}

		// ── topic list ────────────────────────────────────────────────────────

		private void filterTopics() {
			filteredTitles.clear();
			String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
			for(String title : allTitles) {
				if(query.isEmpty() || title.toLowerCase(Locale.ROOT).contains(query)) {
					filteredTitles.add(title);
				}
			}
			if(selectedTitle != null && !filteredTitles.contains(selectedTitle)) {
				selectedTitle = filteredTitles.isEmpty() ? null : filteredTitles.get(0);
			}
		}

		private void rebuildTopicButtons() {
			if(topicsContent == null) return;

			topicsContent.detachAll();
			int y = 0;
			if(filteredTitles.isEmpty()) {
				emptyTopicsOverlay.setPos(8.0F, 8.0F, 0.0F);
				topicsContent.attach(emptyTopicsOverlay);
				topicsContent.setHeight(40.0F);
				return;
			}

			// Group filtered titles by category, preserving the original category order
			for(String category : GuideManager.getCategories()) {
				List<String> titlesInCategory = new ArrayList<>();
				for(String title : filteredTitles) {
					if(GuideManager.getCategory(title).equals(category)) titlesInCategory.add(title);
				}
				if(titlesInCategory.isEmpty()) continue;

				if(!category.isEmpty()) y += addCategoryHeader(y, category);
				if(category.isEmpty() || !collapsedCategories.contains(category)) {
					for(String title : titlesInCategory) y += addTopicButton(y, title);
				}
			}
			topicsContent.setHeight(Math.max(40.0F, y));
		}

		private int addCategoryHeader(int y, String category) {
			boolean collapsed = collapsedCategories.contains(category);
			String arrow = collapsed ? "\u25B6 " : "\u25BC ";
			int rowWidth = Math.max(120, (int) topicsContent.getWidth());

			GUITextButton button = new GUITextButton(getState(), rowWidth, TOPIC_BUTTON_HEIGHT, GUITextButton.ColorPalette.NEUTRAL, arrow + category.toUpperCase(Locale.ROOT), new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if(event.pressedLeftMouse()) {
						if(collapsedCategories.contains(category)) collapsedCategories.remove(category);
						else collapsedCategories.add(category);
						rebuildTopicButtons();
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			});
			button.setTextPos(6, 4);
			button.setMouseUpdateEnabled(true);
			button.setPos(0, y, 0);
			button.onInit();
			// Tint the button text blue to distinguish it from topic entries
			button.setColor(0.55F, 0.70F, 1.0F, 1.0F);
			topicsContent.attach(button);
			return TOPIC_BUTTON_HEIGHT + TOPIC_BUTTON_GAP;
		}

		private int addTopicButton(int y, String title) {
			boolean selected = title.equals(selectedTitle);
			int rowWidth = Math.max(120, (int) topicsContent.getWidth());
			GUITextButton.ColorPalette palette = selected ? GUITextButton.ColorPalette.FRIENDLY : GUITextButton.ColorPalette.NEUTRAL;

			GUITextButton button = new GUITextButton(getState(), rowWidth, TOPIC_BUTTON_HEIGHT, palette, title, new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if(event.pressedLeftMouse()) selectTopic(title);
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			});
			button.setTextPos(10, 4);
			button.setMouseUpdateEnabled(true);
			button.setPos(TOPIC_INDENT, y, 0);
			button.onInit();
			topicsContent.attach(button);
			return TOPIC_BUTTON_HEIGHT + TOPIC_BUTTON_GAP;
		}

		// ── topic selection & content rendering ───────────────────────────────

		private void selectTopic(String title) {
			selectedTitle = title;
			rebuildTopicButtons();
			rebuildRenderedContent();
			if(contentScrollPanel != null) contentScrollPanel.scrollVerticalPercent(0.0F);
		}

		private void rebuildRenderedContent() {
			if(contentBlocks == null) return;

			contentBlocks.detachAll();
			if(selectedTitle == null) {
				GUITextOverlay emptyOverlay = new GUITextOverlay(400, 24, FontLibrary.FontSize.BIG, getState());
				emptyOverlay.setTextSimple("No guide topic selected.");
				emptyOverlay.onInit();
				emptyOverlay.setPos(CONTENT_MARGIN, CONTENT_MARGIN, 0);
				contentBlocks.attach(emptyOverlay);
				contentBlocks.setHeight(60.0F);
				return;
			}

			List<MarkdownDocRenderer.RenderedBlock> blocks = MarkdownDocRenderer.render(GuideManager.getRaw(selectedTitle));
			int width = Math.max(280, (int) contentBlocks.getWidth() - (CONTENT_MARGIN * 2));
			int finalY = MarkdownGuiBlockRenderer.renderBlocks(getState(), contentBlocks, blocks, width, CONTENT_MARGIN);
			contentBlocks.setHeight(Math.max(finalY + CONTENT_MARGIN, contentScrollPanel.getHeight()));
		}
	}
}
