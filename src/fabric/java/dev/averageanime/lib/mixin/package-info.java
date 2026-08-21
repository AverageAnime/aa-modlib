/**
 * Fabric mixins the shared library supplies.
 *
 * <p>Fabric has no equivalent of NeoForge's client fluid extensions, so the fog colour and the
 * submerged overlay have to be reached by patching the renderers directly. Both read what
 * {@code FluidBlock} recorded when the fluid was declared.
 *
 * <p>These are relocated per consuming mod like the rest of the library, so each mod needs its own
 * mixin config naming the relocated package. A mixin config carries a single package prefix, which is
 * why this cannot share the mod's existing one.
 */
package dev.averageanime.lib.mixin;
