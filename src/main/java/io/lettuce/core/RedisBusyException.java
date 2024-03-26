package io.lettuce.core;

/**
 * Exception that gets thrown when Redis is busy executing a Lua script with a {@code BUSY} error response.
 *
 * @author Mark Paluch
 * @since 4.5
 */
@SuppressWarnings("serial")
public class RedisBusyException extends RedisCommandExecutionException {

    /**
     * Create a {@code RedisBusyException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisBusyException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisNoScriptException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisBusyException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
