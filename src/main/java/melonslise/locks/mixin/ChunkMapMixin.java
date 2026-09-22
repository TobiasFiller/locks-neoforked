package melonslise.locks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksNetwork;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.util.Lockable;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkMap.class)
public class ChunkMapMixin
{
	@Inject(at = @At("TAIL"), method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V", remap = false)
	private static void markChunkPendingToSend(ServerPlayer player, LevelChunk ch, CallbackInfo ci)
	{
		var st = LocksCapabilities.getStorage(ch);
		if(st != null)
		{
			// Snapshot the lockables so iteration can't throw ConcurrentModificationException if the
			// handler/storage is mutated on another path while this player is being synced.
			for(Lockable lkb : st.snapshot())
				LocksNetwork.sendToPlayer(player, new AddLockableToChunkPacket(lkb, ch));
		}
	}
}
