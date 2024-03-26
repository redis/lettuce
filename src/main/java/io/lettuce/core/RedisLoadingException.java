package io.lettuce.core;

/**
 * Exception that gets thrown when Redis is loading a dataset into memory and replying with a {@code LOADING} error response.
 *
 * @author Mark Paluch
 * @since 4.5
 */
@SuppressWarnings("serial")
public class RedisLoadingException extends RedisCommandExecutionException {

    /**
     * Create a {@code RedisLoadingException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisLoadingException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisLoadingException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisLoadingException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
