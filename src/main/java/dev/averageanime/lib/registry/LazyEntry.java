package dev.averageanime.lib.registry;

import java.util.function.Supplier;

/**
 * A declared entry that is bound to its registered object later.
 *
 * <p>Content is declared as data long before registration runs, and on NeoForge the registered object
 * does not exist until well after that. So a declaration carries an id from the start and gets its
 * object attached once the loader has made one.
 *
 * <p>Both failure modes throw rather than returning null: binding twice means two registrations claimed
 * the same declaration, and reading early means something ran before registration. Neither is
 * recoverable, and both are much easier to find at the point they happen.
 */
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

    /** Whether this declaration has been through registration yet. */
    public boolean isBound() {
        return registered != null;
    }
}
