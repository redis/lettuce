package io.lettuce.core.resource;

import io.netty.resolver.AddressResolverGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Predefined DNS resolvers.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public enum DnsResolvers implements DnsResolver {

    /**
     * Java VM default resolver.
     *
     * @deprecated since 6.7. Configure {@link AddressResolverGroup} instead.
     */
    JVM_DEFAULT {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return InetAddress.getAllByName(host);
        }

    },

    /**
     * Non-resolving {@link DnsResolver}. Returns an empty {@link InetAddress} to indicate an unresolved address.
     *
     * @see java.net.InetSocketAddress#createUnresolved(String, int)
     * @since 4.4
     */
    UNRESOLVED {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            return new InetAddress[0];
        }

    };

}
