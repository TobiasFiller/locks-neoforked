package melonslise.locks.common.network.toclient;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.util.Lockable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class UpdateLockablePacket
{
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

	public static void handle(UpdateLockablePacket pkt, CustomPayloadEvent.Context ctx)
	{
		// Use runnable, lambda causes issues with class loading
		ctx.enqueueWork(new Runnable()
		{
			@Override
			public void run()
			{
				if(Minecraft.getInstance().level == null)
					return;
				Minecraft.getInstance().level.getCapability(LocksCapabilities.LOCKABLE_HANDLER).ifPresent(handler ->
				{
					Int2ObjectMap<Lockable> lkbs = handler.getLoaded();
					Lockable lkb = lkbs.get(pkt.id);
					if(lkb == lkbs.defaultReturnValue())
						return;
					lkb.lock.setLocked(pkt.locked);
				});
			}
		});
		ctx.setPacketHandled(true);
	}
}
