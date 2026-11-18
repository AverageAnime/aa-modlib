package dev.averageanime.lib.recipe;

import dev.averageanime.lib.recipe.adapted.ChanceOutput;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Each helper takes several candidate method names; every failure path returns a harmless default. */
public final class RecipeReflection {

    private RecipeReflection() {}

    /** Results of a processing recipe, rolling chance-gated outputs; plain result otherwise. */
    public static List<ItemStack> getRollableResults(Recipe<?> recipe, RandomSource random) {
        try {
            Method getRollable = recipe.getClass().getMethod("getRollableResults");
            List<?> rollable = (List<?>) getRollable.invoke(recipe);
            List<ItemStack> out = new ArrayList<>();
            for (Object entry : rollable) {
                float chance = invokeFloat(entry, "chance", "getChance");
                if (chance >= 1.0f || random.nextFloat() < chance) {
                    ItemStack stack = invokeStack(entry, "stack", "getStack");
                    if (!stack.isEmpty()) out.add(stack);
                }
            }
            if (!out.isEmpty()) return out;
        } catch (Exception ignored) {
        }

        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        return result.isEmpty() ? List.of() : List.of(result.copy());
    }

    /** Every possible result with its chance, unrolled; the plain result at chance 1 when there is no rollable list. */
    public static List<ChanceOutput> getChanceOutputs(Recipe<?> recipe) {
        try {
            Method getRollable = recipe.getClass().getMethod("getRollableResults");
            List<?> rollable = (List<?>) getRollable.invoke(recipe);
            List<ChanceOutput> out = new ArrayList<>();
            for (Object entry : rollable) {
                ItemStack stack = invokeStack(entry, "stack", "getStack");
                if (!stack.isEmpty()) out.add(new ChanceOutput(stack.copy(), invokeFloat(entry, "chance", "getChance")));
            }
            if (!out.isEmpty()) return out;
        } catch (Exception ignored) {
        }

        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        return result.isEmpty() ? List.of() : List.of(new ChanceOutput(result.copy(), 1.0f));
    }

    /**
     * Whether {@code tool} satisfies a recipe's {@code getTool()} (Farmer's Delight's cutting board, Ube's
     * Delight's baking mat), read by reflection so neither mod is a compile dependency. Permissive: a
     * recipe with no readable tool accepts any.
     */
    public static boolean toolMatches(Object recipe, ItemStack tool) {
        try {
            Object toolResult = recipe.getClass().getMethod("getTool").invoke(recipe);
            if (toolResult == null) return true;
            if (toolResult instanceof Optional<?> opt) return opt.isEmpty() || testIngredient(opt.get(), tool);
            if (toolResult instanceof List<?> list) {
                if (list.isEmpty()) return true;
                for (Object elem : list) {
                    if (testIngredient(elem, tool)) return true;
                }
                return false;
            }
            return testIngredient(toolResult, tool);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean testIngredient(Object ingredient, ItemStack tool) {
        try {
            if (ingredient instanceof Ingredient ing) return ing.test(tool);
            return (boolean) ingredient.getClass().getMethod("test", ItemStack.class).invoke(ingredient, tool);
        } catch (Exception e) {
            return true;
        }
    }

    public static float invokeFloat(Object target, String... methodNames) {
        for (String name : methodNames) {
            try { return (float) target.getClass().getMethod(name).invoke(target); }
            catch (NoSuchMethodException ignored) {}
            catch (Exception e) { return 1.0f; }
        }
        return 1.0f;
    }

    public static ItemStack invokeStack(Object target, String... methodNames) {
        for (String name : methodNames) {
            try { return (ItemStack) target.getClass().getMethod(name).invoke(target); }
            catch (NoSuchMethodException ignored) {}
            catch (Exception e) { return ItemStack.EMPTY; }
        }
        return ItemStack.EMPTY;
    }

    public static int invokeInt(Object target, int fallback, String... methodNames) {
        for (String name : methodNames) {
            try {
                Object value = target.getClass().getMethod(name).invoke(target);
                if (value instanceof Number number) return number.intValue();
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) { return fallback; }
        }
        return fallback;
    }

    public static Object invoke(Object target, String... methodNames) {
        for (String name : methodNames) {
            try { return target.getClass().getMethod(name).invoke(target); }
            catch (NoSuchMethodException ignored) {}
            catch (Exception e) { return null; }
        }
        return null;
    }
}
