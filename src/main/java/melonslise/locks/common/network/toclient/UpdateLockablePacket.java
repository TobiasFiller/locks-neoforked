package melonslise.locks.common.network.toclient;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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

public class UpdateLockablePacket implements CustomPacketPayload
{
	public static final Type<UpdateLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "update_lockable"));
	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateLockablePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), UpdateLockablePacket::decode);

	private final int id;
	// Expandable
	private final boolean locked;

	public UpdateLockablePacket(int id, boolean locked)
	{
		this.id = id;
		this.locked = locked;
	}

	public UpdateLockablePacket(Lockable lkb)
	{
		this(lkb.id, lkb.lock.isLocked());
	}

	public static UpdateLockablePacket decode(FriendlyByteBuf buf)
	{
		return new UpdateLockablePacket(buf.readInt(), buf.readBoolean());
	}

	public static void encode(UpdateLockablePacket pkt, FriendlyByteBuf buf)
	{
		buf.writeInt(pkt.id);
		buf.writeBoolean(pkt.locked);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(UpdateLockablePacket pkt, IPayloadContext ctx)
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
				if(handler != null)
				{
					Int2ObjectMap<Lockable> lkbs = handler.getLoaded();
					Lockable lkb = lkbs.get(pkt.id);
					if(lkb == lkbs.defaultReturnValue())
						return;
					lkb.lock.setLocked(pkt.locked);
				}
			}
		});
	}
}
