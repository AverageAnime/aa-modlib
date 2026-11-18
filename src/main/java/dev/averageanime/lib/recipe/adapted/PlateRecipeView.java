package dev.averageanime.lib.recipe.adapted;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * A plate-runnable recipe, whether it came live from another mod (read by reflection) or from an adapted
 * copy. Results stay unrolled until the recipe is used.
 *
 * @param tool null when the recipe needs no particular tool (pressing, compacting)
 */
public record PlateRecipeView(Ingredient input, @Nullable Predicate<ItemStack> tool, List<ChanceOutput> results) {

    public boolean accepts(ItemStack food) {
        return input.test(food);
    }

    public boolean acceptsTool(ItemStack held) {
        return tool == null || tool.test(held);
    }

    public List<ItemStack> roll(RandomSource random) {
        return ChanceOutput.roll(results, random);
    }
}
