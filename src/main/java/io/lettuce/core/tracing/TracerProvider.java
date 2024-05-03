package io.lettuce.core.tracing;

/**
 * Interface to obtain a {@link Tracer}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@FunctionalInterface
public interface TracerProvider {

    /**
     * @return the {@link Tracer}.
     */
    Tracer getTracer();

}
