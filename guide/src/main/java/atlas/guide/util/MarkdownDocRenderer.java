package atlas.guide.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownDocRenderer {
	private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
	private static final Pattern ORDERED_PATTERN = Pattern.compile("^(\\d+)\\.\\s+(.*)$");
	private static final Pattern BULLET_PATTERN = Pattern.compile("^[-*+]\\s+(.*)$");
	private static final Pattern THEMATIC_PATTERN = Pattern.compile("^([*-_])\\1{2,}$");

	private MarkdownDocRenderer() {
	}

	public static List<RenderedBlock> render(String markdown) {
		String normalized = markdown == null ? "" : markdown.replace("\r", "");
		if(normalized.trim().isEmpty()) {
			return Collections.emptyList();
		}

		List<RenderedBlock> blocks = new ArrayList<>();
		String[] lines = normalized.split("\\n", -1);
		StringBuilder paragraph = new StringBuilder();
		boolean inCodeFence = false;
		StringBuilder fencedCode = new StringBuilder();

		for(String rawLine : lines) {
			String line = rawLine == null ? "" : rawLine;
			String trimmed = line.trim();

			if(inCodeFence) {
				if(trimmed.startsWith("```")) {
					addCodeBlock(blocks, fencedCode.toString());
					fencedCode.setLength(0);
					inCodeFence = false;
				} else {
					if(fencedCode.length() > 0) {
						fencedCode.append('\n');
					}
					fencedCode.append(line);
				}
				continue;
			}

			if(trimmed.startsWith("```")) {
				flushParagraph(blocks, paragraph);
				inCodeFence = true;
				continue;
			}

			if(trimmed.isEmpty()) {
				flushParagraph(blocks, paragraph);
				continue;
			}

			Matcher headingMatcher = HEADING_PATTERN.matcher(trimmed);
			if(headingMatcher.matches()) {
				flushParagraph(blocks, paragraph);
				int level = Math.min(3, headingMatcher.group(1).length());
				List<InlineSegment> segments = parseInlineSegments(headingMatcher.group(2));
				String text = normalize(flattenSegments(segments));
				if(!text.isEmpty()) {
					blocks.add(new RenderedBlock(levelToType(level), text, segments));
				}
				continue;
			}

			Matcher orderedMatcher = ORDERED_PATTERN.matcher(trimmed);
			if(orderedMatcher.matches()) {
				flushParagraph(blocks, paragraph);
				String textBody = orderedMatcher.group(2);
				String prefix = orderedMatcher.group(1) + ". ";
				addListBlock(blocks, BlockType.ORDERED, prefix, textBody);
				continue;
			}

			Matcher bulletMatcher = BULLET_PATTERN.matcher(trimmed);
			if(bulletMatcher.matches()) {
				flushParagraph(blocks, paragraph);
				addListBlock(blocks, BlockType.BULLET, "- ", bulletMatcher.group(1));
				continue;
			}

			if(isThematicBreak(trimmed)) {
				flushParagraph(blocks, paragraph);
				blocks.add(new RenderedBlock(BlockType.SEPARATOR, "--------------------------------"));
				continue;
			}

			if(paragraph.length() > 0) {
				paragraph.append('\n');
			}
			paragraph.append(line);
		}

		if(inCodeFence) {
			addCodeBlock(blocks, fencedCode.toString());
		}
		flushParagraph(blocks, paragraph);
		return blocks;
	}

	private static boolean isThematicBreak(String trimmed) {
		String compact = trimmed.replace(" ", "");
		return THEMATIC_PATTERN.matcher(compact).matches();
	}

	private static BlockType levelToType(int level) {
		switch(level) {
			case 1:
				return BlockType.HEADING_1;
			case 2:
				return BlockType.HEADING_2;
			default:
				return BlockType.HEADING_3;
		}
	}

	private static void addListBlock(List<RenderedBlock> blocks, BlockType type, String prefix, String textBody) {
		List<InlineSegment> segments = new ArrayList<>();
		segments.add(new InlineSegment(InlineStyle.NORMAL, prefix));
		segments.addAll(parseInlineSegments(textBody));
		String text = normalize(flattenSegments(segments));
		if(!text.isEmpty()) {
			blocks.add(new RenderedBlock(type, text, segments));
		}
	}

	private static void flushParagraph(List<RenderedBlock> blocks, StringBuilder paragraph) {
		if(paragraph.length() == 0) {
			return;
		}
		String text = paragraph.toString().trim();
		paragraph.setLength(0);
		if(text.isEmpty()) {
			return;
		}

		List<InlineSegment> segments = parseInlineSegments(text.replace("\n", " "));
		String flattened = normalize(flattenSegments(segments));
		if(!flattened.isEmpty()) {
			blocks.add(new RenderedBlock(BlockType.PARAGRAPH, flattened, segments));
		}
	}

	private static void addCodeBlock(List<RenderedBlock> blocks, String rawCode) {
		String text = rawCode == null ? "" : rawCode.trim();
		if(!text.isEmpty()) {
			blocks.add(new RenderedBlock(BlockType.CODE, text));
		}
	}

	private static String normalize(String text) {
		if(text == null) {
			return "";
		}
		return text.replace("\r", "").trim();
	}

	public enum BlockType {
		HEADING_1,
		HEADING_2,
		HEADING_3,
		PARAGRAPH,
		BULLET,
		ORDERED,
		CODE,
		SEPARATOR
	}

	private static String flattenSegments(List<InlineSegment> segments) {
		StringBuilder builder = new StringBuilder();
		for(InlineSegment segment : segments) {
			builder.append(segment.getText());
		}
		return builder.toString();
	}

	public enum InlineStyle {
		NORMAL,
		BOLD,
		ITALIC,
		BOLD_ITALIC,
		INLINE_CODE
	}

	public static final class InlineSegment {
		private final InlineStyle style;
		private final String text;

		public InlineSegment(InlineStyle style, String text) {
			this.style = style;
			this.text = text;
		}

		public InlineStyle getStyle() {
			return style;
		}

		public String getText() {
			return text;
		}
	}

	public static final class RenderedBlock {
		private final BlockType type;
		private final String text;
		private final List<InlineSegment> segments;

		public RenderedBlock(BlockType type, String text) {
			this(type, text, Collections.singletonList(new InlineSegment(InlineStyle.NORMAL, text)));
		}

		public RenderedBlock(BlockType type, String text, List<InlineSegment> segments) {
			this.type = type;
			this.text = text;
			this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
		}

		public BlockType getType() {
			return type;
		}

		public String getText() {
			return text;
		}

		public List<InlineSegment> getSegments() {
			return segments;
		}
	}

	private static List<InlineSegment> parseInlineSegments(String text) {
		String normalized = text == null ? "" : text.replace("\r", "");
		if(normalized.isEmpty()) {
			return Collections.emptyList();
		}

		List<InlineSegment> segments = new ArrayList<>();
		StringBuilder normal = new StringBuilder();
		int i = 0;
		while(i < normalized.length()) {
			char c = normalized.charAt(i);

			if(c == '`') {
				int end = normalized.indexOf('`', i + 1);
				if(end > i + 1) {
					flushNormal(segments, normal);
					appendSegment(segments, InlineStyle.INLINE_CODE, normalized.substring(i + 1, end));
					i = end + 1;
					continue;
				}
			}

			if(i + 2 < normalized.length() && isTripleMarker(normalized, i)) {
				String marker = normalized.substring(i, i + 3);
				int end = normalized.indexOf(marker, i + 3);
				if(end > i + 3) {
					flushNormal(segments, normal);
					appendSegment(segments, InlineStyle.BOLD_ITALIC, normalized.substring(i + 3, end));
					i = end + 3;
					continue;
				}
			}

			if(i + 1 < normalized.length() && isDoubleMarker(normalized, i)) {
				String marker = normalized.substring(i, i + 2);
				int end = normalized.indexOf(marker, i + 2);
				if(end > i + 2) {
					flushNormal(segments, normal);
					appendSegment(segments, InlineStyle.BOLD, normalized.substring(i + 2, end));
					i = end + 2;
					continue;
				}
			}

			if(c == '*' || c == '_') {
				int end = normalized.indexOf(c, i + 1);
				if(end > i + 1) {
					flushNormal(segments, normal);
					appendSegment(segments, InlineStyle.ITALIC, normalized.substring(i + 1, end));
					i = end + 1;
					continue;
				}
			}

			normal.append(c);
			i++;
		}

		flushNormal(segments, normal);
		if(segments.isEmpty()) {
			segments.add(new InlineSegment(InlineStyle.NORMAL, normalized));
		}
		return segments;
	}

	private static boolean isTripleMarker(String text, int index) {
		char c = text.charAt(index);
		return (c == '*' || c == '_') && text.charAt(index + 1) == c && text.charAt(index + 2) == c;
	}

	private static boolean isDoubleMarker(String text, int index) {
		char c = text.charAt(index);
		return (c == '*' || c == '_') && text.charAt(index + 1) == c;
	}

	private static void flushNormal(List<InlineSegment> segments, StringBuilder builder) {
		if(builder.length() == 0) {
			return;
		}
		appendSegment(segments, InlineStyle.NORMAL, builder.toString());
		builder.setLength(0);
	}

	private static void appendSegment(List<InlineSegment> segments, InlineStyle style, String text) {
		if(text == null || text.isEmpty()) {
			return;
		}
		InlineSegment previous = segments.isEmpty() ? null : segments.get(segments.size() - 1);
		if(previous != null && previous.getStyle() == style) {
			segments.set(segments.size() - 1, new InlineSegment(style, previous.getText() + text));
		} else {
			segments.add(new InlineSegment(style, text));
		}
	}
}

