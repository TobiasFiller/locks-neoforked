package melonslise.locks.common.init;

import melonslise.locks.Locks;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LocksCriteria
{
	public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Locks.ID);

	public static final DeferredHolder<CriterionTrigger<?>, LockPickedTrigger> LOCK_PICKED =
		TRIGGERS.register("lock_picked", LockPickedTrigger::new);

	// The lifetime total is kept as a custom stat: it persists with the player, is copied on death and also
	// shows up in the vanilla statistics screen.
	public static final DeferredRegister<ResourceLocation> STATS = DeferredRegister.create(Registries.CUSTOM_STAT, Locks.ID);

	public static final DeferredHolder<ResourceLocation, ResourceLocation> LOCKS_PICKED =
		STATS.register("locks_picked", () -> ResourceLocation.fromNamespaceAndPath(Locks.ID, "locks_picked"));

	private LocksCriteria() {}

	public static void register(IEventBus modBus)
	{
		TRIGGERS.register(modBus);
		STATS.register(modBus);
	}

	public static void onLockPicked(ServerPlayer player)
	{
		player.awardStat(Stats.CUSTOM.get(LOCKS_PICKED.get()));
		int count = player.getStats().getValue(Stats.CUSTOM.get(LOCKS_PICKED.get()));
		LOCK_PICKED.get().trigger(player, count);
	}
}
