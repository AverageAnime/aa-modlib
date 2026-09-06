package dev.averageanime.lib.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** A loader-neutral config spec declaration; values come back as {@link Supplier} so a reload is picked up. */
public interface ConfigSpecBuilder {

    void push(String path);

    void pop();

    Supplier<Boolean> defineBool(String key, boolean defaultValue);

    Supplier<Integer> defineInt(String key, int defaultValue, int min, int max, boolean gameRestart);

    /** {@code elementHint} describes an element's format; {@code elementValidator} rejects a malformed element; registration lists always require a restart. */
    Supplier<List<? extends String>> defineList(String key, List<String> defaultValue,
            Supplier<String> elementHint, Predicate<Object> elementValidator, boolean gameRestart);
}
