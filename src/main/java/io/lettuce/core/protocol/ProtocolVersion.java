package io.lettuce.core.protocol;

/**
 * Versions of the native protocol supported by the driver.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public enum ProtocolVersion {

    /**
     * Redis 2 to Redis 5.
     */
    RESP2,

    /**
     * Redis 6.
     */
    RESP3;

    /**
     * Returns the newest supported protocol version.
     *
     * @return the newest supported protocol version.
     */
    public static ProtocolVersion newestSupported() {
        return RESP3;
    }

}
