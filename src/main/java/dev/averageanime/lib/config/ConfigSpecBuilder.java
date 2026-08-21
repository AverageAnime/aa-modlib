package dev.averageanime.lib.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A loader-neutral way to declare a config spec.
 *
 * <p>NeoForge and Fabric both have a builder for this, with different types and different notions of
 * what a list element may be. A mod describes its options once against this interface and each loader
 * supplies a thin adapter, so the schema itself is shared and only the adapters are written twice.
 *
 * <p>Values come back as {@link Supplier} rather than as plain values because a config can reload at
 * runtime; reading through the supplier picks up the new value without re-registering anything.
 */
public interface ConfigSpecBuilder {

    void push(String path);

    void pop();

    Supplier<Boolean> defineBool(String key, boolean defaultValue);

    Supplier<Integer> defineInt(String key, int defaultValue, int min, int max, boolean gameRestart);

    /**
     * @param elementHint      one-line description of an element's format, shown by loaders that can
     * @param elementValidator rejects a malformed element before it reaches registration
     * @param gameRestart      whether changing this requires a restart; registration lists always do
     */
    Supplier<List<? extends String>> defineList(String key, List<String> defaultValue,
            Supplier<String> elementHint, Predicate<Object> elementValidator, boolean gameRestart);
}
