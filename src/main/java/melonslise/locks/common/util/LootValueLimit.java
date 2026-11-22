package melonslise.locks.common.util;

/**
 * Pure rule for the optional upper cutoff in loot-scaled lock generation.
 *
 * <p>Locks already has a lower bound: a chest below the lowest entry in
 * {@code Loot Value Tiers} gets no lock. This adds the opposite end without touching that table —
 * a server can leave the richest chests bare so another lock mod can claim them. No game state,
 * no config access, so the boundary is unit-testable on its own.
 */
public final class LootValueLimit
{
	/** Value used by the config default to mean "no upper limit". */
	public static final double DISABLED = -1.0d;

	private LootValueLimit() {}

	/**
	 * @param lootValue the estimated value of the chest's loot table
	 * @param maximum   the configured cutoff; {@link #DISABLED} or any negative value disables it
	 * @return true when Locks may still place a lock on this chest
	 */
	public static boolean isAllowed(double lootValue, double maximum)
	{
		// Written as "reject only when definitely above" so NaN falls through to the caller's
		// existing unknown-loot-value fallback instead of being silently treated as rich.
		return maximum < 0.0d || !(lootValue > maximum);
	}
}
