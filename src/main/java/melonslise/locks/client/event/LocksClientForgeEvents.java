package melonslise.locks.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import melonslise.locks.Locks;
import melonslise.locks.client.init.LocksRenderTypes;
import melonslise.locks.client.util.LocksClientUtil;
import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.capability.ISelection;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LocksTagHelper;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.SwingProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = Locks.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class LocksClientForgeEvents
{
	public static Lockable tooltipLockable;

	// Cached quaternions to avoid per-frame allocation during lock rendering
	private static final Quaternionf LOCK_ROT_Y = new Quaternionf();
	private static final Quaternionf LOCK_ROT_X = new Quaternionf();
	private static final Quaternionf LOCK_ROT_Z = new Quaternionf();

	private LocksClientForgeEvents() {}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre e)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null || mc.isPaused())
			return;
		var handler = LocksCapabilities.getHandler(mc.level);
		if(handler != null)
			handler.getLoaded().values().forEach(lkb -> lkb.tick());
	}

	@SubscribeEvent
	public static void onRenderWorld(RenderLevelStageEvent e)
	{
		if(e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
			return;
		Minecraft mc = Minecraft.getInstance();
		PoseStack mtx = new PoseStack();
		MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

		renderLocks(mtx, buf, e.getFrustum(), e.getPartialTick().getGameTimeDeltaPartialTick(false));
		renderSelection(mtx, buf);
	}

	public static boolean holdingPick(Player player)
	{
		for(InteractionHand hand : InteractionHand.values())
			if(LocksTagHelper.isLockPick(player.getItemInHand(hand)))
				return true;
		return false;
	}

	public static void renderLocks(PoseStack mtx, MultiBufferSource.BufferSource buf, Frustum ch, float pt)
	{
		Minecraft mc = Minecraft.getInstance();
		Vec3 o = LocksClientUtil.getCamera().getPosition();
		BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

		double dMin = 0d;
		Vec3 look = o.add(mc.player.getViewVector(pt));

		ILockableHandler handler = LocksCapabilities.getHandler(mc.level);
		if(handler == null)
			return;
		for(Lockable lkb : handler.getLoaded().values())
		{
			Lockable.State state = lkb.getLockState(mc.level);
			if(state == null || !state.inRange(o) || !state.inView(ch))
				continue;

			double d = o.subtract(state.pos).lengthSqr();
			if(d <= 25d)
			{
				double d1 = LocksClientUtil.distanceToLineSq(state.pos, o, look);
				if(d1 <= 4d && (dMin == 0d || d1 < dMin))
				{
					tooltipLockable = lkb;
					dMin = d1;
				}
			}

			mtx.pushPose();
			// RenderLevelStageEvent in 1.21 no longer exposes the old world PoseStack. Item rendering still
			// expects camera-relative model coordinates here; using the event Matrix4f makes locks follow view-space.
			mtx.translate(state.pos.x - o.x, state.pos.y - o.y, state.pos.z - o.z);
			mtx.mulPose(LOCK_ROT_Y.rotationY((float) Math.toRadians(-state.tr.dir.toYRot() - 180f)));
			if(state.tr.face != AttachFace.WALL)
				mtx.mulPose(LOCK_ROT_X.rotationX((float) Math.toRadians(90f)));
			mtx.translate(0d, 0.1d, 0d);
			// A lock that has never swung has all three counters at 0, so the old raw division was 0/0 -> NaN ->
			// a non-finite rotation. SwingProgress reports an explicit 0 for that case and the lock hangs still.
			float swing = SwingProgress.normalized(lkb.oldSwingTicks, lkb.swingTicks, lkb.maxSwingTicks, pt);
			float swingAngle = lkb.maxSwingTicks <= 0 ? 0f
				: Mth.sin(LocksClientUtil.cubicBezier1d(1f, 1f, swing) * lkb.maxSwingTicks / 5f * 3.14f) * 10f;
			mtx.mulPose(LOCK_ROT_Z.rotationZ((float) Math.toRadians(swingAngle)));
			mtx.translate(0d, -0.1d, 0d);
			mtx.scale(0.5f, 0.5f, 0.5f);
			int light = LevelRenderer.getLightColor(mc.level, mut.set(state.pos.x, state.pos.y, state.pos.z));
			mc.getItemRenderer().renderStatic(lkb.stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, mtx, buf, mc.level, 0);
			mtx.popPose();
		}
		buf.endBatch();
	}

	public static void renderSelection(PoseStack mtx, MultiBufferSource.BufferSource buf)
	{
		Minecraft mc = Minecraft.getInstance();
		Vec3 o = LocksClientUtil.getCamera().getPosition();
		ISelection select = LocksCapabilities.getSelection(mc.player);
		if(select == null)
			return;
		BlockPos pos = select.get();
		if(pos == null)
			return;
		BlockPos pos1 = mc.hitResult instanceof BlockHitResult ? ((BlockHitResult) mc.hitResult).getBlockPos() : pos;
		boolean allow = Math.abs(pos.getX() - pos1.getX()) * Math.abs(pos.getY() - pos1.getY()) * Math.abs(pos.getZ() - pos1.getZ()) <= LocksServerConfig.MAX_LOCKABLE_VOLUME.get() && LocksServerConfig.canLock(mc.level, pos1);
		// Same as above
		LevelRenderer.renderLineBox(mtx, buf.getBuffer(LocksRenderTypes.OVERLAY_LINES), Math.min(pos.getX(), pos1.getX()) - o.x, Math.min(pos.getY(), pos1.getY()) - o.y, Math.min(pos.getZ(), pos1.getZ()) - o.z, Math.max(pos.getX(), pos1.getX()) + 1d - o.x, Math.max(pos.getY(), pos1.getY()) + 1d - o.y, Math.max(pos.getZ(), pos1.getZ()) + 1d - o.z, allow ? 0f : 1f, allow ? 1f : 0f, 0f, 0.5f);
		buf.endBatch();
	}
}
