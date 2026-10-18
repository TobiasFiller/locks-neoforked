package melonslise.locks.mixin;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import melonslise.locks.Locks;
import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.util.Cuboid6i;
import melonslise.locks.common.util.Lock;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LockableInfo;
import melonslise.locks.common.util.LocksThreadUtil;
import melonslise.locks.common.util.LocksUtil;
import melonslise.locks.common.util.Transform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin
{
	private final List<LockableInfo> lockableInfos = new ArrayList<>();

	@Inject(at = @At("HEAD"), method = "fillFromWorld", remap = false)
	private void fillFromWorld(Level world, BlockPos start, Vec3i size, boolean takeEntities, @Nullable Block toIgnore, CallbackInfo ci)
	{
		if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1)
		{
			this.lockableInfos.clear();
			ILockableHandler handler = LocksCapabilities.getHandler(world);
			if(handler == null)
				return;
			Cuboid6i bb = new Cuboid6i(start, start.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
			// fillFromWorld can run on a C2ME worker thread. Use the handler's synchronized snapshot rather than
			// copying getLoaded() directly: building a copy of the fastutil map while the main thread rehashes it
			// reads a torn backing array -> ArrayIndexOutOfBoundsException ("Index -1"). snapshotLoaded() takes
			// the handler's monitor so the read is consistent with concurrent main-thread mutations.
			for(Lockable lkb : handler.snapshotLoaded())
			{
				if(!lkb.bb.intersects(bb))
					continue;
				Cuboid6i newBB = bb.intersection(lkb.bb).offset(-start.getX(), -start.getY(), -start.getZ());
				this.lockableInfos.add(new LockableInfo(newBB, lkb.lock.copy(), lkb.tr, lkb.stack.copy(), lkb.id));
			}
		}
	}

	// Second return
	@Inject(at = @At(value = "RETURN", ordinal = 1), method = "placeInWorld", remap = false)
	private void placeInWorld(ServerLevelAccessor world, BlockPos start, BlockPos size, StructurePlaceSettings settings, RandomSource rng, int i, CallbackInfoReturnable<Boolean> cir)
	{
		Level level;
		try
		{
			level = world.getLevel();
		}
		catch(Exception e)
		{
			Locks.LOGGER.warn(world + "#getLevel threw an error! Skipping lockable placement for this template ");
			return;
		}
		ILockableHandler handler = LocksCapabilities.getHandler(level);
		if(handler == null)
			return;
		// Build the lockables now (so RNG consumption and transforms keep their exact timing), then defer
		// only the handler mutation + packet sync. Without C2ME this runs inline on the server thread and
		// behavior is unchanged; under C2ME it is deferred to the main thread to keep the handler map safe.
		List<Lockable> built = new ArrayList<>();
		// Randomized template locks obey the same biome gate as worldgen, so randomizing a template into the
		// Overworld cannot mint a netherite lock there.
		boolean gatedAllowed = LocksUtil.biomeGatedLocksAllowed(level, start);
		for(LockableInfo lkb : this.lockableInfos)
		{
			BlockPos pos1 = LocksUtil.transform(lkb.bb.x1, lkb.bb.y1, lkb.bb.z1, settings);
			BlockPos pos2 = LocksUtil.transform(lkb.bb.x2, lkb.bb.y2, lkb.bb.z2, settings);
			Cuboid6i bb = new Cuboid6i(pos1.getX() + start.getX(), pos1.getY() + start.getY(), pos1.getZ() + start.getZ(), pos2.getX() + start.getX(), pos2.getY() + start.getY(), pos2.getZ() + start.getZ());
			ItemStack stack = LocksConfig.RANDOMIZE_LOADED_LOCKS.get() ? LocksConfig.getRandomLock(rng, gatedAllowed) : lkb.stack;
			// Every generated lock can be gated out here (e.g. a netherite-only template dropped in the Overworld,
			// or an empty draw). Keep the template's own lock in that case rather than minting a lockless Lockable.
			boolean randomized = LocksConfig.RANDOMIZE_LOADED_LOCKS.get() && !stack.isEmpty();
			if(!randomized)
				stack = lkb.stack;
			// Never hand out the template's own Lock: reusing it would give every placement of this structure —
			// and the world lock the template was captured from — one shared object and one shared combination.
			// With randomization off the credential is preserved (existing keys keep working), but the pin order
			// is drawn fresh for each placement.
			Lock lock = randomized
				? Lock.from(stack)
				: Lock.generate(lkb.lock.id, lkb.lock.getLength(), lkb.lock.isLocked(), ThreadLocalRandom.current());
			Transform tr = Transform.fromDirectionAndFace(settings.getRotation().rotate(settings.getMirror().getRotation(lkb.tr.dir).rotate(lkb.tr.dir)), lkb.tr.face, Direction.NORTH);
			built.add(new Lockable(bb, lock, tr, stack, level));
		}
		if(built.isEmpty())
			return;
		ILockableHandler handlerFinal = handler;
		LocksThreadUtil.runOnServerThread(level.getServer(), () ->
		{
			for(Lockable lkb : built)
				handlerFinal.add(lkb);
		});
	}

	private static final String KEY_LOCKABLES = "Lockables";

	@Inject(at = @At("HEAD"), method = "save", remap = false)
	private void save(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir)
	{
		ListTag list = new ListTag();
		for(LockableInfo lkb : this.lockableInfos)
			list.add(LockableInfo.toNbt(lkb));
		nbt.put(KEY_LOCKABLES, list);
	}

	@Inject(at = @At("HEAD"), method = "load", remap = false)
	private void read(HolderGetter<Block> blockGetter, CompoundTag nbt, CallbackInfo ci)
	{
		this.lockableInfos.clear();
		ListTag list = nbt.getList(KEY_LOCKABLES, Tag.TAG_COMPOUND);
		for(int a = 0, b = list.size(); a < b; ++a)
			this.lockableInfos.add(LockableInfo.fromNbt(list.getCompound(a)));
	}
}
