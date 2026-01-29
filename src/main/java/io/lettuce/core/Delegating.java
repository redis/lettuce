package io.lettuce.core;

import io.lettuce.core.annotations.Experimental;

/**
 * A delegating interface that allows access to the underlying delegate.
 * 
 * @param <T> the type of the delegate.
 * 
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface Delegating<T> {

    /**
     * The underlying delegate.
     *
     * @return never {@code null}.
     */
    T getDelegate();

    /**
     * Unwrap the underlying delegate, through the recursively wrapped {@link Delegating} interfaces if available.
     *
     * @return the unwrapped delegate.
     */
    default T unwrap() {
        T delegate = getDelegate();
        if (delegate instanceof Delegating) {
            @SuppressWarnings("unchecked")
            T unwrapped = ((Delegating<T>) delegate).unwrap();
            return unwrapped;
        }
        return delegate;
    }

}
