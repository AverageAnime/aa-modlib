package dev.averageanime.lib.datagen;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** One instance per provider run. */
public final class DedupedTagAppender<T> {

    private final Set<String> emitted = new HashSet<>();
    private final Function<TagKey<T>, TagsProvider.TagAppender<T>> tagger;

    /** @param tagger the provider's own {@code tag(TagKey)}, e.g. {@code this::tag} */
    public DedupedTagAppender(Function<TagKey<T>, TagsProvider.TagAppender<T>> tagger) {
        this.tagger = tagger;
    }

    public Appender<T> tag(TagKey<T> key) {
        return new Appender<>(key, tagger.apply(key), emitted);
    }

    public int emittedCount() {
        return emitted.size();
    }

    public static final class Appender<T> {

        private final TagKey<T> key;
        private final TagsProvider.TagAppender<T> delegate;
        private final Set<String> emitted;

        private Appender(TagKey<T> key, TagsProvider.TagAppender<T> delegate, Set<String> emitted) {
            this.key = key;
            this.delegate = delegate;
            this.emitted = emitted;
        }

        /** Optional so a tag naming another mod's item does not break loading when that mod is absent. */
        public Appender<T> addOptional(ResourceLocation id) {
            if (emitted.add(key.location() + "|" + id)) {
                delegate.addOptional(id);
            }
            return this;
        }

        /** Optional so a tag naming another mod's tag does not break loading when that mod is absent. */
        public Appender<T> addOptionalTag(TagKey<T> nested) {
            if (emitted.add(key.location() + "|#" + nested.location())) {
                delegate.addOptionalTag(nested.location());
            }
            return this;
        }
    }
}
