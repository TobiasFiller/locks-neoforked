package melonslise.locks.common.event;

import melonslise.locks.Locks;
import melonslise.locks.common.compat.CarryOnCompat;
import melonslise.locks.common.compat.CuriosHelper;
import melonslise.locks.common.compat.RespawningStructuresCompat;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LockTypeRegistry;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksNetwork;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Locks.ID, bus = EventBusSubscriber.Bus.MOD)
public final class LocksModEvents
{
	private LocksModEvents() {}

	@SubscribeEvent
	public static void onSetup(FMLCommonSetupEvent e)
	{
		e.enqueueWork(CuriosHelper::init);
		e.enqueueWork(RespawningStructuresCompat::init);
		e.enqueueWork(CarryOnCompat::init);
	}

	@SubscribeEvent
	public static void onRegisterCaps(RegisterCapabilitiesEvent event)
	{
		LocksCapabilities.registerCapabilities(event);
	}

	@SubscribeEvent
	public static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
	{
		LocksNetwork.register(event);
	}

	@SubscribeEvent
	public static void onConfigLoad(ModConfigEvent e)
	{
		if(e.getConfig().getSpec() == LocksConfig.SPEC)
		{
			LocksConfig.init();
			if (LockTypeRegistry.isLoaded())
				LockTypeRegistry.resetStats();
		}
		if(e.getConfig().getSpec() == LocksServerConfig.SPEC)
			LocksServerConfig.init();
	}
}
