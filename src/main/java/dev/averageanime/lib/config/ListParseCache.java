package dev.averageanime.lib.config;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Memoises the parsed form of a config list, keyed on its contents.
 *
 * <p>Config lists are read on hot paths but change only when someone edits the file, so parsing them
 * per call is waste. Keying on a snapshot of the contents rather than on identity means a reload
 * naturally produces a new key instead of needing to be invalidated.
 *
 * <p>Cleared wholesale past a small size: the number of distinct lists in play is tiny, so a large
 * cache means something is generating fresh lists and evicting is cheaper than tracking why.
 */
public final class ListParseCache<V> {

    private final Function<List<? extends String>, V> parser;
    private final ConcurrentHashMap<List<String>, V> cache = new ConcurrentHashMap<>();

    public ListParseCache(Function<List<? extends String>, V> parser) {
        this.parser = parser;
    }

    public V get(List<? extends String> source) {
        if (cache.size() > 64) cache.clear();
        List<String> snapshot = List.copyOf(source);
        return cache.computeIfAbsent(snapshot, key -> parser.apply(source));
    }
}
