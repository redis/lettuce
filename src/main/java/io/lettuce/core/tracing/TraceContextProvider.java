package io.lettuce.core.tracing;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface to obtain a {@link TraceContext} allowing propagation of {@link Tracer.Span} {@link TraceContext}s across threads.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@FunctionalInterface
public interface TraceContextProvider {

    /**
     * Returns the {@link TraceContext} in a blocking fashion.
     * <p>
     * Return value can be null depending on the implementation, and application context it is called from.
     * 
     * @return the {@link TraceContext}.
     */
    TraceContext getTraceContext();

    /**
     * Returns a {@link Supplier} that resolves the {@link TraceContext} on demand, using the given application context to
     * obtain or populate a particular context where required.
     * <p>
     * The value produced by the {@link Supplier} may be {@code null} depending on the implementation and the application
     * context it is called from.
     *
     * @param appContext the application context used to resolve the {@link TraceContext}.
     * @return a {@link Supplier} of the {@link TraceContext}.
     * @since 7.8
     */
    default Supplier<TraceContext> getTraceContextAsync(Map<Object, Object> appContext) {
        return this::getTraceContext;
    }

}
