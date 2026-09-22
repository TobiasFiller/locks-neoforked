package melonslise.locks.common.network.toclient;

import melonslise.locks.Locks;
import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.init.LocksCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RemoveLockablePacket implements CustomPacketPayload
{
	public static final Type<RemoveLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "remove_lockable"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RemoveLockablePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), RemoveLockablePacket::decode);

	private final int id;

	public RemoveLockablePacket(int id)
	{
		this.id = id;
	}

	public static RemoveLockablePacket decode(FriendlyByteBuf buf)
	{
		return new RemoveLockablePacket(buf.readInt());
	}

	public static void encode(RemoveLockablePacket pkt, FriendlyByteBuf buf)
	{
		buf.writeInt(pkt.id);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(RemoveLockablePacket pkt, IPayloadContext ctx)
	{
		// Use runnable, lambda causes issues with class loading
		ctx.enqueueWork(new Runnable()
		{
			@Override
			public void run()
			{
				if(Minecraft.getInstance().level == null)
					return;
				ILockableHandler handler = LocksCapabilities.getHandler(Minecraft.getInstance().level);
				if(handler != null) handler.remove(pkt.id);
			}
		});
	}
}
