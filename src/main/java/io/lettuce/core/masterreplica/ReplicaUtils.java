/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterreplica;

import java.util.Collection;
import java.util.Comparator;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * @author Mark Paluch
 */
class ReplicaUtils {

    /**
     * Check if properties changed.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return {@code true} if {@code MASTER} or {@code SLAVE} flags changed or the URIs are changed.
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
     * @return the {@link RedisNodeDescription} or {@code null}
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
     * @return the {@link RedisNodeDescription} or {@code null}
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
     * @return {@code true} if {@code MASTER} or {@code SLAVE} flags changed or the URI changed.
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

    /**
     * Compare {@link RedisURI} based on their host and port representation.
     */
    enum RedisURIComparator implements Comparator<RedisURI> {

        INSTANCE;

        @Override
        public int compare(RedisURI o1, RedisURI o2) {
            String h1 = "";
            String h2 = "";

            if (o1 != null) {
                h1 = o1.getHost() + ":" + o1.getPort();
            }

            if (o2 != null) {
                h2 = o2.getHost() + ":" + o2.getPort();
            }

            return h1.compareToIgnoreCase(h2);
        }

    }

}
