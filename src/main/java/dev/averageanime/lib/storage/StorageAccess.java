package dev.averageanime.lib.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * An inventory, without saying whose.
 *
 * <p>NeoForge and Fabric each have their own item-handler abstraction and they do not agree, so code
 * that just wants to move stacks around talks to this instead and each loader supplies an adapter.
 *
 * <p>{@code setOnChanged} exists because the owner usually has to mark itself dirty or resync when the
 * contents move, and the inventory is the only thing that knows when that happened.
 */
public interface StorageAccess {

    ItemStack getInventoryItem(int slot);

    void setInventoryItem(int slot, ItemStack stack);

    int getInventorySize();

    int getSlotLimit(int slot);

    boolean canPlaceItem(int slot, ItemStack stack);

    ItemStack removeItem(int slot, int amount);

    ItemStack insertItem(int slot, ItemStack stack);

    CompoundTag serializeInventory(HolderLookup.Provider registries);

    void deserializeInventory(HolderLookup.Provider registries, CompoundTag tag);

    void setOnChanged(Runnable onChanged);
}