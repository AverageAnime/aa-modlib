package dev.averageanime.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

/**
 * A block that holds one fluid: a bowl, a crucible, a vat.
 *
 * <p>Deliberately not a loader fluid handler. Both loaders have one and they do not agree, so this
 * stays a plain amount-of-one-fluid that either can wrap. The contents are part of how the block looks,
 * so they ride along in both the update tag and the update packet.
 *
 * <p>Capacity is a supplier rather than a constant because it is usually a config value that can change
 * without the block being rebuilt.
 */
public class FluidTankBlockEntity extends BlockEntity {

    private static final String TAG_FLUID = "fluid";
    private static final String TAG_AMOUNT = "amount";

    private final IntSupplier capacity;

    private Fluid fluid = Fluids.EMPTY;
    private int amount = 0;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, IntSupplier capacity) {
        super(type, pos, state);
        this.capacity = capacity;
    }

    /** Current capacity in mB. Re-read on every use, so a config change takes effect immediately. */
    public int getCapacityMb() {
        return capacity.getAsInt();
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }

    public boolean isFull() {
        return amount >= getCapacityMb();
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public boolean canAccept(Fluid incoming, int mb) {
        if (incoming == null || incoming == Fluids.EMPTY || mb <= 0) return false;
        if (!isEmpty() && fluid != incoming) return false;
        return getAmount() + mb <= getCapacityMb();
    }

    public boolean fill(Fluid incoming, int mb) {
        if (!canAccept(incoming, mb)) return false;
        this.amount = getAmount() + mb;
        this.fluid = incoming;
        markUpdated();
        return true;
    }

    public boolean drain(int mb) {
        if (mb <= 0 || getAmount() < mb) return false;
        this.amount -= mb;
        if (this.amount <= 0) {
            this.amount = 0;
            this.fluid = Fluids.EMPTY;
        }
        markUpdated();
        return true;
    }

    public void clear() {
        if (isEmpty()) return;
        this.fluid = Fluids.EMPTY;
        this.amount = 0;
        markUpdated();
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void writeTank(CompoundTag tag) {
        tag.putInt(TAG_AMOUNT, isEmpty() ? 0 : amount);
        if (isEmpty()) return;
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key != null) tag.putString(TAG_FLUID, key.toString());
    }

    private void readTank(CompoundTag tag) {
        fluid = Fluids.EMPTY;
        amount = 0;
        if (!tag.contains(TAG_FLUID)) return;

        ResourceLocation key = ResourceLocation.tryParse(tag.getString(TAG_FLUID));
        if (key == null) return;
        Fluid stored = BuiltInRegistries.FLUID.get(key);
        if (stored == null || stored == Fluids.EMPTY) return;

        fluid = stored;
        amount = Math.max(0, Math.min(getCapacityMb(), tag.getInt(TAG_AMOUNT)));
        if (amount == 0) fluid = Fluids.EMPTY;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        writeTank(tag);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        readTank(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeTank(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        return saveWithFullMetadata(registries);
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        loadWithComponents(tag, registries);
    }
}
