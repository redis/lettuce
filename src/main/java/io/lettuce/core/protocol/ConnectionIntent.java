package io.lettuce.core.protocol;

/**
 * Enumeration of intentions for how a connection is going to be used.
 *
 * @author Mark Paluch
 * @since 6.2
 */
public enum ConnectionIntent {

    /**
     * Intention to use read-only operations through a connection.
     */
    READ,

    /**
     * Intention to run read or write commands through a connection.
     */
    WRITE;
}
