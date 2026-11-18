package dev.averageanime.lib.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

/** Another mod's recipes, looked up by type id so that mod need not be on the classpath. */
final class LiveRecipes {

    private LiveRecipes() {}

    /** @return empty when the type is not registered, which is how an absent source mod shows up */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static List<Recipe<?>> of(Level level, ResourceLocation typeId) {
        try {
            RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.get(typeId);
            if (type == null) return List.of();
            Collection<RecipeHolder<?>> holders = (Collection) level.getRecipeManager().getAllRecipesFor((RecipeType) type);
            return holders.stream().<Recipe<?>>map(RecipeHolder::value).toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
