package io.lettuce.core.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Memoizing cache keyed by a {@link Function} provider. Each provider is applied at most once against the cache owner; the
 * resulting instance is then returned on subsequent lookups for the same provider.
 *
 * @param <C> the owner type passed to providers
 */
public final class SuppliedItemStore<C> {

    private final C owner;

    private final Map<Function<C, ?>, Object> cache = new HashMap<>();

    public SuppliedItemStore(C owner) {
        this.owner = owner;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Function<C, T> supplier) {
        T t = (T) cache.get(supplier);
        if (t == null) {
            t = supplier.apply(owner);
            cache.put(supplier, t);
        }
        return t;
    }

}
