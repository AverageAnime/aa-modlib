package dev.averageanime.lib.registry;

import java.util.function.Supplier;

/** Returns a supplier, because on NeoForge the object does not exist yet at registration. */
@FunctionalInterface
public interface RegistryHooks<T> {

    Supplier<T> register(String id, Supplier<T> factory);
}
