package melonslise.locks.common.item;

import melonslise.locks.common.capability.CapabilityProvider;
import melonslise.locks.common.capability.KeyRingInventory;
import melonslise.locks.common.container.KeyRingContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeServerPlayer;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.OptionalInt;

public class KeyRingItem extends Item
{
	public final int rows;

	public KeyRingItem(int rows, Properties props)
	{
		super(props.stacksTo(1));
		this.rows = rows;
	}

	@Override
	public ICapabilityProvider getCapabilityProvider(ItemStack stack)
	{
		return new CapabilityProvider(ForgeCapabilities.ITEM_HANDLER, new KeyRingInventory(stack, this.rows, 9));
	}

	public static boolean containsId(ItemStack stack, int id)
	{
		IItemHandler inv = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
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
			((IForgeServerPlayer) (ServerPlayer) player).openMenu(new KeyRingContainer.Provider(stack), new KeyRingContainer.Writer(hand));
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
	}

}
