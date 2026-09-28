package dev.averageanime.lib.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One pack a mod ships inside its own jar under {@code resourcepacks/<name>}.
 *
 * <p>The directory is fixed by Fabric API, which derives it from the pack id and always prefixes
 * {@code resourcepacks/}; NeoForge accepts an arbitrary jar path and is handed the same one, so both
 * loaders read one directory.
 *
 * <p>The directory must exist and carry a readable {@code pack.mcmeta}. NeoForge's
 * {@code Pack.readMetaAndCreate} returns null when it cannot read the metadata and the pack-finder
 * helper does not check, so a declared-but-absent pack is a startup NPE rather than a warning.
 * Fabric synthesizes metadata instead and would let the mistake through.
 */
public record BuiltinPackSpec(String modId, String name) {
    private static final String JAR_DIR = "resourcepacks";

    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(modId, name);
    }

    public String jarPath() {
        return JAR_DIR + "/" + name;
    }

    public Component title() {
        return Component.translatable("pack." + modId + "." + name);
    }
}
