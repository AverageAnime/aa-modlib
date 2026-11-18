package dev.averageanime.lib.recipe.adapted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

/**
 * Create's fluid ingredient: {@code {type: "fluid_tag", fluid_tag, amount}} or
 * {@code {type: "fluid_stack", fluid, amount}}. {@code type} and {@code components} are ignored, so a
 * fluid stack matches its fluid whatever its components.
 */
public record FluidRequirement(Optional<TagKey<Fluid>> tag, Optional<Fluid> fluid, int amount) {

    public static final Codec<FluidRequirement> CODEC = RecordCodecBuilder.<FluidRequirement>create(i -> i.group(
            TagKey.codec(Registries.FLUID).optionalFieldOf("fluid_tag").forGetter(FluidRequirement::tag),
            BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(FluidRequirement::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidRequirement::amount)
    ).apply(i, FluidRequirement::new)).validate(r -> r.tag().isPresent() || r.fluid().isPresent()
            ? DataResult.success(r)
            : DataResult.error(() -> "fluid ingredient needs fluid_tag or fluid"));

    private static final StreamCodec<RegistryFriendlyByteBuf, TagKey<Fluid>> TAG_STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.FLUID, id), TagKey::location)
                    .cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidRequirement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(TAG_STREAM_CODEC), FluidRequirement::tag,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.FLUID)), FluidRequirement::fluid,
            ByteBufCodecs.VAR_INT, FluidRequirement::amount,
            FluidRequirement::new);

    public boolean matches(Fluid stored) {
        if (fluid.isPresent() && fluid.get() == stored) return true;
        return tag.isPresent() && stored.builtInRegistryHolder().is(tag.get());
    }
}
