package dev.averageanime.lib.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.averageanime.lib.platform.AddonSource;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Finds addon specs in loaded mod files and turns them into config entries.
 *
 * <p>The mechanism is the same for any mod: probe every mod file for a well-known JSON resource, read
 * the sections it declares, validate each entry, and hand back strings in the pipe-delimited form the
 * config lists already use. What differs per mod is the <em>shape</em> of an entry -- whether an item
 * carries nutrition or a melting point -- so that part is supplied as a {@link Section}.
 *
 * <p>Entries are validated with the same predicates the config file uses, so a malformed addon produces
 * a log line naming the addon and section rather than a failure deep inside registration.
 *
 * <p>Loading is lazy and happens once. The first read must come late enough that mod discovery has
 * finished, which on both loaders means mod construction onward.
 */
public final class AddonSpecLoader {

    /**
     * Turns one JSON entry into zero or more config strings.
     *
     * <p>Zero is a legitimate result: an entry the reader does not recognise should be skipped rather
     * than guessed at.
     */
    public interface Section {
        List<String> flatten(JsonObject entry);
    }

    private record Registered(String name, Predicate<Object> validator, Section section) {}

    private final String resourcePath;
    private final String rootsProperty;
    private final int supportedSpecVersion;
    private final Logger logger;
    private final Function<String, List<AddonSource>> finder;
    private final List<Registered> sections = new ArrayList<>();
    private final Map<String, List<String>> results = new LinkedHashMap<>();

    private boolean loaded;

    /**
     * @param resourcePath  probed in every mod file, e.g. {@code yourmod/addon.json}. Keep it outside
     *                      {@code assets/} and {@code data/} so no pack machinery touches it
     * @param rootsProperty system property naming extra directories to scan, so an addon that is still
     *                      a source tree is picked up in dev without building a jar
     * @param finder        locates the resource in loaded mod files, normally the consuming mod's
     *                      platform service. Passed in rather than reached for, since the library has no
     *                      handle on which service that is
     */
    public AddonSpecLoader(String resourcePath, String rootsProperty, int supportedSpecVersion,
                           Logger logger, Function<String, List<AddonSource>> finder) {
        this.resourcePath = resourcePath;
        this.rootsProperty = rootsProperty;
        this.supportedSpecVersion = supportedSpecVersion;
        this.logger = logger;
        this.finder = finder;
    }

    /** Declares a section and how to read one of its entries. Call before the first {@link #get}. */
    public AddonSpecLoader section(String name, Predicate<Object> validator, Section section) {
        sections.add(new Registered(name, validator, section));
        results.put(name, new ArrayList<>());
        return this;
    }

    /** Flattened entries for a section, in the order the specs declared them. */
    public List<String> get(String section) {
        load();
        return List.copyOf(results.getOrDefault(section, List.of()));
    }

    public String resourcePath() {
        return resourcePath;
    }

    public String rootsProperty() {
        return rootsProperty;
    }

    private synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (AddonSource source : sources()) {
            try (BufferedReader reader = Files.newBufferedReader(source.path(), StandardCharsets.UTF_8)) {
                read(source.modId(), JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception e) {
                logger.warn("Failed to read addon spec from {}", source.modId(), e);
            }
        }
    }

    /**
     * Mod files carrying the resource, plus anything named by the roots property.
     *
     * <p>The platform lookup is attempted rather than assumed: a bare-JVM verification tool has no
     * platform service, and supplies its specs through the property instead.
     */
    private List<AddonSource> sources() {
        List<AddonSource> found = new ArrayList<>();
        try {
            found.addAll(finder.apply(resourcePath));
        } catch (Throwable t) {
            logger.debug("No platform service; scanning addon roots only");
        }
        String roots = System.getProperty(rootsProperty);
        if (roots == null || roots.isBlank()) return found;
        for (String root : roots.split(File.pathSeparator)) {
            if (root.isBlank()) continue;
            Path candidate = Path.of(root.trim()).resolve(resourcePath);
            if (Files.exists(candidate)) found.add(new AddonSource("dev:" + root.trim(), candidate));
        }
        return found;
    }

    private void read(String modId, JsonObject root) {
        int version = root.has("spec_version") ? root.get("spec_version").getAsInt() : 0;
        if (version > supportedSpecVersion) {
            logger.warn("Addon {} declares spec_version {} but this build understands {}; "
                    + "unrecognised entries will be ignored", modId, version, supportedSpecVersion);
        }
        String addonId = root.has("addon_id") ? root.get("addon_id").getAsString() : modId;
        int accepted = 0;

        for (Registered registered : sections) {
            JsonElement raw = root.get(registered.name());
            if (raw == null || !raw.isJsonArray()) continue;
            for (JsonElement element : (JsonArray) raw) {
                if (!element.isJsonObject() && !element.isJsonPrimitive()) continue;
                JsonObject entry = element.isJsonObject()
                        ? element.getAsJsonObject()
                        : wrap(element);
                for (String flattened : registered.section().flatten(entry)) {
                    if (registered.validator() != null && !registered.validator().test(flattened)) {
                        logger.warn("Addon {} has an invalid {} entry, skipping: {}",
                                addonId, registered.name(), flattened);
                        continue;
                    }
                    results.get(registered.name()).add(flattened);
                    accepted++;
                }
            }
        }
        logger.info("Loaded addon spec {} ({} entries)", addonId, accepted);
    }

    /** A section may hold bare strings rather than objects; give the reader a uniform shape. */
    private static JsonObject wrap(JsonElement primitive) {
        JsonObject object = new JsonObject();
        object.add("value", primitive);
        return object;
    }
}
