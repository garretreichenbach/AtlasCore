package atlas.core.tests;

import atlas.core.manager.PlayerActionRegistry;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link PlayerActionRegistry}.
 * Run in-game via {@code /run_tests atlas.core.tests.*}.
 */
public class PlayerActionRegistryTest {

	@Test
	public void testRegisterReturnsKey() {
		String key = PlayerActionRegistry.register("atlas_core_test:returns_key", (args, sender) -> {
		});
		Assert.assertEquals("Registered action key must be returned verbatim", "atlas_core_test:returns_key", key);
	}

	@Test
	public void testDuplicateKeyIsRejected() {
		PlayerActionRegistry.register("atlas_core_test:duplicate", (args, sender) -> {
		});
		try {
			PlayerActionRegistry.register("atlas_core_test:duplicate", (args, sender) -> {
			});
			Assert.fail("Registering a duplicate key must throw");
		} catch(IllegalArgumentException expected) {
			// expected
		}
	}

	@Test
	public void testHandlerIsInvokedWithCorrectArgs() {
		boolean[] invoked = {false};
		String[] captured = {null};
		String key = PlayerActionRegistry.register("atlas_core_test:invoke", (args, sender) -> {
			invoked[0] = true;
			if(args != null && args.length > 0) captured[0] = args[0];
		});
		PlayerActionRegistry.process(key, new String[]{"ping"}, null);
		Assert.assertTrue("Handler must be invoked after process()", invoked[0]);
		Assert.assertEquals("Handler must receive the correct argument", "ping", captured[0]);
	}

	@Test
	public void testProcessWithEmptyArgsDoesNotThrow() {
		String key = PlayerActionRegistry.register("atlas_core_test:empty_args", (args, sender) -> {
		});
		// Must not throw even when args array is empty
		PlayerActionRegistry.process(key, new String[0], null);
	}
}
