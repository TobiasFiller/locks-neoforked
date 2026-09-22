package melonslise.locks.common.item;

import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.init.LocksCapabilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.OptionalInt;

public class KeyRingItem extends Item
{
	public final int rows;

	public KeyRingItem(int rows, Properties props)
	{
		super(props.stacksTo(1));
		this.rows = rows;
	}

	public static boolean containsId(ItemStack stack, int id)
	{
		IItemHandler inv = LocksCapabilities.getItemHandler(stack);
		if(inv == null)
			return false;
		for(int a = 0; a < inv.getSlots(); ++a)
		{
			OptionalInt slotId = LockingItem.readId(inv.getStackInSlot(a));
			if(slotId.isPresent() && slotId.getAsInt() == id)
				return true;
		}
		return false;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(!player.level().isClientSide)
			((ServerPlayer) player).openMenu(new KeyRingContainer.Provider(stack), new KeyRingContainer.Writer(hand));
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

}
