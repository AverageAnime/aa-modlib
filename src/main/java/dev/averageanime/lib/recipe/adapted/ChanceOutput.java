package dev.averageanime.lib.recipe.adapted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One possible result, before its chance is rolled.
 *
 * <p>Reads every result shape Create and Farmer's Delight write: {@code {item: {id, count?}, count?,
 * chance?}} and the flat {@code {id, count?, chance?}}. An outer {@code count} overrides the inner one.
 */
public record ChanceOutput(ItemStack stack, float chance) {

    private static final Codec<ChanceOutput> NESTED = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("item").forGetter(ChanceOutput::stack),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count").forGetter(o -> Optional.empty()),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(ChanceOutput::chance)
    ).apply(i, (stack, count, chance) ->
            new ChanceOutput(count.map(stack::copyWithCount).orElse(stack), chance)));

    private static final Codec<ChanceOutput> FLAT = RecordCodecBuilder.create(i -> i.group(
            ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(o -> o.stack().getItemHolder()),
            ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(o -> o.stack().getCount()),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(o -> o.stack().getComponentsPatch()),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(ChanceOutput::chance)
    ).apply(i, (item, count, components, chance) -> new ChanceOutput(new ItemStack(item, count, components), chance)));

    public static final Codec<ChanceOutput> CODEC = Codec.withAlternative(NESTED, FLAT);

    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ChanceOutput::stack,
            ByteBufCodecs.FLOAT, ChanceOutput::chance,
            ChanceOutput::new);

    public static List<ItemStack> roll(List<ChanceOutput> outputs, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (ChanceOutput output : outputs) {
            if (output.chance() >= 1.0f || random.nextFloat() < output.chance()) out.add(output.stack().copy());
        }
        return out;
    }
}
