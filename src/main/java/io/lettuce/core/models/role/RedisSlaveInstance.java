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

import io.lettuce.core.internal.LettuceAssert;

/**
 * Redis replica instance.
 *
 * @author Mark Paluch
 * @since 3.0
 * @deprecated since 6.0, in favor or {@link RedisReplicaInstance}.
 */
@SuppressWarnings("serial")
@Deprecated
public class RedisSlaveInstance extends RedisReplicaInstance implements RedisInstance, Serializable {

    public RedisSlaveInstance() {
    }

    /**
     * Constructs a {@link RedisSlaveInstance}
     *
     * @param master master for the replication, must not be {@code null}
     * @param state replica state, must not be {@code null}
     */
    RedisSlaveInstance(ReplicationPartner master, State state) {
        super(master, state);
    }

    /**
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#SLAVE}
     */
    @Override
    public Role getRole() {
        return Role.SLAVE;
    }

    /**
     *
     * @return the replication master.
     */
    public ReplicationPartner getMaster() {
        return getUpstream();
    }

    public void setMaster(ReplicationPartner master) {
        LettuceAssert.notNull(master, "Master must not be null");
        setUpstream(master);
    }

}
