package dev.averageanime.lib.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            for (BuiltinPackSpec spec : specs) {
                // Only register a pack for a type it actually carries. Fabric API drops a pack whose
                // namespaces are empty for the type; NeoForge's helper does not, so without this an
                // assets-only pack also appears in the datapack screen as an empty entry.
                if (!carries(modFile, spec, event.getPackType())) continue;
                event.addPackFinders(
                        ResourceLocation.fromNamespaceAndPath(spec.modId(), spec.jarPath()),
                        event.getPackType(),
                        spec.title(),
                        PackSource.BUILT_IN,
                        false,
                        Pack.Position.TOP);
            }
            // Guarded twice over: DefaultPackSelection reaches into Minecraft, so it must not be
            // class-loaded on a dedicated server. The pack-type check alone would already do that,
            // since CLIENT_RESOURCES never fires there, but the dist check states the constraint.
            if (event.getPackType() == PackType.CLIENT_RESOURCES && FMLEnvironment.dist == Dist.CLIENT) {
                DefaultPackSelection.enableOnce(modId, specs);
            }
        });
    }

    /** Whether the pack directory holds the subdirectory this pack type reads ({@code assets} or {@code data}). */
    private static boolean carries(IModFileInfo modFile, BuiltinPackSpec spec, PackType type) {
        if (modFile == null) return false;
        Path dir = modFile.getFile().findResource(spec.jarPath(), type.getDirectory());
        return dir != null && Files.isDirectory(dir);
    }
}
