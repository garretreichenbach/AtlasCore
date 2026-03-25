package atlas.guide.renderer;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;

/**
 * Converts CommonMark markdown to a plain-text string suitable for display in
 * StarMade's {@code GUITextOverlay}. StarMade's text system supports newlines and
 * basic character sequences but not HTML or rich markup, so all markdown formatting
 * is translated to readable plain text.
 *
 * <p>Formatting rules:
 * <ul>
 *   <li>H1 headings: uppercased and underlined with {@code ═} characters</li>
 *   <li>H2 headings: prefixed with {@code ──} and followed by a rule</li>
 *   <li>H3+ headings: prefixed with {@code ▸ }</li>
 *   <li>Paragraphs: separated by a blank line</li>
 *   <li>Unordered list items: prefixed with {@code • }</li>
 *   <li>Ordered list items: prefixed with {@code N. }</li>
 *   <li>Code blocks: indented with two spaces per line</li>
 *   <li>Inline code: wrapped in backticks</li>
 *   <li>Bold/italic: stripped (not rendered)</li>
 *   <li>Links: displayed as {@code text (url)}</li>
 *   <li>Horizontal rules: rendered as {@code ────────────────────}</li>
 *   <li>Block quotes: prefixed with {@code │ }</li>
 * </ul>
 */
public final class AtlasMarkdownRenderer {

	private static final Parser PARSER = Parser.builder().build();

	private AtlasMarkdownRenderer() {}

	public static String render(String markdown) {
		Node document = PARSER.parse(markdown);
		StringBuilder sb = new StringBuilder();
		renderNode(document, sb, 0);
		// Trim trailing blank lines
		return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
	}

	// ── recursive node renderer ───────────────────────────────────────────────

	private static void renderNode(Node node, StringBuilder sb, int listDepth) {
		if(node instanceof Document) {
			renderChildren(node, sb, listDepth);

		} else if(node instanceof Heading) {
			Heading h = (Heading) node;
			String text = collectText(node);
			if(h.getLevel() == 1) {
				String upper = text.toUpperCase();
				String bar   = repeat('═', upper.length());
				sb.append('\n').append(upper).append('\n').append(bar).append('\n');
			} else if(h.getLevel() == 2) {
				sb.append('\n').append("── ").append(text).append('\n')
				  .append(repeat('─', text.length() + 3)).append('\n');
			} else {
				sb.append('\n').append("▸ ").append(text).append('\n');
			}

		} else if(node instanceof Paragraph) {
			sb.append('\n');
			renderChildren(node, sb, listDepth);
			sb.append('\n');

		} else if(node instanceof BulletList || node instanceof OrderedList) {
			sb.append('\n');
			renderChildren(node, sb, listDepth + 1);

		} else if(node instanceof ListItem) {
			Node parent = node.getParent();
			if(parent instanceof OrderedList) {
				// Count position
				int pos = 1;
				Node sibling = parent.getFirstChild();
				while(sibling != null && sibling != node) { pos++; sibling = sibling.getNext(); }
				sb.append(repeat(' ', (listDepth - 1) * 2)).append(pos).append(". ");
			} else {
				sb.append(repeat(' ', (listDepth - 1) * 2)).append("• ");
			}
			renderChildren(node, sb, listDepth);
			if(!sb.toString().endsWith("\n")) sb.append('\n');

		} else if(node instanceof FencedCodeBlock) {
			FencedCodeBlock code = (FencedCodeBlock) node;
			sb.append('\n');
			for(String line : code.getLiteral().split("\n")) {
				sb.append("  ").append(line).append('\n');
			}

		} else if(node instanceof IndentedCodeBlock) {
			IndentedCodeBlock code = (IndentedCodeBlock) node;
			sb.append('\n');
			for(String line : code.getLiteral().split("\n")) {
				sb.append("  ").append(line).append('\n');
			}

		} else if(node instanceof BlockQuote) {
			sb.append('\n');
			StringBuilder inner = new StringBuilder();
			renderChildren(node, inner, listDepth);
			for(String line : inner.toString().split("\n")) {
				sb.append("│ ").append(line).append('\n');
			}

		} else if(node instanceof ThematicBreak) {
			sb.append('\n').append("────────────────────").append('\n');

		} else if(node instanceof Text) {
			sb.append(((Text) node).getLiteral());

		} else if(node instanceof Code) {
			sb.append('`').append(((Code) node).getLiteral()).append('`');

		} else if(node instanceof Link) {
			String linkText = collectText(node);
			String url      = ((Link) node).getDestination();
			if(linkText.equals(url)) {
				sb.append(url);
			} else {
				sb.append(linkText).append(" (").append(url).append(')');
			}

		} else if(node instanceof SoftLineBreak || node instanceof HardLineBreak) {
			sb.append('\n');

		} else if(node instanceof StrongEmphasis || node instanceof Emphasis) {
			// Strip bold/italic markers; just render the content
			renderChildren(node, sb, listDepth);

		} else if(node instanceof Image) {
			// Skip images entirely (can't render in StarMade text)

		} else {
			// Unknown node type — recurse into children
			renderChildren(node, sb, listDepth);
		}
	}

	private static void renderChildren(Node parent, StringBuilder sb, int listDepth) {
		Node child = parent.getFirstChild();
		while(child != null) {
			renderNode(child, sb, listDepth);
			child = child.getNext();
		}
	}

	/** Collects all descendant text content as a plain string. */
	private static String collectText(Node node) {
		StringBuilder sb = new StringBuilder();
		collectTextInto(node, sb);
		return sb.toString();
	}

	private static void collectTextInto(Node node, StringBuilder sb) {
		if(node instanceof Text) {
			sb.append(((Text) node).getLiteral());
		} else if(node instanceof Code) {
			sb.append(((Code) node).getLiteral());
		} else {
			Node child = node.getFirstChild();
			while(child != null) {
				collectTextInto(child, sb);
				child = child.getNext();
			}
		}
	}

	private static String repeat(char c, int times) {
		char[] chars = new char[Math.max(0, times)];
		java.util.Arrays.fill(chars, c);
		return new String(chars);
	}
}
