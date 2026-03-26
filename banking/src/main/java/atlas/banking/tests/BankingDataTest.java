package atlas.banking.tests;

import atlas.banking.data.BankingData;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link BankingData} JSON serialization.
 * Run in-game via {@code /run_tests atlas.banking.tests.*}.
 */
public class BankingDataTest {

	@Test
	public void testNewDataHasZeroCredits() {
		BankingData data = new BankingData("Alice");
		Assert.assertEquals("New BankingData must start with 0 credits",
				0.0, data.getStoredCredits(), 0.0);
	}

	@Test
	public void testNewDataHasEmptyTransactionHistory() {
		BankingData data = new BankingData("Bob");
		Assert.assertTrue("New BankingData must have no transaction history",
				data.getTransactionHistory().isEmpty());
	}

	@Test
	public void testSetAndGetCredits() {
		BankingData data = new BankingData("Charlie");
		data.setStoredCredits(500.75);
		Assert.assertEquals(500.75, data.getStoredCredits(), 0.001);
	}

	@Test
	public void testSerializeRoundTrip() {
		BankingData original = new BankingData("Dave");
		original.setStoredCredits(1234.56);

		JSONObject json = original.serialize();
		BankingData restored = new BankingData(json);

		Assert.assertEquals("Player name must survive JSON round-trip",
				original.getPlayerName(), restored.getPlayerName());
		Assert.assertEquals("Credits must survive JSON round-trip",
				original.getStoredCredits(), restored.getStoredCredits(), 0.001);
		Assert.assertEquals("UUID must survive JSON round-trip",
				original.getUUID(), restored.getUUID());
	}

	@Test
	public void testTransactionRoundTrip() {
		BankingData.BankTransactionData tx = new BankingData.BankTransactionData(
				250.0, "from-uuid", "to-uuid", "Test subject", "Test message",
				BankingData.BankTransactionData.TransactionType.TRANSFER
		);

		JSONObject json = tx.serialize();
		BankingData.BankTransactionData restored = new BankingData.BankTransactionData(json);

		Assert.assertEquals("Amount must survive round-trip", tx.getAmount(), restored.getAmount(), 0.001);
		Assert.assertEquals("FromUUID must survive round-trip", tx.getFromUUID(), restored.getFromUUID());
		Assert.assertEquals("ToUUID must survive round-trip", tx.getToUUID(), restored.getToUUID());
		Assert.assertEquals("Subject must survive round-trip", tx.getSubject(), restored.getSubject());
		Assert.assertEquals("Message must survive round-trip", tx.getMessage(), restored.getMessage());
		Assert.assertEquals("TransactionType must survive round-trip",
				tx.getTransactionType(), restored.getTransactionType());
	}

	@Test
	public void testAddTransactionIncreasesHistorySize() {
		BankingData data = new BankingData("Eve");
		BankingData.BankTransactionData tx = new BankingData.BankTransactionData(
				100.0, "a", "b", "subj", "msg",
				BankingData.BankTransactionData.TransactionType.DEPOSIT
		);
		data.addTransaction(tx);
		Assert.assertEquals("Transaction history must contain the added transaction",
				1, data.getTransactionHistory().size());
	}

	@Test
	public void testDataTypeName() {
		BankingData data = new BankingData("Test");
		Assert.assertEquals("BANKING_DATA", data.getDataTypeName());
	}
}
