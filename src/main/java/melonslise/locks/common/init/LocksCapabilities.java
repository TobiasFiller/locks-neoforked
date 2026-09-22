package melonslise.locks.common.init;

import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.capability.ILockableStorage;
import melonslise.locks.common.capability.ISelection;
import melonslise.locks.common.capability.KeyRingInventory;
import melonslise.locks.common.capability.LockableHandler;
import melonslise.locks.common.capability.LockableStorage;
import melonslise.locks.common.capability.Selection;
import melonslise.locks.common.item.KeyRingItem;
import melonslise.locks.Locks;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LocksCapabilities
{
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Locks.ID);

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockableHandler>> LOCKABLE_HANDLER = ATTACHMENTS.register("lockable_handler", () -> AttachmentType.serializable(holder -> new LockableHandler((Level) holder)).build());

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockableStorage>> LOCKABLE_STORAGE = ATTACHMENTS.register("lockable_storage", () -> AttachmentType.serializable(holder -> new LockableStorage((LevelChunk) holder)).build());

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Selection>> SELECTION = ATTACHMENTS.register("selection", () -> AttachmentType.builder(Selection::new).build());

	private LocksCapabilities() {}

	public static void register(IEventBus modBus)
	{
		ATTACHMENTS.register(modBus);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event)
	{
		event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) ->
		{
			if(stack.getItem() instanceof KeyRingItem keyRing)
				return new KeyRingInventory(stack, keyRing.rows, 9);
			return null;
		}, LocksItems.KEY_RING.get());
	}

	public static ILockableHandler getHandler(Level level)
	{
		return level == null ? null : level.getData(LOCKABLE_HANDLER);
	}

	public static ILockableStorage getStorage(LevelChunk chunk)
	{
		return chunk == null ? null : chunk.getData(LOCKABLE_STORAGE);
	}

	public static ISelection getSelection(Player player)
	{
		return player == null ? null : player.getData(SELECTION);
	}

	public static IItemHandler getItemHandler(ItemStack stack)
	{
		return stack == null ? null : stack.getCapability(Capabilities.ItemHandler.ITEM);
	}
}
