package dev.averageanime.lib.recipe.adapted;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/**
 * Read only by lookup, never matched by a crafting container. {@code isSpecial} keeps these out of the
 * recipe book, which would otherwise warn about an unknown category for every one of them.
 */
public interface AdaptedRecipe extends Recipe<RecipeInput> {

    @Override
    default boolean matches(RecipeInput input, Level level) { return false; }

    @Override
    default ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }

    @Override
    default boolean canCraftInDimensions(int width, int height) { return false; }

    @Override
    default boolean isSpecial() { return true; }

    @Override
    default boolean showNotification() { return false; }
}
