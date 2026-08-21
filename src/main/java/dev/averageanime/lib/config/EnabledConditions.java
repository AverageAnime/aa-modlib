package dev.averageanime.lib.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The shared half of a recipe condition that gates on whether content is present and enabled.
 *
 * <p>NeoForge and Fabric each have their own condition interface, so the record implementing it has to
 * be written twice — but the JSON shape and the test are the same on both sides, and that is what lives
 * here.
 *
 * <p>The shape accepts either a single {@code id} or a list of {@code ids}, because most conditions gate
 * on one thing and spelling that as a one-element array in every recipe file would be noise.
 */
public final class EnabledConditions {

    private EnabledConditions() {}

    /**
     * Builds the codec for a loader's condition record.
     *
     * @param factory wraps the resolved id list in that loader's record type
     */
    public static <T> MapCodec<T> codec(Function<List<String>, T> factory, Function<T, List<String>> ids) {
        return RecordCodecBuilder.mapCodec(builder -> builder.group(
                Codec.STRING.listOf().optionalFieldOf("ids", List.of()).forGetter(ids),
                Codec.STRING.optionalFieldOf("id", "").forGetter(condition -> {
                    List<String> list = ids.apply(condition);
                    return list.isEmpty() ? "" : list.getFirst();
                })
        ).apply(builder, (many, one) -> factory.apply(resolve(many, one))));
    }

    /** Prefers the list form; falls back to the single id; empty when neither is given. */
    public static List<String> resolve(List<String> many, String one) {
        if (!many.isEmpty()) return many;
        return one.isEmpty() ? List.of() : List.of(one);
    }

    /**
     * True when every id passes. An empty condition passes: a recipe that names nothing is not asking
     * for anything.
     */
    public static boolean test(List<String> ids, Predicate<String> available) {
        for (String id : ids) {
            if (!available.test(id)) return false;
        }
        return true;
    }
}
