package melonslise.locks.common.util;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Non-blocking world access for worldgen and mixin code.
 *
 * <p>Chunk generation in 1.21.1 runs on worker threads even without C2ME. Calling
 * {@code Level#getBiome} or {@code Level#getChunk} from those threads resolves through
 * {@code ServerChunkCache#getChunk} and parks on a {@code CompletableFuture}. If the server thread is
 * waiting for that generation task, the result is a permanent deadlock and the watchdog kills the server.
 *
 * <p>Every method here returns either an already-available chunk/biome or {@code null}. Never add a
 * method to this class that can force-load a chunk — that would reintroduce the deadlock this class exists
 * to prevent. Callers must treat {@code null} as "not available" and fail safe.
 */
public final class LocksWorldAccess
{
	private LocksWorldAccess() {}

	/**
	 * The chunk containing {@code pos}, but only if it can be read without loading.
	 * {@code WorldGenRegion#getChunk} is a cache-only lookup: it throws instead of blocking when the chunk
	 * is outside the current generation dependencies. Outside worldgen we use the non-blocking
	 * {@code getChunkNow} peek.
	 */
	@Nullable
	public static ChunkAccess chunkIfPresent(LevelAccessor access, BlockPos pos)
	{
		return chunkIfPresent(access,
			SectionPos.blockToSectionCoord(pos.getX()),
			SectionPos.blockToSectionCoord(pos.getZ()));
	}

	@Nullable
	public static ChunkAccess chunkIfPresent(LevelAccessor access, int cx, int cz)
	{
		if(access instanceof WorldGenRegion region)
		{
			try
			{
				return region.getChunk(cx, cz);
			}
			catch(IllegalStateException e)
			{
				// Not in the generation dependency cache. Deliberately no fall back to a blocking fetch.
				return null;
			}
		}
		return access.getChunkSource().getChunkNow(cx, cz);
	}

	/** The biome at {@code pos} from a chunk we already hold, or {@code null} if unavailable. */
	@Nullable
	public static Holder<Biome> biomeAt(@Nullable ChunkAccess chunk, BlockPos pos)
	{
		if(chunk == null)
			return null;
		return chunk.getNoiseBiome(
			QuartPos.fromBlock(pos.getX()),
			QuartPos.fromBlock(pos.getY()),
			QuartPos.fromBlock(pos.getZ()));
	}

	@Nullable
	public static Holder<Biome> biomeAt(LevelAccessor access, BlockPos pos)
	{
		return biomeAt(chunkIfPresent(access, pos), pos);
	}
}
