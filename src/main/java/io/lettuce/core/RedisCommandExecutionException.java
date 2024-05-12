package io.lettuce.core;

/**
 * Exception for errors states reported by Redis.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisCommandExecutionException extends RedisException {

    /**
     * Create a {@code RedisCommandExecutionException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisCommandExecutionException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisCommandExecutionException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisCommandExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a {@code RedisCommandExecutionException} with the specified nested exception.
     *
     * @param cause the nested exception.
     */
    public RedisCommandExecutionException(Throwable cause) {
        super(cause);
    }

}
