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
package io.lettuce.core.protocol;

import io.lettuce.core.tracing.TraceContext;
import io.lettuce.core.tracing.TraceContextProvider;
import io.lettuce.core.tracing.Tracer;
import io.netty.buffer.ByteBuf;

/**
 * Redis command that is aware of an associated {@link TraceContext}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class TracedCommand<K, V, T> extends CommandWrapper<K, V, T> implements TraceContextProvider {

    private final TraceContext traceContext;

    private Tracer.Span span;

    public TracedCommand(RedisCommand<K, V, T> command, TraceContext traceContext) {
        super(command);
        this.traceContext = traceContext;
    }

    @Override
    public TraceContext getTraceContext() {
        return traceContext;
    }

    public Tracer.Span getSpan() {
        return span;
    }

    public void setSpan(Tracer.Span span) {
        this.span = span;
    }

    @Override
    public void encode(ByteBuf buf) {

        if (span != null) {
            span.annotate("redis.encode.start");
        }
        super.encode(buf);

        if (span != null) {
            span.annotate("redis.encode.end");
        }
    }

}
