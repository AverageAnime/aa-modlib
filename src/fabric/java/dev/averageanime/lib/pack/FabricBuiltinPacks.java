package dev.averageanime.lib.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.packs.PackType;

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
            // Same rule as NeoForgeBuiltinPacks: an override pack for a mod that is not installed
            // changes nothing, but would still be one more entry in the pack screen.
            if (!overridesLoadedMod(container, spec)) continue;
            boolean found = ResourceManagerHelper.registerBuiltinResourcePack(
                    spec.id(), container, spec.title(), ResourcePackActivationType.DEFAULT_ENABLED);
            if (!found) {
                throw new IllegalStateException("Missing built-in pack directory: " + spec.jarPath());
            }
        }
    }

    /**
     * Whether any namespace directory in the pack, under either pack type, belongs to a loaded mod.
     * A pack whose directories cannot be found or listed is kept, so a missing pack still reaches the
     * registration call and fails there loudly rather than being skipped here.
     */
    private static boolean overridesLoadedMod(ModContainer container, BuiltinPackSpec spec) {
        boolean listed = false;
        for (PackType type : PackType.values()) {
            Optional<Path> dir = container.findPath(spec.jarPath() + "/" + type.getDirectory());
            if (dir.isEmpty() || !Files.isDirectory(dir.get())) continue;
            try (Stream<Path> namespaces = Files.list(dir.get())) {
                listed = true;
                boolean loaded = namespaces.filter(Files::isDirectory)
                        // Some jar filesystems report a directory's name with its trailing separator.
                        .map(ns -> ns.getFileName().toString().replace("/", ""))
                        .anyMatch(ns -> ns.equals("minecraft") || FabricLoader.getInstance().isModLoaded(ns));
                if (loaded) return true;
            } catch (IOException e) {
                return true;
            }
        }
        return !listed;
    }
}
