package dev.averageanime.lib.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Insertion order is the match order: {@code chocolate_cake} must come before {@code cake}. */
public final class PatternRegistry<T> {

    private final Map<String, List<T>> patterns;
    private final Set<String> excluded;
    private final List<String> skipPatterns;

    private PatternRegistry(Map<String, List<T>> patterns, Set<String> excluded, List<String> skipPatterns) {
        this.patterns = Collections.unmodifiableMap(patterns);
        this.excluded = Set.copyOf(excluded);
        this.skipPatterns = List.copyOf(skipPatterns);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** @return values from the first pattern {@code name} contains, empty if excluded or unmatched */
    public List<T> find(String name) {
        if (!accepts(name)) return List.of();
        for (Map.Entry<String, List<T>> entry : patterns.entrySet()) {
            if (name.contains(entry.getKey())) return entry.getValue();
        }
        return List.of();
    }

    public boolean accepts(String name) {
        if (excluded.contains(name)) return false;
        for (String skip : skipPatterns) {
            if (name.contains(skip)) return false;
        }
        return true;
    }

    public static final class Builder<T> {
        private final Map<String, List<T>> patterns = new LinkedHashMap<>();
        private final java.util.Set<String> excluded = new java.util.HashSet<>();
        private final java.util.List<String> skipPatterns = new java.util.ArrayList<>();

        /** Order matters: put a specific pattern before the general one it would otherwise shadow. */
        @SafeVarargs
        public final Builder<T> pattern(String pattern, T... values) {
            patterns.put(pattern, List.of(values));
            return this;
        }

        /** Exact names that match a pattern but should not be treated as part of its family. */
        public Builder<T> exclude(String... names) {
            excluded.addAll(List.of(names));
            return this;
        }

        /** Substrings that disqualify a name outright, before patterns are tried. */
        public Builder<T> skip(String... patterns) {
            skipPatterns.addAll(List.of(patterns));
            return this;
        }

        public PatternRegistry<T> build() {
            return new PatternRegistry<>(patterns, excluded, skipPatterns);
        }
    }
}
