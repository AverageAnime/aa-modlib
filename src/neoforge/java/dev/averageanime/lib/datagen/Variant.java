package dev.averageanime.lib.datagen;

import java.util.ArrayList;
import java.util.List;

/** {@code suffix} keeps sibling variants from overwriting each other. */
public record Variant<T>(T value, List<LoadCondition> conditions, String suffix) {

    /** A null owner ends the list. */
    public static <T> List<Variant<T>> exclusive(List<T> candidates, List<String> owners) {
        List<Variant<T>> variants = new ArrayList<>();
        List<LoadCondition> earlierAbsent = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            String owner = owners.get(i);
            List<LoadCondition> conditions = new ArrayList<>(earlierAbsent);
            if (owner != null) {
                conditions.add(LoadCondition.modLoaded(owner));
                variants.add(new Variant<>(candidates.get(i), List.copyOf(conditions), "_from_" + owner));
                earlierAbsent.add(LoadCondition.modAbsent(owner));
            } else {
                // Unconditional: it takes whatever case is left, and nothing after it is reachable.
                variants.add(new Variant<>(candidates.get(i), List.copyOf(conditions), ""));
                break;
            }
        }
        return variants;
    }
}
