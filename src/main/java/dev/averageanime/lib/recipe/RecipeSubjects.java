package dev.averageanime.lib.recipe;

import java.util.function.Predicate;

/** Everything is available until a consumer calls {@link #gate}, so a mod that never registers the condition behaves as though it did not exist. */
public final class RecipeSubjects {

    private static Predicate<String> available = id -> true;

    private RecipeSubjects() {}

    /** @param check true when {@code id} names something the mod both enabled in config and registered */
    public static void gate(Predicate<String> check) {
        available = check;
    }

    public static boolean isAvailable(String id) {
        return available.test(id);
    }
}
