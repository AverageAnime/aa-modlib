package dev.averageanime.lib.recipe.adapted;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.averageanime.lib.recipe.RecipeReflection;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Collection;

/**
 * Which pressing, compacting and baking-mat recipes a plate can reproduce faithfully: one item in, no
 * heat, no fluid either way. A plate holds one item and cannot heat or hold fluid, so anything else would
 * run as a different recipe than its author wrote.
 *
 * <p>The JSON tests gate what {@link RecipeAdapter} copies; the live tests gate what the finders read from
 * the source mod's own recipe objects. Keep each pair in step, or a recipe would behave differently with
 * the source mod installed than without it.
 */
public final class PlateRecipeEligibility {

    private PlateRecipeEligibility() {}

    public static boolean pressingJson(JsonObject recipe) {
        return singleItemIngredient(recipe);
    }

    public static boolean compactingJson(JsonObject recipe) {
        JsonElement heat = recipe.get("heat_requirement");
        if (heat != null && !(heat.isJsonPrimitive() && "none".equals(heat.getAsString()))) return false;
        if (!singleItemIngredient(recipe)) return false;
        JsonElement results = recipe.get("results");
        if (results instanceof JsonArray array) {
            for (JsonElement result : array) {
                if (result.isJsonObject() && result.getAsJsonObject().has("amount")) return false;
            }
        }
        return true;
    }

    public static boolean bakingMatJson(JsonObject recipe) {
        return recipe.get("ingredients") instanceof JsonArray array && array.size() == 1;
    }

    /** Live {@code create:pressing}. Fluid ingredients are not in {@code getIngredients()}, so they are read separately. */
    public static boolean pressingLive(Recipe<?> recipe) {
        return recipe.getIngredients().size() == 1 && isEmpty(RecipeReflection.invoke(recipe, "getFluidIngredients"));
    }

    /** Live {@code create:compacting}. Create's {@code HeatCondition} is read by name, so Create need not be on the classpath. */
    public static boolean compactingLive(Recipe<?> recipe) {
        if (!pressingLive(recipe)) return false;
        if (!isEmpty(RecipeReflection.invoke(recipe, "getFluidResults"))) return false;
        Object heat = RecipeReflection.invoke(recipe, "getRequiredHeat");
        return heat == null || (heat instanceof Enum<?> e && e.name().equals("NONE"));
    }

    public static boolean bakingMatLive(Recipe<?> recipe) {
        return recipe.getIngredients().size() == 1;
    }

    private static boolean singleItemIngredient(JsonObject recipe) {
        if (!(recipe.get("ingredients") instanceof JsonArray array) || array.size() != 1) return false;
        if (!array.get(0).isJsonObject()) return true;
        JsonObject ingredient = array.get(0).getAsJsonObject();
        return !ingredient.has("amount") && !ingredient.has("fluid_tag") && !ingredient.has("fluid");
    }

    private static boolean isEmpty(Object maybeCollection) {
        return !(maybeCollection instanceof Collection<?> collection) || collection.isEmpty();
    }
}
