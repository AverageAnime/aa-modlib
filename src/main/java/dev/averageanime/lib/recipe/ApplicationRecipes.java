package dev.averageanime.lib.recipe;

import dev.averageanime.lib.recipe.adapted.AdaptedDeployingRecipe;
import dev.averageanime.lib.recipe.adapted.AdaptedRecipeTypes;
import dev.averageanime.lib.recipe.adapted.ChanceOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/** Create must not appear on the compile classpath. */
public final class ApplicationRecipes {

    private static final ResourceLocation DEPLOYING_TYPE =
            ResourceLocation.fromNamespaceAndPath("create", "deploying");

    private ApplicationRecipes() {}

    public record Applied(ItemStack result, List<ItemStack> leftovers) {}

    /** Create's own deploying recipes first, then the adapted copies and any written for the adapted type. */
    @Nullable
    public static Applied find(Level level, ItemStack placed, ItemStack held) {
        if (placed.isEmpty() || held.isEmpty()) return null;

        Applied live = findLive(level, placed, held);
        if (live != null) return live;

        for (AdaptedDeployingRecipe recipe : AdaptedRecipeTypes.DEPLOYING.recipes(level)) {
            if (!recipe.base().test(placed) || !recipe.applied().test(held)) continue;
            List<ItemStack> results = ChanceOutput.roll(recipe.results(), level.random);
            if (results.isEmpty() || results.get(0).isEmpty()) continue;
            return new Applied(results.get(0).copy(), results.subList(1, results.size()).stream().map(ItemStack::copy).toList());
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private static Applied findLive(Level level, ItemStack placed, ItemStack held) {
        try {
            RecipeType<?> deployingType = BuiltInRegistries.RECIPE_TYPE.get(DEPLOYING_TYPE);
            if (deployingType == null) return null;

            Collection<RecipeHolder<?>> recipes =
                    (Collection) level.getRecipeManager().getAllRecipesFor((RecipeType) deployingType);

            for (RecipeHolder<?> holder : recipes) {
                Recipe<?> recipe = holder.value();

                var ingredients = recipe.getIngredients();
                if (ingredients.size() < 2) continue;
                if (!ingredients.get(0).test(placed) || !ingredients.get(1).test(held)) continue;

                List<ItemStack> results = RecipeReflection.getRollableResults(recipe, level.random);
                if (results.isEmpty()) continue;

                ItemStack result = results.get(0);
                if (result.isEmpty()) continue;

                List<ItemStack> leftovers = results.subList(1, results.size())
                        .stream().map(ItemStack::copy).toList();
                return new Applied(result.copy(), leftovers);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
