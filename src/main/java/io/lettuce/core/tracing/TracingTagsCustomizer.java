package io.lettuce.core.tracing;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Interface declaring methods to customize the tags that are associated with {@link Tracer.Span}s.
 *
 * Providing a custom implementation can be useful when there is a need to handle tags differently, for example when entries do not have a valid
 * {@link String} representation.
 *
 * @since 5.2
 * @see DefaultTracingTagsCustomizer
 */
public interface TracingTagsCustomizer {

    /**
     * Method to customize a {@link Tracer.Span} associated with a {@link io.lettuce.core.protocol.RedisCommand}.
     *
     * @param span the {@link Tracer.Span} to customize.
     * @param type the protocol for this redis command.
     * @param args the redis {@link CommandArgs}.
     *
     * @see io.lettuce.core.protocol.CommandHandler
     */
    void handleCommandArgs(Tracer.Span span, ProtocolKeyword type, CommandArgs<?, ?> args);

    /**
     * Method to customize a {@link Tracer.Span} when there was an error processing a {@link io.lettuce.core.protocol.RedisCommand}.
     *
     * @param span the {@link Tracer.Span} to customize.
     * @param error the error that occurred.
     * @see io.lettuce.core.protocol.CommandHandler
     */
    void handleError(Tracer.Span span, String error);

    /**
     * Method to customize a {@link Tracer.Span} when there was an exception processing a {@link io.lettuce.core.protocol.RedisCommand}.
     *
     * @param span the {@link Tracer.Span} to customize.
     * @param throwable the {@link Throwable} that was raised for this command.
     * @see io.lettuce.core.protocol.CommandHandler
     */
    void handleException(Tracer.Span span, Throwable throwable);
}
