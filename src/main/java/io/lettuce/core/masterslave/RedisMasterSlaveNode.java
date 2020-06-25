/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.masterslave;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * A node within a Redis Master-Slave setup.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
class RedisMasterSlaveNode implements RedisNodeDescription {

    private final RedisURI redisURI;

    private final Role role;

    RedisMasterSlaveNode(String host, int port, RedisURI seed, Role role) {

        RedisURI.Builder builder = RedisURI.Builder.redis(host, port).withSsl(seed.isSsl()).withVerifyPeer(seed.isVerifyPeer())
                .withStartTls(seed.isStartTls());
        if (seed.getPassword() != null && seed.getPassword().length != 0) {
            builder.withPassword(new String(seed.getPassword()));
        }

        if (seed.getClientName() != null) {
            builder.withClientName(seed.getClientName());
        }

        builder.withDatabase(seed.getDatabase());

        this.redisURI = builder.build();
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
        if (!(o instanceof RedisMasterSlaveNode))
            return false;

        RedisMasterSlaveNode that = (RedisMasterSlaveNode) o;

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
