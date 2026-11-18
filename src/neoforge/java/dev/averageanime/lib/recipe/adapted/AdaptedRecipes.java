package dev.averageanime.lib.recipe.adapted;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the adapted types under {@code modId} and switches the adapter on. The rewrite itself also
 * needs the consumer to register the {@code RecipePrepareAdaptMixin} config.
 */
public final class AdaptedRecipes {

    private AdaptedRecipes() {}

    public static void register(IEventBus modEventBus, String modId) {
        DeferredRegister<RecipeType<?>> types = DeferredRegister.create(Registries.RECIPE_TYPE, modId);
        DeferredRegister<RecipeSerializer<?>> serializers = DeferredRegister.create(Registries.RECIPE_SERIALIZER, modId);
        for (AdaptedRecipeTypes.Entry<?> entry : AdaptedRecipeTypes.ALL) {
            types.register(entry.path(), entry::type);
            serializers.register(entry.path(), entry::serializer);
        }
        types.register(modEventBus);
        serializers.register(modEventBus);
        RecipeAdapter.install(modId, id -> ModList.get().isLoaded(id));
    }
}
