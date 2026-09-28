package dev.averageanime.lib.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Selects a mod's built-in resource packs the first time the player runs with them, so they are on by
 * default without being locked on.
 *
 * <p>Client only — it touches {@link Minecraft}, so it must never be class-loaded on a dedicated
 * server. Its one caller keeps it behind a {@code CLIENT_RESOURCES} and {@code Dist.CLIENT} check.
 *
 * <p>NeoForge has no equivalent of Fabric API's {@code DEFAULT_ENABLED} for client packs: a pack that
 * is not required is simply available-but-unselected. So this does what Fabric's {@code
 * GameOptionsMixin} does — appends each id to {@code options.resourcePacks} once, and records that it
 * has done so. Because an id is never offered twice, a player who later removes the pack keeps it
 * removed.
 *
 * <p>Timing is the reason this works: {@code AddPackFindersEvent} for client resources fires inside
 * {@code Minecraft.<init>} after {@code options} is assigned and before
 * {@code options.loadSelectedResourcePacks}, so the list is read after this writes to it.
 *
 * <p>The tracker is runtime state under the game directory, not a shipped file. Deleting it re-enables
 * every pack on the next launch, which is also how Fabric's behaves.
 */
final class DefaultPackSelection {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DefaultPackSelection() {}

    static void enableOnce(String modId, List<BuiltinPackSpec> specs) {
        Path tracker = FMLPaths.GAMEDIR.get()
                .resolve("data").resolve(modId + "_default_packs.txt");

        Set<String> seen;
        try {
            seen = read(tracker);
        } catch (IOException e) {
            // Treating an unreadable tracker as empty would re-enable packs the player had removed,
            // every launch. Doing nothing leaves them merely unselected, which is recoverable.
            LOGGER.warn("Could not read {}; leaving built-in packs unselected", tracker, e);
            return;
        }

        Options options = Minecraft.getInstance().options;
        boolean changed = false;
        for (BuiltinPackSpec spec : specs) {
            String id = neoForgeId(spec);
            if (!seen.add(id)) continue;
            changed = true;
            if (!options.resourcePacks.contains(id)) {
                options.resourcePacks.add(id);
            }
        }
        if (!changed) return;

        try {
            Files.createDirectories(tracker.getParent());
            Files.write(tracker, seen, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}; built-in packs may re-enable next launch", tracker, e);
        }
    }

    private static Set<String> read(Path tracker) throws IOException {
        Set<String> seen = new LinkedHashSet<>();
        if (!Files.exists(tracker)) return seen;
        for (String line : Files.readAllLines(tracker, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) seen.add(trimmed);
        }
        return seen;
    }

    /**
     * The id {@code AddPackFindersEvent.addPackFinders} gives the pack it builds. Derived rather than
     * observed, because the pack is registered and selected in the same event and there is nothing to
     * read it back from; it must track that method if NeoForge ever changes the prefix.
     */
    private static String neoForgeId(BuiltinPackSpec spec) {
        return "mod/" + spec.modId() + ":" + spec.jarPath();
    }
}
