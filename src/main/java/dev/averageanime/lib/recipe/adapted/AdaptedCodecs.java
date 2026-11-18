package dev.averageanime.lib.recipe.adapted;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/** Field codecs shared by the adapted recipe types. Each reads the source mod's own JSON keys. */
final class AdaptedCodecs {

    private AdaptedCodecs() {}

    /**
     * One entry of a Create {@code ingredients} list. The fluid side is tried first: it needs {@code amount},
     * which an item ingredient never has, so an item ingredient always falls through to the right.
     */
    static final Codec<Either<FluidRequirement, Ingredient>> INGREDIENT_ENTRY =
            Codec.either(FluidRequirement.CODEC, Ingredient.CODEC_NONEMPTY);

    /** One entry of a Create {@code results} list. Fluid first, for the same reason. */
    static final Codec<Either<FluidOutput, ChanceOutput>> RESULT_ENTRY =
            Codec.either(FluidOutput.CODEC, ChanceOutput.CODEC);

    static final StreamCodec<RegistryFriendlyByteBuf, List<ChanceOutput>> OUTPUTS_STREAM_CODEC =
            ChanceOutput.STREAM_CODEC.apply(ByteBufCodecs.list());

    static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INGREDIENTS_STREAM_CODEC =
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list());

    /** {@code ingredients} holding exactly one item ingredient. */
    static MapCodec<Ingredient> singleIngredient() {
        return Ingredient.CODEC_NONEMPTY.listOf().flatXmap(
                list -> list.size() == 1
                        ? DataResult.success(list.get(0))
                        : DataResult.error(() -> "expected exactly one ingredient, found " + list.size()),
                ingredient -> DataResult.success(List.of(ingredient))
        ).fieldOf("ingredients");
    }

    /** {@code ingredients} holding exactly {@code count} item ingredients and no fluid. */
    static MapCodec<List<Ingredient>> itemIngredients(int count) {
        return INGREDIENT_ENTRY.listOf().flatXmap(
                list -> {
                    List<Ingredient> items = new ArrayList<>();
                    for (Either<FluidRequirement, Ingredient> entry : list) {
                        if (entry.left().isPresent()) return DataResult.error(() -> "fluid ingredients are not supported here");
                        items.add(entry.right().orElseThrow());
                    }
                    return items.size() == count
                            ? DataResult.success(items)
                            : DataResult.error(() -> "expected " + count + " item ingredients, found " + items.size());
                },
                items -> DataResult.success(items.stream().map(Either::<FluidRequirement, Ingredient>right).toList())
        ).fieldOf("ingredients");
    }

    /** A {@code results} list that must hold items only. */
    static MapCodec<List<ChanceOutput>> itemResults(String key) {
        return RESULT_ENTRY.listOf().flatXmap(
                list -> {
                    List<ChanceOutput> items = new ArrayList<>();
                    for (Either<FluidOutput, ChanceOutput> entry : list) {
                        if (entry.left().isPresent()) return DataResult.error(() -> "fluid results are not supported here");
                        items.add(entry.right().orElseThrow());
                    }
                    return items.isEmpty()
                            ? DataResult.error(() -> "a recipe needs at least one result")
                            : DataResult.success(items);
                },
                items -> DataResult.success(items.stream().map(Either::<FluidOutput, ChanceOutput>right).toList())
        ).fieldOf(key);
    }
}
