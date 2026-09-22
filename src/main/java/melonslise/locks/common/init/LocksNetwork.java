package melonslise.locks.common.init;

import java.util.Objects;
import java.util.stream.Stream;

import melonslise.locks.common.network.toclient.AddLockablePacket;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.network.toclient.RemoveLockablePacket;
import melonslise.locks.common.network.toclient.TryPinResultPacket;
import melonslise.locks.common.network.toclient.UpdateLockablePacket;
import melonslise.locks.common.network.toserver.TryPinPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LocksNetwork
{
	private static final String PROTOCOL_VERSION = "4";

	private LocksNetwork() {}

	public static void register(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
		registrar.playToClient(AddLockablePacket.TYPE, AddLockablePacket.STREAM_CODEC, AddLockablePacket::handle);
		registrar.playToClient(AddLockableToChunkPacket.TYPE, AddLockableToChunkPacket.STREAM_CODEC, AddLockableToChunkPacket::handle);
		registrar.playToClient(RemoveLockablePacket.TYPE, RemoveLockablePacket.STREAM_CODEC, RemoveLockablePacket::handle);
		registrar.playToClient(UpdateLockablePacket.TYPE, UpdateLockablePacket.STREAM_CODEC, UpdateLockablePacket::handle);
		registrar.playToServer(TryPinPacket.TYPE, TryPinPacket.STREAM_CODEC, TryPinPacket::handle);
		registrar.playToClient(TryPinResultPacket.TYPE, TryPinResultPacket.STREAM_CODEC, TryPinResultPacket::handle);
	}

	public static void sendToServer(CustomPacketPayload payload)
	{
		PacketDistributor.sendToServer(payload);
	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
	{
		PacketDistributor.sendToPlayer(player, payload);
	}

	public static void sendToTrackingChunk(LevelChunk chunk, CustomPacketPayload payload)
	{
		PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) chunk.getLevel(), chunk.getPos(), payload);
	}

	public static void sendToTrackingArea(Stream<LevelChunk> chunks, CustomPacketPayload payload)
	{
		chunks
			.filter(Objects::nonNull)
			.flatMap(chunk -> ((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false).stream())
			.distinct()
			.forEach(player -> sendToPlayer(player, payload));
	}
}
