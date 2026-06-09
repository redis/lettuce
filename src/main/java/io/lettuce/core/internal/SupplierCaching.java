package io.lettuce.core.internal;

import java.util.function.Function;

/**
 * Mixin interface for types that expose a {@link SuppliedItemStore} to memoize derived instances.
 *
 * @param <C> the owner type passed to providers
 */
public interface SupplierCaching<C> {

    SuppliedItemStore<C> getStore();

    default <T> T getCachedBySupplier(Function<C, T> supplier) {
        return getStore().get(supplier);
    }

}
