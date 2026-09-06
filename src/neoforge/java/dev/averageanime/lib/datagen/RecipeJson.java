package dev.averageanime.lib.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/** Builders for the recipe JSON shapes Create and its addons read. Amounts are the caller's business. */
public final class RecipeJson {

    private RecipeJson() {}

    public static JsonObject recipe(String type, List<LoadCondition> conditions) {
        JsonObject json = new JsonObject();
        json.add("neoforge:conditions", LoadCondition.neoForgeArray(conditions));
        json.add("fabric:load_conditions", LoadCondition.fabricArray(conditions));
        json.addProperty("type", type);
        return json;
    }

    public static JsonObject fluidIngredient(String fluidTag, int amount) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:tag");
        json.addProperty("amount", amount);
        json.addProperty("tag", fluidTag);
        return json;
    }

    public static JsonObject concreteFluidIngredient(String fluidId, int amount) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:single");
        json.addProperty("amount", amount);
        json.addProperty("fluid", fluidId);
        return json;
    }

    public static JsonObject itemIngredient(String itemId) {
        JsonObject json = new JsonObject();
        json.addProperty("item", itemId);
        return json;
    }

    public static JsonObject tagIngredient(String tag) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tag);
        return json;
    }

    public static JsonObject fluidResult(String fluidId, int amount) {
        JsonObject json = new JsonObject();
        json.addProperty("amount", amount);
        json.addProperty("id", fluidId);
        return json;
    }

    public static JsonObject itemResult(String itemId) {
        JsonObject json = new JsonObject();
        json.addProperty("id", itemId);
        return json;
    }

    public static JsonObject itemResult(String itemId, int count) {
        JsonObject json = itemResult(itemId);
        json.addProperty("count", count);
        return json;
    }

    /** Recipes whose result is a tag land on whichever mod's item is present. */
    public static JsonObject tagResult(String tag) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tag);
        return json;
    }

    public static JsonObject chanceResult(String itemId, double chance) {
        JsonObject json = itemResult(itemId);
        json.addProperty("chance", chance);
        return json;
    }

    public static JsonObject chanceResult(String itemId, double chance, int count) {
        JsonObject json = chanceResult(itemId, chance);
        json.addProperty("count", count);
        return json;
    }

    public static JsonArray stringArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    public static JsonArray array(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) array.add(entry);
        return array;
    }

    public static JsonArray array(List<JsonObject> entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) array.add(entry);
        return array;
    }
}
