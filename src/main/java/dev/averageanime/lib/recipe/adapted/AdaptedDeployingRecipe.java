package dev.averageanime.lib.recipe.adapted;

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

/** {@code create:deploying}: the placed item, then the applied item; the first result replaces the placed one. */
public record AdaptedDeployingRecipe(Ingredient base, Ingredient applied, List<ChanceOutput> results)
        implements AdaptedRecipe {

    public static final MapCodec<AdaptedDeployingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            AdaptedCodecs.itemIngredients(2).forGetter(r -> List.of(r.base(), r.applied())),
            AdaptedCodecs.itemResults("results").forGetter(AdaptedDeployingRecipe::results)
    ).apply(i, (ingredients, results) -> new AdaptedDeployingRecipe(ingredients.get(0), ingredients.get(1), results)));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptedDeployingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AdaptedDeployingRecipe::base,
            Ingredient.CONTENTS_STREAM_CODEC, AdaptedDeployingRecipe::applied,
            AdaptedCodecs.OUTPUTS_STREAM_CODEC, AdaptedDeployingRecipe::results,
            AdaptedDeployingRecipe::new);

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, base, applied);
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return AdaptedRecipeTypes.DEPLOYING.serializer(); }

    @Override
    public RecipeType<?> getType() { return AdaptedRecipeTypes.DEPLOYING.type(); }
}
