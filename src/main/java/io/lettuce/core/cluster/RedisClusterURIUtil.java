package io.lettuce.core.cluster;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.HostAndPort;

/**
 * {@link RedisClusterURIUtil} is a collection of {@link RedisURI}-based utility methods for {@link RedisClusterClient} use.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public abstract class RedisClusterURIUtil {

    private RedisClusterURIUtil() {
    }

    /**
     * Parse a Redis Cluster URI with potentially multiple hosts into a {@link List} of {@link RedisURI}.
     *
     * An URI follows the syntax: {@code redis://[password@]host[:port][,host2[:port2]]}
     *
     * @param uri must not be empty or {@code null}.
     * @return {@link List} of {@link RedisURI}.
     */
    public static List<RedisURI> toRedisURIs(URI uri) {

        RedisURI redisURI = RedisURI.create(uri);

        String[] parts = redisURI.getHost().split("\\,");

        List<RedisURI> redisURIs = new ArrayList<>(parts.length);

        for (String part : parts) {
            HostAndPort hostAndPort = HostAndPort.parse(part);

            RedisURI nodeUri = RedisURI.create(hostAndPort.getHostText(),
                    hostAndPort.hasPort() ? hostAndPort.getPort() : redisURI.getPort());

            applyUriConnectionSettings(redisURI, nodeUri);

            redisURIs.add(nodeUri);
        }

        return redisURIs;
    }

    /**
     * Apply {@link RedisURI} settings such as SSL/Timeout/password.
     *
     * @param from from {@link RedisURI}.
     * @param to from {@link RedisURI}.
     */
    static void applyUriConnectionSettings(RedisURI from, RedisURI to) {

        to.applyAuthentication(from);
        to.applySsl(from);
        to.setTimeout(from.getTimeout());
    }

}
