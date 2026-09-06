package dev.averageanime.lib.registry;

import java.util.function.Supplier;

/** Binding twice and reading early both throw. */
public abstract class LazyEntry<T> {

    public final String id;

    private Supplier<T> registered;

    protected LazyEntry(String id) {
        this.id = id;
    }

    public void bind(Supplier<T> supplier) {
        if (this.registered != null) throw new IllegalStateException("Already bound: " + id);
        this.registered = supplier;
    }

    public T get() {
        if (registered == null) throw new IllegalStateException("Not yet registered: " + id);
        return registered.get();
    }

    public boolean isBound() {
        return registered != null;
    }
}
