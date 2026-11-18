package dev.averageanime.lib.recipe.adapted;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/** {@code create:emptying}: one item in; one fluid out, plus at most one leftover item (empty if none). */
public record AdaptedEmptyingRecipe(Ingredient input, ItemStack leftover, Fluid fluid, int amount)
        implements AdaptedRecipe {

    private record Outputs(ItemStack leftover, FluidOutput fluid) {}

    private static final MapCodec<Outputs> RESULTS = AdaptedCodecs.RESULT_ENTRY.listOf().flatXmap(
            list -> {
                ItemStack leftover = ItemStack.EMPTY;
                FluidOutput fluid = null;
                for (Either<FluidOutput, ChanceOutput> entry : list) {
                    if (entry.left().isPresent()) {
                        if (fluid != null) return DataResult.<Outputs>error(() -> "more than one fluid result");
                        fluid = entry.left().get();
                    } else {
                        if (!leftover.isEmpty()) return DataResult.<Outputs>error(() -> "more than one item result");
                        leftover = entry.right().orElseThrow().stack();
                    }
                }
                if (fluid == null) return DataResult.<Outputs>error(() -> "emptying needs a fluid result");
                return DataResult.success(new Outputs(leftover, fluid));
            },
            outputs -> {
                List<Either<FluidOutput, ChanceOutput>> list = new ArrayList<>();
                if (!outputs.leftover().isEmpty()) list.add(Either.right(new ChanceOutput(outputs.leftover(), 1.0f)));
                list.add(Either.left(outputs.fluid()));
                return DataResult.success(list);
            }
    ).fieldOf("results");

    public static final MapCodec<AdaptedEmptyingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            AdaptedCodecs.singleIngredient().forGetter(AdaptedEmptyingRecipe::input),
            RESULTS.forGetter(r -> new Outputs(r.leftover(), new FluidOutput(r.fluid(), r.amount())))
    ).apply(i, (input, outputs) ->
            new AdaptedEmptyingRecipe(input, outputs.leftover(), outputs.fluid().fluid(), outputs.fluid().amount())));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptedEmptyingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, AdaptedEmptyingRecipe::input,
            ItemStack.OPTIONAL_STREAM_CODEC, AdaptedEmptyingRecipe::leftover,
            ByteBufCodecs.registry(Registries.FLUID), AdaptedEmptyingRecipe::fluid,
            ByteBufCodecs.VAR_INT, AdaptedEmptyingRecipe::amount,
            AdaptedEmptyingRecipe::new);

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return leftover; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return AdaptedRecipeTypes.EMPTYING.serializer(); }

    @Override
    public RecipeType<?> getType() { return AdaptedRecipeTypes.EMPTYING.type(); }
}
