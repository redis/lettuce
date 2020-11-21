package io.lettuce.core.resource;

import java.util.function.Supplier;

import io.lettuce.core.Transports;
import io.netty.channel.socket.SocketChannel;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides {@link AddressResolverGroup} classes. This is to protect the user from {@link ClassNotFoundException}'s
 * caused by the absence of the {@literal netty-dns-resolver} library during runtime. This class will be deleted when
 * {@literal netty-dns-resolver} becomes mandatory. Internal API.
 *
 * @author Yohei Ueki
 * @since xxx
 */
class AddressResolverGroupProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AddressResolverGroupProvider.class);

    private static final AddressResolverGroup<?> ADDRESS_RESOLVER_GROUP;

    static {
        boolean dnsResolverAvailable;
        try {
            Class.forName("io.netty.resolver.dns.DnsAddressResolverGroup");
            dnsResolverAvailable = true;
        } catch (ClassNotFoundException e) {
            dnsResolverAvailable = false;
        }

        // create addressResolverGroup instance via Supplier to avoid NoClassDefFoundError.
        Supplier<AddressResolverGroup<?>> supplier;
        if (dnsResolverAvailable) {
            logger.debug("Starting with netty's non-blocking DNS resolver library");
            supplier = AddressResolverGroupProvider::defaultDnsAddressResolverGroup;
        } else {
            logger.debug("Starting without optional netty's non-blocking DNS resolver library");
            supplier = () -> DefaultAddressResolverGroup.INSTANCE;
        }
        ADDRESS_RESOLVER_GROUP = supplier.get();
    }

    /**
     * Returns the {@link AddressResolverGroup} for dns resolution.
     *
     * @return the {@link DnsAddressResolverGroup} if {@literal netty-dns-resolver} is available, otherwise return
     *         {@link DefaultAddressResolverGroup#INSTANCE}.
     * @since xxx
     */
    static AddressResolverGroup<?> addressResolverGroup() {
        return ADDRESS_RESOLVER_GROUP;
    }

    private static DnsAddressResolverGroup defaultDnsAddressResolverGroup() {
        return new DnsAddressResolverGroup(new DnsNameResolverBuilder().channelType(Transports.datagramChannelClass())
                .socketChannelType(Transports.socketChannelClass().asSubclass(SocketChannel.class)));
    }

}
