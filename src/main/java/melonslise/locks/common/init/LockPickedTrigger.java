package melonslise.locks.common.init;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired every time a lock is successfully picked, carrying the player's lifetime total so advancements can
 * require a minimum (1 for the first lock, 100 for the milestone).
 */
public class LockPickedTrigger extends SimpleCriterionTrigger<LockPickedTrigger.TriggerInstance>
{
	@Override
	public Codec<TriggerInstance> codec()
	{
		return TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer player, int count)
	{
		this.trigger(player, instance -> instance.matches(count));
	}

	public record TriggerInstance(Optional<ContextAwarePredicate> player, int min) implements SimpleCriterionTrigger.SimpleInstance
	{
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
			Codec.INT.optionalFieldOf("min", 1).forGetter(TriggerInstance::min)
		).apply(instance, TriggerInstance::new));

		public boolean matches(int count)
		{
			return count >= this.min;
		}
	}
}
