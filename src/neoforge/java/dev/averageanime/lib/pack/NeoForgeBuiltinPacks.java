package dev.averageanime.lib.pack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;

public final class NeoForgeBuiltinPacks {
    private NeoForgeBuiltinPacks() {}

    /**
     * Call from the {@code @Mod} constructor. The event fires once per {@link PackType} as the pack
     * repository is assembled.
     *
     * <p>{@code alwaysActive} is false so the player can remove a pack. That is enough for the data
     * half on its own: {@code MinecraftServer.configurePackRepository} adds any available pack whose
     * {@link PackSource#shouldAddAutomatically()} is true and that the world has not explicitly
     * disabled, for existing worlds as well as new ones, and {@code PackSource.BUILT_IN} is such a
     * source. The client half has no equivalent — {@code PackRepository.rebuildSelected} takes its
     * selection from {@code options.resourcePacks} and force-inserts only required packs — so
     * {@link DefaultPackSelection} puts the ids there once instead.
     */
    public static void register(IEventBus modEventBus, String modId, List<String> names) {
        List<BuiltinPackSpec> specs = new ArrayList<>(names.size());
        for (String name : names) {
            specs.add(new BuiltinPackSpec(modId, name));
        }
        modEventBus.addListener((AddPackFindersEvent event) -> {
            IModFileInfo modFile = ModList.get().getModFileById(modId);
            List<BuiltinPackSpec> registered = new ArrayList<>(specs.size());
            for (BuiltinPackSpec spec : specs) {
                // Only register a pack for a type it actually carries. Fabric API drops a pack whose
                // namespaces are empty for the type; NeoForge's helper does not, so without this an
                // assets-only pack also appears in the datapack screen as an empty entry.
                if (!carries(modFile, spec, event.getPackType())) continue;
                // An override pack for a mod that is not installed changes nothing, but would still be
                // one more entry in the pack screen -- a mod shipping one pack per supported mod fills
                // the list. The pack's namespaces name the mods it overrides.
                if (!overridesLoadedMod(modFile, spec, event.getPackType())) continue;
                event.addPackFinders(
                        ResourceLocation.fromNamespaceAndPath(spec.modId(), spec.jarPath()),
                        event.getPackType(),
                        spec.title(),
                        PackSource.BUILT_IN,
                        false,
                        Pack.Position.TOP);
                registered.add(spec);
            }
            // Guarded twice over: DefaultPackSelection reaches into Minecraft, so it must not be
            // class-loaded on a dedicated server. The pack-type check alone would already do that,
            // since CLIENT_RESOURCES never fires there, but the dist check states the constraint.
            if (event.getPackType() == PackType.CLIENT_RESOURCES && FMLEnvironment.dist == Dist.CLIENT) {
                DefaultPackSelection.enableOnce(modId, registered);
            }
        });
    }

    /** Whether the pack directory holds the subdirectory this pack type reads ({@code assets} or {@code data}). */
    private static boolean carries(IModFileInfo modFile, BuiltinPackSpec spec, PackType type) {
        if (modFile == null) return false;
        Path dir = modFile.getFile().findResource(spec.jarPath(), type.getDirectory());
        return dir != null && Files.isDirectory(dir);
    }

    /**
     * Whether any namespace directory in the pack belongs to a loaded mod. If the directory cannot be
     * listed the pack is kept: an unreadable pack showing up is a smaller fault than a working one
     * silently vanishing. Only called after {@link #carries} has found the directory.
     */
    private static boolean overridesLoadedMod(IModFileInfo modFile, BuiltinPackSpec spec, PackType type) {
        Path dir = modFile.getFile().findResource(spec.jarPath(), type.getDirectory());
        try (Stream<Path> namespaces = Files.list(dir)) {
            return namespaces.filter(Files::isDirectory)
                    // Some jar filesystems report a directory's name with its trailing separator.
                    .map(ns -> ns.getFileName().toString().replace("/", ""))
                    .anyMatch(ns -> ns.equals("minecraft") || ModList.get().isLoaded(ns));
        } catch (IOException e) {
            return true;
        }
    }
}
