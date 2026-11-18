package dev.averageanime.lib.recipe.adapted;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** {@code create:filling}: one item ingredient, one fluid ingredient. */
public record AdaptedFillingRecipe(Ingredient item, FluidRequirement fluid, List<ChanceOutput> results)
        implements AdaptedRecipe {

    private static final MapCodec<Pair<Ingredient, FluidRequirement>> INGREDIENTS =
            AdaptedCodecs.INGREDIENT_ENTRY.listOf().flatXmap(
                    list -> {
                        Ingredient item = null;
                        FluidRequirement fluid = null;
                        for (Either<FluidRequirement, Ingredient> entry : list) {
                            if (entry.left().isPresent()) {
                                if (fluid != null) return DataResult.<Pair<Ingredient, FluidRequirement>>error(() -> "more than one fluid ingredient");
                                fluid = entry.left().get();
                            } else {
                                if (item != null) return DataResult.<Pair<Ingredient, FluidRequirement>>error(() -> "more than one item ingredient");
                                item = entry.right().orElseThrow();
                            }
                        }
                        if (item == null || fluid == null) {
                            return DataResult.<Pair<Ingredient, FluidRequirement>>error(() -> "filling needs one item and one fluid ingredient");
                        }
                        return DataResult.success(Pair.of(item, fluid));
                    },
                    pair -> DataResult.success(List.of(
                            Either.<FluidRequirement, Ingredient>right(pair.getFirst()),
                            Either.<FluidRequirement, Ingredient>left(pair.getSecond())))
            ).fieldOf("ingredients");

    public static final MapCodec<AdaptedFillingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            INGREDIENTS.forGetter(r -> Pair.of(r.item(), r.fluid())),
            AdaptedCodecs.itemResults("results").forGetter(AdaptedFillingRecipe::results)
    ).apply(i, (ingredients, results) -> new AdaptedFillingRecipe(ingredients.getFirst(), ingredients.getSecond(), results)));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptedFillingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AdaptedFillingRecipe::item,
            FluidRequirement.STREAM_CODEC, AdaptedFillingRecipe::fluid,
            AdaptedCodecs.OUTPUTS_STREAM_CODEC, AdaptedFillingRecipe::results,
            AdaptedFillingRecipe::new);

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, item);
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return AdaptedRecipeTypes.FILLING.serializer(); }

    @Override
    public RecipeType<?> getType() { return AdaptedRecipeTypes.FILLING.type(); }
}
