package io.lettuce.core.cluster;

import io.lettuce.core.RedisException;

/**
 * Partition access exception thrown when a partition-specific operations fails.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@SuppressWarnings("serial")
public class PartitionException extends RedisException {

    /**
     * Create a {@code PartitionException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public PartitionException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code PartitionException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public PartitionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a {@code PartitionException} with the specified nested exception.
     *
     * @param cause the nested exception.
     */
    public PartitionException(Throwable cause) {
        super(cause);
    }

}
