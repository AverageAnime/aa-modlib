package dev.averageanime.lib.mixin.create;

import dev.averageanime.lib.compat.BasinFluidCapacity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lifts the recipe-side fluid caps to match {@link BasinFluidCapacityMixin}. Required: a recipe over the cap fails codec validation. */
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinRecipe", remap = false)
public class BasinRecipeFluidLimitMixin {

    @ModifyReturnValue(method = "getMaxFluidInputCount", at = @At("RETURN"))
    private int aalib$moreFluidInputs(int original) {
        return BasinFluidCapacity.isEnabled()
                ? Math.max(original, BasinFluidCapacity.INPUT_TANKS) : original;
    }

    @ModifyReturnValue(method = "getMaxFluidOutputCount", at = @At("RETURN"))
    private int aalib$moreFluidOutputs(int original) {
        return BasinFluidCapacity.isEnabled()
                ? Math.max(original, BasinFluidCapacity.OUTPUT_TANKS) : original;
    }
}
