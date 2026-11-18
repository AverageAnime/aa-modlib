package dev.averageanime.lib.recipe.adapted;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the adapted types under {@code modId} and switches the adapter on. The rewrite itself also
 * needs the consumer to register the {@code RecipePrepareAdaptMixin} config.
 */
public final class AdaptedRecipes {

    private AdaptedRecipes() {}

    public static void register(String modId) {
        for (AdaptedRecipeTypes.Entry<?> entry : AdaptedRecipeTypes.ALL) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modId, entry.path());
            Registry.register(BuiltInRegistries.RECIPE_TYPE, id, entry.type());
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, entry.serializer());
        }
        FabricLoader loader = FabricLoader.getInstance();
        RecipeAdapter.install(modId, loader::isModLoaded);
    }
}
