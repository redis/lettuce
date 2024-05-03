package io.lettuce.core.tracing;

/**
 * Marker interface for a context propagation of parent and child spans. Subclasses may add their propagation metadata.
 *
 * @author Mark Paluch
 * @since 5.1
 *
 */
public interface TraceContext {

    TraceContext EMPTY = new TraceContext() {
    };

}
