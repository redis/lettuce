/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.tracing;

import java.net.SocketAddress;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Interface declaring methods to trace Redis commands. This interface contains declarations of basic required interfaces and
 * value objects to represent traces, spans and metadata in an dependency-agnostic manner.
 *
 * @author Mark Paluch
 * @author Daniel Albuquerque
 * @since 5.1
 * @see TracerProvider
 * @see TraceContextProvider
 */
public interface Tracing {

    /**
     * @return the {@link TracerProvider}.
     */
    TracerProvider getTracerProvider();

    /**
     * @return the {@link TraceContextProvider} supplying the initial {@link TraceContext} (i.e. if there is no active span).
     */
    TraceContextProvider initialTraceContextProvider();

    /**
     * Returns {@code true} if tracing is enabled.
     *
     * @return {@code true} if tracing is enabled.
     */
    boolean isEnabled();

    /**
     * Returns {@code true} if tags for {@link Tracer.Span}s should include the command arguments.
     *
     * @return {@code true} if tags for {@link Tracer.Span}s should include the command arguments.
     * @since 5.2
     */
    boolean includeCommandArgsInSpanTags();

    /**
     * Create an {@link Endpoint} given {@link SocketAddress}.
     *
     * @param socketAddress the remote address.
     * @return the {@link Endpoint} for {@link SocketAddress}.
     */
    Endpoint createEndpoint(SocketAddress socketAddress);

    /**
     * Returns a {@link TracerProvider} that is disabled.
     *
     * @return a disabled {@link TracerProvider}.
     */
    static Tracing disabled() {
        return NoOpTracing.INSTANCE;
    }

    /**
     * Gets the {@link TraceContextProvider} from Reactor {@link Context}.
     *
     * @return the {@link TraceContextProvider}.
     */
    static Mono<TraceContextProvider> getContext() {
        return Mono.subscriberContext().filter(c -> c.hasKey(TraceContextProvider.class))
                .map(c -> c.get(TraceContextProvider.class));
    }

    /**
     * Clears the {@code Mono<TracerProvider>} from Reactor {@link Context}.
     *
     * @return Return a {@link Function} that clears the {@link TraceContextProvider} context.
     */
    static Function<Context, Context> clearContext() {
        return context -> context.delete(TraceContextProvider.class);
    }

    /**
     * Creates a Reactor {@link Context} that contains the {@code Mono<TraceContextProvider>}. that can be merged into another
     * {@link Context}.
     *
     * @param supplier the {@link TraceContextProvider} to set in the returned Reactor {@link Context}.
     * @return a Reactor {@link Context} that contains the {@code Mono<TraceContextProvider>}.
     */
    static Context withTraceContextProvider(TraceContextProvider supplier) {
        return Context.of(TraceContextProvider.class, supplier);
    }

    /**
     * Value object interface to represent an endpoint. Used by {@link Tracer.Span}.
     *
     * @since 5.1
     */
    interface Endpoint {
    }

}
