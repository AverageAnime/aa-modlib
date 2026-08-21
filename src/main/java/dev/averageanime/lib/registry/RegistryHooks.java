package dev.averageanime.lib.registry;

import java.util.function.Supplier;

/**
 * How a loader registers one thing.
 *
 * <p>The two loaders disagree about when registration happens -- NeoForge defers it behind a
 * {@code DeferredRegister}, Fabric does it immediately -- so shared code cannot call either directly.
 * It declares what it wants instead and hands it to whichever implementation is present.
 *
 * <p>Returns a supplier rather than the object, because on NeoForge the object does not exist yet at
 * the point of registration.
 */
@FunctionalInterface
public interface RegistryHooks<T> {

    Supplier<T> register(String id, Supplier<T> factory);
}
