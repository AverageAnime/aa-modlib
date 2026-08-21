package dev.averageanime.lib.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Late-resolving item references.
 *
 * <p>One declaration referring to another -- a cake naming its slice, an item naming its crafting
 * remainder -- cannot resolve at declaration time, because the target may not be registered yet and may
 * not even be declared in the same place. Config-defined content exists only in the game registry, while
 * hardcoded content is reachable through the mod's own tables.
 *
 * <p>Resolving lazily through here means a target can move between the two without the reference
 * breaking, which is what makes content extractable to an addon at all.
 *
 * <p>An unresolvable reference yields {@link Items#BARRIER} rather than throwing: one bad id should be
 * visible in the world, not fatal at load.
 */
public final class RegistryLookup {

    private RegistryLookup() {}

    /** Resolves a namespaced item id, falling back to a barrier. */
    public static Supplier<Item> byFullId(String itemId) {
        return () -> {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) return Items.BARRIER;
            Item item = BuiltInRegistries.ITEM.get(id);
            return item != Items.AIR ? item : Items.BARRIER;
        };
    }

    /**
     * Resolves a bare id, preferring the mod's own declaration and falling back to the game registry.
     *
     * @param declared looks an id up among the mod's hardcoded entries, returning null when it is not
     *                 one of them -- typically because it now lives in config or an addon
     */
    public static Supplier<Item> byModId(String modId, String itemId, Function<String, Item> declared) {
        return () -> {
            Item fromTable = declared.apply(itemId);
            if (fromTable != null) return fromTable;
            return byFullId(modId + ":" + itemId).get();
        };
    }
}
