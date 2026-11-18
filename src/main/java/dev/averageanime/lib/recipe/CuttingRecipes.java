package dev.averageanime.lib.recipe;

import dev.averageanime.lib.recipe.adapted.AdaptedPlateRecipe;
import dev.averageanime.lib.recipe.adapted.AdaptedRecipeTypes;
import dev.averageanime.lib.recipe.adapted.PlateRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Farmer's Delight's cutting recipes, live and adapted, for a plate. Farmer's Delight need not be on the classpath. */
public final class CuttingRecipes {

    private static final ResourceLocation CUTTING_TYPE = ResourceLocation.fromNamespaceAndPath("farmersdelight", "cutting");

    private CuttingRecipes() {}

    public static List<PlateRecipeView> all(Level level) {
        List<PlateRecipeView> views = new ArrayList<>();
        for (Recipe<?> recipe : LiveRecipes.of(level, CUTTING_TYPE)) {
            if (recipe.getIngredients().isEmpty()) continue;
            views.add(new PlateRecipeView(recipe.getIngredients().get(0),
                    tool -> RecipeReflection.toolMatches(recipe, tool),
                    RecipeReflection.getChanceOutputs(recipe)));
        }
        for (AdaptedPlateRecipe recipe : AdaptedRecipeTypes.CUTTING.recipes(level)) views.add(recipe.view());
        return views;
    }
}
