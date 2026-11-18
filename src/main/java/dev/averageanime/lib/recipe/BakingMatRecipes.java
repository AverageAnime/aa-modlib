package dev.averageanime.lib.recipe;

import dev.averageanime.lib.recipe.adapted.AdaptedPlateRecipe;
import dev.averageanime.lib.recipe.adapted.AdaptedRecipeTypes;
import dev.averageanime.lib.recipe.adapted.PlateRecipeEligibility;
import dev.averageanime.lib.recipe.adapted.PlateRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Ube's Delight's baking-mat recipes a plate can run: single-ingredient only, live and adapted. The mat's
 * processing stages are its step-by-step visuals, which a plate does not have, so a match rolls the
 * results in one use.
 */
public final class BakingMatRecipes {

    private static final ResourceLocation BAKING_MAT_TYPE = ResourceLocation.fromNamespaceAndPath("ubesdelight", "baking_mat");

    private BakingMatRecipes() {}

    public static List<PlateRecipeView> all(Level level) {
        List<PlateRecipeView> views = new ArrayList<>();
        for (Recipe<?> recipe : LiveRecipes.of(level, BAKING_MAT_TYPE)) {
            if (!PlateRecipeEligibility.bakingMatLive(recipe)) continue;
            views.add(new PlateRecipeView(recipe.getIngredients().get(0),
                    tool -> RecipeReflection.toolMatches(recipe, tool),
                    RecipeReflection.getChanceOutputs(recipe)));
        }
        for (AdaptedPlateRecipe recipe : AdaptedRecipeTypes.BAKING_MAT.recipes(level)) views.add(recipe.view());
        return views;
    }
}
