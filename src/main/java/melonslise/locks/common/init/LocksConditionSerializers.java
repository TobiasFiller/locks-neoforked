package melonslise.locks.common.init;

import com.mojang.serialization.MapCodec;
import melonslise.locks.Locks;
import melonslise.locks.common.steel.NativeSteelCondition;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LocksConditionSerializers
{
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS = DeferredRegister.create(ForgeRegistries.CONDITION_SERIALIZERS, Locks.ID);

	public static final RegistryObject<MapCodec<? extends ICondition>> STEEL = CONDITION_SERIALIZERS.register("steel", () -> NativeSteelCondition.CODEC);

	private LocksConditionSerializers() {}

	public static void register()
	{
		CONDITION_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}
