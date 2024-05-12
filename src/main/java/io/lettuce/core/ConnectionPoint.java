package io.lettuce.core;

/**
 * Interface for a connection point described with a host and port or socket.
 *
 * @author Mark Paluch
 */
public interface ConnectionPoint {

    /**
     * Returns the host that should represent the hostname or IPv4/IPv6 literal.
     *
     * @return the hostname/IP address
     */
    String getHost();

    /**
     * Get the current port number.
     *
     * @return the port number
     */
    int getPort();

    /**
     * Get the socket path.
     *
     * @return path to a Unix Domain Socket
     */
    String getSocket();

}
