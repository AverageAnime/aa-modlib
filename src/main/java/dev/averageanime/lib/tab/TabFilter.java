package dev.averageanime.lib.tab;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Decides which registry paths belong in a creative tab.
 *
 * <p>A mod registers a lot of things that should not all appear together: buckets belong in a fluid tab,
 * the empty container blocks are placement mechanics rather than content, and anything the config has
 * hidden should not show at all. Rather than each tab hand-rolling a chain of comparisons, a filter is
 * built from the three questions that actually get asked -- suffix, exact name, and a live enabled check.
 *
 * <p>Immutable and cheap to hold as a constant next to the tab it filters.
 */
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

    /**
     * @param path        registry path, without a namespace
     * @param alsoExclude names excluded only for this call, for a set that is not known until runtime
     */
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

        /** Exact registry paths to keep out, e.g. the empty container blocks. */
        public Builder exclude(String... paths) {
            names.addAll(java.util.List.of(paths));
            return this;
        }

        /** Paths ending this way are kept out, e.g. {@code _bucket} in a non-fluid tab. */
        public Builder excludeSuffix(String... suffixes) {
            excludedSuffixes.addAll(java.util.List.of(suffixes));
            return this;
        }

        /** When set, a path must end one of these ways to be accepted at all. */
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
