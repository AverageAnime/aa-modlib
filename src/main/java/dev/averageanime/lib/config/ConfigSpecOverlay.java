package dev.averageanime.lib.config;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Each default is offered to {@link Defaults} keyed by the option's dotted path; wrapped once rather than threading an argument through every {@code define} call. */
public final class ConfigSpecOverlay {

    /** Supplies the default an option is finally declared with, given the one the schema asked for. */
    public interface Defaults {

        /** {@code validator} is the option's own element validator, for rejecting a bad contribution early. */
        List<String> list(String dottedKey, List<String> builtIn, Predicate<Object> validator);

        boolean bool(String dottedKey, boolean builtIn);

        int integer(String dottedKey, int builtIn);

        /**
         * Called once the schema is fully declared, with every key it declared. A contribution naming a key
         * absent from this set reached nothing -- almost always a typo, and otherwise silently inert.
         */
        default void declared(Set<String> dottedKeys) {}
    }

    private ConfigSpecOverlay() {}

    public static ConfigSpecBuilder wrap(ConfigSpecBuilder delegate, Defaults defaults) {
        return new Overlaid(delegate, defaults);
    }

    /** Call after the last spec is built, so a contribution naming a key nobody declares can be reported rather than silently ignored. */
    public static void reportDeclared(ConfigSpecBuilder... wrapped) {
        Set<String> all = new LinkedHashSet<>();
        Defaults defaults = null;
        for (ConfigSpecBuilder builder : wrapped) {
            if (!(builder instanceof Overlaid overlaid)) continue;
            all.addAll(overlaid.defined);
            defaults = overlaid.defaults;
        }
        if (defaults != null) defaults.declared(Set.copyOf(all));
    }

    private static final class Overlaid implements ConfigSpecBuilder {

        private final ConfigSpecBuilder delegate;
        private final Defaults defaults;
        private final Deque<String> path = new ArrayDeque<>();
        private final Set<String> defined = new LinkedHashSet<>();

        Overlaid(ConfigSpecBuilder delegate, Defaults defaults) {
            this.delegate = delegate;
            this.defaults = defaults;
        }

        @Override
        public void push(String segment) {
            delegate.push(segment);
            path.addLast(segment);
        }

        @Override
        public void pop() {
            delegate.pop();
            if (!path.isEmpty()) path.removeLast();
        }

        @Override
        public Supplier<Boolean> defineBool(String key, boolean defaultValue) {
            String dotted = dotted(key);
            return delegate.defineBool(key, defaults.bool(dotted, defaultValue));
        }

        @Override
        public Supplier<Integer> defineInt(String key, int defaultValue, int min, int max, boolean gameRestart) {
            String dotted = dotted(key);
            return delegate.defineInt(key, defaults.integer(dotted, defaultValue), min, max, gameRestart);
        }

        @Override
        public Supplier<List<? extends String>> defineList(String key, List<String> defaultValue,
                Supplier<String> elementHint, Predicate<Object> elementValidator, boolean gameRestart) {
            String dotted = dotted(key);
            return delegate.defineList(key, defaults.list(dotted, defaultValue, elementValidator),
                    elementHint, elementValidator, gameRestart);
        }

        /** The option's full key: every open section, then the option's own name. */
        private String dotted(String key) {
            if (path.isEmpty()) {
                defined.add(key);
                return key;
            }
            List<String> parts = new ArrayList<>(path);
            parts.add(key);
            String dotted = String.join(".", parts);
            defined.add(dotted);
            return dotted;
        }
    }
}
