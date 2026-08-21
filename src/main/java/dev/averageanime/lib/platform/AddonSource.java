package dev.averageanime.lib.platform;

import java.nio.file.Path;

/**
 * One mod file that carries an addon resource, paired with the id of the mod that owns it.
 *
 * @param modId owning mod's id, used only for logging and for attributing bad entries
 * @param path  path to the resource inside that mod file
 */
public record AddonSource(String modId, Path path) {}
