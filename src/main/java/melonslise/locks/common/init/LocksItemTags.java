package melonslise.locks.common.init;

import melonslise.locks.Locks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class LocksItemTags
{
	private LocksItemTags() {}

	public static final TagKey<Item>
		KEYS = bind("keys"),
		LOCKS = bind("locks"),
		LOCK_PICKS = bind("lock_picks");

	/**
	 * Locks that may only be generated in {@link LocksBiomeTags#BIOME_GATED_LOCK_BIOMES}. Ships with
	 * {@code locks:netherite_lock}, which makes the netherite lock End-exclusive.
	 */
	public static final TagKey<Item> BIOME_GATED_LOCKS = bind("biome_gated_locks");

	/**
	 * Locks that are only generated when some other mod provides steel through {@code c:ingots/steel}. Ships with
	 * {@code locks:steel_lock}. This is a hard gate, not a default: a lock listed in {@code Generated Locks} is
	 * still skipped while {@code c:ingots/steel} is empty, so a stale config cannot put it back into the world.
	 */
	public static final TagKey<Item> STEEL_PROVIDER_LOCKS = bind("steel_provider_locks");

	public static TagKey<Item> bind(String name)
	{
		return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Locks.ID, name));
	}
}
