package dev.averageanime.lib.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads a handful of list options straight out of a config TOML, ahead of the loader.
 *
 * <p>Config-driven <em>registration</em> has a bootstrapping problem: entries have to be known during
 * mod construction, which is before either loader has loaded its config spec and therefore before the
 * normal accessors return anything. The values are read here directly instead, from the same file the
 * loader will later manage.
 *
 * <p>Both loaders bundle night-config, so reading the file this way needs no extra dependency.
 *
 * <p>Loaded once and cached. A key that is present in the file wins even when its list is empty, so
 * deleting an entry keeps it deleted rather than falling back to a default that would resurrect it.
 */
public final class TomlListReader {

    private final Path file;
    private final List<String> keys;
    private final Logger logger;
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    /**
     * @param file the config TOML, usually {@code <configDir>/<modid>-common.toml}
     * @param keys dotted paths to read, e.g. {@code items.item}
     */
    public TomlListReader(Path file, List<String> keys, Logger logger) {
        this.file = file;
        this.keys = List.copyOf(keys);
        this.logger = logger;
    }

    /** The configured entries for {@code key}, or {@code defaults} when the file does not mention it. */
    public List<String> read(String key, List<String> defaults) {
        load();
        List<String> configured = cache.get(key);
        return configured != null ? configured : defaults;
    }

    /** What was actually found in the file, for comparing against defaults. Never null. */
    public Map<String, List<String>> stored() {
        load();
        return Map.copyOf(cache);
    }

    /** True once a load has been attempted, whether or not the file existed. */
    public boolean isLoaded() {
        return loaded;
    }

    private void load() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            loaded = true;
            if (!Files.exists(file)) {
                logger.info("{} not written yet; using built-in defaults for this launch", file.getFileName());
                return;
            }
            try (FileConfig raw = FileConfig.of(file.toFile())) {
                raw.load();
                for (String key : keys) {
                    Object value = raw.get(key);
                    if (value instanceof List<?> list) {
                        cache.put(key, list.stream().map(String::valueOf).toList());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to read {}; using built-in defaults", file.getFileName(), e);
            }
        }
    }
}
