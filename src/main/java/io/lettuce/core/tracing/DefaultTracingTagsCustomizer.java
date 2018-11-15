package io.lettuce.core.tracing;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Default implementation of a {@link TracingTagsCustomizer}.
 *
 * It associates tags containing information about {@link CommandArgs}, errors or exceptions with a running {@link Tracer.Span}.
 *
 * @since 5.2
 */
public class DefaultTracingTagsCustomizer implements TracingTagsCustomizer {

    @Override
    public void handleCommandArgs(Tracer.Span span, ProtocolKeyword type, CommandArgs<?, ?> args) {
        span.tag("redis.arg", args.toCommandString());
    }

    @Override
    public void handleError(Tracer.Span span, String error) {
        span.tag("error", error);
    }

    @Override
    public void handleException(Tracer.Span span, Throwable throwable) {
        span.tag("exception", throwable.toString());
        span.error(throwable);
    }
}
