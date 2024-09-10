package io.lettuce.core;

/**
 * Exception that gets thrown when Redis indicates absence of a Lua script referenced by its SHA-256 digest with a
 * {@code NOSCRIPT} error response.
 *
 * @author Mark Paluch
 * @since 4.5
 */
@SuppressWarnings("serial")
public class RedisNoScriptException extends RedisCommandExecutionException {

    /**
     * Create a {@code RedisNoScriptException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisNoScriptException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisNoScriptException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisNoScriptException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
