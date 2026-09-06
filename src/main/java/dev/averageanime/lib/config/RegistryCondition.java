package dev.averageanime.lib.config;

import dev.averageanime.lib.platform.ModPlatform;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A trailing {@code @} clause gating whether an entry registers, e.g. {@code tin_dust@absent:alltheores}. Clauses chain and all must hold. */
public record RegistryCondition(Kind kind, List<String> modIds, List<RegistryCondition> and) {

    public enum Kind {
        ALWAYS,
        REQUIRE_PRESENT,
        REQUIRE_ANY,
        REQUIRE_ABSENT
    }

    public RegistryCondition(Kind kind, List<String> modIds) {
        this(kind, modIds, List.of());
    }

    public static final RegistryCondition ALWAYS = new RegistryCondition(Kind.ALWAYS, List.of());

    private static final char SEPARATOR = '@';

    /** {@code payload} is the line with any {@code @} clause removed. */
    public record Split(String payload, RegistryCondition condition) {}

    /** @return the split, or null if a clause is malformed and the whole entry should be skipped */
    public static Split split(String entry, Logger logger) {
        int at = entry.indexOf(SEPARATOR);
        if (at < 0) return new Split(entry, ALWAYS);

        String payload = entry.substring(0, at);

        List<RegistryCondition> clauses = new ArrayList<>();
        for (String clause : entry.substring(at + 1).split(String.valueOf(SEPARATOR), -1)) {
            RegistryCondition condition = parseClause(clause, entry, logger);
            if (condition == null) return null;
            if (condition.kind != Kind.ALWAYS) clauses.add(condition);
        }
        if (clauses.isEmpty()) return new Split(payload, ALWAYS);

        RegistryCondition first = clauses.get(0);
        return new Split(payload, new RegistryCondition(first.kind, first.modIds,
                List.copyOf(clauses.subList(1, clauses.size()))));
    }

    /** @return the clause on its own, or null if it is malformed (already logged) */
    private static RegistryCondition parseClause(String clause, String entry, Logger logger) {
        if (clause.equals("always")) return ALWAYS;

        int colon = clause.indexOf(':');
        if (colon < 0 || colon == clause.length() - 1) {
            logger.warn("Skipping entry: expected @mod:, @any:, @absent: or @always, got: {}", entry);
            return null;
        }

        String keyword = clause.substring(0, colon);
        Kind kind = switch (keyword) {
            case "mod" -> Kind.REQUIRE_PRESENT;
            case "any" -> Kind.REQUIRE_ANY;
            case "absent" -> Kind.REQUIRE_ABSENT;
            default -> null;
        };
        if (kind == null) {
            logger.warn("Skipping entry with an unknown condition '{}': {}", keyword, entry);
            return null;
        }

        List<String> modIds = new ArrayList<>();
        for (String part : clause.substring(colon + 1).split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) modIds.add(trimmed);
        }
        if (modIds.isEmpty()) {
            logger.warn("Skipping entry with an empty condition: {}", entry);
            return null;
        }
        return new RegistryCondition(kind, List.copyOf(modIds));
    }

    /** Every clause in the chain must hold. Always true during datagen. */
    public boolean isSatisfied(ModPlatform platform) {
        if (kind == Kind.ALWAYS && and.isEmpty()) return true;
        if (platform.isRunningDataGen()) return true;

        if (!holds(platform)) return false;
        for (RegistryCondition part : and) {
            if (!part.holds(platform)) return false;
        }
        return true;
    }

    /** This clause alone, ignoring the rest of the chain and the datagen exemption. */
    private boolean holds(ModPlatform platform) {
        if (kind == Kind.ALWAYS) return true;

        if (kind == Kind.REQUIRE_PRESENT) {
            for (String modId : modIds) {
                if (!platform.isModLoaded(modId)) return false;
            }
            return true;
        }
        if (kind == Kind.REQUIRE_ANY) {
            for (String modId : modIds) {
                if (platform.isModLoaded(modId)) return true;
            }
            return false;
        }
        for (String modId : modIds) {
            if (platform.isModLoaded(modId)) return false;
        }
        return true;
    }

    public String reason(ModPlatform platform) {
        if (!holds(platform)) return ownReason(platform);
        for (RegistryCondition part : and) {
            if (!part.holds(platform)) return part.ownReason(platform);
        }
        return ownReason(platform);
    }

    private String ownReason(ModPlatform platform) {
        if (kind == Kind.REQUIRE_PRESENT) return String.join(" and ", modIds) + " is not installed";
        if (kind == Kind.REQUIRE_ANY) return "none of " + String.join(", ", modIds) + " is installed";
        for (String modId : modIds) {
            if (platform.isModLoaded(modId)) return modId + " already provides it";
        }
        return String.join(" or ", modIds) + " already provides it";
    }

    /** Copies a default entry's {@code @} clauses onto a stored entry that has none. {@code idOf} reads the entry's name out of a line. */
    public static List<String> inherit(List<String> entries, List<String> defaults,
                                       java.util.function.UnaryOperator<String> idOf, Logger logger) {
        Map<String, String> clauses = new HashMap<>();
        for (String entry : defaults) {
            int at = entry.indexOf(SEPARATOR);
            if (at < 0) continue;
            clauses.put(idOf.apply(entry.substring(0, at)), entry.substring(at));
        }
        if (clauses.isEmpty()) return entries;

        List<String> merged = new ArrayList<>(entries.size());
        int inherited = 0;
        for (String entry : entries) {
            if (entry.indexOf(SEPARATOR) >= 0) {
                merged.add(entry);
                continue;
            }
            String clause = clauses.get(idOf.apply(entry));
            if (clause == null) {
                merged.add(entry);
            } else {
                merged.add(entry + clause);
                inherited++;
            }
        }
        if (inherited > 0) {
            logger.info("Applied {} default registration conditions to config entries that predate them",
                    inherited);
        }
        return merged;
    }
}
