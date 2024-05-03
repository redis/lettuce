package io.lettuce.core.cluster;

import io.lettuce.core.cluster.models.partitions.Partitions;

/**
 * Exception thrown when a partition selection fails (slot not covered, no read candidates available).
 *
 * @author Mark Paluch
 * @since 5.1
 */
@SuppressWarnings("serial")
public class PartitionSelectorException extends PartitionException {

    private final Partitions partitions;

    /**
     * Create a {@code UnknownPartitionException} with the specified detail message.
     *
     * @param msg the detail message.
     * @param partitions read-only view of the current topology view.
     */
    public PartitionSelectorException(String msg, Partitions partitions) {

        super(msg);
        this.partitions = partitions;
    }

    public Partitions getPartitions() {
        return partitions;
    }

}
