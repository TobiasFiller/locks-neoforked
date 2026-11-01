package melonslise.locks.common.init;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a lock is successfully placed on a door, trapdoor or barrel, so the
 * "Not Just Chests" advancement can teach that locks work on more than chests.
 */
public class LockPlacedOnTargetTrigger extends SimpleCriterionTrigger<LockPlacedOnTargetTrigger.TriggerInstance>
{
	@Override
	public Codec<TriggerInstance> codec()
	{
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player)
	{
		this.trigger(player, instance -> true);
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance
	{
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
		).apply(instance, TriggerInstance::new));
	}
}
