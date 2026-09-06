package dev.averageanime.lib.config;

import io.github.fabricators_of_create.porting_lib.config.ModConfigSpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Restart flags and element hints are accepted and dropped. */
public final class FabricConfigSpec {

    private FabricConfigSpec() {}

    public static ConfigSpecBuilder adapt(ModConfigSpec.Builder b) {
        return new ConfigSpecBuilder() {
            @Override public void push(String path) { b.push(path); }
            @Override public void pop() { b.pop(); }

            @Override
            public Supplier<Boolean> defineBool(String key, boolean defaultValue) {
                return b.define(key, defaultValue)::get;
            }

            @Override
            public Supplier<Integer> defineInt(String key, int defaultValue, int min, int max, boolean gameRestart) {
                return b.defineInRange(key, defaultValue, min, max)::get;
            }

            @Override
            public Supplier<List<? extends String>> defineList(String key, List<String> defaultValue,
                    Supplier<String> elementHint, Predicate<Object> elementValidator, boolean gameRestart) {
                return b.defineListAllowEmpty(key, defaultValue, elementValidator)::get;
            }
        };
    }
}
