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
     * @return the {@link TraceContext}.
     */
    TraceContext getTraceContext();

    /**
     * @return the {@link TraceContext}.
     */
    @Deprecated
    default Mono<TraceContext> getTraceContextLater() {
        return Mono.justOrEmpty(getTraceContext());
    }

    default Supplier<TraceContext> getTraceContextAsync(Map<Object, Object> appContext) {
        return () -> getTraceContext();
    }

}
