package atlas.guide.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import atlas.guide.manager.GuideManager;
import org.newdawn.slick.UnicodeFont;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.view.mainmenu.MarkdownDocRenderer;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-game guide dialog with a two-panel layout: a searchable topic list on the left
 * and rendered markdown content on the right.
 *
 * <p>Each document registered via {@link GuideManager} appears as a selectable topic.
 * Content is rendered using StarMade's native {@link MarkdownDocRenderer} with full
 * inline-style support (bold, italic, inline code, code blocks, headings, etc.).
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
		private static final int INLINE_CODE_PADDING_X = 4;
		private static final int INLINE_CODE_PADDING_Y = 1;
		private static final int CODE_BLOCK_PADDING_X = 8;
		private static final int CODE_BLOCK_PADDING_Y = 3;
		private static final int SCROLLBAR_WIDTH = 16;
		private static final int RIGHT_PANEL_MARGIN = 14;
		private static final int TOPICS_CONTENT_HORIZONTAL_PADDING = 4;
		private static final int CODE_TAB_WIDTH = 12;
		private static final int TOPICS_SCROLL_BOTTOM_CLAMP = 12;
		private static final int CONTENT_SCROLL_BOTTOM_CLAMP = 18;

		// ── state ─────────────────────────────────────────────────────────────
		private final List<String> allTitles = new ArrayList<>(GuideManager.getTitles());
		private final List<String> filteredTitles = new ArrayList<>();
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

			for(String title : filteredTitles) {
				y += addTopicButton(y, title);
			}
			topicsContent.setHeight(Math.max(40.0F, y));
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
				emptyOverlay.setPos(12, 12, 0);
				contentBlocks.attach(emptyOverlay);
				contentBlocks.setHeight(60.0F);
				return;
			}

			List<MarkdownDocRenderer.RenderedBlock> blocks = MarkdownDocRenderer.render(GuideManager.getRaw(selectedTitle));
			int y = 12;
			int width = Math.max(280, (int) contentBlocks.getWidth() - (CONTENT_MARGIN * 2));

			for(MarkdownDocRenderer.RenderedBlock block : blocks) {
				y += addRenderedBlock(width, y, block);
			}
			contentBlocks.setHeight(Math.max(y + 12.0F, contentScrollPanel.getHeight()));
		}

		// ── block rendering ───────────────────────────────────────────────────

		private int addRenderedBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			switch(block.getType()) {
				case CODE:
					return addCodeBlock(width, y, block);
				case SEPARATOR:
					return addSeparatorBlock();
				default:
					return addInlineBlock(width, y, block);
			}
		}

		private int addCodeBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			UnicodeFont font = getFontForBlock(block.getType());
			GUITextOverlay overlay = new GUITextOverlay(width, 10, font, getState());
			overlay.setTextSimple(formatCodeText(block.getText()));
			overlay.autoWrapOn = contentBlocks;
			overlay.onInit();
			overlay.updateTextSize();
			overlay.setHeight(overlay.getTextHeight());

			int textHeight = Math.max(1, overlay.getTextHeight());
			int blockHeight = textHeight + (CODE_BLOCK_PADDING_Y * 2);

			GUIColoredRectangle bg = new GUIColoredRectangle(getState(), width + CODE_BLOCK_PADDING_X, blockHeight, new Vector4f(0.08F, 0.10F, 0.15F, 0.80F));
			bg.onInit();
			bg.setPos(CONTENT_MARGIN - 4, y, 0);
			contentBlocks.attach(bg);

			overlay.setColor(0.95F, 0.95F, 0.95F, 1.0F);
			overlay.setPos(CONTENT_MARGIN + 2, y + CODE_BLOCK_PADDING_Y, 0);
			contentBlocks.attach(overlay);
			return blockHeight + 8;
		}

		private int addSeparatorBlock() {
			return 10;
		}

		private int addInlineBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			int startX = CONTENT_MARGIN;
			int maxX = CONTENT_MARGIN + width;
			int lineMaxWidth = Math.max(48, maxX - startX);
			int currentX = startX;
			int currentY = y;
			int lineHeight = getDefaultLineHeight(block.getType());
			int blockSpacing = getBlockSpacing(block.getType());

			for(InlineToken token : tokenizeSegments(block.getSegments())) {
				if(token.lineBreak) {
					currentY += lineHeight + 3;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					continue;
				}

				UnicodeFont font = getFontForInlineSegment(block.getType(), token.style);
				String rendered = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? formatCodeText(token.text) : token.text;
				int textWidth = Math.max(0, font.getWidth(rendered));
				int advanceWidth = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? textWidth + (INLINE_CODE_PADDING_X * 2) + 2 : textWidth;

				if(currentX > startX && currentX + advanceWidth > maxX) {
					currentY += lineHeight + 3;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					if(token.whitespace) continue;
				}

				if(token.whitespace) {
					if(currentX > startX) {
						if(currentX + textWidth > maxX) {
							currentY += lineHeight + 3;
							currentX = startX;
							lineHeight = getDefaultLineHeight(block.getType());
							continue;
						}
						currentX += textWidth;
					}
					continue;
				}

				int tokenMaxWidth = lineMaxWidth - (token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? (INLINE_CODE_PADDING_X * 2) + 2 : 0);
				List<String> chunks = splitTokenForWidth(rendered, font, tokenMaxWidth);

				for(int ci = 0; ci < chunks.size(); ci++) {
					String chunk = chunks.get(ci);
					int chunkWidth = Math.max(0, font.getWidth(chunk));
					int chunkAdvance = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? chunkWidth + (INLINE_CODE_PADDING_X * 2) + 2 : chunkWidth;

					if(currentX > startX && currentX + chunkAdvance > maxX) {
						currentY += lineHeight + 3;
						currentX = startX;
						lineHeight = getDefaultLineHeight(block.getType());
					}

					GUITextOverlay overlay = new GUITextOverlay(Math.max(12, chunkWidth + 8), 10, font, getState());
					overlay.setTextSimple(chunk);
					overlay.onInit();
					overlay.updateTextSize();
					int overlayHeight = Math.max(getDefaultLineHeight(block.getType()), overlay.getTextHeight());

					if(token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
						int inlineCodeHeight = overlay.getTextHeight() + (INLINE_CODE_PADDING_Y * 2);
						GUIColoredRectangle inlineBg = new GUIColoredRectangle(getState(), chunkWidth + (INLINE_CODE_PADDING_X * 2), inlineCodeHeight, new Vector4f(0.12F, 0.15F, 0.23F, 0.95F));
						inlineBg.onInit();
						inlineBg.setPos(currentX - 1, currentY, 0);
						contentBlocks.attach(inlineBg);
						overlay.setPos(currentX + INLINE_CODE_PADDING_X - 1, currentY + INLINE_CODE_PADDING_Y, 0);
						overlayHeight = inlineCodeHeight;
					} else {
						overlay.setPos(currentX, currentY, 0);
					}

					Vector4f color = getInlineColor(block.getType(), token.style);
					overlay.setColor(color.x, color.y, color.z, color.w);
					contentBlocks.attach(overlay);

					currentX += chunkAdvance;
					lineHeight = Math.max(lineHeight, overlayHeight);

					if(ci < chunks.size() - 1) {
						currentY += lineHeight + 3;
						currentX = startX;
						lineHeight = getDefaultLineHeight(block.getType());
					}
				}
			}

			return (currentY - y) + lineHeight + blockSpacing;
		}

		// ── tokenization ──────────────────────────────────────────────────────

		private List<InlineToken> tokenizeSegments(List<MarkdownDocRenderer.InlineSegment> segments) {
			List<InlineToken> tokens = new ArrayList<>();
			for(MarkdownDocRenderer.InlineSegment segment : segments) {
				String text = segment.getText();
				if(text == null || text.isEmpty()) continue;
				if(segment.getStyle() == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
					appendInlineCodeTokens(tokens, text, segment.getStyle());
				} else {
					appendWrappedTokens(tokens, text, segment.getStyle());
				}
			}
			return tokens;
		}

		private void appendInlineCodeTokens(List<InlineToken> tokens, String text, MarkdownDocRenderer.InlineStyle style) {
			String[] lines = text.split("\\n", -1);
			for(int i = 0; i < lines.length; i++) {
				if(!lines[i].isEmpty()) tokens.add(new InlineToken(lines[i], style, false, false));
				if(i < lines.length - 1) tokens.add(new InlineToken("", style, false, true));
			}
		}

		private void appendWrappedTokens(List<InlineToken> tokens, String text, MarkdownDocRenderer.InlineStyle style) {
			StringBuilder current = new StringBuilder();
			Boolean whitespace = null;
			for(int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if(c == '\n') {
					flushInlineToken(tokens, current, style, whitespace != null && whitespace);
					tokens.add(new InlineToken("", style, false, true));
					whitespace = null;
					continue;
				}
				boolean isWs = Character.isWhitespace(c);
				if(whitespace != null && whitespace != isWs) flushInlineToken(tokens, current, style, whitespace);
				current.append(c);
				whitespace = isWs;
			}
			flushInlineToken(tokens, current, style, whitespace != null && whitespace);
		}

		private void flushInlineToken(List<InlineToken> tokens, StringBuilder current, MarkdownDocRenderer.InlineStyle style, boolean whitespace) {
			if(current.length() == 0) return;
			tokens.add(new InlineToken(current.toString(), style, whitespace, false));
			current.setLength(0);
		}

		private List<String> splitTokenForWidth(String text, UnicodeFont font, int maxWidth) {
			List<String> chunks = new ArrayList<>();
			if(text == null || text.isEmpty()) return chunks;
			if(font.getWidth(text) <= maxWidth) {
				chunks.add(text);
				return chunks;
			}
			StringBuilder current = new StringBuilder();
			int currentWidth = 0;
			for(int i = 0; i < text.length(); i++) {
				String ch = String.valueOf(text.charAt(i));
				int cWidth = Math.max(1, font.getWidth(ch));
				if(current.length() > 0 && currentWidth + cWidth > maxWidth) {
					chunks.add(current.toString());
					current.setLength(0);
					currentWidth = 0;
				}
				current.append(ch);
				currentWidth += cWidth;
			}
			if(current.length() > 0) chunks.add(current.toString());
			return chunks;
		}

		// ── font / colour / spacing helpers ───────────────────────────────────

		private int getDefaultLineHeight(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return 34;
				case HEADING_2:
					return 28;
				case HEADING_3:
					return 24;
				default:
					return 16;
			}
		}

		private int getBlockSpacing(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return 18;
				case HEADING_2:
					return 14;
				case HEADING_3:
					return 12;
				case BULLET:
				case ORDERED:
					return 6;
				default:
					return 8;
			}
		}

		private UnicodeFont getFontForBlock(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return FontLibrary.getBoldArial30WhiteNoOutline();
				case HEADING_2:
					return FontLibrary.getBoldArial24WhiteNoOutline();
				case HEADING_3:
					return FontLibrary.getBoldArial20WhiteNoOutline();
				case CODE:
					return FontLibrary.getCourierNew12White();
				default:
					return FontLibrary.FontSize.MEDIUM.getFont();
			}
		}

		private UnicodeFont getFontForInlineSegment(MarkdownDocRenderer.BlockType blockType, MarkdownDocRenderer.InlineStyle style) {
			if(style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) return FontLibrary.getCourierNew12White();
			switch(blockType) {
				case HEADING_1:
					return FontLibrary.getBoldArial30WhiteNoOutline();
				case HEADING_2:
					return FontLibrary.getBoldArial24WhiteNoOutline();
				case HEADING_3:
					return FontLibrary.getBoldArial20WhiteNoOutline();
				default:
					if(style == MarkdownDocRenderer.InlineStyle.BOLD || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC)
						return FontLibrary.getBoldArial14White();
					return FontLibrary.getRegularArial13White();
			}
		}

		private Vector4f getInlineColor(MarkdownDocRenderer.BlockType blockType, MarkdownDocRenderer.InlineStyle style) {
			switch(blockType) {
				case HEADING_1:
					return (style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) ? new Vector4f(0.92F, 0.97F, 1.0F, 1.0F) : new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
				case HEADING_2:
					return (style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) ? new Vector4f(0.90F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.95F, 0.95F, 1.0F, 1.0F);
				case HEADING_3:
					return (style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) ? new Vector4f(0.88F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.90F, 0.95F, 1.0F, 1.0F);
				default:
					if(style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
						return new Vector4f(0.97F, 0.97F, 0.97F, 1.0F);
					}
					if(style == MarkdownDocRenderer.InlineStyle.BOLD || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) {
						return new Vector4f(0.98F, 0.98F, 0.98F, 1.0F);
					}
					if(style == MarkdownDocRenderer.InlineStyle.ITALIC) {
						return new Vector4f(0.84F, 0.92F, 1.0F, 1.0F);
					}
					return new Vector4f(0.88F, 0.88F, 0.88F, 1.0F);
			}
		}

		// ── code text formatting ──────────────────────────────────────────────

		/**
		 * Converts spaces/tabs to non-breaking equivalents so StarMade doesn't collapse them.
		 */
		private String formatCodeText(String text) {
			if(text == null || text.isEmpty()) return "";
			String normalized = text.replace("\r", "");
			String[] lines = normalized.split("\\n", -1);
			StringBuilder sb = new StringBuilder(normalized.length() + 16);
			for(int li = 0; li < lines.length; li++) {
				String line = lines[li];
				int i = 0;
				// Replace leading whitespace with non-breaking spaces
				while(i < line.length()) {
					char c = line.charAt(i);
					if(c == ' ') {
						sb.append('\u00A0');
						i++;
						continue;
					}
					if(c == '\t') {
						for(int t = 0; t < CODE_TAB_WIDTH; t++) sb.append('\u00A0');
						i++;
						continue;
					}
					break;
				}
				// Render remainder of line (tabs → spaces)
				for(; i < line.length(); i++) {
					char c = line.charAt(i);
					if(c == '\t') {
						for(int t = 0; t < CODE_TAB_WIDTH; t++) sb.append(' ');
					} else sb.append(c);
				}
				if(li < lines.length - 1) sb.append('\n');
			}
			return sb.toString();
		}

		// ── inner types ───────────────────────────────────────────────────────

		private static final class InlineToken {
			final String text;
			final MarkdownDocRenderer.InlineStyle style;
			final boolean whitespace;
			final boolean lineBreak;

			InlineToken(String text, MarkdownDocRenderer.InlineStyle style, boolean whitespace, boolean lineBreak) {
				this.text = text;
				this.style = style;
				this.whitespace = whitespace;
				this.lineBreak = lineBreak;
			}
		}
	}
}
