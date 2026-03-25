package atlas.guide.manager;

import api.mod.StarMod;

import java.io.BufferedReader;
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
 * <p>Rendering is intentionally deferred to the GUI layer so it runs on the graphics
 * thread via {@link atlas.guide.util.MarkdownGuiBlockRenderer}.
 */
public final class GuideManager {

    /** Maps display title → raw markdown source. */
    private static final Map<String, String> rawByTitle = new LinkedHashMap<>();

    private GuideManager() {}

    // ── public API ────────────────────────────────────────────────────────────

    public static void loadDocs(StarMod mod) {
        rawByTitle.clear();

        InputStream indexStream = GuideManager.class.getResourceAsStream("/docs/docs.index");
        if(indexStream == null) {
            mod.logWarning("[AtlasGuide] docs.index not found — no guide documents loaded.");
            return;
        }

        for(String path : readLines(indexStream)) {
            if(path.trim().isEmpty()) continue;
            String resourcePath = "/docs/" + path.trim();
            InputStream docStream = GuideManager.class.getResourceAsStream(resourcePath);
            if(docStream == null) {
                mod.logWarning("[AtlasGuide] Missing doc resource: " + resourcePath);
                continue;
            }
            String markdown = readString(docStream);
            String title    = extractTitle(markdown, path);
            rawByTitle.put(title, markdown);
        }
        mod.logInfo("[AtlasGuide] Loaded " + rawByTitle.size() + " guide document(s).");
    }

    public static List<String> getTitles() {
        return Collections.unmodifiableList(new ArrayList<>(rawByTitle.keySet()));
    }

    /** Returns the raw markdown for the given title, or an empty string if not found. */
    public static String getRaw(String title) {
        return rawByTitle.getOrDefault(title, "");
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static String extractTitle(String markdown, String fallback) {
        for(String line : markdown.split("\\n")) {
            String trimmed = line.trim();
            if(trimmed.startsWith("# ")) return trimmed.substring(2).trim();
        }
        String name = fallback.contains("/")
                ? fallback.substring(fallback.lastIndexOf('/') + 1)
                : fallback;
        return name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
    }

    private static List<String> readLines(InputStream stream) {
        List<String> lines = new ArrayList<>();
        try(BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while((line = r.readLine()) != null) lines.add(line);
        } catch(Exception ignored) {}
        return lines;
    }

    private static String readString(InputStream stream) {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch(Exception ignored) {}
        return sb.toString();
    }
}
