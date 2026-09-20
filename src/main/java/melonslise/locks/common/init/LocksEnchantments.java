package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.ForgeConfigSpec;

public final class LocksEnchantments
{
	public static final ResourceKey<Enchantment>
		SHOCKING = key("shocking"),
		STURDY = key("sturdy"),
		COMPLEXITY = key("complexity"),
		SILENT = key("silent"),
		AUTO_PICK = key("auto_pick"),
		REINFORCED = key("reinforced"),
		AWARENESS = key("awareness"),
		FINESSE = key("finesse"),
		ATTUNEMENT = key("attunement"),
		GROUNDED = key("grounded"),
		QUIET_HAND = key("quiet_hand"),
		LAST_CATCH = key("last_catch");

	private LocksEnchantments() {}

	private static ResourceKey<Enchantment> key(String name)
	{
		return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Locks.ID, name));
	}

	public static int level(ResourceKey<Enchantment> enchantment, ItemStack stack)
	{
		if(!isEnabled(enchantment))
			return 0;
		for(Holder<Enchantment> holder : stack.getEnchantments().keySet())
			if(holder.is(enchantment))
				return stack.getEnchantments().getLevel(holder);
		return 0;
	}

	private static boolean isEnabled(ResourceKey<Enchantment> enchantment)
	{
		ForgeConfigSpec.BooleanValue config = enableConfig(enchantment);
		return config == null || config.get();
	}

	private static ForgeConfigSpec.BooleanValue enableConfig(ResourceKey<Enchantment> enchantment)
	{
		if(enchantment == SHOCKING)
			return LocksServerConfig.ENABLE_SHOCKING;
		if(enchantment == STURDY)
			return LocksServerConfig.ENABLE_STURDY;
		if(enchantment == COMPLEXITY)
			return LocksServerConfig.ENABLE_COMPLEXITY;
		if(enchantment == SILENT)
			return LocksServerConfig.ENABLE_SILENT;
		if(enchantment == AUTO_PICK)
			return LocksServerConfig.ENABLE_AUTO_PICK;
		if(enchantment == REINFORCED)
			return LocksServerConfig.ENABLE_REINFORCED;
		if(enchantment == AWARENESS)
			return LocksServerConfig.ENABLE_AWARENESS;
		if(enchantment == FINESSE)
			return LocksServerConfig.ENABLE_FINESSE;
		if(enchantment == ATTUNEMENT)
			return LocksServerConfig.ENABLE_ATTUNEMENT;
		if(enchantment == GROUNDED)
			return LocksServerConfig.ENABLE_GROUNDED;
		if(enchantment == QUIET_HAND)
			return LocksServerConfig.ENABLE_QUIET_HAND;
		if(enchantment == LAST_CATCH)
			return LocksServerConfig.ENABLE_LAST_CATCH;
		return null;
	}

	public static void register() {}
}
