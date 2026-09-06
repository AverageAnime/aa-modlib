package dev.averageanime.lib.registry;

import net.minecraft.core.particles.ParticleOptions;

import java.util.function.Supplier;

public record DisplayEntry<T>(T type, int maxStack, double height,
                              boolean hasParticles, Supplier<ParticleOptions> particleType) {

    public static <T> Builder<T> of(T type) {
        return new Builder<>(type);
    }

    public static final class Builder<T> {
        private final T type;
        private int maxStack = 1;
        private double height = 12;
        private Supplier<ParticleOptions> particleType = null;

        private Builder(T type) { this.type = type; }

        public Builder<T> maxStack(int v) { this.maxStack = v; return this; }
        public Builder<T> height(double v) { this.height = v; return this; }

        public Builder<T> particles(Supplier<ParticleOptions> supplier) {
            this.particleType = supplier;
            return this;
        }

        public DisplayEntry<T> build() {
            return new DisplayEntry<>(type, maxStack, height, particleType != null, particleType);
        }
    }
}