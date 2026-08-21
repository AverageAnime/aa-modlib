package dev.averageanime.lib.util;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dropping an item into the world with a nudge.
 *
 * <p>Server-side only: spawning on the client produces an entity the server does not know about, which
 * flickers and vanishes. The pickup delay stops the item flying straight back into the hand that just
 * placed it.
 */
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