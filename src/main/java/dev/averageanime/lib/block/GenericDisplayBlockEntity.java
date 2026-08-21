package dev.averageanime.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity holding a single displayed stack, kept in sync with the client.
 *
 * <p>The stack is part of what the block looks like, so it has to reach the client: this writes it into
 * both the update tag and the update packet, which is the pair a renderer needs to draw a block that
 * was already loaded and one that just came into view.
 */
public class GenericDisplayBlockEntity extends BlockEntity {

    private ItemStack displayedItem = ItemStack.EMPTY;

    public GenericDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getDisplayedItem() {
        return displayedItem.copy();
    }

    public ItemStack getDisplayedItemForRender() {
        return displayedItem;
    }

    public boolean isEmpty() {
        return displayedItem.isEmpty();
    }

    public ItemStack takeDisplayedItem() {
        ItemStack taken = displayedItem;
        displayedItem = ItemStack.EMPTY;
        return taken;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!displayedItem.isEmpty()) {
            tag.put("item", displayedItem.save(registries, new CompoundTag()));
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("item")) {
            displayedItem = ItemStack.parseOptional(registries, tag.getCompound("item"));
        } else {
            displayedItem = ItemStack.EMPTY;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!displayedItem.isEmpty()) {
            tag.put("item", displayedItem.save(registries, new CompoundTag()));
        }
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