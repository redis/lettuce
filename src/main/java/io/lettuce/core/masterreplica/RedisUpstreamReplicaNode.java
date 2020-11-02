/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterreplica;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * A node within a Redis Master-Replica setup.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
class RedisUpstreamReplicaNode implements RedisNodeDescription {

    private final RedisURI redisURI;

    private final Role role;

    RedisUpstreamReplicaNode(String host, int port, RedisURI seed, Role role) {

        this.redisURI = RedisURI.builder(seed).withHost(host).withPort(port).build();
        this.role = role;
    }

    @Override
    public RedisURI getUri() {
        return redisURI;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RedisUpstreamReplicaNode))
            return false;

        RedisUpstreamReplicaNode that = (RedisUpstreamReplicaNode) o;

        if (!redisURI.equals(that.redisURI))
            return false;
        return role == that.role;
    }

    @Override
    public int hashCode() {
        int result = redisURI.hashCode();
        result = 31 * result + role.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [redisURI=").append(redisURI);
        sb.append(", role=").append(role);
        sb.append(']');
        return sb.toString();
    }

}
