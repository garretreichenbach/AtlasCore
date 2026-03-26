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
	public void testRegisterReturnsNonNegativeId() {
		int id = PlayerActionRegistry.register((args, sender) -> {
		});
		Assert.assertTrue("Registered action ID must be >= 0", id >= 0);
	}

	@Test
	public void testConsecutiveRegistrationsReturnDistinctIds() {
		int id1 = PlayerActionRegistry.register((args, sender) -> {
		});
		int id2 = PlayerActionRegistry.register((args, sender) -> {
		});
		Assert.assertFalse("Consecutive registrations must produce distinct IDs", id1 == id2);
	}

	@Test
	public void testHandlerIsInvokedWithCorrectArgs() {
		boolean[] invoked = {false};
		String[] captured = {null};
		int id = PlayerActionRegistry.register((args, sender) -> {
			invoked[0] = true;
			if(args != null && args.length > 0) captured[0] = args[0];
		});
		PlayerActionRegistry.process(id, new String[]{"ping"}, null);
		Assert.assertTrue("Handler must be invoked after process()", invoked[0]);
		Assert.assertEquals("Handler must receive the correct argument", "ping", captured[0]);
	}

	@Test
	public void testProcessWithEmptyArgsDoesNotThrow() {
		int id = PlayerActionRegistry.register((args, sender) -> {
		});
		// Must not throw even when args array is empty
		PlayerActionRegistry.process(id, new String[0], null);
	}
}
