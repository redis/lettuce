package io.lettuce.core.resource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import io.netty.resolver.AddressResolverGroup;

/**
 * Users may implement this interface to override the normal DNS lookup offered by the OS.
 *
 * @author Mark Paluch
 * @author Euiyoung Nam
 * @since 4.2
 * @deprecated since 6.6 replaced by{@link AddressResolverGroup} instead.
 */
@Deprecated
public interface DnsResolver {

    /**
     * Java VM default resolver.
     *
     * @since 5.1
     */
    static DnsResolver jvmDefault() {
        return DnsResolvers.JVM_DEFAULT;
    }

    /**
     * Non-resolving {@link DnsResolver}. Returns an empty {@link InetAddress} to indicate an unresolved address.
     *
     * @since 5.1
     * @see java.net.InetSocketAddress#createUnresolved(String, int)
     */
    static DnsResolver unresolved() {
        return DnsResolvers.UNRESOLVED;
    }

    /**
     * Returns the IP address for the specified host name.
     *
     * @param host the hostname, must not be empty or {@code null}.
     * @return array of one or more {@link InetAddress adresses}. An empty array indicates that DNS resolution is not supported
     *         by this {@link DnsResolver} and should happen by netty, see
     *         {@link java.net.InetSocketAddress#createUnresolved(String, int)}.
     * @throws UnknownHostException if the given host is not recognized or the associated IP address cannot be used to build an
     *         {@link InetAddress} instance
     */
    InetAddress[] resolve(String host) throws UnknownHostException;

}
