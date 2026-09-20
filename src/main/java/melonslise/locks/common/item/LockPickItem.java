package melonslise.locks.common.item;

import java.util.List;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.init.LocksTagHelper;
import melonslise.locks.common.init.LockTypeRegistry;
import melonslise.locks.common.util.ItemStackNbt;
import melonslise.locks.common.util.Lockable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class LockPickItem extends Item
{
	public static final int MAX_STACK_SIZE = 64;
	public static final Component TOO_COMPLEX_MESSAGE = Component.translatable(Locks.ID + ".status.too_complex");
	public static final ResourceLocation NETHERITE_LOCK_PICK_ID = ResourceLocation.fromNamespaceAndPath(Locks.ID, "netherite_lock_pick");
	public static final String KEY_DAMAGE = "LockPickDamage";

	public LockPickItem(Properties props)
	{
		super(props);
	}

	public static final String KEY_STRENGTH = "Strength";

	// Read-only, unlike the 1.7.2 getOrSetStrength it replaces. That one wrote NBT on every read, so a
	// pick that had merely been looked at stopped stacking with a fresh one, and every caller had to
	// guarantee it never saw an empty stack (whose tag is a shared singleton). The registry is the source
	// of truth; a legacy Strength tag is still honoured so picks saved before 1.7.3 keep their value.
	public static float getStrength(ItemStack stack)
	{
		if(stack.isEmpty())
			return 0f;
		CompoundTag nbt = ItemStackNbt.getCustomData(stack);
		if(nbt.contains(KEY_STRENGTH))
			return nbt.getFloat(KEY_STRENGTH);
		return LockTypeRegistry.getLockPickStats(stack.getItem()).strength();
	}

	public static boolean canPick(ItemStack stack, int cmp)
	{
		float strength = getStrength(stack);
		if(LocksServerConfig.ENABLE_ATTUNEMENT.get())
			strength += LocksEnchantments.level(LocksEnchantments.ATTUNEMENT, stack)
				* (float) (double) LocksServerConfig.ATTUNEMENT_STRENGTH_PER_LEVEL.get();
		return strength > cmp * 0.25f;
	}

	public static boolean canPick(ItemStack stack, Lockable lkb)
	{
		return canPick(stack, LocksServerConfig.ENABLE_COMPLEXITY.get()
			? LocksEnchantments.level(LocksEnchantments.COMPLEXITY, lkb.stack) : 0);
	}

	// Lockpicks are stackable, so they cannot use vanilla item durability in 1.21. The data-driven
	// durability value is instead applied through custom data while picking locks.
	public static boolean usesDurability(ItemStack stack)
	{
		return getMaxPickDamage(stack) > LockTypeRegistry.UNBREAKABLE_DURABILITY;
	}

	public static int getPickDamage(ItemStack stack)
	{
		if(stack.isEmpty())
			return 0;
		CompoundTag nbt = ItemStackNbt.getCustomData(stack);
		return Math.max(0, nbt.getInt(KEY_DAMAGE));
	}

	public static int getMaxPickDamage(ItemStack stack)
	{
		if(stack.isEmpty())
			return 0;
		return LockTypeRegistry.getLockPickStats(stack.getItem()).durability();
	}

	/**
	 * Damages exactly one pick and reports whether that pick broke.
	 *
	 * Damage is stored on an ItemStack, not on an individual item within it. Keeping a damaged multi-item
	 * stack would therefore make every pick appear damaged and would duplicate that damage if the player
	 * split the stack. Work on a singleton copy instead. If it survives with new damage, it becomes the held
	 * pick and the untouched remainder goes back into the inventory. If it breaks, only one item is removed
	 * from the original stack and the next pristine pick remains ready in the hand.
	 */
	public static boolean damagePick(ItemStack stack, Player player, InteractionHand hand, int amount)
	{
		if(amount <= 0 || !usesDurability(stack))
			return false;

		if(stack.getCount() <= 1)
			return damageSinglePick(stack, player, hand, amount);

		ItemStack wornPick = stack.copy();
		wornPick.setCount(1);
		int damageBefore = getPickDamage(wornPick);
		boolean broke = damageSinglePick(wornPick, player, hand, amount);

		if(broke)
		{
			stack.shrink(1);
			return true;
		}

		// Creative mode and Unbreaking can prevent all requested damage. Keep the stack together unless
		// this particular pick actually acquired wear.
		if(getPickDamage(wornPick) == damageBefore)
			return false;

		ItemStack pristineRemainder = stack.copy();
		pristineRemainder.shrink(1);
		clearPickDamage(pristineRemainder);
		player.setItemInHand(hand, wornPick);
		// This merges with another pristine stack when possible and drops the remainder beside the player
		// when every inventory slot is full, so splitting can never silently delete picks.
		player.getInventory().placeItemBackInInventory(pristineRemainder);
		return false;
	}

	private static boolean damageSinglePick(ItemStack stack, Player player, InteractionHand hand, int amount)
	{
		if(!(player.level() instanceof ServerLevel level))
			return false;
		if(player instanceof ServerPlayer serverPlayer && serverPlayer.hasInfiniteMaterials())
			return false;
		amount = EnchantmentHelper.processDurabilityChange(level, stack, amount);
		if(amount <= 0)
			return false;

		int damage = getPickDamage(stack) + amount;
		int maxDamage = getMaxPickDamage(stack);
		if(damage >= maxDamage)
		{
			EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
			Item item = stack.getItem();
			stack.shrink(1);
			ForgeEventFactory.onPlayerDestroyItem(player, stack, slot);
			player.onEquippedItemBroken(item, slot);
			return true;
		}
		setPickDamage(stack, damage);
		return false;
	}

	private static void setPickDamage(ItemStack stack, int damage)
	{
		ItemStackNbt.updateCustomData(stack, nbt -> nbt.putInt(KEY_DAMAGE, Math.max(0, damage)));
	}

	private static void clearPickDamage(ItemStack stack)
	{
		ItemStackNbt.updateCustomData(stack, nbt -> nbt.remove(KEY_DAMAGE));
	}

	/**
	 * Keeps pristine lockpick stacks at 64. Worn picks carry custom data, so vanilla stack matching keeps
	 * them separate from pristine stacks.
	 */
	public int getMaxStackSize(ItemStack stack)
	{
		return MAX_STACK_SIZE;
	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return getPickDamage(stack) > 0 && getMaxPickDamage(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		return Mth.clamp(Math.round(13.0F - (float) getPickDamage(stack) * 13.0F / (float) getMaxPickDamage(stack)), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		int maxDamage = getMaxPickDamage(stack);
		float left = maxDamage <= 0 ? 1.0F : Math.max(0.0F, ((float) maxDamage - (float) getPickDamage(stack)) / (float) maxDamage);
		return Mth.hsvToRgb(left / 3.0F, 1.0F, 1.0F);
	}

	public static boolean isNetheriteLockPick(ItemStack stack)
	{
		return isNetheriteLockPick(stack.getItem());
	}

	private static boolean isNetheriteLockPick(Item item)
	{
		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
		return NETHERITE_LOCK_PICK_ID.equals(itemId);
	}

	@Override
	public boolean isEnchantable(ItemStack stack)
	{
		return stack.getCount() == 1 && LocksTagHelper.isLockPick(stack);
	}

	// The anvil never consults isEnchantable, and its result is a copy of the whole left input, count
	// included: one enchanted book laid against 64 pristine picks would enchant all 64 for a flat 40
	// levels. Forge calls this hook at the end of AnvilMenu#createResult, so refusing a stack here blanks
	// the result slot. A single pick is unaffected.
	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book)
	{
		return stack.getCount() == 1;
	}

	@Override
	public int getEnchantmentValue()
	{
		int value = LockTypeRegistry.getLockPickStats(this).enchantmentValue();
		return value > 0 ? value : super.getEnchantmentValue();
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
	{
		return isNetheriteLockPick(toRepair) && repair.is(Items.NETHERITE_INGOT) || super.isValidRepairItem(toRepair, repair);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag)
	{
		super.appendHoverText(stack, context, lines, flag);
		lines.add(Component.translatable(Locks.ID + ".tooltip.strength", String.format(java.util.Locale.ROOT, "%.2f", getStrength(stack))).withStyle(ChatFormatting.DARK_GREEN));
	}
}
