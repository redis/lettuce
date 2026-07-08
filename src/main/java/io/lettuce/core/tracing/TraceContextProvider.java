package io.lettuce.core.tracing;

import java.util.Map;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

/**
 * Interface to obtain a {@link TraceContext} allowing propagation of {@link Tracer.Span} {@link TraceContext}s across threads.
 *
 * @author Mark Paluch
 * @author Aleksandar Todorov
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
     * Returns the {@link TraceContext} in a deferred fashion as a {@link Mono}.
     * <p>
     * The emitted value may be {@code null} depending on the implementation and the application context it is called from.
     *
     * @return a {@link Mono} emitting the {@link TraceContext}.
     * @deprecated since 7.7, override {@link #getTraceContextAsync(Map)} instead; scheduled for removal in Lettuce 8.0 as part
     *             of making Reactor an optional dependency. See
     *             <a href="https://github.com/redis/lettuce/issues/3614">lettuce#3614</a>.
     */
    @Deprecated
    default Mono<TraceContext> getTraceContextLater() {
        return Mono.justOrEmpty(getTraceContext());
    }

    /**
     * Returns a {@link Supplier} that resolves the {@link TraceContext} on demand, using the given application context to
     * obtain or populate a particular context where required.
     * <p>
     * The value produced by the {@link Supplier} may be {@code null} depending on the implementation and the application
     * context it is called from.
     *
     * @param appContext the application context used to resolve the {@link TraceContext}.
     * @return a {@link Supplier} of the {@link TraceContext}.
     * @since 7.7
     */
    default Supplier<TraceContext> getTraceContextAsync(Map<Object, Object> appContext) {
        return () -> getTraceContext();
    }

}
