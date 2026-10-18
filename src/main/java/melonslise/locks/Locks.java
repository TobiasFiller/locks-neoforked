package melonslise.locks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import melonslise.locks.common.config.LocksClientConfig;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksCreativeTabs;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.init.LocksFeatures;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.init.LocksMenuTypes;
import melonslise.locks.common.init.LocksPlacements;
import melonslise.locks.common.init.LocksRecipeSerializers;
import melonslise.locks.common.init.LocksSoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod("locks")
public final class Locks
{
	public static final String ID = "locks";

	public static final Logger LOGGER = LogManager.getLogger();

	public Locks(IEventBus modBus, ModContainer modContainer)
	{
		modContainer.registerConfig(Type.SERVER, LocksServerConfig.SPEC);
		modContainer.registerConfig(Type.COMMON, LocksConfig.SPEC);
		modContainer.registerConfig(Type.CLIENT, LocksClientConfig.SPEC);

		LocksCapabilities.register(modBus);
		LocksItems.register(modBus);
		LocksEnchantments.register();
		LocksSoundEvents.register(modBus);
		LocksPlacements.register(modBus);
		LocksFeatures.register(modBus);
		LocksMenuTypes.register(modBus);
		LocksRecipeSerializers.register(modBus);
		LocksCreativeTabs.register(modBus);
	}
}
