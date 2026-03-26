package atlas.guide.tests;

import atlas.guide.manager.GuideManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link GuideManager}.
 * Run in-game via {@code /run_tests atlas.guide.tests.*}.
 *
 * <p>Note: these tests run after {@code AtlasGuide.onClientCreated()} has loaded
 * the guide documents, so the cache will already be populated if any docs exist.
 */
public class GuideManagerTest {

	@Test
	public void testGetTitlesReturnsNonNull() {
		List<String> titles = GuideManager.getTitles();
		Assert.assertNotNull("getTitles() must never return null", titles);
	}

	@Test
	public void testGetRawUnknownTitleReturnsEmpty() {
		String raw = GuideManager.getRaw("__NONEXISTENT_GUIDE_TITLE_XYZ__");
		Assert.assertEquals("getRaw() must return empty string for unknown title", "", raw);
	}

	@Test
	public void testGetRawMatchesTitleForEachDoc() {
		List<String> titles = GuideManager.getTitles();
		for(String title : titles) {
			String raw = GuideManager.getRaw(title);
			Assert.assertNotNull("getRaw() must not return null for a registered title: " + title, raw);
			Assert.assertFalse("getRaw() must return non-empty content for registered title: " + title,
					raw.isEmpty());
		}
	}

	@Test
	public void testTitleListIsUnmodifiable() {
		List<String> titles = GuideManager.getTitles();
		try {
			titles.add("INJECTED_TITLE");
			Assert.fail("getTitles() must return an unmodifiable list");
		} catch(UnsupportedOperationException expected) {
			// correct behaviour
		}
	}
}
