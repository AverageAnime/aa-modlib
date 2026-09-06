package dev.averageanime.lib.tab;

import java.util.Set;
import java.util.function.Predicate;

public final class TabFilter {

    private final Set<String> excludedNames;
    private final Set<String> excludedSuffixes;
    private final Set<String> requiredSuffixes;
    private final Predicate<String> enabled;

    private TabFilter(Set<String> excludedNames, Set<String> excludedSuffixes,
                      Set<String> requiredSuffixes, Predicate<String> enabled) {
        this.excludedNames = Set.copyOf(excludedNames);
        this.excludedSuffixes = Set.copyOf(excludedSuffixes);
        this.requiredSuffixes = Set.copyOf(requiredSuffixes);
        this.enabled = enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @param alsoExclude names excluded only for this call, for a set not known until runtime */
    public boolean accepts(String path, Set<String> alsoExclude) {
        if (excludedNames.contains(path) || alsoExclude.contains(path)) return false;
        for (String suffix : excludedSuffixes) {
            if (path.endsWith(suffix)) return false;
        }
        if (!requiredSuffixes.isEmpty()) {
            boolean matched = false;
            for (String suffix : requiredSuffixes) {
                if (path.endsWith(suffix)) { matched = true; break; }
            }
            if (!matched) return false;
        }
        return enabled.test(path);
    }

    public boolean accepts(String path) {
        return accepts(path, Set.of());
    }

    public static final class Builder {
        private final java.util.Set<String> names = new java.util.HashSet<>();
        private final java.util.Set<String> excludedSuffixes = new java.util.HashSet<>();
        private final java.util.Set<String> requiredSuffixes = new java.util.HashSet<>();
        private Predicate<String> enabled = path -> true;

        public Builder exclude(String... paths) {
            names.addAll(java.util.List.of(paths));
            return this;
        }

        public Builder excludeSuffix(String... suffixes) {
            excludedSuffixes.addAll(java.util.List.of(suffixes));
            return this;
        }

        public Builder requireSuffix(String... suffixes) {
            requiredSuffixes.addAll(java.util.List.of(suffixes));
            return this;
        }

        /** The config check, applied last. Evaluated per call, so a config reload is picked up. */
        public Builder enabledWhen(Predicate<String> enabled) {
            this.enabled = enabled;
            return this;
        }

        public TabFilter build() {
            return new TabFilter(names, excludedSuffixes, requiredSuffixes, enabled);
        }
    }
}
