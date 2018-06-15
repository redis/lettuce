/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.tracing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import reactor.core.publisher.Mono;
import brave.Span;
import brave.propagation.TraceContextOrSamplingFlags;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link Tracing} integration with OpenZipkin's Brave {@link brave.Tracer}. This implementation creates Brave
 * {@link brave.Span}s that are optionally associated with a {@link TraceContext}.
 *
 * <h3>{@link TraceContext} Propagation</h3> Redis commands can use a parent trace context to create a
 * {@link io.lettuce.core.tracing.Tracer.Span} to trace the actual command. A parent {@link brave.Span} is picked up for
 * imperative (synchronous/asynchronous) API usage from {@link brave.Tracing#currentTracer()}. The context is not propagated
 * across asynchronous call chains resulting from {@link java.util.concurrent.CompletionStage} chaining.
 * <p/>
 * Reactive API usage leverages Reactor's {@link reactor.util.context.Context} so that subscribers can register one of the
 * following objects (using their {@link Class} as context key):
 *
 * <ol>
 * <li>A {@link TraceContextProvider}</li>
 * <li>A Brave {@link Span}: Commands extract the {@link brave.propagation.TraceContext}</li>
 * <li>A Brave {@link brave.propagation.TraceContext}</li>
 * </ol>
 *
 * If one of the context objects above is found, it's used to determine the parent context for the command {@link Span}.
 *
 * @author Mark Paluch
 * @see brave.Tracer
 * @see brave.Tracing#currentTracer()
 * @see BraveTraceContextProvider
 * @since 5.1
 */
public class BraveTracing implements Tracing {

    private final BraveTracer tracer;

    /**
     * Create a new {@link BraveTracing} instance.
     *
     * @param tracer
     */
    private BraveTracing(BraveTracer tracer) {

        LettuceAssert.notNull(tracer, "Tracer must not be null");

        this.tracer = tracer;
    }

    /**
     * Create a new {@link BraveTracing} instance.
     *
     * @param tracing must not be {@literal null}.
     * @return the {@link BraveTracing}.
     */
    public static BraveTracing create(brave.Tracing tracing) {
        return new BraveTracing(new BraveTracer(tracing));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return () -> tracer;
    }

    @Override
    public TraceContextProvider initialTraceContextProvider() {
        return BraveTraceContextProvider.INSTANCE;
    }

    @Override
    public Endpoint createEndpoint(SocketAddress socketAddress) {

        zipkin2.Endpoint.Builder builder = zipkin2.Endpoint.newBuilder();

        if (socketAddress instanceof InetSocketAddress) {

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            return new BraveEndpoint(builder.serviceName("redis").ip(inetSocketAddress.getAddress())
                    .port(inetSocketAddress.getPort()).build());
        }

        return new BraveEndpoint(builder.serviceName("redis").build());
    }

    /**
     * Brave-specific implementation of {@link Tracer}.
     */
    static class BraveTracer extends Tracer {

        private final brave.Tracing tracing;

        public BraveTracer(brave.Tracing tracing) {
            this.tracing = tracing;
        }

        @Override
        public Span nextSpan() {
            return postProcessSpan(tracing.tracer().nextSpan());
        }

        @Override
        public Span nextSpan(TraceContext traceContext) {

            if (!(traceContext instanceof BraveTraceContext)) {
                return nextSpan();
            }

            BraveTraceContext braveTraceContext = BraveTraceContext.class.cast(traceContext);

            if (braveTraceContext.traceContext == null) {
                return nextSpan();
            }

            return postProcessSpan(tracing.tracer()
                    .nextSpan(TraceContextOrSamplingFlags.create(braveTraceContext.traceContext)));
        }

        private Span postProcessSpan(brave.Span span) {

            if (span == null || span.isNoop()) {
                return NoOpTracing.NoOpSpan.INSTANCE;
            }

            return new BraveSpan(span.kind(brave.Span.Kind.CLIENT));
        }
    }

    /**
     * Brave-specific {@link io.lettuce.core.tracing.Tracer.Span}.
     */
    static class BraveSpan extends Tracer.Span {

        private final brave.Span span;

        BraveSpan(brave.Span span) {
            this.span = span;
        }

        @Override
        public BraveSpan start() {

            span.start();

            return this;
        }

        @Override
        public BraveSpan name(String name) {

            span.name(name);

            return this;
        }

        @Override
        public BraveSpan annotate(String value) {

            span.annotate(value);

            return this;
        }

        @Override
        public BraveSpan tag(String key, String value) {

            span.tag(key, value);

            return this;
        }

        @Override
        public BraveSpan error(Throwable throwable) {

            span.error(throwable);

            return this;
        }

        @Override
        public BraveSpan remoteEndpoint(Endpoint endpoint) {

            span.remoteEndpoint(BraveEndpoint.class.cast(endpoint).endpoint);

            return this;
        }

        @Override
        public void finish() {
            span.finish();
        }

        public brave.Span getSpan() {
            return span;
        }
    }

    /**
     * {@link Endpoint} implementation for Zipkin's {@link zipkin2.Endpoint}.
     */
    public static class BraveEndpoint implements Endpoint {

        final zipkin2.Endpoint endpoint;

        public BraveEndpoint(zipkin2.Endpoint endpoint) {
            this.endpoint = endpoint;
        }
    }

    /**
     * {@link TraceContext} implementation for Brave's {@link brave.propagation.TraceContext}.
     */
    public static class BraveTraceContext implements TraceContext {

        final brave.propagation.TraceContext traceContext;

        private BraveTraceContext(brave.propagation.TraceContext traceContext) {
            this.traceContext = traceContext;
        }

        public static BraveTraceContext create(brave.propagation.TraceContext traceContext) {
            return new BraveTraceContext(traceContext);
        }
    }

    enum BraveTraceContextProvider implements TraceContextProvider {
        INSTANCE;

        @Override
        public TraceContext getTraceContext() {

            brave.Tracer tracer = brave.Tracing.currentTracer();

            if (tracer != null) {

                Span span = tracer.currentSpan();

                if (span != null) {
                    return new BraveTraceContext(span.context());
                }
            }
            return null;
        }

        @Override
        public Mono<TraceContext> getTraceContextLater() {

            return Mono.subscriberContext()
                    .filter(it -> it.hasKey(Span.class) || it.hasKey(brave.propagation.TraceContext.class)).map(it -> {

                        if (it.hasKey(Span.class)) {
                            return new BraveTraceContext(it.get(Span.class).context());
                        }

                        return new BraveTraceContext(it.get(brave.propagation.TraceContext.class));
                    });
        }
    }
}
