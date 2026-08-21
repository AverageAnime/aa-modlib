package dev.averageanime.lib.platform;

import java.nio.file.Path;
import java.util.List;

/**
 * The parts of a mod loader a mod actually has to ask about, with nothing domain-specific attached.
 *
 * <p>A consuming mod extends this with its own interface for anything of its own that has to differ per
 * loader -- block entity factories, menu opening, and so on -- and implements the combined interface
 * once per loader. This half stays the same for every mod, which is the point of it living here.
 *
 * <p>Loaded through {@link Services}.
 */
public interface ModPlatform {

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /**
     * True while a datagen run is in progress. Config-driven registration falls back to its built-in
     * defaults in that case, so generated resources depend only on committed source rather than on
     * whatever the dev run directory's config happens to contain.
     */
    default boolean isRunningDataGen() { return false; }

    boolean isClient();

    boolean isServer();

    boolean isFabric();

    boolean isNeoforge();

    Path getConfigDir();

    /**
     * Locates {@code path} inside every loaded mod file, including this one. Safe from mod construction
     * onward on both loaders: NeoForge reads {@code LoadingModList}, which mod discovery fills before any
     * mod class loads, and Fabric reads {@code FabricLoader.getAllMods()}, which is populated before
     * entrypoints run.
     *
     * <p>Implementations read eagerly rather than returning a lazy handle, so no jar filesystem stays open.
     *
     * @return one entry per mod file containing the resource; never null, empty when nothing matches
     */
    List<AddonSource> findModResources(String path);
}
