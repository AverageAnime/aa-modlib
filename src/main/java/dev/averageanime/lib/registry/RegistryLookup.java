package dev.averageanime.lib.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;
import java.util.function.Supplier;

/** Late-resolving item references; an unresolvable reference yields {@link Items#BARRIER} rather than throwing. */
public final class RegistryLookup {

    private RegistryLookup() {}

    public static Supplier<Item> byFullId(String itemId) {
        return () -> {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) return Items.BARRIER;
            Item item = BuiltInRegistries.ITEM.get(id);
            return item != Items.AIR ? item : Items.BARRIER;
        };
    }

    /** {@code declared} returns null when the id is not one of the mod's hardcoded entries. */
    public static Supplier<Item> byModId(String modId, String itemId, Function<String, Item> declared) {
        return () -> {
            Item fromTable = declared.apply(itemId);
            if (fromTable != null) return fromTable;
            return byFullId(modId + ":" + itemId).get();
        };
    }
}
