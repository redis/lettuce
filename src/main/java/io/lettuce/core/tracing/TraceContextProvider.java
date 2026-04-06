package io.lettuce.core.tracing;

import java.util.Map;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

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
     * @deprecated since 7.6, use {@link #getTraceContextAsync(Map)} instead.
     *             <p>
     *             This is deprecated as part of the initiative to make Reactor dependencies optional.
     * @see https://github.com/redis/lettuce/issues/3614
     * @return the {@link TraceContext}.
     * 
     */
    @Deprecated
    default Mono<TraceContext> getTraceContextLater() {
        return Mono.justOrEmpty(getTraceContext());
    }

    /**
     * Provides a {@link TraceContext} in delayed fashion, accepts an application context to obtain/populate a particular
     * context in case required.
     * <p>
     * Return value can be null depending on the implementation, and application context it is called from.
     * 
     * @param appContext application context
     * @return the {@link TraceContext}.
     */
    default Supplier<TraceContext> getTraceContextAsync(Map<Object, Object> appContext) {
        return () -> getTraceContext();
    }

}
