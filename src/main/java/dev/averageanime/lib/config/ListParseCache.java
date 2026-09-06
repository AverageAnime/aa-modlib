package dev.averageanime.lib.config;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Memoises the parsed form of a config list, keyed on its contents so a reload produces a new key. */
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
