package dev.averageanime.lib.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Overriding {@code entityInside} takes lava's burning without inheriting the rest of being lava. */
public class HotLiquidBlock extends LiquidBlock {

    public HotLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Answer only for the block the entity stands in; up to eight are visited per tick.
        if (!pos.equals(entity.blockPosition())) return;

        // Feet above the surface of a shallow flowing block means standing at the edge, not wading in.
        if (entity.getY() > pos.getY() + state.getFluidState().getHeight(level, pos)) return;

        // Vanilla's own routine: ignites, deals lava damage, plays the sound.
        entity.lavaHurt();
    }
}
