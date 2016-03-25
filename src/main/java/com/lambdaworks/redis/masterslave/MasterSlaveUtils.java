package com.lambdaworks.redis.masterslave;

import java.util.Collection;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * @author Mark Paluch
 */
class MasterSlaveUtils {
    static final Utf8StringCodec CODEC = new Utf8StringCodec();

    /**
     * Check if properties changed.
     * 
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@literal true} if {@code MASTER} or {@code SLAVE} flags changed or the URIs are changed.
     */
    static boolean isChanged(Collection<RedisNodeDescription> o1, Collection<RedisNodeDescription> o2) {

        if (o1.size() != o2.size()) {
            return true;
        }

        for (RedisNodeDescription base : o2) {
            if (!essentiallyEqualsTo(base, findNodeByUri(o1, base.getUri()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Lookup a {@link RedisNodeDescription} by {@link RedisURI}.
     * 
     * @param nodes
     * @param lookupUri
     * @return the {@link RedisNodeDescription} or {@literal null}
     */
    static RedisNodeDescription findNodeByUri(Collection<RedisNodeDescription> nodes, RedisURI lookupUri) {
        return findNodeByHostAndPort(nodes, lookupUri.getHost(), lookupUri.getPort());
    }

    /**
     * Lookup a {@link RedisNodeDescription} by {@code host} and {@code port}.
     * 
     * @param nodes
     * @param host
     * @param port
     * @return the {@link RedisNodeDescription} or {@literal null}
     */
    static RedisNodeDescription findNodeByHostAndPort(Collection<RedisNodeDescription> nodes, String host, int port) {
        for (RedisNodeDescription node : nodes) {
            RedisURI nodeUri = node.getUri();
            if (nodeUri.getHost().equals(host) && nodeUri.getPort() == port) {
                return node;
            }
        }
        return null;
    }

    /**
     * Check for {@code MASTER} or {@code SLAVE} roles and the URI.
     * 
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@literal true} if {@code MASTER} or {@code SLAVE} flags changed or the URI changed.
     */
    static boolean essentiallyEqualsTo(RedisNodeDescription o1, RedisNodeDescription o2) {

        if (o2 == null) {
            return false;
        }

        if (o1.getRole() != o2.getRole()) {
            return false;
        }

        if (!o1.getUri().equals(o2.getUri())) {
            return false;
        }

        return true;
    }

}
