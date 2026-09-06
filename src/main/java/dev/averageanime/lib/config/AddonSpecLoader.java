package dev.averageanime.lib.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.averageanime.lib.platform.AddonSource;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Finds addon specs and turns them into pipe-delimited config entries, remembering which addon supplied
 * each one. Specs come from three places: classpath resources named by {@link #builtin}, the
 * {@code resourcePath} probed in every loaded mod file, and directories named by the
 * {@code rootsProperty} system property.
 *
 * <p>Loading is lazy and happens once, so the first read must come no earlier than mod construction.
 */
public final class AddonSpecLoader {

    /** Receives the config entries one spec entry flattens into; the section named need not be the declaring one. */
    public interface Sink {
        void add(String section, String value);
    }

    /** Turns one JSON entry into config strings; adding nothing legitimately means "skipped". */
    public interface Section {
        void flatten(JsonObject entry, Sink sink);
    }

    /** One loaded spec. {@code origin} says where it came from, for logs. */
    public record Addon(String addonId, String name, int specVersion,
                        boolean defaultEnabled, boolean builtIn, String origin) {}

    /** A default an addon proposes for a config option the consuming mod already defines. */
    public record ConfigContribution(String addonId, String file, String key, JsonElement value) {}

    private record Registered(String name, Predicate<Object> validator, Section section) {}

    /** A spec located but not yet read. */
    private record Candidate(String modId, String origin, boolean builtIn, Opener opener) {}

    private interface Opener {
        Reader open() throws IOException;
    }

    private final String resourcePath;
    private final String rootsProperty;
    private final int supportedSpecVersion;
    private final Logger logger;
    private final Function<String, List<AddonSource>> finder;
    private final List<Registered> sections = new ArrayList<>();
    private final List<String> builtinResources = new ArrayList<>();

    /** addonId to section to entries. Iteration order is load order. */
    private final Map<String, Map<String, List<String>>> byAddon = new LinkedHashMap<>();
    private final List<Addon> addons = new ArrayList<>();
    private final List<ConfigContribution> contributions = new ArrayList<>();

    private Predicate<String> gate = addonId -> true;
    private boolean loaded;

    /** {@code resourcePath} is probed in every mod file; {@code rootsProperty} names extra directories to scan. */
    public AddonSpecLoader(String resourcePath, String rootsProperty, int supportedSpecVersion,
                           Logger logger, Function<String, List<AddonSource>> finder) {
        this.resourcePath = resourcePath;
        this.rootsProperty = rootsProperty;
        this.supportedSpecVersion = supportedSpecVersion;
        this.logger = logger;
        this.finder = finder;
    }

    /** Call before the first {@link #get}. */
    public AddonSpecLoader section(String name, Predicate<Object> validator, Section section) {
        sections.add(new Registered(name, validator, section));
        return this;
    }

    /**
     * A spec the consuming mod ships itself, read from the classpath rather than probed for. The classpath
     * is used because a mod's own resources are not reliably discoverable through the platform's mod-file
     * lookup in a dev run, and because a bare-JVM tool has no platform at all.
     */
    public AddonSpecLoader builtin(String classpathResource) {
        builtinResources.add(classpathResource);
        return this;
    }

    /** Installs the test deciding which addons {@link #get(String)} reports. Call before the first read. */
    public AddonSpecLoader gate(Predicate<String> enabled) {
        this.gate = enabled;
        return this;
    }

    /** Every loaded addon in load order, whatever the gate says. Forces the load. */
    public List<Addon> addons() {
        load();
        return List.copyOf(addons);
    }

    /** A section's entries from every enabled addon, in load order. */
    public List<String> get(String section) {
        load();
        List<String> combined = new ArrayList<>();
        for (Addon addon : addons) {
            if (!gate.test(addon.addonId())) continue;
            combined.addAll(entries(addon.addonId(), section));
        }
        return List.copyOf(combined);
    }

    /** One addon's entries for a section, whatever the gate says. */
    public List<String> get(String section, String addonId) {
        load();
        return List.copyOf(entries(addonId, section));
    }

    /** Config defaults proposed by enabled addons, in load order. */
    public List<ConfigContribution> configContributions() {
        load();
        List<ConfigContribution> enabled = new ArrayList<>();
        for (ConfigContribution contribution : contributions) {
            if (gate.test(contribution.addonId())) enabled.add(contribution);
        }
        return List.copyOf(enabled);
    }

    public String resourcePath() {
        return resourcePath;
    }

    public String rootsProperty() {
        return rootsProperty;
    }

    private List<String> entries(String addonId, String section) {
        return byAddon.getOrDefault(addonId, Map.of()).getOrDefault(section, List.of());
    }

    /**
     * Populates every field before returning, and never consults the gate: the gate is free to call
     * {@link #addons()}, which lands back here.
     */
    private synchronized void load() {
        if (loaded) return;
        for (Candidate candidate : candidates()) {
            try (Reader reader = candidate.opener().open()) {
                read(candidate, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception e) {
                logger.warn("Failed to read addon spec from {}", candidate.origin(), e);
            }
        }
        sortByLoadOrder();
        loaded = true;
    }

    /**
     * Discovery follows the loader's mod list, which is neither stable across loaders nor alphabetical,
     * and load order decides which addon wins a contested config key. Built-ins keep the order the
     * consumer registered them in; everything else is sorted by id.
     */
    private void sortByLoadOrder() {
        List<Addon> builtIn = new ArrayList<>();
        List<Addon> discovered = new ArrayList<>();
        for (Addon addon : addons) {
            (addon.builtIn() ? builtIn : discovered).add(addon);
        }
        discovered.sort(Comparator.comparing(Addon::addonId));
        addons.clear();
        addons.addAll(builtIn);
        addons.addAll(discovered);
    }

    private List<Candidate> candidates() {
        List<Candidate> found = new ArrayList<>();
        for (String resource : builtinResources) {
            if (AddonSpecLoader.class.getResource(resource) == null) {
                logger.warn("Bundled addon spec {} is missing from the classpath", resource);
                continue;
            }
            found.add(new Candidate(resource, resource, true, () -> open(resource)));
        }
        found.addAll(discovered());
        return found;
    }

    private static Reader open(String resource) throws IOException {
        InputStream in = AddonSpecLoader.class.getResourceAsStream(resource);
        if (in == null) throw new IOException("Missing classpath resource " + resource);
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private List<Candidate> discovered() {
        List<Candidate> found = new ArrayList<>();
        for (AddonSource source : probe()) {
            found.add(new Candidate(source.modId(), source.modId(), false,
                    () -> Files.newBufferedReader(source.path(), StandardCharsets.UTF_8)));
        }
        String roots = System.getProperty(rootsProperty);
        if (roots == null || roots.isBlank()) return found;
        for (String root : roots.split(File.pathSeparator)) {
            if (root.isBlank()) continue;
            Path candidate = Path.of(root.trim()).resolve(resourcePath);
            if (!Files.exists(candidate)) continue;
            String origin = "dev:" + root.trim();
            found.add(new Candidate(origin, origin, false,
                    () -> Files.newBufferedReader(candidate, StandardCharsets.UTF_8)));
        }
        return found;
    }

    /** The platform lookup is attempted rather than assumed: a bare-JVM tool has no platform service. */
    private List<AddonSource> probe() {
        try {
            return finder.apply(resourcePath);
        } catch (Throwable t) {
            logger.debug("No platform service; scanning addon roots only");
            return List.of();
        }
    }

    private void read(Candidate candidate, JsonObject root) {
        int version = root.has("spec_version") ? root.get("spec_version").getAsInt() : 0;
        if (version > supportedSpecVersion) {
            logger.warn("Addon {} declares spec_version {} but this build understands {}; "
                    + "unrecognised entries will be ignored", candidate.origin(), version, supportedSpecVersion);
        }
        String addonId = string(root, "addon_id", candidate.modId());
        if (byAddon.containsKey(addonId)) {
            logger.warn("Two addon specs both claim the id {}; ignoring the one from {}",
                    addonId, candidate.origin());
            return;
        }
        String name = string(root, "name", addonId);
        boolean defaultEnabled = !root.has("default_enabled") || root.get("default_enabled").getAsBoolean();

        Map<String, List<String>> results = new LinkedHashMap<>();
        int[] accepted = {0};
        for (Registered registered : sections) {
            JsonElement raw = root.get(registered.name());
            if (raw == null || !raw.isJsonArray()) continue;
            for (JsonElement element : (JsonArray) raw) {
                if (!element.isJsonObject() && !element.isJsonPrimitive()) continue;
                JsonObject entry = element.isJsonObject() ? element.getAsJsonObject() : wrap(element);
                registered.section().flatten(entry, (target, flattened) -> {
                    Predicate<Object> validator = validatorFor(target);
                    if (validator != null && !validator.test(flattened)) {
                        logger.warn("Addon {} has an invalid {} entry, skipping: {}", addonId, target, flattened);
                        return;
                    }
                    results.computeIfAbsent(target, key -> new ArrayList<>()).add(flattened);
                    accepted[0]++;
                });
            }
        }

        int configured = readConfig(addonId, root);
        byAddon.put(addonId, results);
        addons.add(new Addon(addonId, name, version, defaultEnabled, candidate.builtIn(), candidate.origin()));
        logger.info("Addon loaded: {} ({} entries, {} config defaults, {} by default)",
                addonId, accepted[0], configured, defaultEnabled ? "on" : "off");
    }

    /** Reads {@code "config": { "<file>": { "<dotted.key>": <value> } }}. */
    private int readConfig(String addonId, JsonObject root) {
        JsonElement raw = root.get("config");
        if (raw == null || !raw.isJsonObject()) return 0;
        int count = 0;
        for (Map.Entry<String, JsonElement> file : raw.getAsJsonObject().entrySet()) {
            if (!file.getValue().isJsonObject()) {
                logger.warn("Addon {} config section {} is not an object, skipping", addonId, file.getKey());
                continue;
            }
            for (Map.Entry<String, JsonElement> option : file.getValue().getAsJsonObject().entrySet()) {
                contributions.add(new ConfigContribution(addonId, file.getKey(), option.getKey(), option.getValue()));
                count++;
            }
        }
        return count;
    }

    /** Entries are validated by the section they land in, which need not be the one that declared them. */
    private Predicate<Object> validatorFor(String section) {
        for (Registered registered : sections) {
            if (registered.name().equals(section)) return registered.validator();
        }
        return null;
    }

    private static String string(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    /** A section may hold bare strings rather than objects; give the reader a uniform shape. */
    private static JsonObject wrap(JsonElement primitive) {
        JsonObject object = new JsonObject();
        object.add("value", primitive);
        return object;
    }
}
