package melonslise.locks.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The optional upper bound on loot-scaled generation. The lower bound already lives in
 * {@code Loot Value Tiers}; this rule only decides whether a discovered value is too rich for Locks
 * to place a lock at all. Pure doubles, no config spec or Bootstrap.
 */
public class LootValueLimitTest
{
	@Test
	void aDisabledLimitNeverCutsOffAChest()
	{
		assertTrue(LootValueLimit.isAllowed(0.0d, LootValueLimit.DISABLED));
		assertTrue(LootValueLimit.isAllowed(22.2d, LootValueLimit.DISABLED));
		assertTrue(LootValueLimit.isAllowed(1.0E9d, LootValueLimit.DISABLED));
	}

	@Test
	void valuesBelowTheLimitRemainEligible()
	{
		assertTrue(LootValueLimit.isAllowed(2.0d, 20.0d));
		assertTrue(LootValueLimit.isAllowed(19.999d, 20.0d));
	}

	@Test
	void theLimitItselfIsStillEligible()
	{
		// "Above" is exclusive: a chest exactly at the configured value is not cut off.
		assertTrue(LootValueLimit.isAllowed(20.0d, 20.0d));
	}

	@Test
	void valuesAboveTheLimitGetNoLock()
	{
		assertFalse(LootValueLimit.isAllowed(20.001d, 20.0d));
		assertFalse(LootValueLimit.isAllowed(50.4d, 20.0d));
	}

	@Test
	void anUnknownValueIsNotTreatedAsTooRich()
	{
		// LocksUtil already falls back to weighted selection for NaN loot values. The upper cutoff
		// must not quietly turn that into "no lock" as well.
		assertTrue(LootValueLimit.isAllowed(Double.NaN, 20.0d));
	}

	@Test
	void anyNegativeValueDisablesTheLimit()
	{
		// The config clamps to -1, but a hand-edited file should never invert the rule.
		assertTrue(LootValueLimit.isAllowed(1000.0d, -0.5d));
	}
}
