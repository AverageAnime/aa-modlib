package dev.averageanime.lib.platform;

import java.util.ServiceLoader;

/** Both loaders share one classloader, which is why this package is relocated per consuming mod. */
public final class Services {

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to load service for " + clazz.getName()));
    }
}
