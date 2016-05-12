package com.lambdaworks.redis.resource;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Users may implement this interface to override the normal DNS lookup offered by the OS.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public interface DnsResolver {

    /**
     * Returns the IP address for the specified host name.
     * 
     * @param host the hostname, must not be empty or {@literal null}.
     * @return array of one or more {@link InetAddress adresses}
     * @throws UnknownHostException if the given host is not recognized or the associated IP address cannot be used to build an
     *         {@link InetAddress} instance
     */
    InetAddress[] resolve(String host) throws UnknownHostException;
}
