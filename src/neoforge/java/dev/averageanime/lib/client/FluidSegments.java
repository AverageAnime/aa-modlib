package dev.averageanime.lib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class FluidSegments {

    private FluidSegments() {}

    /**
     * Draws the fluids the way Create's basin does: every fluid rises to the same surface, and the
     * surface is split side by side along X, each fluid taking a width in proportion to its amount, in
     * list order from {@code minX}. The height follows the total, eased as the basin eases it
     * ({@code 1 - (1 - level)^2}) so a small amount still reads as liquid rather than a film.
     *
     * @param capacityMb the total that reaches {@code maxY}; more is clamped there
     * @return the surface height drawn, or {@code floorY} when there was nothing to draw
     */
    public static float render(PoseStack poseStack, MultiBufferSource buffers, int light,
                              List<FluidStack> fluids, int capacityMb,
                              float minX, float minZ, float maxX, float maxZ, float floorY, float maxY) {
        long total = 0;
        for (FluidStack fluid : fluids) total += fluid.getAmount();
        if (total <= 0 || capacityMb <= 0) return floorY;

        float level = Math.min(1f, (float) total / capacityMb);
        level = 1 - (1 - level) * (1 - level);
        float surfaceY = floorY + (maxY - floorY) * level;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = poseStack.last();

        float x0 = minX;
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            float x1 = x0 + (maxX - minX) * fluid.getAmount() / total;
            box(consumer, pose, light, fluid, x0, floorY, minZ, x1, surfaceY, maxZ);
            x0 = x1;
        }
        return surfaceY;
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose, int light, FluidStack stack,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(stack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int tint = ext.getTintColor(stack);
        float a = ((tint >>> 24) & 0xFF) / 255f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        // A tint that leaves alpha at zero means "untinted", not "invisible".
        if (a == 0f) a = 1f;
        float[] c = {r, g, b, a};

        // Top.
        quad(consumer, pose, light, c, sprite, 0, 1, 0,
                x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                x0, z0, x0, z1, x1, z1, x1, z0);
        // North, south, west, east; UVs come from each face's own position, so the texture runs on
        // unbroken across neighbouring segments instead of restarting at each one's edge.
        quad(consumer, pose, light, c, sprite, 0, 0, -1,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                x1, 1 - y0, x0, 1 - y0, x0, 1 - y1, x1, 1 - y1);
        quad(consumer, pose, light, c, sprite, 0, 0, 1,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                x0, 1 - y0, x1, 1 - y0, x1, 1 - y1, x0, 1 - y1);
        quad(consumer, pose, light, c, sprite, -1, 0, 0,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
                z0, 1 - y0, z1, 1 - y0, z1, 1 - y1, z0, 1 - y1);
        quad(consumer, pose, light, c, sprite, 1, 0, 0,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
                z1, 1 - y0, z0, 1 - y0, z0, 1 - y1, z1, 1 - y1);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int light, float[] c,
                             TextureAtlasSprite sprite, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float au, float av, float bu, float bv, float cu, float cv, float du, float dv) {
        vertex(consumer, pose, light, c, sprite, nx, ny, nz, ax, ay, az, au, av);
        vertex(consumer, pose, light, c, sprite, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(consumer, pose, light, c, sprite, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(consumer, pose, light, c, sprite, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, float[] c,
                               TextureAtlasSprite sprite, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z)
                .setColor(c[0], c[1], c[2], c[3])
                .setUv(sprite.getU(u), sprite.getV(v))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
