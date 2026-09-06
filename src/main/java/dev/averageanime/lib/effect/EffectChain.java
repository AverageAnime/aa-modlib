package dev.averageanime.lib.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class EffectChain {

    private static Predicate<String> modLoaded = modId -> false;

    /** Wire this once at startup, before any chain resolves. */
    public static void modLoadedCheck(Predicate<String> check) {
        modLoaded = check;
    }


    private final String categoryName;
    private final Candidate[] candidates;
    private Holder<MobEffect> resolved = null;
    private boolean resolveAttempted = false;

    private EffectChain(String categoryName, Candidate[] candidates) {
        this.categoryName = categoryName;
        this.candidates = candidates;
    }

    public void resolve() {
        for (Candidate c : candidates) {
            if (!modLoaded.test(c.modId())) continue;
            Optional<Holder.Reference<MobEffect>> holder =
                    BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(c.effectId()));
            if (holder.isPresent()) {
                this.resolved = holder.get();
                break;
            }
        }
        // Marked even on a miss, or an unresolvable category re-walks the registry on every tooltip and bite.
        resolveAttempted = true;
    }

    public Optional<Holder<MobEffect>> get() {
        if (!resolveAttempted) {
            resolve();
        }
        return Optional.ofNullable(resolved);
    }

    public void invalidate() {
        resolved = null;
        resolveAttempted = false;
    }

    public boolean hasAnyLoadedCandidate() {
        for (Candidate c : candidates) {
            if (modLoaded.test(c.modId())) return true;
        }
        return false;
    }

    public String getCategoryName() {
        return categoryName;
    }

    private record Candidate(String modId, String effectId) {}

    public static Builder category(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final List<Candidate> candidates = new ArrayList<>();

        private Builder(String name) { this.name = name; }

        public Builder or(String modId, String effectId) {
            candidates.add(new Candidate(modId, effectId));
            return this;
        }

        public Builder orAll(EffectChain other) {
            if (other != null) {
                candidates.addAll(Arrays.asList(other.candidates));
            }
            return this;
        }

        public EffectChain build() {
            return new EffectChain(name, candidates.toArray(new Candidate[0]));
        }
    }
}
