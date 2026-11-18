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
import java.util.function.Predicate;

/**
 * Create's pressing, and the compacting a plate can reproduce (one item, unheated, no fluids), live and
 * adapted. Create must not appear on the compile classpath.
 */
public final class PressRecipes {

    private static final ResourceLocation PRESSING_TYPE = ResourceLocation.fromNamespaceAndPath("create", "pressing");
    private static final ResourceLocation COMPACTING_TYPE = ResourceLocation.fromNamespaceAndPath("create", "compacting");

    private PressRecipes() {}

    public static List<PlateRecipeView> pressing(Level level) {
        List<PlateRecipeView> views = live(level, PRESSING_TYPE, PlateRecipeEligibility::pressingLive);
        for (AdaptedPlateRecipe recipe : AdaptedRecipeTypes.PRESSING.recipes(level)) views.add(recipe.view());
        return views;
    }

    public static List<PlateRecipeView> compacting(Level level) {
        List<PlateRecipeView> views = live(level, COMPACTING_TYPE, PlateRecipeEligibility::compactingLive);
        for (AdaptedPlateRecipe recipe : AdaptedRecipeTypes.COMPACTING.recipes(level)) views.add(recipe.view());
        return views;
    }

    private static List<PlateRecipeView> live(Level level, ResourceLocation type, Predicate<Recipe<?>> eligible) {
        List<PlateRecipeView> views = new ArrayList<>();
        for (Recipe<?> recipe : LiveRecipes.of(level, type)) {
            if (!eligible.test(recipe)) continue;
            views.add(new PlateRecipeView(recipe.getIngredients().get(0), null, RecipeReflection.getChanceOutputs(recipe)));
        }
        return views;
    }
}
