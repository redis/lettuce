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

import java.util.List;

/**
 * Represents a upstream (master) instance.
 *
 * @author Mark Paluch
 * @since 3.0
 * @deprecated since 6.0 in favor of {@link RedisUpstreamInstance}
 */
@SuppressWarnings("serial")
@Deprecated
public class RedisMasterInstance extends RedisUpstreamInstance {

    public RedisMasterInstance() {
    }

    /**
     * Constructs a {@link RedisMasterInstance}
     *
     * @param replicationOffset the replication offset
     * @param replicas list of replicas, must not be {@code null} but may be empty
     */
    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> replicas) {
        super(replicationOffset, replicas);
    }

    /**
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#MASTER}
     */
    @Override
    public Role getRole() {
        return Role.MASTER;
    }

}
