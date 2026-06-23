package atlas.contracts.tests;

import atlas.contracts.data.contract.BountyContract;
import atlas.contracts.data.contract.ContractData;
import atlas.contracts.data.contract.EscortContract;
import atlas.contracts.data.contract.ItemsContract;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.schema.common.util.linAlg.Vector3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for contract construction and JSON serialization round-trips.
 * Run in-game via {@code /run_tests atlas.contracts.tests.*}.
 */
public class ContractDataTest {

	private static BountyContract newBounty() {
		JSONObject target = new JSONObject();
		target.put("target_type", BountyContract.PLAYER);
		target.put("player_name", "Victim");
		target.put("reward", 5L);
		return new BountyContract(0, "[Bounty] Kill Victim", target, ContractData.Difficulty.NORMAL);
	}

	@Test
	public void testDataTypeName() {
		Assert.assertEquals("CONTRACT_DATA", newBounty().getDataTypeName());
	}

	@Test
	public void testBountyRoundTrip() {
		BountyContract original = newBounty();
		JSONObject json = original.serialize();
		ContractData restored = ContractData.readContract(json);
		Assert.assertTrue(restored instanceof BountyContract);
		Assert.assertEquals(original.getUUID(), restored.getUUID());
		Assert.assertEquals(original.getName(), restored.getName());
		Assert.assertEquals(original.getReward(), restored.getReward());
		Assert.assertEquals(ContractData.ContractType.BOUNTY, restored.getContractType());
		Assert.assertEquals("Victim", ((BountyContract) restored).getTargetData().getString("player_name"));
	}

	@Test
	public void testItemsRoundTrip() {
		ItemsContract original = new ItemsContract(0, "[Items] Deliver x10", 5L, (short) 343, 10, ContractData.Difficulty.NORMAL);
		JSONObject json = original.serialize();
		ContractData restored = ContractData.readContract(json);
		Assert.assertTrue(restored instanceof ItemsContract);
		Assert.assertEquals(10, ((ItemsContract) restored).getTargetAmount());
		Assert.assertEquals((short) 343, ((ItemsContract) restored).getTargetID());
		Assert.assertEquals(ContractData.ContractType.ITEMS, restored.getContractType());
	}

	@Test
	public void testEscortRoundTrip() {
		List<Vector3i> route = new ArrayList<>();
		route.add(new Vector3i(1, 1, 1));
		route.add(new Vector3i(3, 1, 4));
		List<String> cargo = new ArrayList<>(Collections.singletonList("T180-18"));
		List<String> defenders = new ArrayList<>(Collections.singletonList("C140-9"));
		EscortContract original = new EscortContract(0, "[Escort] Deliver cargo", 7L, ContractData.Difficulty.NORMAL, route, cargo, defenders, 3);
		JSONObject json = original.serialize();
		ContractData restored = ContractData.readContract(json);
		Assert.assertTrue(restored instanceof EscortContract);
		EscortContract e = (EscortContract) restored;
		Assert.assertEquals(2, e.getRoute().size());
		Assert.assertEquals(3, e.getTotalCargoCount());
		Assert.assertEquals(new Vector3i(1, 1, 1), e.getStartSector());
		Assert.assertEquals(ContractData.ContractType.ESCORT, restored.getContractType());
	}

	@Test
	public void testDifficultyScalesRewardOnConstruction() {
		JSONObject target = new JSONObject();
		target.put("target_type", BountyContract.PLAYER);
		target.put("player_name", "Victim");
		target.put("reward", 10L);
		BountyContract hard = new BountyContract(0, "test", target, ContractData.Difficulty.HARD);
		// HARD multiplier is 1.5x.
		Assert.assertEquals(15L, hard.getReward());
	}
}
