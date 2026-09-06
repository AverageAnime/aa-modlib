package dev.averageanime.lib.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A config list selecting items as {@code mod:<id>}, {@code item:<id>} or {@code tag:<id>}; an unrecognised entry is ignored. */
public final class ItemFilter {

    private record Compiled(Set<String> mods, Set<String> items, List<TagKey<Item>> tags) {}

    private static final ListParseCache<Compiled> CACHE = new ListParseCache<>(ItemFilter::compile);

    private ItemFilter() {}

    /** An empty list matches nothing. */
    public static boolean matches(ItemStack stack, List<? extends String> list) {
        if (list.isEmpty()) return false;
        Compiled filter = CACHE.get(list);
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        if (filter.mods().contains(key.getNamespace()) || filter.items().contains(key.toString())) return true;
        for (TagKey<Item> tag : filter.tags()) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    /** An exclude list that always wins, and an optional allow list that, when present, gates everything. */
    public static boolean allows(ItemStack stack, List<? extends String> exclude, List<? extends String> allow) {
        if (matches(stack, exclude)) return false;
        return allow.isEmpty() || matches(stack, allow);
    }

    private static Compiled compile(List<? extends String> list) {
        Set<String> mods = new HashSet<>();
        Set<String> items = new HashSet<>();
        List<TagKey<Item>> tags = new ArrayList<>();
        for (String entry : list) {
            if (entry.startsWith("mod:")) {
                mods.add(entry.substring(4));
            } else if (entry.startsWith("item:")) {
                items.add(entry.substring(5));
            } else if (entry.startsWith("tag:")) {
                ResourceLocation tag = ResourceLocation.tryParse(entry.substring(4));
                if (tag != null) tags.add(TagKey.create(Registries.ITEM, tag));
            }
        }
        return new Compiled(Set.copyOf(mods), Set.copyOf(items), List.copyOf(tags));
    }
}
