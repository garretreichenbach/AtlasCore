package atlas.guide.util;

import org.newdawn.slick.UnicodeFont;
import org.schema.game.client.view.mainmenu.MarkdownDocRenderer;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIColoredRectangle;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared markdown block renderer used by docs and main-menu news/changelog widgets.
 */
public final class MarkdownGuiBlockRenderer {

	private static final int CONTENT_MARGIN = 12;
	private static final int INLINE_CODE_PADDING_X = 4;
	private static final int INLINE_CODE_PADDING_Y = 4;

	private MarkdownGuiBlockRenderer() {
	}

	public static int renderBlocks(InputState state, GUIAncor contentParent, List<org.schema.game.client.view.mainmenu.MarkdownDocRenderer.RenderedBlock> blocks, int width, int startY) {
		int y = startY;
		for(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.RenderedBlock block : blocks) {
			y += addRenderedBlock(state, contentParent, width, y, block);
		}
		return y;
	}

	private static int addRenderedBlock(InputState state, GUIAncor contentParent, int width, int y, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.RenderedBlock block) {
		switch(block.getType()) {
			case CODE:
				return addCodeBlock(state, contentParent, width, y, block);
			case SEPARATOR:
				return addSeparatorBlock(state, contentParent, width, y);
			case HEADING_1:
			case HEADING_2:
			case HEADING_3:
			case BULLET:
			case ORDERED:
			case PARAGRAPH:
			default:
				return addInlineBlock(state, contentParent, width, y, block);
		}
	}

	private static int addCodeBlock(InputState state, GUIAncor contentParent, int width, int y, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.RenderedBlock block) {
		UnicodeFont font = getFontForBlock(block.getType());
		GUITextOverlay overlay = new GUITextOverlay(width, 10, font, state);
		overlay.setTextSimple(block.getText());
		overlay.autoWrapOn = contentParent;
		overlay.onInit();
		overlay.updateTextSize();
		overlay.setHeight(overlay.getTextHeight());

		GUIColoredRectangle codeBackground = new GUIColoredRectangle(state, width + 8, overlay.getTextHeight() + 10, new Vector4f(0.08F, 0.10F, 0.15F, 0.80F));
		codeBackground.onInit();
		codeBackground.setPos(8, y - 3, 0);
		contentParent.attach(codeBackground);

		overlay.setColor(0.95F, 0.95F, 0.95F, 1.0F);
		overlay.setPos(CONTENT_MARGIN + 4, y + 2, 0);
		contentParent.attach(overlay);
		return overlay.getTextHeight() + 18;
	}

	private static int addSeparatorBlock(InputState state, GUIAncor contentParent, int width, int y) {
		GUIColoredRectangle separator = new GUIColoredRectangle(state, width, 2, new Vector4f(0.35F, 0.48F, 0.65F, 0.90F));
		separator.onInit();
		separator.setPos(CONTENT_MARGIN, y + 4, 0);
		contentParent.attach(separator);
		return 12;
	}

	private static int addInlineBlock(InputState state, GUIAncor contentParent, int width, int y, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.RenderedBlock block) {
		int startX = CONTENT_MARGIN;
		int maxX = CONTENT_MARGIN + width;
		int currentX = startX;
		int currentY = y;
		int lineHeight = getDefaultLineHeight(block.getType());
		int blockSpacing = getBlockSpacing(block.getType());

		for(InlineToken token : tokenizeSegments(block.getSegments())) {
			if(token.lineBreak) {
				currentY += lineHeight + 4;
				currentX = startX;
				lineHeight = getDefaultLineHeight(block.getType());
				continue;
			}

			UnicodeFont font = getFontForInlineSegment(block.getType(), token.style);
			int textWidth = Math.max(0, font.getWidth(token.text));
			int advanceWidth = token.style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE ? textWidth + (INLINE_CODE_PADDING_X * 2) + 2 : textWidth;
			if(currentX > startX && currentX + advanceWidth > maxX) {
				currentY += lineHeight + 4;
				currentX = startX;
				lineHeight = getDefaultLineHeight(block.getType());
				if(token.whitespace) {
					continue;
				}
			}

			if(token.whitespace) {
				if(currentX > startX) {
					currentX += textWidth;
				}
				continue;
			}

			GUITextOverlay overlay = new GUITextOverlay(Math.max(12, textWidth + 8), 10, font, state);
			overlay.setTextSimple(token.text);
			overlay.onInit();
			overlay.updateTextSize();
			int overlayHeight = Math.max(getDefaultLineHeight(block.getType()), overlay.getTextHeight());

			if(token.style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
				GUIColoredRectangle inlineCodeBackground = new GUIColoredRectangle(state, textWidth + (INLINE_CODE_PADDING_X * 2), overlayHeight + (INLINE_CODE_PADDING_Y * 2), new Vector4f(0.12F, 0.15F, 0.23F, 0.95F));
				inlineCodeBackground.onInit();
				inlineCodeBackground.setPos(currentX - 1, currentY - 1, 0);
				contentParent.attach(inlineCodeBackground);
				overlay.setPos(currentX + INLINE_CODE_PADDING_X - 1, currentY + INLINE_CODE_PADDING_Y - 1, 0);
			} else {
				overlay.setPos(currentX, currentY, 0);
			}

			Vector4f color = getInlineColor(block.getType(), token.style);
			overlay.setColor(color.x, color.y, color.z, color.w);
			contentParent.attach(overlay);

			currentX += advanceWidth;
			lineHeight = Math.max(lineHeight, overlayHeight + (token.style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE ? INLINE_CODE_PADDING_Y : 0));
		}

		return (currentY - y) + lineHeight + blockSpacing;
	}

	private static List<InlineToken> tokenizeSegments(List<org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineSegment> segments) {
		List<InlineToken> tokens = new ArrayList<>();
		for(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineSegment segment : segments) {
			String text = segment.getText();
			if(text == null || text.isEmpty()) {
				continue;
			}

			if(segment.getStyle() == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
				appendInlineCodeTokens(tokens, text, segment.getStyle());
			} else {
				appendWrappedTokens(tokens, text, segment.getStyle());
			}
		}
		return tokens;
	}

	private static void appendInlineCodeTokens(List<InlineToken> tokens, String text, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style) {
		String[] lines = text.split("\\n", -1);
		for(int i = 0; i < lines.length; i++) {
			if(!lines[i].isEmpty()) {
				tokens.add(new InlineToken(lines[i], style, false, false));
			}
			if(i < lines.length - 1) {
				tokens.add(new InlineToken("", style, false, true));
			}
		}
	}

	private static void appendWrappedTokens(List<InlineToken> tokens, String text, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style) {
		StringBuilder current = new StringBuilder();
		Boolean whitespace = null;
		for(int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if(character == '\n') {
				flushInlineToken(tokens, current, style, whitespace != null && whitespace);
				tokens.add(new InlineToken("", style, false, true));
				whitespace = null;
				continue;
			}

			boolean isWhitespace = Character.isWhitespace(character);
			if(whitespace != null && whitespace != isWhitespace) {
				flushInlineToken(tokens, current, style, whitespace);
			}

			current.append(character);
			whitespace = isWhitespace;
		}
		flushInlineToken(tokens, current, style, whitespace != null && whitespace);
	}

	private static void flushInlineToken(List<InlineToken> tokens, StringBuilder current, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style, boolean whitespace) {
		if(current.length() == 0) {
			return;
		}
		tokens.add(new InlineToken(current.toString(), style, whitespace, false));
		current.setLength(0);
	}

	private static int getDefaultLineHeight(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.BlockType type) {
		switch(type) {
			case HEADING_1:
				return 34;
			case HEADING_2:
				return 28;
			case HEADING_3:
				return 24;
			case CODE:
				return 18;
			case BULLET:
			case ORDERED:
			case PARAGRAPH:
			default:
				return 18;
		}
	}

	private static int getBlockSpacing(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.BlockType type) {
		switch(type) {
			case HEADING_1:
				return 18;
			case HEADING_2:
				return 14;
			case HEADING_3:
				return 12;
			case BULLET:
			case ORDERED:
				return 8;
			case PARAGRAPH:
			default:
				return 10;
		}
	}

	private static UnicodeFont getFontForInlineSegment(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.BlockType blockType, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style) {
		if(style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
			return FontLibrary.getCourierNew12White();
		}

		switch(blockType) {
			case HEADING_1:
				return FontLibrary.getBoldArial30WhiteNoOutline();
			case HEADING_2:
				return FontLibrary.getBoldArial24WhiteNoOutline();
			case HEADING_3:
				return FontLibrary.getBoldArial20WhiteNoOutline();
			case BULLET:
			case ORDERED:
			case PARAGRAPH:
			default:
				if(style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD || style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) {
					return FontLibrary.getBoldArial14White();
				}
				return FontLibrary.getRegularArial13White();
		}
	}

	private static Vector4f getInlineColor(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.BlockType blockType, org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style) {
		switch(blockType) {
			case HEADING_1:
				return style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.ITALIC || style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.92F, 0.97F, 1.0F, 1.0F) : new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
			case HEADING_2:
				return style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.ITALIC || style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.90F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.95F, 0.95F, 1.0F, 1.0F);
			case HEADING_3:
				return style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.ITALIC || style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.88F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.90F, 0.95F, 1.0F, 1.0F);
			case BULLET:
			case ORDERED:
			case PARAGRAPH:
			default:
				if(style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
					return new Vector4f(0.97F, 0.97F, 0.97F, 1.0F);
				}
				if(style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD || style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) {
					return new Vector4f(0.98F, 0.98F, 0.98F, 1.0F);
				}
				if(style == org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle.ITALIC) {
					return new Vector4f(0.84F, 0.92F, 1.0F, 1.0F);
				}
				return new Vector4f(0.88F, 0.88F, 0.88F, 1.0F);
		}
	}

	private static UnicodeFont getFontForBlock(org.schema.game.client.view.mainmenu.MarkdownDocRenderer.BlockType type) {
		switch(type) {
			case HEADING_1:
				return FontLibrary.getBoldArial30WhiteNoOutline();
			case HEADING_2:
				return FontLibrary.getBoldArial24WhiteNoOutline();
			case HEADING_3:
				return FontLibrary.getBoldArial20WhiteNoOutline();
			case CODE:
				return FontLibrary.getCourierNew12White();
			case BULLET:
			case ORDERED:
			case PARAGRAPH:
			case SEPARATOR:
			default:
				return FontLibrary.FontSize.MEDIUM.getFont();
		}
	}

	private static final class InlineToken {
		private final String text;
		private final org.schema.game.client.view.mainmenu.MarkdownDocRenderer.InlineStyle style;
		private final boolean whitespace;
		private final boolean lineBreak;

		private InlineToken(String text, MarkdownDocRenderer.InlineStyle style, boolean whitespace, boolean lineBreak) {
			this.text = text;
			this.style = style;
			this.whitespace = whitespace;
			this.lineBreak = lineBreak;
		}
	}
}

