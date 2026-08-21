package dev.averageanime.lib.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A declarative fluid: everything needed to register a source, a flowing variant, a liquid block and a
 * bucket, without saying how any of that happens. Loaders differ on the how; this stays the same.
 *
 * <p>Declaring rather than building keeps a fluid to one line, which matters when a mod has a hundred
 * of them and each would otherwise be near-identical builder boilerplate.
 *
 * <p>{@link #ALL} is per-copy. Because this class is relocated into each consuming mod, two mods never
 * share the registry even though they share the source.
 */
public final class FluidEntry {

    public static final float DEFAULT_FOG_START = 0.5f;
    public static final float DEFAULT_FOG_END   = 1.5f;

    private static final List<FluidEntry> REGISTRY = new ArrayList<>();
    public  static final List<FluidEntry> ALL      = Collections.unmodifiableList(REGISTRY);

    private static final Map<String, String> TEXTURE_BY_ID = new HashMap<>();

    public final String id;
    public final int    slope;
    public final int    level;
    public final float  fogStart;
    public final float  fogEnd;

    /** Base name of the {@code fluid/<texture>_still|_flow} sprites. Defaults to {@link #id}. */
    public String texture;

    private FluidEntry(String id, int slope, int level, float fogStart, float fogEnd) {
        this.id       = id;
        this.slope    = slope;
        this.level    = level;
        this.fogStart = fogStart;
        this.fogEnd   = fogEnd;
        this.texture  = id;
    }

    /** Reuse another fluid's sprites instead of requiring {@code <id>_still|_flow} textures. */
    public FluidEntry tex(String texture) {
        this.texture = texture;
        TEXTURE_BY_ID.put(id, texture);
        return this;
    }

    /**
     * Sprite base name for a fluid id. Falls back to the id itself, which covers both
     * un-aliased fluids and config-defined ones that never appear in {@link #ALL}.
     */
    public static String textureFor(String id) {
        return TEXTURE_BY_ID.getOrDefault(id, id);
    }

    private static FluidEntry register(FluidEntry def) {
        REGISTRY.add(def);
        return def;
    }

    public static FluidEntry fluid(String id) {
        return register(new FluidEntry(id, -1, -1, DEFAULT_FOG_START, DEFAULT_FOG_END));
    }

    public static FluidEntry fluid(String id, int slope, int level) {
        return register(new FluidEntry(id, slope, level, DEFAULT_FOG_START, DEFAULT_FOG_END));
    }

    public static FluidEntry fluid(String id, float fogStart, float fogEnd) {
        return register(new FluidEntry(id, -1, -1, fogStart, fogEnd));
    }

    public static FluidEntry fluid(String id, int slope, int level, float fogStart, float fogEnd) {
        return register(new FluidEntry(id, slope, level, fogStart, fogEnd));
    }

    public static void init() {}
}
