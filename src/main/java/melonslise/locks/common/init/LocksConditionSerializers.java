package melonslise.locks.common.init;

import com.mojang.serialization.MapCodec;
import melonslise.locks.Locks;
import melonslise.locks.common.steel.NativeSteelCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LocksConditionSerializers
{
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, Locks.ID);

	public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NativeSteelCondition>> STEEL = CONDITION_SERIALIZERS.register("steel", () -> NativeSteelCondition.CODEC);

	private LocksConditionSerializers() {}

	public static void register(IEventBus modBus)
	{
		CONDITION_SERIALIZERS.register(modBus);
	}
}
