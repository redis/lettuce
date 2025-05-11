package io.lettuce.core.resource;

import io.netty.channel.socket.SocketChannel;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.dns.DefaultDnsCache;
import io.netty.resolver.dns.DefaultDnsCnameCache;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides {@link AddressResolverGroup} classes. This is to protect the user from {@link ClassNotFoundException}'s
 * caused by the absence of the {@literal netty-dns-resolver} library during runtime. This class will be deleted when
 * {@literal netty-dns-resolver} becomes mandatory. Internal API.
 * <p>
 * Since Lettuce 6.6, {@literal netty-dns-resolver} became a required dependency so this class now unconditionally uses
 * {@link DnsAddressResolverGroup}
 *
 * @author Yohei Ueki
 * @author Mark Paluch
 * @author Euiyoung Nam
 * @since 6.1
 */
class AddressResolverGroupProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AddressResolverGroupProvider.class);

    private static final AddressResolverGroup<?> ADDRESS_RESOLVER_GROUP = DefaultDnsAddressResolverGroupWrapper.INSTANCE;

    /**
     * Returns the {@link AddressResolverGroup} for DNS resolution.
     *
     * @return the {@link DnsAddressResolverGroup} if {@literal netty-dns-resolver} is available, otherwise return
     *         {@link DefaultAddressResolverGroup#INSTANCE}.
     */
    static AddressResolverGroup<?> addressResolverGroup() {
        return ADDRESS_RESOLVER_GROUP;
    }

    // Wraps DnsAddressResolverGroup to avoid NoClassDefFoundError.
    private static class DefaultDnsAddressResolverGroupWrapper {

        static AddressResolverGroup<?> INSTANCE = new DnsAddressResolverGroup(
                new DnsNameResolverBuilder().channelType(Transports.datagramChannelClass())
                        .socketChannelType(Transports.socketChannelClass().asSubclass(SocketChannel.class))
                        .cnameCache(new DefaultDnsCnameCache()).resolveCache(new DefaultDnsCache()));

    }

}
