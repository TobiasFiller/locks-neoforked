package melonslise.locks.common.network.toserver;

import melonslise.locks.Locks;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.util.LockSecretPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class TryPinPacket implements CustomPacketPayload
{
	public static final Type<TryPinPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "try_pin"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TryPinPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), TryPinPacket::decode);

	// The menu this attempt belongs to, and a client-issued sequence that only ever increases within that menu.
	// Together they stop a result from one session (or one replayed packet) being applied to another attempt.
	private final int containerId;
	private final int sequence;
	private final byte pin;

	public TryPinPacket(int containerId, int sequence, byte pin)
	{
		this.containerId = containerId;
		this.sequence = sequence;
		this.pin = pin;
	}

	public static TryPinPacket decode(FriendlyByteBuf buf)
	{
		int containerId = buf.readVarInt();
		int sequence = buf.readVarInt();
		byte pin = buf.readByte();
		// Bounded here as well as against the real lock length below: nothing downstream may index anything
		// with a pin that could not belong to any lock.
		if(pin < 0 || pin >= LockSecretPolicy.MAX_LENGTH)
			pin = -1;
		return new TryPinPacket(containerId, sequence, pin);
	}

	public static void encode(TryPinPacket pkt, FriendlyByteBuf buf)
	{
		buf.writeVarInt(pkt.containerId);
		buf.writeVarInt(pkt.sequence);
		buf.writeByte(pkt.pin);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(TryPinPacket pkt, IPayloadContext ctx)
	{
		// Use runnable, lambda causes issues with class loading
		ctx.enqueueWork(new Runnable()
		{
			@Override
			public void run()
			{
				ServerPlayer sender = (ServerPlayer) ctx.player();
				if(sender == null)
					return;
				// instanceof, not getType(): AbstractContainerMenu#getType throws for menus built with a null
				// type, and InventoryMenu — every player's default containerMenu — is one of those. Asking a
				// player with no Locks screen open would have thrown.
				if(!(sender.containerMenu instanceof LockPickingContainer lpc))
					return;
				// The client sends nothing but the session identity and the pin. Bounds, lock state, mode, reach,
				// dimension, target identity, config and item requirements are all re-decided server-side.
				if(pkt.pin < 0 || pkt.pin >= lpc.lockable.lock.getLength())
					return;
				if(!lpc.canAttempt(sender))
					return;
				lpc.tryPin(pkt.pin, pkt.containerId, pkt.sequence);
			}
		});
	}
}
