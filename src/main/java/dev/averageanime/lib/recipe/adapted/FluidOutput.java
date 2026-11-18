package dev.averageanime.lib.recipe.adapted;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/** A fluid entry in a Create {@code results} list: {@code {id, amount}}. */
public record FluidOutput(Fluid fluid, int amount) {

    public static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(FluidOutput::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidOutput::amount)
    ).apply(i, FluidOutput::new));
}
