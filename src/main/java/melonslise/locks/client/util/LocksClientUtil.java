package melonslise.locks.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class LocksClientUtil
{
	// Cached quaternions to avoid per-frame allocation in worldToScreen view bobbing
	private static final Quaternionf BOB_ROT_X = new Quaternionf();
	private static final Quaternionf BOB_ROT_Z = new Quaternionf();
	// Cached objects to avoid per-frame allocation in worldToScreen
	private static final Vector3f SCREEN_POS = new Vector3f();
	private static final Quaternionf SCREEN_ROT = new Quaternionf();

	private LocksClientUtil() {}

	public static Camera getCamera()
	{
		return Minecraft.getInstance().gameRenderer.getMainCamera();
	}

	public static Frustum getFrustum(PoseStack mtx, Matrix4f proj)
	{
		Frustum ch = Minecraft.getInstance().levelRenderer.capturedFrustum;
		if(ch != null)
			return ch;
		ch = new Frustum(mtx.last().pose(), proj);
		Vec3 pos = getCamera().getPosition();
		ch.prepare(pos.x, pos.y, pos.z);
		return ch;
	}

	public static double distanceToLineSq(Vec3 p, Vec3 l1, Vec3 l2)
	{
		Vec3 l = l2.subtract(l1);
		return l.cross(p.subtract(l1)).lengthSqr() / l.lengthSqr();
	}

	// https://forums.minecraftforge.net/topic/88562-116solved-3d-to-2d-conversion/
	// And big thanks to JTK222 Lukas!!!
	public static Vector3f worldToScreen(Vec3 pos, float partialTicks)
	{
		Minecraft mc = Minecraft.getInstance();
		Camera cam = getCamera();
		Vec3 o = cam.getPosition();

		Vector3f pos1 = SCREEN_POS.set((float) (o.x - pos.x), (float) (o.y - pos.y), (float) (o.z - pos.z));
		Quaternionf rot = SCREEN_ROT.set(cam.rotation());
		rot.conjugate();
		rot.transform(pos1);

		// Account for view bobbing
		if (mc.options.bobView().get() && mc.getCameraEntity() instanceof Player)
		{
			Player player = (Player) mc.getCameraEntity();
			float f = player.walkDist - player.walkDistO;
			float f1 = -(player.walkDist + f * partialTicks);
			float f2 = Mth.lerp(partialTicks, player.oBob, player.bob);

			BOB_ROT_X.rotationX((float) Math.toRadians(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2f) * f2) * 5f)).conjugate();
			BOB_ROT_Z.rotationZ((float) Math.toRadians(Mth.sin(f1 * (float) Math.PI) * f2 * 3f)).conjugate();
			BOB_ROT_X.transform(pos1);
			BOB_ROT_Z.transform(pos1);
			pos1.add(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5f, Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0f);
		}

		com.mojang.blaze3d.platform.Window w = mc.getWindow();
		float sc = w.getGuiScaledHeight() / 2f / pos1.z / (float) Math.tan(Math.toRadians(mc.gameRenderer.getFov(cam, partialTicks, true) / 2f));
		pos1.mul(-sc, -sc, 1f);
		pos1.add(w.getGuiScaledWidth() / 2f, w.getGuiScaledHeight() / 2f, 0f);

		return pos1;
	}

	public static void blitTexture(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, int u, int v, int width, int height, int texWidth, int texHeight, float alpha)
	{
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
		guiGraphics.blit(texture, (int) x, (int) y, u, v, width, height, texWidth, texHeight);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		RenderSystem.disableBlend();
	}

	public static float lerp(float start, float end, float progress)
	{
		return start + (end - start) * progress;
	}

	public static double lerp(double start, double end, double progress)
	{
		return start + (end - start) * progress;
	}

	/*
	 * Make 2d bezier??
	 * Implement 2d cubic bezier function
	 * https://stackoverflow.com/questions/11696736/recreating-css3-transitions-cubic-bezier-curve
	 * https://math.stackexchange.com/questions/26846/is-there-an-explicit-form-for-cubic-b%C3%A9zier-curves
	 * https://www.gamedev.net/forums/topic/572263-bezier-curve-for-animation/
	 * https://math.stackexchange.com/questions/2571471/understanding-of-cubic-b%C3%A9zier-curves-in-one-dimension
	 */
	public static float cubicBezier1d(float anchor1, float anchor2, float progress)
	{
		float omp = 1f - progress;
		return 3f * omp * omp * progress * anchor1 + 3f * omp * progress * progress * anchor2 + progress * progress * progress;
	}
}
