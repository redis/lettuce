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
 */
@SuppressWarnings("serial")
public class RedisSlaveInstance implements RedisInstance, Serializable {

    private ReplicationPartner master;

    private State state;

    public RedisSlaveInstance() {
    }

    /**
     * Constructs a {@link RedisSlaveInstance}
     *
     * @param master master for the replication, must not be {@code null}
     * @param state replica state, must not be {@code null}
     */
    RedisSlaveInstance(ReplicationPartner master, State state) {
        LettuceAssert.notNull(master, "Master must not be null");
        LettuceAssert.notNull(state, "State must not be null");
        this.master = master;
        this.state = state;
    }

    /**
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#SLAVE}.
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
        return master;
    }

    /**
     *
     * @return Slave state.
     */
    public State getState() {
        return state;
    }

    public void setMaster(ReplicationPartner master) {
        LettuceAssert.notNull(master, "Master must not be null");
        this.master = master;
    }

    public void setState(State state) {
        LettuceAssert.notNull(state, "State must not be null");
        this.state = state;
    }

    /**
     * State of the Replica.
     */
    public enum State {
        /**
         * the instance needs to connect to its master.
         */
        CONNECT,

        /**
         * the replica-master connection is in progress.
         */
        CONNECTING,

        /**
         * the master and replica are trying to perform the synchronization.
         */
        SYNC,

        /**
         * the replica is online.
         */
        CONNECTED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [master=").append(master);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }

}
