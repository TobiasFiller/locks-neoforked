package melonslise.locks.common.compat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.util.Cuboid6i;
import melonslise.locks.common.util.Lockable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Moves Locks Reforged {@link Lockable}s together with a block that Carry On picks up and places.
 *
 * <p>The serialized lockables ride inside the block entity's custom save data, which Carry On already
 * saves into its carried {@code "tile"} NBT and restores on placement. This needs no new storage: the canonical lock
 * store remains the per-chunk {@code LockableStorage} + per-level {@code LockableHandler}, updated here
 * through {@link ILockableHandler#remove(int)} and {@link ILockableHandler#add(Lockable)} which already
 * fan out across every affected chunk, mark them unsaved, and sync clients.
 */
public final class CarriedLockTransfer
{
	public static final String KEY_LOCKABLES = "locks:CarriedLockables";
	public static final String KEY_ORIGIN = "locks:CarriedOrigin";

	private CarriedLockTransfer() {}

	/**
	 * At pickup: serialize every lockable covering {@code pickupPos} into the block entity's custom save
	 * data and remove them from the world. Called from the Carry On {@code CarryOnData.setBlock} hook,
	 * which is the guaranteed pickup-success point (after all of Carry On's own abort checks, and before
	 * the block/entity are removed), so removal here is safe.
	 */
	public static void capture(Level level, BlockPos pickupPos, BlockEntity be)
	{
		if (level.isClientSide)
			return;
		ILockableHandler handler = level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null);
		if (handler == null)
			return;

		List<Lockable> lockables = handler.snapshotLoaded().stream()
			.filter(lkb -> lkb.bb.intersects(pickupPos))
			.collect(Collectors.toList());
		if (lockables.isEmpty())
			return;

		ListTag list = new ListTag();
		for (Lockable lockable : lockables)
		{
			list.add(Lockable.toNbt(lockable));
			handler.remove(lockable.id);
		}

		CompoundTag payload = be.components().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		payload.put(KEY_LOCKABLES, list);
		payload.put(KEY_ORIGIN, writePos(pickupPos));
		setPayload(be, payload);
		be.setChanged();
	}

	/**
	 * At placement: read the captured lockables off the placed block entity, offset their bounding boxes
	 * from the original origin to the new position, and re-register them. Reuses the original ids and
	 * {@link melonslise.locks.common.util.Lock} objects, so keys/key rings/master keys keep matching.
	 */
	public static void restore(ServerLevel level, BlockPos placePos, BlockEntity be)
	{
		if (be == null)
			return;
		ILockableHandler handler = level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null);
		if (handler == null)
			return;

		CompoundTag payload = be.components().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
		ListTag list = payload.getList(KEY_LOCKABLES, Tag.TAG_COMPOUND);
		if (list.isEmpty())
			return;

		BlockPos origin = readPos(payload.getCompound(KEY_ORIGIN));
		Set<Integer> failedIndices = new LinkedHashSet<>();
		for (int i = 0; i < list.size(); ++i)
		{
			Lockable original = Lockable.fromNbt(list.getCompound(i));
			Cuboid6i movedBox = original.bb.offset(placePos.getX() - origin.getX(), placePos.getY() - origin.getY(), placePos.getZ() - origin.getZ());
			if (!handler.add(movedCopy(original, movedBox)))
				failedIndices.add(i);
		}

		ListTag retained = retain(list, failedIndices);
		if (retained.isEmpty())
		{
			payload.remove(KEY_LOCKABLES);
			payload.remove(KEY_ORIGIN);
		}
		else
			payload.put(KEY_LOCKABLES, retained);
		CustomData data = CustomData.of(payload);
		setPayload(be, payload);
		data.loadInto(be, level.registryAccess());
		be.setChanged();
	}

	private static void setPayload(BlockEntity be, CompoundTag payload)
	{
		DataComponentMap components = DataComponentMap.builder()
			.addAll(be.components())
			.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(payload))
			.build();
		be.setComponents(components);
	}

	private static CompoundTag writePos(BlockPos pos)
	{
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
		return tag;
	}

	private static BlockPos readPos(CompoundTag tag)
	{
		return new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
	}

	/**
	 * The payload to keep on the block: exactly the entries at {@code failedIndices}, in their original order.
	 * Pure, so the retention decision is testable without a world.
	 */
	public static ListTag retain(ListTag original, Set<Integer> failedIndices)
	{
		ListTag retained = new ListTag();
		for (int i = 0; i < original.size(); ++i)
			if (failedIndices.contains(i))
				retained.add(original.get(i));
		return retained;
	}

	/** A copy of {@code original} at {@code newBox}, preserving its lock, transform, item and id (a move, not a re-lock). */
	public static Lockable movedCopy(Lockable original, Cuboid6i newBox)
	{
		return new Lockable(newBox, original.lock, original.tr, original.stack.copy(), original.id);
	}
}
