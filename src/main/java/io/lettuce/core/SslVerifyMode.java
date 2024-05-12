package io.lettuce.core;

/**
 * Enumeration of SSL/TLS verification modes.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public enum SslVerifyMode {

    /**
     * No verification at all.
     */
    NONE,

    /**
     * Verify the CA and certificate without verifying that the hostname matches.
     */
    CA,

    /**
     * Full certificate verification.
     */
    FULL;
}
