package melonslise.locks.common.init;

import net.minecraft.core.registries.BuiltInRegistries;
import melonslise.locks.Locks;
import melonslise.locks.common.worldgen.LockChestsFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LocksFeatures
{
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, Locks.ID);

	public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>>
		LOCK_CHESTS = FEATURES.register("lock_chests", () -> new LockChestsFeature(NoneFeatureConfiguration.CODEC));

	private LocksFeatures() {}

	public static void register(IEventBus modBus)
	{
		FEATURES.register(modBus);
	}
}
