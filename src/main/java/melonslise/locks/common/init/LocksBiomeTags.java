package melonslise.locks.common.init;

import melonslise.locks.Locks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class LocksBiomeTags
{
	private LocksBiomeTags() {}

	/**
	 * Biomes a {@link LocksItemTags#BIOME_GATED_LOCKS} lock may be generated in. Ships with
	 * {@code #minecraft:is_end}, i.e. only the netherite lock spawns in the End and no gated lock ever
	 * appears in the Overworld, the Nether or a modded dimension.
	 * <p>
	 * Datapack-overridable: add biomes here to move a gated lock into more dimensions, or clear it to stop
	 * generation of gated locks entirely.
	 */
	public static final TagKey<Biome> BIOME_GATED_LOCK_BIOMES = bind("biome_gated_lock_biomes");

	public static TagKey<Biome> bind(String name)
	{
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Locks.ID, name));
	}
}
