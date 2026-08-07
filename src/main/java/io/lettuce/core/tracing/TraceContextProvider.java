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
     * Provides a {@link TraceContext} in delayed fashion, accepts an application context to obtain/populate a particular
     * context in case required.
     * <p>
     * Return value of the supplier, the {@link TraceContext}, can be null depending on the implementation, and application
     * context it is called from.
     *
     * @param appContext application context
     * @return the supplier for the {@link TraceContext}.
     * @since 7.7
     */
    default Supplier<TraceContext> getTraceContextAsync(Map<Object, Object> appContext) {
        return this::getTraceContext;
    }

}
