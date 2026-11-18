package dev.averageanime.lib.recipe.adapted;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The seven adapted types. Registered under the consuming mod's namespace by the loader's
 * {@code AdaptedRecipes.register}; until then {@link Entry#recipes} finds nothing, so a mod that never
 * registers them pays nothing.
 *
 * <p>The paths are a public contract: datapacks and KubeJS/CraftTweaker scripts name them directly.
 */
public final class AdaptedRecipeTypes {

    private AdaptedRecipeTypes() {}

    public static final class Entry<R extends Recipe<RecipeInput>> {
        private final String path;
        private final RecipeType<R> type;
        private final RecipeSerializer<R> serializer;

        Entry(String path, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec) {
            this.path = path;
            this.type = new RecipeType<>() {
                @Override
                public String toString() { return path; }
            };
            this.serializer = new RecipeSerializer<>() {
                @Override
                public MapCodec<R> codec() { return codec; }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() { return streamCodec; }
            };
        }

        public String path() { return path; }

        public RecipeType<R> type() { return type; }

        public RecipeSerializer<R> serializer() { return serializer; }

        public List<R> recipes(Level level) {
            return level.getRecipeManager().getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
        }
    }

    public static final Entry<AdaptedFillingRecipe> FILLING =
            new Entry<>("adapted_filling", AdaptedFillingRecipe.CODEC, AdaptedFillingRecipe.STREAM_CODEC);
    public static final Entry<AdaptedEmptyingRecipe> EMPTYING =
            new Entry<>("adapted_emptying", AdaptedEmptyingRecipe.CODEC, AdaptedEmptyingRecipe.STREAM_CODEC);
    public static final Entry<AdaptedDeployingRecipe> DEPLOYING =
            new Entry<>("adapted_deploying", AdaptedDeployingRecipe.CODEC, AdaptedDeployingRecipe.STREAM_CODEC);
    public static final Entry<AdaptedPlateRecipe> CUTTING = plateEntry("adapted_cutting", AdaptedPlateRecipe.Kind.CUTTING);
    public static final Entry<AdaptedPlateRecipe> BAKING_MAT = plateEntry("adapted_baking_mat", AdaptedPlateRecipe.Kind.BAKING_MAT);
    public static final Entry<AdaptedPlateRecipe> PRESSING = plateEntry("adapted_pressing", AdaptedPlateRecipe.Kind.PRESSING);
    public static final Entry<AdaptedPlateRecipe> COMPACTING = plateEntry("adapted_compacting", AdaptedPlateRecipe.Kind.COMPACTING);

    public static final List<Entry<?>> ALL = List.of(FILLING, EMPTYING, DEPLOYING, CUTTING, BAKING_MAT, PRESSING, COMPACTING);

    private static Entry<AdaptedPlateRecipe> plateEntry(String path, AdaptedPlateRecipe.Kind kind) {
        return new Entry<>(path, AdaptedPlateRecipe.codec(kind), AdaptedPlateRecipe.streamCodec(kind));
    }

    static Entry<AdaptedPlateRecipe> plate(AdaptedPlateRecipe.Kind kind) {
        return switch (kind) {
            case CUTTING -> CUTTING;
            case BAKING_MAT -> BAKING_MAT;
            case PRESSING -> PRESSING;
            case COMPACTING -> COMPACTING;
        };
    }
}
