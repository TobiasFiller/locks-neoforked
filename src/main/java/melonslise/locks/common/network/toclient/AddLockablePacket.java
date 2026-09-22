package melonslise.locks.common.network.toclient;

import melonslise.locks.Locks;
import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.util.Lockable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class AddLockablePacket implements CustomPacketPayload
{
	public static final Type<AddLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "add_lockable"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AddLockablePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), AddLockablePacket::decode);

	private final Lockable lockable;

	public AddLockablePacket(Lockable lkb)
	{
		this.lockable = lkb;
	}

	public static AddLockablePacket decode(FriendlyByteBuf buf)
	{
		return new AddLockablePacket(Lockable.fromBuf(buf));
	}

	public static void encode(AddLockablePacket pkt, FriendlyByteBuf buf)
	{
		Lockable.toBuf(buf, pkt.lockable);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(AddLockablePacket pkt, IPayloadContext ctx)
	{
		// Use runnable, lambda causes issues with class loading
		ctx.enqueueWork(new Runnable()
		{
			@Override
			public void run()
			{
				if(Minecraft.getInstance().level == null)
					return;
				// Skip validation on client — server already validated volume and intersection
				ILockableHandler handler = LocksCapabilities.getHandler(Minecraft.getInstance().level);
				if(handler != null) handler.addDirect(pkt.lockable);
			}
		});
	}
}
