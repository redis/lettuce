package io.lettuce.core.cluster;

/**
 * Exception thrown when an unknown partition is requested.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@SuppressWarnings("serial")
public class UnknownPartitionException extends PartitionException {

    /**
     * Create a {@code UnknownPartitionException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public UnknownPartitionException(String msg) {
        super(msg);
    }

}
