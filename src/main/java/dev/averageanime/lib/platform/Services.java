package dev.averageanime.lib.platform;

import java.util.ServiceLoader;

/**
 * Resolves the loader-specific implementation of a service interface.
 *
 * <p>Both loaders put every mod on one classloader, so a plain {@link ServiceLoader} is enough. Note
 * that this means a duplicated service interface across two mods would resolve to whichever jar wins
 * the scan, which is why this package is relocated per consuming mod at build time rather than shared
 * verbatim.
 */
public final class Services {

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to load service for " + clazz.getName()));
    }
}
