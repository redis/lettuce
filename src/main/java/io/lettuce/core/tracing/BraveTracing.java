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
import java.util.function.Consumer;

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
 * @see #builder()
 * @since 5.1
 */
public class BraveTracing implements Tracing {

    private final BraveTracer tracer;
    private final BraveTracingOptions tracingOptions;
    private final TracingTagsCustomizer tracingTagsCustomizer;

    /**
     * Create a new {@link BraveTracing} instance.
     *
     * @param builder the {@link BraveTracing.Builder}.
     */
    private BraveTracing(Builder builder) {

        LettuceAssert.notNull(builder.tracing, "Tracing must not be null");
        LettuceAssert.notNull(builder.serviceName, "Service name must not be null");

        this.tracingOptions = new BraveTracingOptions(builder.serviceName, builder.endpointCustomizer, builder.spanCustomizer);
        this.tracer = new BraveTracer(builder.tracing, this.tracingOptions);
        this.tracingTagsCustomizer = builder.tracingTagsCustomizer;
    }

    /**
     * Create a new {@link BraveTracing} instance.
     *
     * @param tracing must not be {@literal null}.
     * @return the {@link BraveTracing}.
     */
    public static BraveTracing create(brave.Tracing tracing) {
        return builder().tracing(tracing).build();
    }

    /**
     * Create a new {@link Builder} to build {@link BraveTracing}.
     *
     * @return a new instance of {@link Builder}.
     * @since 5.2
     */
    public static BraveTracing.Builder builder() {
        return new BraveTracing.Builder();
    }

    /**
     * Builder for {@link BraveTracing}.
     *
     * @since 5.2
     */
    public static class Builder {

        private brave.Tracing tracing;
        private String serviceName = "redis";
        private Consumer<zipkin2.Endpoint.Builder> endpointCustomizer = it -> {
        };
        private Consumer<brave.Span> spanCustomizer = it -> {
        };
        private TracingTagsCustomizer tracingTagsCustomizer = new DefaultTracingTagsCustomizer();

        private Builder() {
        }

        /**
         * Sets the {@link Tracing}.
         *
         * @param tracing the Brave {@link brave.Tracing} object, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder tracing(brave.Tracing tracing) {

            LettuceAssert.notNull(tracing, "Tracing must not be null!");

            this.tracing = tracing;
            return this;
        }

        /**
         * Sets the name used in the {@link zipkin2.Endpoint}.
         *
         * @param serviceName the name for the {@link zipkin2.Endpoint}, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder serviceName(String serviceName) {

            LettuceAssert.notEmpty(serviceName, "Service name must not be null!");

            this.serviceName = serviceName;
            return this;
        }

        /**
         * Sets a {@link TracingTagsCustomizer} to customize how tracing tags are reported.
         * When not set it uses the {@link DefaultTracingTagsCustomizer}.
         *
         * @param tracingTagsCustomizer must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder tracingTagsCustomizer(TracingTagsCustomizer tracingTagsCustomizer) {

            LettuceAssert.notNull(tracingTagsCustomizer, "Tracing tags customizer must not be null!");

            this.tracingTagsCustomizer = tracingTagsCustomizer;
            return this;
        }

        /**
         * Sets an {@link zipkin2.Endpoint} customizer to customize the {@link zipkin2.Endpoint} through its
         * {@link zipkin2.Endpoint.Builder}. The customizer is invoked before {@link zipkin2.Endpoint.Builder#build() building}
         * the endpoint.
         *
         * @param endpointCustomizer must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder endpointCustomizer(Consumer<zipkin2.Endpoint.Builder> endpointCustomizer) {

            LettuceAssert.notNull(endpointCustomizer, "Endpoint customizer must not be null!");

            this.endpointCustomizer = endpointCustomizer;
            return this;
        }

        /**
         * Sets an {@link brave.Span} customizer to customize the {@link brave.Span}. The customizer is invoked before
         * {@link Span#finish()} finishing} the span.
         *
         * @param spanCustomizer must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder spanCustomizer(Consumer<brave.Span> spanCustomizer) {

            LettuceAssert.notNull(spanCustomizer, "Span customizer must not be null!");

            this.spanCustomizer = spanCustomizer;
            return this;
        }

        /**
         * @return a new instance of {@link BraveTracing}
         */
        public BraveTracing build() {

            LettuceAssert.notNull(this.tracing, "Brave Tracing must not be null!");

            return new BraveTracing(this);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public TracingTagsCustomizer getTracingTagsCustomizer() {
        return tracingTagsCustomizer;
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

        zipkin2.Endpoint.Builder builder = zipkin2.Endpoint.newBuilder().serviceName(tracingOptions.serviceName);

        if (socketAddress instanceof InetSocketAddress) {

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            builder.ip(inetSocketAddress.getAddress()).port(inetSocketAddress.getPort());

            tracingOptions.customizeEndpoint(builder);

            return new BraveEndpoint(builder.build());
        }

        tracingOptions.customizeEndpoint(builder);
        return new BraveEndpoint(builder.build());
    }

    /**
     * Brave-specific implementation of {@link Tracer}.
     */
    static class BraveTracer extends Tracer {

        private final brave.Tracing tracing;
        private final BraveTracingOptions tracingOptions;

        BraveTracer(brave.Tracing tracing, BraveTracingOptions tracingOptions) {
            this.tracing = tracing;
            this.tracingOptions = tracingOptions;
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

            return new BraveSpan(span.kind(brave.Span.Kind.CLIENT), this.tracingOptions);
        }
    }

    /**
     * Brave-specific {@link io.lettuce.core.tracing.Tracer.Span}.
     */
    static class BraveSpan extends Tracer.Span {

        private final brave.Span span;
        private final BraveTracingOptions tracingOptions;

        BraveSpan(Span span, BraveTracingOptions tracingOptions) {
            this.span = span;
            this.tracingOptions = tracingOptions;
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

            this.tracingOptions.customizeSpan(span);
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

    /**
     * Value object encapsulating tracing options.
     *
     * @author Mark Paluch
     * @since 5.2
     */
    static class BraveTracingOptions {

        private final String serviceName;
        private final Consumer<zipkin2.Endpoint.Builder> endpointCustomizer;
        private final Consumer<brave.Span> spanCustomizer;

        BraveTracingOptions(String serviceName, Consumer<zipkin2.Endpoint.Builder> endpointCustomizer,
                Consumer<Span> spanCustomizer) {
            this.serviceName = serviceName;
            this.endpointCustomizer = endpointCustomizer;
            this.spanCustomizer = spanCustomizer;
        }

        void customizeEndpoint(zipkin2.Endpoint.Builder builder) {
            this.endpointCustomizer.accept(builder);
        }

        void customizeSpan(brave.Span span) {
            this.spanCustomizer.accept(span);
        }
    }
}
