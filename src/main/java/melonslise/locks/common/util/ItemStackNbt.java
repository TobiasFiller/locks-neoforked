package melonslise.locks.common.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ItemStackNbt
{
	private ItemStackNbt() {}

	public static CompoundTag getCustomData(ItemStack stack)
	{
		CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		return data.copyTag();
	}

	public static void updateCustomData(ItemStack stack, java.util.function.Consumer<CompoundTag> updater)
	{
		CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
	}

	public static boolean contains(ItemStack stack, String key, int type)
	{
		CompoundTag tag = getCustomData(stack);
		return tag.contains(key, type);
	}

	public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag)
	{
		return ItemStack.parseOptional(provider, tag);
	}

	public static Tag save(HolderLookup.Provider provider, ItemStack stack)
	{
		return stack.saveOptional(provider);
	}
}
