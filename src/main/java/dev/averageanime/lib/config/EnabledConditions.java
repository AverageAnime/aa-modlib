package dev.averageanime.lib.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** The shared half of a recipe condition gating on content being present and enabled; accepts a single {@code id} or a list of {@code ids}. */
public final class EnabledConditions {

    private EnabledConditions() {}

    /** @param factory wraps the resolved id list in that loader's record type */
    public static <T> MapCodec<T> codec(Function<List<String>, T> factory, Function<T, List<String>> ids) {
        return RecordCodecBuilder.mapCodec(builder -> builder.group(
                Codec.STRING.listOf().optionalFieldOf("ids", List.of()).forGetter(ids),
                Codec.STRING.optionalFieldOf("id", "").forGetter(condition -> {
                    List<String> list = ids.apply(condition);
                    return list.isEmpty() ? "" : list.getFirst();
                })
        ).apply(builder, (many, one) -> factory.apply(resolve(many, one))));
    }

    public static List<String> resolve(List<String> many, String one) {
        if (!many.isEmpty()) return many;
        return one.isEmpty() ? List.of() : List.of(one);
    }

    /** True when every id passes; an empty condition passes. */
    public static boolean test(List<String> ids, Predicate<String> available) {
        for (String id : ids) {
            if (!available.test(id)) return false;
        }
        return true;
    }
}
