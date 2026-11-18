package dev.averageanime.lib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class BasinItems {

    private BasinItems() {}

    /**
     * Lays the stacks out the way Create's basin does: spaced evenly round a ring, each tipped 65 degrees
     * toward lying flat, with one extra copy per 8 items jittered by up to 1/16. The jitter is seeded by
     * {@code pos}, so a vessel's pile holds still from frame to frame instead of shimmering.
     *
     * <p>Unlike Create's, the jitter is horizontal only. Create's also moves each copy up or down by as much
     * as the whole bob, so whether a pile rode above or below the surface depended on the vessel's position.
     *
     * @param y        the height of each item's centre, in block units; pass the fluid surface to float them
     * @param floating bob the items 1/32 either side of {@code y}; leave them still on a dry floor
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                              Level level, BlockPos pos, float partialTick, List<ItemStack> stacks,
                              float centreX, float centreZ, float radius, float y, float scale,
                              boolean floating) {
        int count = 0;
        for (ItemStack stack : stacks) if (!stack.isEmpty()) count++;
        if (count == 0) return;

        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        RandomSource random = RandomSource.create(pos.hashCode());
        float partition = 360f / count;
        // Wrapped so the float keeps sub-tick precision on an old world; the one jump a day is invisible.
        float time = (level.getGameTime() % 24000L) + partialTick;

        poseStack.pushPose();
        poseStack.translate(centreX, y, centreZ);
        int index = 0;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            float angle = partition * index++;
            poseStack.pushPose();
            // Create feeds the angle in degrees straight into sin; kept so the bob phases match. Its bob only
            // ever rises, from 0.5 to 2.5 times 1/32; centring it on y keeps the item on the surface.
            if (floating) poseStack.translate(0f, Mth.sin(time / 12f + angle) / 32f, 0f);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(radius, 0f, 0f);
            for (int copy = 0; copy <= stack.getCount() / 8; copy++) {
                poseStack.pushPose();
                poseStack.translate(jitter(random), 0f, jitter(random));
                poseStack.mulPose(Axis.YP.rotationDegrees(35f));
                poseStack.mulPose(Axis.XP.rotationDegrees(65f));
                poseStack.scale(scale, scale, scale);
                renderer.renderStatic(stack, ItemDisplayContext.GROUND, light, overlay,
                        poseStack, buffers, level, 0);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float jitter(RandomSource random) {
        return (random.nextFloat() - 0.5f) * 2f / 16f;
    }
}
