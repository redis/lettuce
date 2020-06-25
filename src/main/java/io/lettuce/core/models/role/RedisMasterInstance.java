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
package io.lettuce.core.models.role;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Represents a master instance.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisMasterInstance implements RedisInstance, Serializable {

    private long replicationOffset;

    private List<ReplicationPartner> replicas = Collections.emptyList();

    public RedisMasterInstance() {
    }

    /**
     * Constructs a {@link RedisMasterInstance}
     *
     * @param replicationOffset the replication offset
     * @param replicas list of replicas, must not be {@code null} but may be empty
     */
    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> replicas) {
        LettuceAssert.notNull(replicas, "Replicas must not be null");
        this.replicationOffset = replicationOffset;
        this.replicas = replicas;
    }

    /**
     *
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#MASTER}.
     */
    @Override
    public Role getRole() {
        return Role.MASTER;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    @Deprecated
    public List<ReplicationPartner> getSlaves() {
        return getReplicas();
    }

    public List<ReplicationPartner> getReplicas() {
        return replicas;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    @Deprecated
    public void setSlaves(List<ReplicationPartner> replicas) {
        setReplicas(replicas);
    }

    public void setReplicas(List<ReplicationPartner> replicas) {
        LettuceAssert.notNull(replicas, "Replicas must not be null");
        this.replicas = replicas;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [replicationOffset=").append(replicationOffset);
        sb.append(", replicas=").append(replicas);
        sb.append(']');
        return sb.toString();
    }

}
