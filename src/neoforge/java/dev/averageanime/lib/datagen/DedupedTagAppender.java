package dev.averageanime.lib.datagen;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Appends tag entries, dropping any pair already added.
 *
 * <p>Duplicates are easy to produce in a tag provider without meaning to: walking items and then blocks
 * visits every block's item twice, a suffix rule can re-derive an id a loop already emitted, and a
 * hand-written line can restate what the automatic pass covered. All three end up as repeated entries in
 * the generated JSON.
 *
 * <p>First occurrence wins, so tag ordering in the output is whatever the provider produced.
 *
 * <p>One instance per provider run: it remembers every pair it has seen.
 */
public final class DedupedTagAppender<T> {

    private final Set<String> emitted = new HashSet<>();
    private final Function<TagKey<T>, TagsProvider.TagAppender<T>> tagger;

    /**
     * @param tagger the provider's own {@code tag(TagKey)}, e.g. {@code this::tag}
     */
    public DedupedTagAppender(Function<TagKey<T>, TagsProvider.TagAppender<T>> tagger) {
        this.tagger = tagger;
    }

    /** An appender for {@code key} that silently skips ids already in it. */
    public Appender<T> tag(TagKey<T> key) {
        return new Appender<>(key, tagger.apply(key), emitted);
    }

    /** How many distinct pairs were emitted, for a provider that wants to log its own output. */
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

        /**
         * Adds {@code id} if this tag does not already have it.
         *
         * <p>Optional rather than required, because a tag naming another mod's item must not break
         * loading when that mod is absent.
         */
        public Appender<T> addOptional(ResourceLocation id) {
            if (emitted.add(key.location() + "|" + id)) {
                delegate.addOptional(id);
            }
            return this;
        }

        /**
         * Adds a nested tag if this tag does not already have it.
         *
         * <p>Optional for the same reason as {@link #addOptional}: a tag may name one that only exists
         * when some other mod is installed.
         */
        public Appender<T> addOptionalTag(TagKey<T> nested) {
            if (emitted.add(key.location() + "|#" + nested.location())) {
                delegate.addOptionalTag(nested.location());
            }
            return this;
        }
    }
}
