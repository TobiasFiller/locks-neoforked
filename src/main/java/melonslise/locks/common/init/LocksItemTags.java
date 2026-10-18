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

	public static TagKey<Item> bind(String name)
	{
		return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Locks.ID, name));
	}
}
