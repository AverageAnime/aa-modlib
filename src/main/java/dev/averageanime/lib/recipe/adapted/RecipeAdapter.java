package dev.averageanime.lib.recipe.adapted;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Copies recipes whose type belongs to an absent mod onto the matching adapted type, so plate and bowl
 * interactions can run them anyway. Runs on the raw recipe JSON before either loader evaluates
 * conditions, so every condition except the absent mod's own top-level {@code mod_loaded} is still
 * honoured by the loader itself.
 *
 * <p>The originals are untouched and still dropped by their own conditions. With the source mod
 * installed nothing is copied.
 */
public final class RecipeAdapter {

    private static final Logger LOGGER = LogUtils.getLogger();

    private record Rule(String sourceType, String mod, AdaptedRecipeTypes.Entry<?> target, Predicate<JsonObject> eligible) {}

    private static final List<Rule> RULES = List.of(
            new Rule("create:filling", "create", AdaptedRecipeTypes.FILLING, r -> true),
            new Rule("create:emptying", "create", AdaptedRecipeTypes.EMPTYING, r -> true),
            new Rule("create:deploying", "create", AdaptedRecipeTypes.DEPLOYING, r -> true),
            new Rule("create:pressing", "create", AdaptedRecipeTypes.PRESSING, PlateRecipeEligibility::pressingJson),
            new Rule("create:compacting", "create", AdaptedRecipeTypes.COMPACTING, PlateRecipeEligibility::compactingJson),
            new Rule("farmersdelight:cutting", "farmersdelight", AdaptedRecipeTypes.CUTTING, r -> true),
            new Rule("ubesdelight:baking_mat", "ubesdelight", AdaptedRecipeTypes.BAKING_MAT, PlateRecipeEligibility::bakingMatJson));

    private static final String NEOFORGE_CONDITIONS = "neoforge:conditions";
    private static final String FABRIC_CONDITIONS = "fabric:load_conditions";

    private static volatile String modId;
    private static volatile Predicate<String> isModLoaded = id -> true;

    private RecipeAdapter() {}

    /** Called by the loader's {@code AdaptedRecipes.register}, after which every recipe reload adapts. */
    public static void install(String modId, Predicate<String> isModLoaded) {
        RecipeAdapter.modId = modId;
        RecipeAdapter.isModLoaded = isModLoaded;
    }

    /** @param recipes the recipe manager's prepared map; copies are added to it in place */
    public static void adapt(Map<ResourceLocation, JsonElement> recipes) {
        String namespace = modId;
        if (namespace == null) return;

        Map<String, Rule> active = new HashMap<>();
        for (Rule rule : RULES) {
            if (!isModLoaded.test(rule.mod())) active.put(rule.sourceType(), rule);
        }
        if (active.isEmpty()) return;

        Map<ResourceLocation, JsonElement> copies = new HashMap<>();
        int ineligible = 0, unresolved = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : recipes.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject recipe)) continue;
            // Top-level type only: sequenced assembly nests these types as steps, and is not adapted.
            if (!(recipe.get("type") instanceof JsonElement type) || !type.isJsonPrimitive()) continue;
            Rule rule = active.get(type.getAsString());
            if (rule == null) continue;

            if (!rule.eligible().test(recipe)) {
                ineligible++;
                continue;
            }
            JsonObject copy = recipe.deepCopy();
            copy.addProperty("type", namespace + ":" + rule.target().path());
            dropModLoaded(copy, rule.mod());
            if (!dropForeignTools(copy, rule.mod()) || !idsResolve(copy)) {
                unresolved++;
                continue;
            }
            ResourceLocation id = entry.getKey();
            copies.put(ResourceLocation.fromNamespaceAndPath(namespace,
                    "adapted/" + id.getNamespace() + "/" + id.getPath()), copy);
        }
        recipes.putAll(copies);
        LOGGER.info("Adapted {} recipes for absent mods; skipped {} a plate cannot reproduce and {} naming missing content",
                copies.size(), ineligible, unresolved);
    }

    /** Top-level entries only: a mod_loaded nested in not/or/and is left to evaluate as written. */
    private static void dropModLoaded(JsonObject recipe, String mod) {
        if (recipe.get(NEOFORGE_CONDITIONS) instanceof JsonArray conditions) {
            JsonArray kept = new JsonArray();
            for (JsonElement condition : conditions) {
                boolean isMod = condition instanceof JsonObject c
                        && "neoforge:mod_loaded".equals(string(c, "type"))
                        && mod.equals(string(c, "modid"));
                if (!isMod) kept.add(condition);
            }
            recipe.add(NEOFORGE_CONDITIONS, kept);
        }
        if (recipe.get(FABRIC_CONDITIONS) instanceof JsonArray conditions) {
            JsonArray kept = new JsonArray();
            for (JsonElement condition : conditions) {
                if (condition instanceof JsonObject c
                        && "fabric:all_mods_loaded".equals(string(c, "condition"))
                        && c.get("values") instanceof JsonArray values) {
                    JsonArray remaining = new JsonArray();
                    for (JsonElement value : values) {
                        if (!(value.isJsonPrimitive() && mod.equals(value.getAsString()))) remaining.add(value);
                    }
                    if (remaining.isEmpty()) continue;
                    c.add("values", remaining);
                }
                kept.add(condition);
            }
            recipe.add(FABRIC_CONDITIONS, kept);
        }
    }

    /**
     * Drops tool ingredients of the absent mod's own types (Farmer's Delight's {@code item_ability}), which
     * cannot parse without it.
     *
     * @return false when nothing usable is left, since a recipe with no tool would accept any held item
     */
    private static boolean dropForeignTools(JsonObject recipe, String mod) {
        JsonElement tool = recipe.get("tool");
        if (tool == null) return true;
        if (tool instanceof JsonObject single) return !isModType(single, mod);
        if (tool instanceof JsonArray options) {
            JsonArray kept = new JsonArray();
            for (JsonElement option : options) {
                if (!(option instanceof JsonObject o && isModType(o, mod))) kept.add(option);
            }
            recipe.add("tool", kept);
            return !kept.isEmpty();
        }
        return true;
    }

    private static boolean isModType(JsonObject ingredient, String mod) {
        String type = string(ingredient, "type");
        return type != null && type.startsWith(mod + ":");
    }

    /**
     * Whether every literal item or fluid id outside the conditions is registered. A copy naming the absent
     * mod's own content could never load, and would only log a parse error. Tags are not checked: an empty
     * tag just matches nothing.
     */
    private static boolean idsResolve(JsonObject recipe) {
        for (Map.Entry<String, JsonElement> field : recipe.entrySet()) {
            String key = field.getKey();
            if (key.equals(NEOFORGE_CONDITIONS) || key.equals(FABRIC_CONDITIONS)) continue;
            if (!idsResolveIn(field.getValue())) return false;
        }
        return true;
    }

    private static boolean idsResolveIn(JsonElement element) {
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                if (!idsResolveIn(child)) return false;
            }
        } else if (element instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> field : object.entrySet()) {
                String key = field.getKey();
                JsonElement value = field.getValue();
                if ((key.equals("item") || key.equals("id") || key.equals("fluid"))
                        && value.isJsonPrimitive() && !isRegistered(value.getAsString())) {
                    return false;
                }
                if (!idsResolveIn(value)) return false;
            }
        }
        return true;
    }

    private static boolean isRegistered(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id != null && (BuiltInRegistries.ITEM.containsKey(id) || BuiltInRegistries.FLUID.containsKey(id));
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
