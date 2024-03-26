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
