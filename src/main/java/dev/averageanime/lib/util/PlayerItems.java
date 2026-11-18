package dev.averageanime.lib.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerItems {

    private PlayerItems() {}

    /** Inventory first, then dropped at the player's feet; creative mode is not special-cased. */
    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /** Into the hand when it is free, so a swapped container lands where the player is looking. */
    public static void giveToHandOrInventory(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
            return;
        }
        give(player, stack);
    }
}
