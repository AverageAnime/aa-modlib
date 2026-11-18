package dev.averageanime.lib.recipe.adapted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * One item in, item results out, optionally needing a held tool. Backs four types whose JSON keys differ,
 * so each kind has its own codec. Pressing and compacting have no tool.
 *
 * <p>The compacting codec rejects heat, fluids and extra ingredients outright, so that a handwritten
 * recipe cannot ask for something a plate would silently skip (see {@link PlateRecipeEligibility}).
 */
public record AdaptedPlateRecipe(Kind kind, Ingredient input, Optional<Ingredient> tool, List<ChanceOutput> results)
        implements AdaptedRecipe {

    public enum Kind { CUTTING, BAKING_MAT, PRESSING, COMPACTING }

    private static final Codec<String> UNHEATED = Codec.STRING.validate(heat -> "none".equals(heat)
            ? DataResult.success(heat)
            : DataResult.error(() -> "a plate cannot heat; heat_requirement must be none, was " + heat));

    static MapCodec<AdaptedPlateRecipe> codec(Kind kind) {
        return switch (kind) {
            case CUTTING, BAKING_MAT -> RecordCodecBuilder.mapCodec(i -> i.group(
                    AdaptedCodecs.singleIngredient().forGetter(AdaptedPlateRecipe::input),
                    Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(r -> r.tool().orElse(Ingredient.EMPTY)),
                    AdaptedCodecs.itemResults("result").forGetter(AdaptedPlateRecipe::results)
            ).apply(i, (input, tool, results) -> new AdaptedPlateRecipe(kind, input, Optional.of(tool), results)));
            case PRESSING -> RecordCodecBuilder.mapCodec(i -> i.group(
                    AdaptedCodecs.itemIngredients(1).forGetter(r -> List.of(r.input())),
                    AdaptedCodecs.itemResults("results").forGetter(AdaptedPlateRecipe::results)
            ).apply(i, (inputs, results) -> new AdaptedPlateRecipe(kind, inputs.get(0), Optional.empty(), results)));
            case COMPACTING -> RecordCodecBuilder.mapCodec(i -> i.group(
                    AdaptedCodecs.itemIngredients(1).forGetter(r -> List.of(r.input())),
                    UNHEATED.optionalFieldOf("heat_requirement", "none").forGetter(r -> "none"),
                    AdaptedCodecs.itemResults("results").forGetter(AdaptedPlateRecipe::results)
            ).apply(i, (inputs, heat, results) -> new AdaptedPlateRecipe(kind, inputs.get(0), Optional.empty(), results)));
        };
    }

    static StreamCodec<RegistryFriendlyByteBuf, AdaptedPlateRecipe> streamCodec(Kind kind) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AdaptedPlateRecipe::input,
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), AdaptedPlateRecipe::tool,
                AdaptedCodecs.OUTPUTS_STREAM_CODEC, AdaptedPlateRecipe::results,
                (input, tool, results) -> new AdaptedPlateRecipe(kind, input, tool, results));
    }

    public PlateRecipeView view() {
        return new PlateRecipeView(input, tool.isPresent() ? tool.get() : null, results);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return AdaptedRecipeTypes.plate(kind).serializer(); }

    @Override
    public RecipeType<?> getType() { return AdaptedRecipeTypes.plate(kind).type(); }
}
