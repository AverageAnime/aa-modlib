package dev.averageanime.lib.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public sealed interface LoadCondition {

    JsonObject toNeoForge();

    JsonObject toFabric();

    static LoadCondition modLoaded(String modId) {
        return new ModLoaded(modId);
    }

    static LoadCondition modAbsent(String modId) {
        return not(modLoaded(modId));
    }

    static LoadCondition not(LoadCondition inner) {
        return new Not(inner);
    }

    /** True when something has filled {@code tag}. Preferred over naming a mod where a convention tag exists. */
    static LoadCondition tagPopulated(String registry, String tag) {
        return new TagPopulated(registry, tag);
    }

    /** The consumer mod's own {@code <modId>:enabled} condition: true when every id names something it registered. */
    static LoadCondition enabled(String modId, String... ids) {
        return new Enabled(modId, List.of(ids));
    }

    /** Both loaders read a top-level array as an AND. */
    static JsonArray neoForgeArray(List<LoadCondition> conditions) {
        JsonArray array = new JsonArray();
        for (LoadCondition condition : conditions) array.add(condition.toNeoForge());
        return array;
    }

    static JsonArray fabricArray(List<LoadCondition> conditions) {
        JsonArray array = new JsonArray();
        for (LoadCondition condition : conditions) array.add(condition.toFabric());
        return array;
    }

    record ModLoaded(String modId) implements LoadCondition {

        @Override
        public JsonObject toNeoForge() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:mod_loaded");
            json.addProperty("modid", modId);
            return json;
        }

        @Override
        public JsonObject toFabric() {
            JsonObject json = new JsonObject();
            json.addProperty("condition", "fabric:all_mods_loaded");
            JsonArray values = new JsonArray();
            values.add(modId);
            json.add("values", values);
            return json;
        }
    }

    record Not(LoadCondition inner) implements LoadCondition {

        @Override
        public JsonObject toNeoForge() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:not");
            json.add("value", inner.toNeoForge());
            return json;
        }

        @Override
        public JsonObject toFabric() {
            JsonObject json = new JsonObject();
            json.addProperty("condition", "fabric:not");
            json.add("value", inner.toFabric());
            return json;
        }
    }

    record TagPopulated(String registry, String tag) implements LoadCondition {

        // NeoForge has no positive form, and its tag_empty hardcodes the item registry.
        @Override
        public JsonObject toNeoForge() {
            JsonObject empty = new JsonObject();
            empty.addProperty("type", "neoforge:tag_empty");
            empty.addProperty("tag", tag);

            JsonObject json = new JsonObject();
            json.addProperty("type", "neoforge:not");
            json.add("value", empty);
            return json;
        }

        @Override
        public JsonObject toFabric() {
            JsonObject json = new JsonObject();
            json.addProperty("condition", "fabric:tags_populated");
            json.addProperty("registry", registry);
            JsonArray values = new JsonArray();
            values.add(tag);
            json.add("values", values);
            return json;
        }
    }

    record Enabled(String modId, List<String> ids) implements LoadCondition {

        @Override
        public JsonObject toNeoForge() {
            JsonObject json = new JsonObject();
            json.addProperty("type", modId + ":enabled");
            return write(json);
        }

        @Override
        public JsonObject toFabric() {
            JsonObject json = new JsonObject();
            json.addProperty("condition", modId + ":enabled");
            return write(json);
        }

        // The codec on the other side accepts either spelling; one id keeps the shorter one.
        private JsonObject write(JsonObject json) {
            if (ids.size() == 1) {
                json.addProperty("id", ids.get(0));
                return json;
            }
            JsonArray values = new JsonArray();
            for (String id : ids) values.add(id);
            json.add("ids", values);
            return json;
        }
    }
}
