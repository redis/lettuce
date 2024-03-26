package io.lettuce.core.api;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link AsyncCloseable} is a resource that can be closed. The {@link #closeAsync()} method is invoked to request resources
 * release that the object is holding (such as open files).
 *
 * @since 6.2
 * @author Mark Paluch
 */
public interface AsyncCloseable extends io.lettuce.core.internal.AsyncCloseable {

    /**
     * Requests to close this object and releases any system resources associated with it. If the object is already closed then
     * invoking this method has no effect.
     * <p>
     * Calls to this method return a {@link CompletableFuture} that is notified with the outcome of the close request.
     */
    CompletableFuture<Void> closeAsync();

}
