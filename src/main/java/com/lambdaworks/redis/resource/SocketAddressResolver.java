package com.lambdaworks.redis.resource;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.lambdaworks.redis.RedisURI;

/**
 * Resolves a {@link com.lambdaworks.redis.RedisURI} to a {@link java.net.SocketAddress}.
 * 
 * @author Mark Paluch
 */
public class SocketAddressResolver {

    /**
     * Resolves a {@link com.lambdaworks.redis.RedisURI} to a {@link java.net.SocketAddress}.
     * 
     * @param redisURI must not be {@literal null}
     * @param dnsResolver must not be {@literal null}
     * @return the resolved {@link SocketAddress}
     */
    public static SocketAddress resolve(RedisURI redisURI, DnsResolver dnsResolver) {

        if (redisURI.getSocket() != null) {
            return redisURI.getResolvedAddress();
        }

        try {
            InetAddress inetAddress = dnsResolver.resolve(redisURI.getHost())[0];
            return new InetSocketAddress(inetAddress, redisURI.getPort());
        } catch (UnknownHostException e) {
            return redisURI.getResolvedAddress();
        }
    }
}
