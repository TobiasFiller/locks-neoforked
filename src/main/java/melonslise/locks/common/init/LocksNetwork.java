package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.network.toclient.AddLockablePacket;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.network.toclient.RemoveLockablePacket;
import melonslise.locks.common.network.toclient.TryPinResultPacket;
import melonslise.locks.common.network.toclient.UpdateLockablePacket;
import melonslise.locks.common.network.toserver.TryPinPacket;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class LocksNetwork
{
	// Bumped for 1.7.5: the pin packets gained the picking-session fields (container id, request sequence, and
	// the authoritative progress echoed back). Both acceptors are exact-match, so a mixed 1.7.4/1.7.5 connection
	// is refused at the channel handshake instead of decoding bad state.
	private static final String PROTOCOL_VERSION = "4";
	public static final SimpleChannel MAIN = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(Locks.ID, "main"))
		.networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
		.acceptedVersions((status, version) -> Integer.toString(version).equals(PROTOCOL_VERSION))
		.simpleChannel();

	private LocksNetwork() {}

	public static void register()
	{
		MAIN.messageBuilder(AddLockablePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(AddLockablePacket::encode).decoder(AddLockablePacket::decode).consumerMainThread(AddLockablePacket::handle).add();
		MAIN.messageBuilder(AddLockableToChunkPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(AddLockableToChunkPacket::encode).decoder(AddLockableToChunkPacket::decode).consumerMainThread(AddLockableToChunkPacket::handle).add();
		MAIN.messageBuilder(RemoveLockablePacket.class, 2, NetworkDirection.PLAY_TO_CLIENT).encoder(RemoveLockablePacket::encode).decoder(RemoveLockablePacket::decode).consumerMainThread(RemoveLockablePacket::handle).add();
		MAIN.messageBuilder(UpdateLockablePacket.class, 3, NetworkDirection.PLAY_TO_CLIENT).encoder(UpdateLockablePacket::encode).decoder(UpdateLockablePacket::decode).consumerMainThread(UpdateLockablePacket::handle).add();
		MAIN.messageBuilder(TryPinPacket.class, 4, NetworkDirection.PLAY_TO_SERVER).encoder(TryPinPacket::encode).decoder(TryPinPacket::decode).consumerMainThread(TryPinPacket::handle).add();
		MAIN.messageBuilder(TryPinResultPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT).encoder(TryPinResultPacket::encode).decoder(TryPinResultPacket::decode).consumerMainThread(TryPinResultPacket::handle).add();
		MAIN.build();
	}
}
