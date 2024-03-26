package io.lettuce.core;

/**
 * Exception thrown when the command waiting timeout is exceeded.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisCommandTimeoutException extends RedisException {

    /**
     * Create a {@code RedisCommandTimeoutException} with a default message.
     */
    public RedisCommandTimeoutException() {
        super("Command timed out");
    }

    /**
     * Create a {@code RedisCommandTimeoutException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisCommandTimeoutException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisException} with the specified nested exception.
     *
     * @param cause the nested exception.
     */
    public RedisCommandTimeoutException(Throwable cause) {
        super(cause);
    }

}
