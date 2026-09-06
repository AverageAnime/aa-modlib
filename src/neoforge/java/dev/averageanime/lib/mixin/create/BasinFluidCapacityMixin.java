package dev.averageanime.lib.mixin.create;

import dev.averageanime.lib.compat.BasinFluidCapacity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Raises Create's basin from 2 to 4 fluid segments on both sides; the ordinal picks the tank (input is constructed first), and arg 2 is the segment count. */
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinBlockEntity", remap = false)
public class BasinFluidCapacityMixin {

    @ModifyArg(
            method = "addBehaviours",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;"
                            + "<init>(Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;"
                            + "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;IIZ)V"),
            index = 2)
    private int aalib$moreInputTanks(int tanks) {
        return BasinFluidCapacity.isEnabled() ? Math.max(tanks, BasinFluidCapacity.INPUT_TANKS) : tanks;
    }

    @ModifyArg(
            method = "addBehaviours",
            at = @At(
                    value = "INVOKE",
                    ordinal = 1,
                    target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/fluid/SmartFluidTankBehaviour;"
                            + "<init>(Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;"
                            + "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;IIZ)V"),
            index = 2)
    private int aalib$moreOutputTanks(int tanks) {
        return BasinFluidCapacity.isEnabled() ? Math.max(tanks, BasinFluidCapacity.OUTPUT_TANKS) : tanks;
    }
}
