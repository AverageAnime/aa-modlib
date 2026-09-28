package dev.averageanime.lib.pack;

import java.util.List;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public final class FabricBuiltinPacks {
    private FabricBuiltinPacks() {}

    /**
     * Call from {@code ModInitializer.onInitialize}. Registration only fills a set that the pack
     * repository drains later, so it has to happen before the repository is built.
     *
     * <p>One call covers both pack types: the implementation builds a client pack and a data pack over
     * the same directory and discards whichever finds no namespaces, so an assets-only pack yields no
     * data pack and never appears in the datapack screen.
     *
     * <p>{@code DEFAULT_ENABLED} is on-by-default and still removable on all three surfaces — the
     * client's selected list, new worlds, and existing worlds. Fabric API records the first
     * auto-enable in {@code <gamedir>/data/fabricDefaultResourcePacks.dat} so a later manual disable
     * is not undone on the next launch.
     *
     * @throws IllegalStateException if a pack's directory is missing, which would otherwise be silent
     */
    public static void register(String modId, List<String> names) {
        ModContainer container = FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("No mod container for " + modId));
        for (String name : names) {
            BuiltinPackSpec spec = new BuiltinPackSpec(modId, name);
            boolean found = ResourceManagerHelper.registerBuiltinResourcePack(
                    spec.id(), container, spec.title(), ResourcePackActivationType.DEFAULT_ENABLED);
            if (!found) {
                throw new IllegalStateException("Missing built-in pack directory: " + spec.jarPath());
            }
        }
    }
}
