package dev.averageanime.lib.platform;

import java.nio.file.Path;
import java.util.List;

public interface ModPlatform {

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** Config-driven registration falls back to its defaults while this is true. */
    default boolean isRunningDataGen() { return false; }

    boolean isClient();

    boolean isServer();

    boolean isFabric();

    boolean isNeoforge();

    Path getConfigDir();

    /** Includes this mod's own file. Never null; empty when nothing matches. */
    List<AddonSource> findModResources(String path);
}
