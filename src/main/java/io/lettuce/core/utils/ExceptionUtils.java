package io.lettuce.core.utils;

import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.util.internal.logging.InternalLogger;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class ExceptionUtils {

    private static final Set<String> SUPPRESS_IO_EXCEPTION_MESSAGES = new HashSet<>(
            Arrays.asList("Connection reset by peer", "Broken pipe", "Connection timed out"));

    private ExceptionUtils() {
    }

    public static void maybeLogSendError(InternalLogger logger, Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            return;
        }

        if (cause instanceof IOException && (SUPPRESS_IO_EXCEPTION_MESSAGES.contains(cause.getMessage())
                || cause instanceof ChannelOutputShutdownException)) {
            logger.debug("[maybeLogSendError] error during request: {}", cause.getMessage(), cause);
        } else {
            logger.error("[maybeLogSendError][attention] unexpected exception during request: {}", cause.getMessage(), cause);
        }
    }

    public static <T extends Throwable> T castTo(Throwable throwable, Class<T> clazz, Function<Throwable, T> supplier) {
        if (clazz.isInstance(throwable)) {
            return clazz.cast(throwable);
        }
        return supplier.apply(throwable);
    }

    public static <T extends Throwable> T clearStackTrace(T throwable) {
        throwable.setStackTrace(new StackTraceElement[0]);
        return throwable;
    }

    /**
     * Returns whether the throwable is one of the exception types or one of the cause in the cause chain is one of the
     * exception types
     *
     * @param throwable exception to check
     * @param exceptionTypes target exception types.
     * @return whether the throwable is one of the exception types or one of the cause in the cause chain is one of the
     *         exception types
     */
    public static boolean oneOf(final Throwable throwable, final Collection<Class<? extends Throwable>> exceptionTypes) {
        Throwable cause = throwable;
        do {
            for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                if (exceptionType.isInstance(cause)) {
                    return true;
                }
            }
            cause = cause.getCause();
        } while (cause != null);
        return false;
    }

    public static void maybeFire(InternalLogger logger, boolean canFire, String msg) {
        final IllegalStateException ex = new IllegalStateException(msg);
        logger.error("[unexpected] {}", msg, ex);
        if (canFire) {
            throw ex;
        }
    }

    public static void logUnexpectedDone(InternalLogger logger, String logPrefix, RedisCommand<?, ?, ?> cmd) {
        if (cmd.isCancelled()) {
            logger.warn("[logUnexpectedDone][{}] command is cancelled: {}", logPrefix, cmd);
            return;
        }

        final CommandOutput<?, ?, ?> output = cmd.getOutput();
        final String err = output.getError();
        if (err != null) {
            logger.warn("[logUnexpectedDone][{}] command completes with err, cmd: [{}], err: [{}]", logPrefix, cmd, err);
            return;
        }

        logger.warn("[logUnexpectedDone][{}] command completes normally, cmd: [{}], value: [{}]", logPrefix, cmd, output.get());
    }

}
