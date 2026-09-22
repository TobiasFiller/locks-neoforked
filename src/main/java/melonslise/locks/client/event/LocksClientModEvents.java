package melonslise.locks.client.event;

import melonslise.locks.Locks;
import melonslise.locks.client.init.LocksItemModelsProperties;
import melonslise.locks.client.init.LocksScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Locks.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LocksClientModEvents
{
	private LocksClientModEvents() {}

	@SubscribeEvent
	public static void onSetup(FMLClientSetupEvent e)
	{
		// FMLClientSetupEvent is MOD-bus and parallel-dispatched: neither the screen registry nor the item
		// property registry is thread-safe, so both registrations are queued onto the main thread.
			e.enqueueWork(() ->
		{
			LocksItemModelsProperties.register();
		});
	}

	@SubscribeEvent
	public static void onRegisterScreens(RegisterMenuScreensEvent event)
	{
		LocksScreens.register(event);
	}
}
