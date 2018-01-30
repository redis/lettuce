/*
 * Copyright 2011-2018 the original author or authors.
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
    private List<ReplicationPartner> slaves = Collections.emptyList();

    public RedisMasterInstance() {
    }

    /**
     * Constructs a {@link RedisMasterInstance}
     *
     * @param replicationOffset the replication offset
     * @param slaves list of slaves, must not be {@literal null} but may be empty
     */
    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> slaves) {
        LettuceAssert.notNull(slaves, "Slaves must not be null");
        this.replicationOffset = replicationOffset;
        this.slaves = slaves;
    }

    /**
     *
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#MASTER}
     */
    @Override
    public Role getRole() {
        return Role.MASTER;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    public List<ReplicationPartner> getSlaves() {
        return slaves;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    public void setSlaves(List<ReplicationPartner> slaves) {
        LettuceAssert.notNull(slaves, "Slaves must not be null");
        this.slaves = slaves;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [replicationOffset=").append(replicationOffset);
        sb.append(", slaves=").append(slaves);
        sb.append(']');
        return sb.toString();
    }
}
