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

/**
 * No-Op {@link Tracing} support that does not trace at all.
 *
 * @author Mark Paluch
 * @author Daniel Albuquerque
 * @since 5.1
 */
enum NoOpTracing implements Tracing, TraceContextProvider, TracerProvider {

    INSTANCE;

    private final Endpoint NOOP_ENDPOINT = new Endpoint() {
    };

    @Override
    public TraceContext getTraceContext() {
        return TraceContext.EMPTY;
    }

    @Override
    public Tracer getTracer() {
        return NoOpTracer.INSTANCE;
    }

    @Override
    public TracerProvider getTracerProvider() {
        return this;
    }

    @Override
    public TraceContextProvider initialTraceContextProvider() {
        return this;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean includeCommandArgsInSpanTags() {
        return false;
    }

    @Override
    public Endpoint createEndpoint(SocketAddress socketAddress) {
        return NOOP_ENDPOINT;
    }

    static class NoOpTracer extends Tracer {

        static final Tracer INSTANCE = new NoOpTracer();

        @Override
        public Span nextSpan(TraceContext traceContext) {
            return NoOpSpan.INSTANCE;
        }

        @Override
        public Span nextSpan() {
            return NoOpSpan.INSTANCE;
        }

    }

    public static class NoOpSpan extends Tracer.Span {

        static final NoOpSpan INSTANCE = new NoOpSpan();

        @Override
        public Tracer.Span start() {
            return this;
        }

        @Override
        public Tracer.Span name(String name) {
            return this;
        }

        @Override
        public Tracer.Span annotate(String value) {
            return this;
        }

        @Override
        public Tracer.Span tag(String key, String value) {
            return this;
        }

        @Override
        public Tracer.Span error(Throwable throwable) {
            return this;
        }

        @Override
        public Tracer.Span remoteEndpoint(Tracing.Endpoint endpoint) {
            return this;
        }

        @Override
        public void finish() {
        }

    }

}
