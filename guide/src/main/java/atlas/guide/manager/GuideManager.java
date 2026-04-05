package atlas.guide.manager;

import api.mod.StarMod;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads and caches guide documents from the mod jar's {@code /docs/} resource folder.
 *
 * <p>At build time, {@code syncDocumentationResources} copies {@code docs/markdown/*.md}
 * into {@code src/main/resources/docs/}, and {@code generateDocumentationIndex} writes a
 * {@code docs.index} file listing all relative paths. At runtime this class reads that
 * index and loads each document as raw markdown, extracting the first H1 heading as the
 * display title.
 *
 * <p>Category grouping is derived from the subdirectory component of each path in
 * {@code docs.index}. For example, {@code atlas-mods/atlas-core.md} belongs to the
 * {@code "Atlas Mods"} category. Files at the root (e.g. {@code server-rules.md}) are
 * placed in an implicit {@code ""} (uncategorized) group rendered at the top of the list.
 *
 * <p>Rendering is intentionally deferred to the GUI layer so it runs on the graphics
 * thread.
 */
public final class GuideManager {

	/** Maps display title → raw markdown source. */
	private static final Map<String, String> rawByTitle = new LinkedHashMap<>();

	/** Maps display title → category name (empty = uncategorized). */
	private static final Map<String, String> categoryByTitle = new LinkedHashMap<>();

	private GuideManager() {
	}

	// ── public API ────────────────────────────────────────────────────────────

	/**
	 * Loads guide documents from the given mod's jar into the shared title registry.
	 *
	 * <p>This method is <em>additive</em> — calling it from multiple mods accumulates all
	 * of their documents into the same guide dialog. AtlasGuide calls it for its own docs
	 * first; third-party mods should call it from their {@code onClientCreated()} so their
	 * documents appear alongside the built-in ones.
	 *
	 * <p>The mod's jar must contain a {@code docs/docs.index} manifest listing one relative
	 * {@code .md} path per line, and the corresponding {@code docs/} resource files. The
	 * Gradle tasks {@code syncDocumentationResources} and {@code generateDocumentationIndex}
	 * (copied from {@code guide/build.gradle}) produce both automatically at build time.
	 *
	 * <p>If a document title already exists in the registry it will be overwritten by the
	 * newly loaded version; use unique first-level headings to avoid collisions.
	 *
	 * @param mod the mod whose jar should be searched for {@code docs/docs.index}
	 */
	public static void loadDocs(StarMod mod) {
		ClassLoader cl = mod.getClass().getClassLoader();

		InputStream indexStream = cl.getResourceAsStream("atlas-guide-docs/docs.index");
		if(indexStream == null) {
			mod.logWarning("[AtlasGuide] docs.index not found in " + mod.getClass().getName() + " — no guide documents loaded.");
			return;
		}

		int before = rawByTitle.size();
		for(String path : readLines(indexStream)) {
			if(path.trim().isEmpty()) continue;
			String resourcePath = "atlas-guide-docs/" + path.trim();
			InputStream docStream = cl.getResourceAsStream(resourcePath);
			if(docStream == null) {
				mod.logWarning("[AtlasGuide] Missing doc resource: " + resourcePath);
				continue;
			}
			String markdown = readString(docStream);
			String title = extractTitle(markdown, path);
			String category = extractCategory(path.trim());
			rawByTitle.put(title, markdown);
			categoryByTitle.put(title, category);
		}
		mod.logInfo("[AtlasGuide] Loaded " + (rawByTitle.size() - before) + " guide document(s) from " + mod.getClass().getName() + " (total: " + rawByTitle.size() + ").");
	}

	/**
	 * Loads guide documents from all {@code .md} files found under {@code dir} (recursively).
	 *
	 * <p>Unlike {@link #loadDocs(StarMod)}, this method reads directly from the filesystem,
	 * so server administrators can drop or edit documents without recompiling any jar.
	 * AtlasGuide calls this automatically for its own {@code moddata/AtlasGuide/docs/}
	 * folder; call it yourself with a custom directory if you want the same behaviour for
	 * your own mod:
	 *
	 * <pre>{@code
	 * File myDocsDir = new File(mod.getSkeleton().getResourcesFolder(), "docs");
	 * GuideManager.loadDocsFromDirectory(myDocsDir, mod);
	 * }</pre>
	 *
	 * <p>The directory is created automatically if it does not exist so the server operator
	 * can immediately see where to place files. Documents are added to the shared registry
	 * in filename-alphabetical order; duplicate titles overwrite earlier entries.
	 *
	 * @param dir the directory to scan (subdirectories are included)
	 * @param mod the mod used for log messages
	 */
	public static void loadDocsFromDirectory(File dir, StarMod mod) {
		if(!dir.exists()) {
			if(dir.mkdirs()) {
				mod.logInfo("[AtlasGuide] Created docs directory: " + dir.getAbsolutePath());
			} else {
				mod.logWarning("[AtlasGuide] Could not create docs directory: " + dir.getAbsolutePath());
				return;
			}
		}

		List<File> mdFiles = collectMarkdownFiles(dir);
		if(mdFiles.isEmpty()) {
			mod.logInfo("[AtlasGuide] No .md files found in: " + dir.getAbsolutePath());
			return;
		}

		int before = rawByTitle.size();
		for(File file : mdFiles) {
			try {
				String markdown = readFile(file);
				String title = extractTitle(markdown, file.getName());
				// Derive category from the path relative to the root docs dir
				String relativePath = dir.toURI().relativize(file.toURI()).getPath();
				String category = extractCategory(relativePath);
				rawByTitle.put(title, markdown);
				categoryByTitle.put(title, category);
			} catch(Exception e) {
				mod.logWarning("[AtlasGuide] Failed to load " + file.getAbsolutePath() + ": " + e.getMessage());
			}
		}
		mod.logInfo("[AtlasGuide] Loaded " + (rawByTitle.size() - before) + " guide document(s) from " + dir.getAbsolutePath() + " (total: " + rawByTitle.size() + ").");
	}

	/**
	 * Returns all unique category names in the order they were first encountered.
	 * The empty string {@code ""} represents uncategorized (root-level) documents
	 * and is always first if any exist.
	 */
	public static List<String> getCategories() {
		List<String> categories = new ArrayList<>();
		for(String category : categoryByTitle.values()) {
			if(!categories.contains(category)) categories.add(category);
		}
		return Collections.unmodifiableList(categories);
	}

	/**
	 * Returns all document titles belonging to the given category, in insertion order.
	 * Pass {@code ""} for uncategorized (root-level) documents.
	 */
	public static List<String> getTitlesForCategory(String category) {
		List<String> titles = new ArrayList<>();
		for(Map.Entry<String, String> entry : categoryByTitle.entrySet()) {
			if(entry.getValue().equals(category)) titles.add(entry.getKey());
		}
		return Collections.unmodifiableList(titles);
	}

	/** Returns all document titles across all categories, in insertion order. */
	public static List<String> getTitles() {
		return Collections.unmodifiableList(new ArrayList<>(rawByTitle.keySet()));
	}

	/** Returns the raw markdown for the given title, or an empty string if not found. */
	public static String getRaw(String title) {
		return rawByTitle.getOrDefault(title, "");
	}

	/** Returns the category for the given title, or {@code ""} if uncategorized. */
	public static String getCategory(String title) {
		return categoryByTitle.getOrDefault(title, "");
	}

	// ── private helpers ───────────────────────────────────────────────────────

	/**
	 * Extracts a human-readable category name from a doc path.
	 * {@code "atlas-mods/atlas-core.md"} → {@code "Atlas Mods"}.
	 * Root-level paths (no directory component) return {@code ""}.
	 */
	private static String extractCategory(String path) {
		int slash = path.lastIndexOf('/');
		if(slash <= 0) return "";
		String dir = path.substring(0, slash);
		// Convert hyphen-separated dir name to title case: "atlas-mods" → "Atlas Mods"
		String[] parts = dir.split("[-_]");
		StringBuilder sb = new StringBuilder();
		for(String part : parts) {
			if(part.isEmpty()) continue;
			if(sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0)));
			sb.append(part.substring(1).toLowerCase(Locale.ROOT));
		}
		return sb.toString();
	}

	private static String extractTitle(String markdown, String fallback) {
		for(String line : markdown.split("\\n")) {
			String trimmed = line.trim();
			if(trimmed.startsWith("# ")) return trimmed.substring(2).trim();
		}
		String name = fallback.contains("/") ? fallback.substring(fallback.lastIndexOf('/') + 1) : fallback;
		return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
	}

	private static List<String> readLines(InputStream stream) {
		List<String> lines = new ArrayList<>();
		try(BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while((line = r.readLine()) != null) lines.add(line);
		} catch(Exception ignored) {
		}
		return lines;
	}

	/** Recursively collects all {@code .md} files under {@code dir}, sorted by path. */
	private static List<File> collectMarkdownFiles(File dir) {
		List<File> results = new ArrayList<>();
		File[] entries = dir.listFiles();
		if(entries == null) return results;
		Arrays.sort(entries);
		for(File entry : entries) {
			if(entry.isDirectory()) {
				results.addAll(collectMarkdownFiles(entry));
			} else if(entry.getName().endsWith(".md")) {
				results.add(entry);
			}
		}
		return results;
	}

	private static String readFile(File file) throws java.io.IOException {
		StringBuilder sb = new StringBuilder();
		try(BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while((line = r.readLine()) != null) sb.append(line).append('\n');
		}
		return sb.toString();
	}

	private static String readString(InputStream stream) {
		StringBuilder sb = new StringBuilder();
		try(BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while((line = r.readLine()) != null) sb.append(line).append('\n');
		} catch(Exception ignored) {
		}
		return sb.toString();
	}
}
