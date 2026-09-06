package dev.averageanime.lib.util;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Server-side only; the pickup delay stops it flying back into the hand that placed it. */
public final class ItemSpawns {

    private ItemSpawns() {}

    public static void spawnItemEntity(Level level, ItemStack stack,
                                       double x, double y, double z,
                                       double motionX, double motionY, double motionZ) {
        if (level.isClientSide) return;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement(motionX, motionY, motionZ);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}